package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatistiquesDAO {

    public static int compterDemandes() throws SQLException {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM demande")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static double sumMontantBons() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montant_total), 0) FROM demande " +
                     "WHERE statuts IN ('APPROUVE','GENERE','PAYE','ENVOYE')";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }

    public static int compterBonsActifs() throws SQLException {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bon WHERE statut = 'ACTIF'")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public static double getTauxRedemption() throws SQLException {
        int totalBons = 0;
        int redeemedBons = 0;
        try (Connection conn = DBconnect.getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bon")) {
                if (rs.next()) totalBons = rs.getInt(1);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bon WHERE statut = 'REDIME'")) {
                if (rs.next()) redeemedBons = rs.getInt(1);
            }
        }
        return totalBons > 0 ? (redeemedBons * 100.0 / totalBons) : 0.0;
    }

    /** Returns rows of {statut, count, montantTotal}. */
    public static List<Object[]> getStatsByStatut() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT statuts, COUNT(*) AS nombre, COALESCE(SUM(montant_total),0) AS montant " +
                     "FROM demande GROUP BY statuts ORDER BY nombre DESC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new Object[]{
                    rs.getString("statuts"),
                    rs.getInt("nombre"),
                    rs.getDouble("montant")
                });
            }
        }
        return rows;
    }

    /** DB monitoring: version info + per-table row counts. */
    public static class DbStats {
        public String info = "";
        public List<Object[]> rows = new ArrayList<>();
    }

    public static DbStats getDBStats() throws SQLException {
        DbStats stats = new DbStats();
        try (Connection conn = DBconnect.getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT version()")) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    stats.info = "PostgreSQL : " + v.substring(0, Math.min(v.length(), 60));
                }
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT pg_size_pretty(pg_database_size(current_database()))")) {
                if (rs.next()) stats.info += "  |  Taille : " + rs.getString(1);
            }
            String[] tables = {"societe", "magasin", "utilisateur", "client", "demande", "bon", "redemption", "audit_log", "app_settings"};
            for (String t : tables) {
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) {
                    if (rs.next()) stats.rows.add(new Object[]{t, rs.getInt(1)});
                } catch (SQLException ignored) {
                    stats.rows.add(new Object[]{t, "N/A"});
                }
            }
        }
        return stats;
    }

    public static void vacuumDB() throws SQLException {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement()) {
            st.execute("VACUUM ANALYZE");
        }
    }

    /** Returns rows of {rank, clientName, nbDemandes, montantTotal}. */
    public static List<Object[]> getTopClients(int limit) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        String sql = "SELECT c.name, COUNT(d.demande_id) AS nb_demandes, " +
                     "COALESCE(SUM(d.montant_total),0) AS total_montant " +
                     "FROM client c LEFT JOIN demande d ON c.clientid = d.clientid " +
                     "WHERE c.actif = true " +
                     "GROUP BY c.clientid, c.name " +
                     "ORDER BY total_montant DESC LIMIT ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    rows.add(new Object[]{
                        rank++,
                        rs.getString("name"),
                        rs.getInt("nb_demandes"),
                        rs.getDouble("total_montant")
                    });
                }
            }
        }
        return rows;
    }
}
