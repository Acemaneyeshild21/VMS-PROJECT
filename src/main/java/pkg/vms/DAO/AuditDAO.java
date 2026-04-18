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
                    e.id = rs.getInt("audit_id");
                    e.tableName = rs.getString("table_name");
                    e.recordId = rs.getInt("record_id");
                    e.action = rs.getString("action");
                    e.username = rs.getString("username");
                    e.contexte = rs.getString("contexte");
                    e.dateEvt = rs.getTimestamp("date_action");
                    out.add(e);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur audit getRecent : " + e.getMessage());
        }
        return out;
    }

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
}
