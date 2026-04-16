package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BonDAO {

    public static class BonInfo {
        public int bonId;
        public String codeUnique;
        public double valeur;
        public String statut;
        public String dateEmission;
        public String dateExpiration;
        public int demandeId;
        public String clientNom;
        public String clientEmail;
        public String reference;
        public String pdfPath;
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
     * Met à jour le chemin PDF d'un bon.
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
     * Appelle la procédure stockée sp_redimer_bon pour la rédemption sécurisée.
     */
    public static RedemptionResult redimerBon(String codeUnique, int magasinId, int utilisateurId) throws SQLException {
        String sql = "SELECT * FROM sp_redimer_bon(?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            // Timeout de verrouillage pour éviter les blocages infinis en cas de haute concurrence
            try (Statement st = conn.createStatement()) {
                st.execute("SET lock_timeout = '5s'");
            }

            ps.setString(1, codeUnique);
            ps.setInt(2, magasinId);
            ps.setInt(3, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    RedemptionResult r = new RedemptionResult();
                    r.succes = rs.getBoolean("succes");
                    r.message = rs.getString("message");
                    r.valeur = rs.getDouble("bon_valeur");
                    
                    if (r.succes) {
                        AuditDAO.logSimple("bon", -1, "UTILISATION_BON", utilisateurId, "Code: " + codeUnique + ", Magasin: " + magasinId);
                    }
                    return r;
                }
            }
        } catch (SQLException e) {
            // Code 55P03 = lock_not_available en PostgreSQL
            if ("55P03".equals(e.getSQLState())) {
                return new RedemptionResult(false, "Le bon est en cours de traitement ailleurs. Réessayez.", 0);
            }
            throw e;
        }
        return new RedemptionResult(false, "Erreur interne", 0);
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

    public static class RedemptionResult {
        public boolean succes;
        public String message;
        public double valeur;

        public RedemptionResult() {}

        public RedemptionResult(boolean succes, String message, double valeur) {
            this.succes = succes;
            this.message = message;
            this.valeur = valeur;
        }
    }
}
