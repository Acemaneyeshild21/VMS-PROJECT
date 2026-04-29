package pkg.vms.DAO;

import java.io.File;
import java.io.FileWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BonDAO {

    public static class BonInfo {
        public int    bonId;
        public String codeUnique;
        public double valeur;
        public String statut;
        public String dateEmission;
        public String dateExpiration;
        public int    demandeId;
        public String clientNom;
        public String clientEmail;
        public String reference;
        public String pdfPath;
        public byte[] pdfData;   // octets PDF stockés en base (BYTEA)
    }

    /**
     * Appelle la procédure stockée sp_generer_bons pour générer les bons d'une demande approuvée.
     * @return nombre de bons générés
     */
    public static int genererBons(int demandeId, int genereParUserId) throws SQLException {
        String sql = "SELECT sp_generer_bons(?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            ps.setInt(2, genereParUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Récupère tous les bons d'une demande donnée.
     */
    public static List<BonInfo> getBonsByDemande(int demandeId) throws SQLException {
        List<BonInfo> bons = new ArrayList<>();
        String sql = "SELECT b.bon_id, b.code_unique, b.valeur, b.statut, " +
                     "b.date_emission, b.date_expiration, b.pdf_path, " +
                     "d.reference, c.name, c.email " +
                     "FROM bon b " +
                     "JOIN demande d ON b.demande_id = d.demande_id " +
                     "LEFT JOIN client c ON d.clientid = c.clientid " +
                     "WHERE b.demande_id = ? ORDER BY b.bon_id";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BonInfo b = new BonInfo();
                    b.bonId = rs.getInt("bon_id");
                    b.codeUnique = rs.getString("code_unique");
                    b.valeur = rs.getDouble("valeur");
                    b.statut = rs.getString("statut");
                    b.dateEmission = rs.getString("date_emission");
                    b.dateExpiration = rs.getString("date_expiration");
                    b.pdfPath = rs.getString("pdf_path");
                    b.reference = rs.getString("reference");
                    b.clientNom = rs.getString("name");
                    b.clientEmail = rs.getString("email");
                    b.demandeId = demandeId;
                    bons.add(b);
                }
            }
        }
        return bons;
    }

    /**
     * Met à jour le chemin PDF d'un bon (stockage disque).
     */
    public static void updatePdfPath(int bonId, String pdfPath) throws SQLException {
        String sql = "UPDATE bon SET pdf_path = ? WHERE bon_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pdfPath);
            ps.setInt(2, bonId);
            ps.executeUpdate();
        }
    }

    /**
     * Stocke les octets bruts du PDF dans la colonne bon.pdf_data (BYTEA).
     */
    public static void updatePdfData(int bonId, byte[] pdfData) throws SQLException {
        String sql = "UPDATE bon SET pdf_data = ? WHERE bon_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBytes(1, pdfData);
            ps.setInt(2, bonId);
            ps.executeUpdate();
        }
    }

    /**
     * Enregistre une erreur d'envoi email pour une demande.
     * La colonne tentative est incrémentée automatiquement par rapport aux erreurs existantes.
     * Ne lève pas d'exception : l'échec de log ne doit pas masquer l'erreur d'origine.
     */
    public static void logEmailError(int demandeId, String erreur) {
        // Récupère demande_ref et email client pour respecter les contraintes NOT NULL de email_errors
        String sql = "INSERT INTO email_errors (demande_id, demande_ref, to_email, email_type, derniere_erreur) " +
                     "SELECT d.demande_id, d.reference, COALESCE(c.email, ''), 'VOUCHER', ? " +
                     "FROM demande d LEFT JOIN client c ON d.clientid = c.clientid " +
                     "WHERE d.demande_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, erreur != null ? erreur : "Erreur inconnue");
            ps.setInt(2, demandeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Impossible de logger l'erreur email : " + e.getMessage());
        }
    }

    /**
     * Marque toutes les erreurs email non résolues d'une demande comme résolues.
     */
    public static void resolveEmailErrors(int demandeId) throws SQLException {
        String sql = "UPDATE email_errors SET resolved = TRUE, resolved_at = NOW() WHERE demande_id = ? AND resolved = FALSE";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            ps.executeUpdate();
        }
    }

    /**
     * Appelle la procédure stockée sp_redimer_bon pour la rédemption sécurisée.
     * Gère les pertes de connexion, les timeouts de verrou, et parse les messages
     * du SP pour identifier le type d'erreur précis (EXPIRÉ, DÉJÀ UTILISÉ, etc.).
     */
    public static RedemptionResult redimerBon(String codeUnique, int magasinId, int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM sp_redimer_bon(?, ?, ?)";
        try {
            try (Connection conn = DBconnect.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                // Timeout de verrouillage pour éviter les blocages infinis en haute concurrence
                try (Statement st = conn.createStatement()) {
                    st.execute("SET lock_timeout = '5s'");
                }

                ps.setString(1, codeUnique);
                ps.setInt(2, magasinId);
                ps.setInt(3, utilisateurId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean succes = rs.getBoolean("succes");
                        String message  = rs.getString("message");
                        double  valeur  = rs.getDouble("bon_valeur");
                        RedemptionResult.ErrorType type = succes
                                ? RedemptionResult.ErrorType.SUCCESS
                                : parseErrorType(message);

                        if (succes) {
                            AuditDAO.logSimple("bon", -1, "UTILISATION_BON", utilisateurId,
                                    "Code: " + codeUnique + ", Magasin: " + magasinId);
                        } else {
                            // Tracer toute tentative de rédemption refusée
                            AuditDAO.logSimple("bon", -1, "REDEMPTION_ECHOUEE", utilisateurId,
                                    "Refus [" + type + "] : " + message
                                    + " | Code: " + codeUnique + " | Magasin: " + magasinId);
                        }
                        return new RedemptionResult(succes, message, valeur, type);
                    }
                }
            }
        } catch (SQLException e) {
            if ("55P03".equals(e.getSQLState())) {
                AuditDAO.logSimple("bon", -1, "REDEMPTION_ECHOUEE", utilisateurId,
                        "Timeout verrou [LOCK_TIMEOUT] | Code: " + codeUnique + " | Magasin: " + magasinId);
                return new RedemptionResult(false,
                        "Le bon est en cours de traitement ailleurs. Réessayez.",
                        0, RedemptionResult.ErrorType.LOCK_TIMEOUT);
            }
            if (isConnectionError(e)) {
                logOfflineAttempt(codeUnique, magasinId, utilisateurId, e.getMessage());
                return new RedemptionResult(false,
                        "Connexion indisponible — Validation impossible. Tentative enregistrée.",
                        0, RedemptionResult.ErrorType.CONNECTION_ERROR);
            }
            throw e;
        }
        return new RedemptionResult(false, "Erreur interne", 0, RedemptionResult.ErrorType.UNKNOWN);
    }

    private static boolean isConnectionError(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) return true;
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Connection is not available")
                || msg.contains("Unable to acquire JDBC Connection")
                || msg.contains("Connection refused")
                || msg.contains("timed out")
                || msg.contains("Communications link failure"))) {
            return true;
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof java.net.ConnectException
                    || cause instanceof java.net.SocketException
                    || cause instanceof java.net.UnknownHostException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static RedemptionResult.ErrorType parseErrorType(String message) {
        if (message == null) return RedemptionResult.ErrorType.UNKNOWN;
        String lc = message.toLowerCase();
        if (lc.contains("déjà été utilisé") || lc.contains("deja ete utilise"))
            return RedemptionResult.ErrorType.ALREADY_USED;
        if (lc.contains("expiré") || lc.contains("expire"))
            return RedemptionResult.ErrorType.EXPIRED;
        if (lc.contains("introuvable") || lc.contains("invalide"))
            return RedemptionResult.ErrorType.INVALID_CODE;
        if (lc.contains("annul"))
            return RedemptionResult.ErrorType.CANCELLED;
        return RedemptionResult.ErrorType.UNKNOWN;
    }

    private static void logOfflineAttempt(String codeUnique, int magasinId, int utilisateurId, String erreur) {
        try {
            File logDir = new File(System.getProperty("user.home") + "/exports/logs");
            if (!logDir.exists()) logDir.mkdirs();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String line = String.format("[%s] CODE=%s MAGASIN=%d USER=%d ERREUR=%s%n",
                    timestamp, codeUnique, magasinId, utilisateurId, erreur);
            try (FileWriter fw = new FileWriter(new File(logDir, "redemption_hors_ligne.log"), true)) {
                fw.write(line);
            }
        } catch (Exception ex) {
            System.err.println("Impossible de logger la tentative hors-ligne: " + ex.getMessage());
        }
    }

    /**
     * Recherche un bon par son code unique (pour affichage avant rédemption).
     */
    public static BonInfo getBonByCode(String codeUnique) throws SQLException {
        String sql = "SELECT b.bon_id, b.code_unique, b.valeur, b.statut, " +
                     "b.date_emission, b.date_expiration, b.demande_id, b.pdf_path, " +
                     "d.reference, c.name, c.email " +
                     "FROM bon b " +
                     "JOIN demande d ON b.demande_id = d.demande_id " +
                     "LEFT JOIN client c ON d.clientid = c.clientid " +
                     "WHERE b.code_unique = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codeUnique);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BonInfo b = new BonInfo();
                    b.bonId = rs.getInt("bon_id");
                    b.codeUnique = rs.getString("code_unique");
                    b.valeur = rs.getDouble("valeur");
                    b.statut = rs.getString("statut");
                    b.dateEmission = rs.getString("date_emission");
                    b.dateExpiration = rs.getString("date_expiration");
                    b.demandeId = rs.getInt("demande_id");
                    b.pdfPath = rs.getString("pdf_path");
                    b.reference = rs.getString("reference");
                    b.clientNom = rs.getString("name");
                    b.clientEmail = rs.getString("email");
                    return b;
                }
            }
        }
        return null;
    }

    public static class VerifInfo {
        public BonInfo bon;
        public String  redemptionDate;   // null si non rachet\u00e9
        public String  redemptionMagasin;
        public String  redemptionUtilisateur;
        public boolean expired;          // date_expiration < now
    }

    /** V\u00e9rification riche : bon + info redemption + flag expir\u00e9. */
    public static VerifInfo verifierBon(String codeUnique) throws SQLException {
        BonInfo bi = getBonByCode(codeUnique);
        if (bi == null) return null;
        VerifInfo v = new VerifInfo();
        v.bon = bi;
        // Expir\u00e9 si date_expiration < now et statut != REDIME
        String sqlExp = "SELECT (date_expiration < CURRENT_TIMESTAMP) AS expired FROM bon WHERE bon_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlExp)) {
            ps.setInt(1, bi.bonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) v.expired = rs.getBoolean("expired");
            }
        }
        // Infos redemption si existe
        String sqlR = "SELECT r.date_redemption, m.nom_magasin, u.username " +
                      "FROM redemption r " +
                      "LEFT JOIN magasin m ON r.magasin_id = m.magasin_id " +
                      "LEFT JOIN utilisateur u ON r.utilisateur_id = u.userid " +
                      "WHERE r.bon_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlR)) {
            ps.setInt(1, bi.bonId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    v.redemptionDate       = rs.getString("date_redemption");
                    v.redemptionMagasin    = rs.getString("nom_magasin");
                    v.redemptionUtilisateur= rs.getString("username");
                }
            }
        }
        return v;
    }

    /**
     * Compte les bons par statut pour le dashboard.
     */
    public static int countBonsByStatut(String statut) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bon WHERE statut = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statut);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Récupère toutes les demandes pour l'export Excel.
     */
    public static java.util.List<java.util.Map<String, Object>> getDemandesForExport() throws SQLException {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        String sql = "SELECT d.demande_id, d.reference, d.invoice_reference, c.name AS client, " +
                     "d.montant_total, d.nombre_bons, d.statuts, d.date_creation " +
                     "FROM demande d LEFT JOIN client c ON d.clientid = c.clientid " +
                     "ORDER BY d.date_creation DESC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("ID", rs.getInt("demande_id"));
                row.put("Référence", rs.getString("reference"));
                row.put("Facture", rs.getString("invoice_reference"));
                row.put("Client", rs.getString("client"));
                row.put("Montant", rs.getDouble("montant_total"));
                row.put("Nb Bons", rs.getInt("nombre_bons"));
                row.put("Statut", rs.getString("statuts"));
                row.put("Date", rs.getString("date_creation"));
                list.add(row);
            }
        }
        return list;
    }

    /**
     * Récupère tous les bons pour l'export Excel.
     */
    public static java.util.List<java.util.Map<String, Object>> getBonsForExport() throws SQLException {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        String sql = "SELECT b.bon_id, b.code_unique, b.valeur, b.statut, b.date_emission, " +
                     "d.reference, c.name AS client " +
                     "FROM bon b JOIN demande d ON b.demande_id = d.demande_id " +
                     "LEFT JOIN client c ON d.clientid = c.clientid " +
                     "ORDER BY b.bon_id DESC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("ID", rs.getInt("bon_id"));
                row.put("Code Unique", rs.getString("code_unique"));
                row.put("Valeur", rs.getDouble("valeur"));
                row.put("Statut", rs.getString("statut"));
                row.put("Émission", rs.getString("date_emission"));
                row.put("Réf Demande", rs.getString("reference"));
                row.put("Client", rs.getString("client"));
                list.add(row);
            }
        }
        return list;
    }

    /**
     * Récupère les bons actifs dont la date d'expiration est dans moins de {@code seuilJours} jours.
     * Utilisé pour l'export Excel « Bons proches d'expiration ».
     */
    public static java.util.List<java.util.Map<String, Object>> getBonsProchesExpirationForExport(
            int seuilJours) throws SQLException {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        String sql = "SELECT b.bon_id, b.code_unique, b.valeur, b.statut, "
                   + "b.date_emission, b.date_expiration, d.reference, c.name AS client, "
                   + "EXTRACT(DAY FROM (b.date_expiration - CURRENT_TIMESTAMP)) AS jours_restants "
                   + "FROM bon b "
                   + "JOIN demande d ON b.demande_id = d.demande_id "
                   + "LEFT JOIN client c ON d.clientid = c.clientid "
                   + "WHERE b.statut = 'ACTIF' "
                   + "AND b.date_expiration > CURRENT_TIMESTAMP "
                   + "AND b.date_expiration < CURRENT_TIMESTAMP + (? || ' days')::INTERVAL "
                   + "ORDER BY b.date_expiration ASC";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, seuilJours);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("ID",             rs.getInt("bon_id"));
                    row.put("Code Unique",    rs.getString("code_unique"));
                    row.put("Valeur",         rs.getDouble("valeur"));
                    row.put("Réf Demande",    rs.getString("reference"));
                    row.put("Client",         rs.getString("client"));
                    row.put("Émission",       rs.getString("date_emission"));
                    row.put("Expiration",     rs.getString("date_expiration"));
                    row.put("Jours Restants", (int) rs.getDouble("jours_restants"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    // ── Historique rédemptions du jour pour un magasin ───────────────────────

    public static class RedemptionRecord {
        public String heure;
        public String codeUnique;
        public double valeur;
        public String statut;      // "OK" | "ECHEC"
        public String message;
    }

    /**
     * Rédemptions du jour (heure locale) pour un magasin donné.
     * Retourne les 50 dernières, les plus récentes en premier.
     */
    public static List<RedemptionRecord> getRedemptionsAujourdhui(int magasinId) throws SQLException {
        List<RedemptionRecord> out = new ArrayList<>();
        String sql =
            "SELECT r.date_redemption, b.code_unique, b.valeur " +
            "FROM redemption r " +
            "JOIN bon b ON r.bon_id = b.bon_id " +
            "WHERE r.magasin_id = ? " +
            "  AND r.date_redemption::date = CURRENT_DATE " +
            "ORDER BY r.date_redemption DESC " +
            "LIMIT 50";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, magasinId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RedemptionRecord rec = new RedemptionRecord();
                    java.sql.Timestamp ts = rs.getTimestamp("date_redemption");
                    rec.heure      = ts != null ? ts.toString().substring(11, 19) : "";
                    rec.codeUnique = rs.getString("code_unique");
                    rec.valeur     = rs.getDouble("valeur");
                    rec.statut     = "OK";
                    rec.message    = "Validé";
                    out.add(rec);
                }
            }
        }
        return out;
    }

    public static class RedemptionResult {
        public boolean   succes;
        public String    message;
        public double    valeur;
        public ErrorType errorType;

        public enum ErrorType {
            SUCCESS, CONNECTION_ERROR, EXPIRED, ALREADY_USED,
            INVALID_CODE, CANCELLED, LOCK_TIMEOUT, UNKNOWN
        }

        public RedemptionResult() {}

        public RedemptionResult(boolean succes, String message, double valeur) {
            this.succes    = succes;
            this.message   = message;
            this.valeur    = valeur;
            this.errorType = succes ? ErrorType.SUCCESS : ErrorType.UNKNOWN;
        }

        public RedemptionResult(boolean succes, String message, double valeur, ErrorType errorType) {
            this.succes    = succes;
            this.message   = message;
            this.valeur    = valeur;
            this.errorType = errorType;
        }
    }
}
