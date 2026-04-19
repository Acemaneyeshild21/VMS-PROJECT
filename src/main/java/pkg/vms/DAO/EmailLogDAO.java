package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour le journal d'envoi d'emails (table email_log).
 * Trace chaque tentative d'envoi : succ\u00e8s, \u00e9chec, ou simulation (SMTP non configur\u00e9).
 * Permet la consultation par l'administrateur dans un dialog d\u00e9di\u00e9.
 */
public class EmailLogDAO {

    /** Enumeration des statuts possibles d'un envoi. */
    public static final String STATUT_ENVOYE     = "ENVOYE";
    public static final String STATUT_ECHEC      = "ECHEC";
    public static final String STATUT_SIMULATION = "SIMULATION";

    public static class EmailEntry {
        public int       emailId;
        public Integer   demandeId;           // peut \u00eatre null
        public String    destinataire;
        public String    cc;
        public String    sujet;
        public String    statut;
        public String    erreur;
        public int       nbPiecesJointes;
        public Integer   utilisateurId;
        public Timestamp dateEnvoi;
    }

    /**
     * Enregistre un \u00e9v\u00e9nement d'envoi (succ\u00e8s ou \u00e9chec).
     * Silencieux en cas d'erreur BD (ne doit jamais faire tomber un envoi d'email).
     */
    public static void log(Integer demandeId,
                           String destinataire,
                           String cc,
                           String sujet,
                           String statut,
                           String erreur,
                           int nbPiecesJointes,
                           Integer utilisateurId) {
        String sql = "INSERT INTO email_log (demande_id, destinataire, cc, sujet, statut, erreur, " +
                     "nb_pieces_jointes, utilisateur_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (demandeId != null) ps.setInt(1, demandeId); else ps.setNull(1, Types.INTEGER);
            ps.setString(2, destinataire);
            ps.setString(3, cc);
            // Tronquer le sujet si > 500 (garde-fou)
            ps.setString(4, sujet != null && sujet.length() > 500 ? sujet.substring(0, 500) : sujet);
            ps.setString(5, statut);
            ps.setString(6, erreur);
            ps.setInt(7, nbPiecesJointes);
            if (utilisateurId != null) ps.setInt(8, utilisateurId); else ps.setNull(8, Types.INTEGER);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[EmailLogDAO] log : " + e.getMessage());
        }
    }

    /**
     * Recherche des entr\u00e9es avec filtres optionnels.
     * Tous les param\u00e8tres sont optionnels (null = pas de filtre).
     *
     * @param destinataireLike filtre LIKE sur destinataire (insensible \u00e0 la casse)
     * @param statut           ENVOYE / ECHEC / SIMULATION, ou null pour tous
     * @param heures           fen\u00eatre temporelle en heures (<=0 = pas de limite)
     * @param limit            limite de r\u00e9sultats (par d\u00e9faut 500)
     */
    public static List<EmailEntry> search(String destinataireLike,
                                          String statut,
                                          int heures,
                                          int limit) throws SQLException {
        List<EmailEntry> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT email_id, demande_id, destinataire, cc, sujet, statut, erreur, " +
            "nb_pieces_jointes, utilisateur_id, date_envoi FROM email_log WHERE 1=1");

        List<Object> params = new ArrayList<>();
        if (destinataireLike != null && !destinataireLike.isBlank()) {
            sql.append(" AND LOWER(destinataire) LIKE ?");
            params.add("%" + destinataireLike.toLowerCase() + "%");
        }
        if (statut != null && !statut.isBlank()) {
            sql.append(" AND statut = ?");
            params.add(statut);
        }
        if (heures > 0) {
            sql.append(" AND date_envoi >= CURRENT_TIMESTAMP - (? || ' hours')::interval");
            params.add(String.valueOf(heures));
        }
        sql.append(" ORDER BY date_envoi DESC LIMIT ?");
        params.add(limit > 0 ? limit : 500);

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    EmailEntry e = new EmailEntry();
                    e.emailId         = rs.getInt("email_id");
                    int did           = rs.getInt("demande_id");
                    e.demandeId       = rs.wasNull() ? null : did;
                    e.destinataire    = rs.getString("destinataire");
                    e.cc              = rs.getString("cc");
                    e.sujet           = rs.getString("sujet");
                    e.statut          = rs.getString("statut");
                    e.erreur          = rs.getString("erreur");
                    e.nbPiecesJointes = rs.getInt("nb_pieces_jointes");
                    int uid           = rs.getInt("utilisateur_id");
                    e.utilisateurId   = rs.wasNull() ? null : uid;
                    e.dateEnvoi       = rs.getTimestamp("date_envoi");
                    list.add(e);
                }
            }
        }
        return list;
    }

    /** Statistiques par statut pour le panneau r\u00e9capitulatif. */
    public static class Stats {
        public int envoyes;
        public int echecs;
        public int simulations;
        public int total() { return envoyes + echecs + simulations; }
    }

    /** R\u00e9cup\u00e8re les compteurs sur une p\u00e9riode (heures; <=0 = tout l'historique). */
    public static Stats getStats(int heures) throws SQLException {
        Stats s = new Stats();
        StringBuilder sql = new StringBuilder("SELECT statut, COUNT(*) AS nb FROM email_log");
        if (heures > 0) {
            sql.append(" WHERE date_envoi >= CURRENT_TIMESTAMP - (? || ' hours')::interval");
        }
        sql.append(" GROUP BY statut");
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (heures > 0) ps.setString(1, String.valueOf(heures));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String st = rs.getString("statut");
                    int nb = rs.getInt("nb");
                    if (STATUT_ENVOYE.equals(st))     s.envoyes     = nb;
                    else if (STATUT_ECHEC.equals(st)) s.echecs      = nb;
                    else if (STATUT_SIMULATION.equals(st)) s.simulations = nb;
                }
            }
        }
        return s;
    }

    /** Cr\u00e9e la table si elle n'existe pas (pour d\u00e9ploiement \u00e0 chaud sans schema.sql). */
    public static void ensureSchema() {
        String ddl =
            "CREATE TABLE IF NOT EXISTS email_log (" +
            "  email_id          SERIAL PRIMARY KEY," +
            "  demande_id        INT," +
            "  destinataire      VARCHAR(255) NOT NULL," +
            "  cc                VARCHAR(255)," +
            "  sujet             VARCHAR(500)," +
            "  statut            VARCHAR(20) NOT NULL," +
            "  erreur            TEXT," +
            "  nb_pieces_jointes INT DEFAULT 0," +
            "  utilisateur_id    INT," +
            "  date_envoi        TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
            st.execute("CREATE INDEX IF NOT EXISTS idx_email_log_date ON email_log(date_envoi DESC)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_email_log_statut ON email_log(statut)");
        } catch (SQLException e) {
            System.err.println("[EmailLogDAO] ensureSchema : " + e.getMessage());
        }
    }
}
