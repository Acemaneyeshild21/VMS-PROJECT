package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Log spécialisé pour les envois email (ENVOI ou ENVOI_ECHEC).
     * succes=true → action ENVOI ; succes=false → action ENVOI_ECHEC.
     */
    public static void logEnvoi(int demandeId, boolean succes, int userId, String details) {
        String action = succes ? "ENVOI" : "ENVOI_ECHEC";
        String nouveauVal = succes
                ? "{\"succes\":true}"
                : "{\"succes\":false,\"erreur\":\""
                  + (details != null ? details.replace("\\", "\\\\").replace("\"", "'") : "") + "\"}";
        String contexte = succes
                ? "Envoi email réussi — demande #" + demandeId
                : "Échec envoi email — demande #" + demandeId + " : " + details;
        log("demande", demandeId, action, null, nouveauVal, userId, null, contexte);
    }

    /**
     * Récupère l'historique d'audit selon le rôle de l'utilisateur.
     * <ul>
     *   <li>ADMIN_SIEGE (Administrateur) et Manager : trail complet</li>
     *   <li>Tous les autres rôles (Comptable, Approbateur…) : uniquement leurs propres actions</li>
     * </ul>
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
