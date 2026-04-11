package pkg.vms.DAO;

import java.sql.*;

public class AuditDAO {

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
