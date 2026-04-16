package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SocieteDAO {

    public static class Societe {
        public int societeId;
        public String nom;
        public String adresse;
        public String telephone;
        public String email;
        public boolean actif;

        public Societe(int societeId, String nom, String adresse, String telephone, String email, boolean actif) {
            this.societeId = societeId;
            this.nom = nom;
            this.adresse = adresse;
            this.telephone = telephone;
            this.email = email;
            this.actif = actif;
        }
    }

    public static List<Societe> getAllSocietes() throws SQLException {
        List<Societe> societes = new ArrayList<>();
        String query = "SELECT societe_id, nom, adresse, telephone, email, actif FROM societe ORDER BY societe_id";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                societes.add(new Societe(
                        rs.getInt("societe_id"),
                        rs.getString("nom"),
                        rs.getString("adresse"),
                        rs.getString("telephone"),
                        rs.getString("email"),
                        rs.getBoolean("actif")
                ));
            }
        }
        return societes;
    }

    public static boolean addSociete(String nom, String adresse, String telephone, String email) throws SQLException {
        String query = "INSERT INTO societe (nom, adresse, telephone, email) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nom);
            ps.setString(2, adresse);
            ps.setString(3, telephone);
            ps.setString(4, email);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("societe", -1, "CREATION", -1, "Nouvelle société: " + nom);
                return true;
            }
        }
        return false;
    }

    public static boolean updateSociete(int societeId, String nom, String adresse, String telephone, String email) throws SQLException {
        String query = "UPDATE societe SET nom = ?, adresse = ?, telephone = ?, email = ? WHERE societe_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nom);
            ps.setString(2, adresse);
            ps.setString(3, telephone);
            ps.setString(4, email);
            ps.setInt(5, societeId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("societe", societeId, "MODIFICATION", -1, "Société mise à jour: " + nom);
                return true;
            }
        }
        return false;
    }

    public static boolean deleteSociete(int societeId) throws SQLException {
        String query = "DELETE FROM societe WHERE societe_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, societeId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("societe", societeId, "SUPPRESSION", -1, "Société supprimée");
                return true;
            }
        }
        return false;
    }
}
