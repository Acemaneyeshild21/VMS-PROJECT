package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuditDAO {

    public static class Event {
        public int    id;
        public String tableName;
        public int    recordId;
        public String action;
        public String username;
        public String contexte;
        public Timestamp dateEvt;
    }

    public static class Session {
        public String    username;
        public Timestamp derniereConnexion;
        public int       nbConnexions;
        public int       echecs;
    }

    // ── Lecture récente ──────────────────────────────────────────────────────

    /** Liste des derniers événements d'audit — pour la timeline. */
    public static List<Event> getRecent(int limit) {
        List<Event> out = new ArrayList<>();
        String sql = "SELECT audit_id, table_name, record_id, action, username, contexte, date_action " +
                     "FROM audit_log ORDER BY date_action DESC LIMIT ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Event e = new Event();
                    e.id        = rs.getInt("audit_id");
                    e.tableName = rs.getString("table_name");
                    e.recordId  = rs.getInt("record_id");
                    e.action    = rs.getString("action");
                    e.username  = rs.getString("username");
                    e.contexte  = rs.getString("contexte");
                    e.dateEvt   = rs.getTimestamp("date_action");
                    out.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur audit getRecent : " + e.getMessage());
        }
        return out;
    }

    // ── Recherche avancée pour journal d'audit ───────────────────────────────

    public static List<Event> search(String action, String username, String tableName,
                                     Timestamp dateDebut, Timestamp dateFin, int limit) {
        List<Event> out = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT audit_id, table_name, record_id, action, username, contexte, date_action " +
                "FROM audit_log WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (action    != null && !action.isEmpty()) {
            sql.append("AND action = ? "); params.add(action);
        }
        if (username  != null && !username.isEmpty()) {
            sql.append("AND LOWER(COALESCE(username,'')) LIKE ? ");
            params.add("%" + username.toLowerCase() + "%");
        }
        if (tableName != null && !tableName.isEmpty()) {
            sql.append("AND table_name = ? "); params.add(tableName);
        }
        if (dateDebut != null) { sql.append("AND date_action >= ? "); params.add(dateDebut); }
        if (dateFin   != null) { sql.append("AND date_action <= ? "); params.add(dateFin);   }
        sql.append("ORDER BY date_action DESC LIMIT ?");
        params.add(limit);

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Event e = new Event();
                    e.id        = rs.getInt("audit_id");
                    e.tableName = rs.getString("table_name");
                    e.recordId  = rs.getInt("record_id");
                    e.action    = rs.getString("action");
                    e.username  = rs.getString("username");
                    e.contexte  = rs.getString("contexte");
                    e.dateEvt   = rs.getTimestamp("date_action");
                    out.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur audit search : " + e.getMessage());
        }
        return out;
    }

    /** Sessions "actives" = utilisateurs connectés récemment (dernières N heures). */
    public static List<Session> getSessionsActives(int heures) {
        List<Session> out = new ArrayList<>();
        String sql =
            "SELECT COALESCE(u.username, a.username) AS name, " +
            "  MAX(a.date_action) FILTER (WHERE a.action = 'CONNEXION') AS derniere, " +
            "  COUNT(*) FILTER (WHERE a.action = 'CONNEXION')           AS nb_ok, " +
            "  COUNT(*) FILTER (WHERE a.action = 'CONNEXION_ECHOUEE')   AS nb_ko " +
            "FROM audit_log a LEFT JOIN utilisateur u ON a.utilisateur_id = u.userid " +
            "WHERE a.action IN ('CONNEXION','CONNEXION_ECHOUEE') " +
            "  AND a.date_action >= CURRENT_TIMESTAMP - (? || ' hours')::interval " +
            "  AND COALESCE(u.username, a.username) IS NOT NULL " +
            "GROUP BY COALESCE(u.username, a.username) " +
            "HAVING MAX(a.date_action) FILTER (WHERE a.action = 'CONNEXION') IS NOT NULL " +
            "ORDER BY derniere DESC";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, heures);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Session s = new Session();
                    s.username          = rs.getString("name");
                    s.derniereConnexion = rs.getTimestamp("derniere");
                    s.nbConnexions      = rs.getInt("nb_ok");
                    s.echecs            = rs.getInt("nb_ko");
                    out.add(s);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur getSessionsActives : " + e.getMessage());
        }
        return out;
    }

    /** Liste distincte des actions trouvées (pour filtres). */
    public static List<String> getDistinctActions() {
        List<String> out = new ArrayList<>();
        String sql = "SELECT DISTINCT action FROM audit_log ORDER BY action";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString(1));
        } catch (SQLException e) {
            System.err.println("Erreur getDistinctActions : " + e.getMessage());
        }
        return out;
    }

    // ── Écriture ─────────────────────────────────────────────────────────────

    public static void log(String tableName, int recordId, String action,
                           String ancienVal, String nouveauVal,
                           int utilisateurId, String username, String contexte) {
        String sql = "INSERT INTO audit_log (table_name, record_id, action, ancien_val, nouveau_val, " +
                     "utilisateur_id, username, contexte) VALUES (?, ?, ?, ?::jsonb, ?::jsonb, ?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setInt(2, recordId);
            ps.setString(3, action);
            ps.setString(4, ancienVal);
            ps.setString(5, nouveauVal);
            ps.setInt(6, utilisateurId);
            ps.setString(7, username);
            ps.setString(8, contexte);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur audit_log : " + e.getMessage());
        }
    }

    public static void logSimple(String tableName, int recordId, String action,
                                 int utilisateurId, String contexte) {
        log(tableName, recordId, action, null, null, utilisateurId, null, contexte);
    }

    /**
     * Log spécialisé pour les envois email (ENVOI ou ENVOI_ECHEC).
     * succes=true → action ENVOI ; succes=false → action ENVOI_ECHEC.
     */
    public static void logEnvoi(int demandeId, boolean succes, int userId, String details) {
        String action     = succes ? "ENVOI" : "ENVOI_ECHEC";
        String nouveauVal = succes
                ? "{\"succes\":true}"
                : "{\"succes\":false,\"erreur\":\""
                  + (details != null ? details.replace("\\", "\\\\").replace("\"", "'") : "") + "\"}";
        String contexte   = succes
                ? "Envoi email réussi — demande #" + demandeId
                : "Échec envoi email — demande #" + demandeId + " : " + details;
        log("demande", demandeId, action, null, nouveauVal, userId, null, contexte);
    }

    /**
     * Récupère l'historique d'audit selon le rôle de l'utilisateur.
     * ADMIN_SIEGE / Manager → trail complet ; autres → uniquement leurs propres actions.
     *
     * @return liste de String[] {date, action, contexte, username, table, record_id}
     */
    public static List<String[]> getAuditTrail(String role, int userId) throws SQLException {
        List<String[]> result = new ArrayList<>();
        boolean voitTout = "Administrateur".equalsIgnoreCase(role)
                        || "Manager".equalsIgnoreCase(role);

        String sql = "SELECT date_action, action, contexte, username, table_name, record_id "
                   + "FROM audit_log "
                   + (voitTout ? "" : "WHERE utilisateur_id = ? ")
                   + "ORDER BY date_action DESC LIMIT 200";

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (!voitTout) ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                java.text.SimpleDateFormat sdf =
                        new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("date_action");
                    result.add(new String[]{
                        ts != null ? sdf.format(ts) : "",
                        rs.getString("action"),
                        rs.getString("contexte"),
                        rs.getString("username") != null ? rs.getString("username") : "—",
                        rs.getString("table_name"),
                        String.valueOf(rs.getInt("record_id"))
                    });
                }
            }
        }
        return result;
    }
}
