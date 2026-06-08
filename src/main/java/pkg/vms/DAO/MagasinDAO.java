package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MagasinDAO {

    public static class Magasin {
        public int magasinId;
        public String nomMagasin;
        public String adresse;
        public Integer superviseurId;
        public String superviseurNom;

        public Magasin(int magasinId, String nomMagasin, String adresse, Integer superviseurId, String superviseurNom) {
            this.magasinId = magasinId;
            this.nomMagasin = nomMagasin;
            this.adresse = adresse;
            this.superviseurId = superviseurId;
            this.superviseurNom = superviseurNom;
        }
    }

    public static List<Magasin> getAllMagasins() throws SQLException {
        List<Magasin> magasins = new ArrayList<>();
        String query = "SELECT m.magasin_id, m.nom_magasin, m.adresse, m.superviseur_id, u.username " +
                "FROM magasin m LEFT JOIN utilisateur u ON m.superviseur_id = u.userid " +
                "ORDER BY m.magasin_id";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                magasins.add(new Magasin(
                        rs.getInt("magasin_id"),
                        rs.getString("nom_magasin"),
                        rs.getString("adresse"),
                        rs.getObject("superviseur_id") != null ? rs.getInt("superviseur_id") : null,
                        rs.getString("username")
                ));
            }
        }
        return magasins;
    }

    public static boolean addMagasin(String nomMagasin, String adresse, Integer superviseurId) throws SQLException {
        String query = "INSERT INTO magasin (nom_magasin, adresse, superviseur_id) VALUES (?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nomMagasin);
            ps.setString(2, adresse);
            if (superviseurId != null) {
                ps.setInt(3, superviseurId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("magasin", -1, "CREATION", -1, "Nouveau magasin: " + nomMagasin);
                return true;
            }
        }
        return false;
    }

    public static boolean updateMagasin(int magasinId, String nomMagasin, String adresse, Integer superviseurId) throws SQLException {
        String query = "UPDATE magasin SET nom_magasin = ?, adresse = ?, superviseur_id = ? WHERE magasin_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nomMagasin);
            ps.setString(2, adresse);
            if (superviseurId != null) {
                ps.setInt(3, superviseurId);
            } else {
                ps.setNull(3, Types.INTEGER);
            }
            ps.setInt(4, magasinId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("magasin", magasinId, "MODIFICATION", -1, "Magasin mis à jour: " + nomMagasin);
                return true;
            }
        }
        return false;
    }

    public static boolean deleteMagasin(int magasinId) throws SQLException {
        String query = "DELETE FROM magasin WHERE magasin_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, magasinId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("magasin", magasinId, "SUPPRESSION", -1, "Magasin supprimé");
                return true;
            }
        }
        return false;
    }
}