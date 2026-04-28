package pkg.vms.DAO;

import pkg.vms.EmailService.EmailType;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la table {@code email_errors}.
 *
 * <p>Chaque envoi SMTP qui échoue génère une ligne ici.
 * L'interface ParametresPanel peut lister ces erreurs et lancer une relance
 * via {@link pkg.vms.EmailService#resendEmail}.</p>
 *
 * <p>La table est créée automatiquement au premier accès si elle n'existe pas.</p>
 */
public class EmailDAO {

    // ── Création de la table si absente ──────────────────────────────────────

    static {
        ensureTable();
    }

    private static void ensureTable() {
        // La table est également définie dans schema.sql — ce bloc garantit
        // son existence même si le schema n'a pas été rejoué.
        String sql = """
            CREATE TABLE IF NOT EXISTS email_errors (
                id              SERIAL PRIMARY KEY,
                demande_id      INTEGER REFERENCES demande(demande_id) ON DELETE SET NULL,
                demande_ref     VARCHAR(50),
                to_email        VARCHAR(255) NOT NULL,
                email_type      VARCHAR(50)  NOT NULL,
                nb_tentatives   INTEGER      NOT NULL DEFAULT 1,
                derniere_erreur TEXT,
                payload         TEXT,
                created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                resolved        BOOLEAN      NOT NULL DEFAULT FALSE,
                resolved_at     TIMESTAMP
            )
            """;
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(sql);
            st.execute("CREATE INDEX IF NOT EXISTS idx_email_errors_resolved " +
                       "ON email_errors(resolved) WHERE resolved = FALSE");
        } catch (SQLException e) {
            System.err.println("[EmailDAO] Impossible de créer email_errors : " + e.getMessage());
        }
    }

    // ── Modèle ───────────────────────────────────────────────────────────────

    /** Représente une ligne de la table email_errors. */
    public static class EmailError {
        public long      id;
        public int       demandeId;
        public String    demandeRef;
        public String    toEmail;
        public EmailType emailType;
        public int       nbTentatives;
        public String    derniereErreur;
        public Timestamp createdAt;
        public boolean   resolved;
        public Timestamp resolvedAt;
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    /**
     * Insère une nouvelle erreur d'envoi email en base.
     *
     * @return l'id généré (pour référence future / relance)
     */
    public static long saveError(int demandeId, String toEmail, String demandeRef,
                                 EmailType type, String erreurMessage) throws SQLException {
        String sql = """
            INSERT INTO email_errors
              (demande_id, demande_ref, to_email, email_type, derniere_erreur)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (demandeId > 0) ps.setInt(1, demandeId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, demandeRef);
            ps.setString(3, toEmail);
            ps.setString(4, type != null ? type.name() : "UNKNOWN");
            ps.setString(5, erreurMessage);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        }
        return -1;
    }

    /**
     * Marque un email comme résolu (relance réussie).
     */
    public static void markResolved(long id) {
        String sql = "UPDATE email_errors SET resolved = TRUE, resolved_at = NOW() WHERE id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EmailDAO] markResolved #" + id + " : " + e.getMessage());
        }
    }

    /**
     * Incrémente le compteur de tentatives et met à jour le message d'erreur.
     */
    public static void incrementTentatives(long id, String nouvelleErreur) {
        String sql = """
            UPDATE email_errors
               SET nb_tentatives   = nb_tentatives + 1,
                   derniere_erreur = ?
             WHERE id = ?
            """;
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nouvelleErreur);
            ps.setLong(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EmailDAO] incrementTentatives #" + id + " : " + e.getMessage());
        }
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    /**
     * Retourne une erreur email par son id, ou {@code null} si introuvable.
     */
    public static EmailError getError(long id) throws SQLException {
        String sql = "SELECT * FROM email_errors WHERE id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    /**
     * Retourne toutes les erreurs non résolues (pour affichage dans ParametresPanel).
     * Triées de la plus récente à la plus ancienne.
     */
    public static List<EmailError> getUnresolvedErrors() throws SQLException {
        List<EmailError> list = new ArrayList<>();
        String sql = "SELECT * FROM email_errors WHERE resolved = FALSE ORDER BY created_at DESC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Retourne les N dernières erreurs (résolues ou non) pour l'historique.
     */
    public static List<EmailError> getRecentErrors(int limit) throws SQLException {
        List<EmailError> list = new ArrayList<>();
        String sql = "SELECT * FROM email_errors ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Retourne le nombre d'erreurs email non résolues (pour badge dans l'UI).
     */
    public static int countUnresolved() throws SQLException {
        String sql = "SELECT COUNT(*) FROM email_errors WHERE resolved = FALSE";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private static EmailError mapRow(ResultSet rs) throws SQLException {
        EmailError e = new EmailError();
        e.id             = rs.getLong("id");
        e.demandeId      = rs.getInt("demande_id");
        e.demandeRef     = rs.getString("demande_ref");
        e.toEmail        = rs.getString("to_email");
        e.nbTentatives   = rs.getInt("nb_tentatives");
        e.derniereErreur = rs.getString("derniere_erreur");
        e.createdAt      = rs.getTimestamp("created_at");
        e.resolved       = rs.getBoolean("resolved");
        e.resolvedAt     = rs.getTimestamp("resolved_at");
        try {
            String typeStr = rs.getString("email_type");
            e.emailType = typeStr != null ? EmailType.valueOf(typeStr) : null;
        } catch (IllegalArgumentException ex) {
            e.emailType = null;
        }
        return e;
    }
}
