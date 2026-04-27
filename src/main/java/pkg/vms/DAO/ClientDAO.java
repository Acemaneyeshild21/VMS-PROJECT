package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import pkg.vms.Client;

public class ClientDAO {

    public static class ClientInfo {
        public int id;
        public String name;
        public String email;

        public ClientInfo(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() { return name + " (" + email + ")"; }
    }

    public static class MagasinInfo {
        public int id;
        public String nom;

        public MagasinInfo(int id, String nom) {
            this.id = id;
            this.nom = nom;
        }

        @Override
        public String toString() { return nom; }
    }

    public static List<ClientInfo> getActiveClients() throws SQLException {
        List<ClientInfo> clients = new ArrayList<>();
        String query = "SELECT clientid, name, email FROM client WHERE actif = true ORDER BY name ASC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                clients.add(new ClientInfo(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
        }
        return clients;
    }

    public static List<MagasinInfo> getAllMagasins() throws SQLException {
        List<MagasinInfo> magasins = new ArrayList<>();
        String query = "SELECT magasin_id, nom_magasin FROM magasin ORDER BY nom_magasin ASC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                magasins.add(new MagasinInfo(rs.getInt(1), rs.getString(2)));
            }
        }
        return magasins;
    }

    public static List<Client> getAllClients() throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT clientid, name, email, contact_number, company, date_creation, actif FROM client ORDER BY clientid DESC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                clients.add(new Client(
                    rs.getInt("clientid"), rs.getString("name"), rs.getString("email"),
                    rs.getString("contact_number"), rs.getString("company"),
                    rs.getTimestamp("date_creation"), rs.getBoolean("actif")
                ));
            }
        }
        return clients;
    }

    public static List<Client> searchClients(String search) throws SQLException {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT clientid, name, email, contact_number, company, date_creation, actif FROM client " +
                     "WHERE LOWER(name) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) OR LOWER(company) LIKE LOWER(?) " +
                     "ORDER BY clientid DESC";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String pattern = "%" + search + "%";
            ps.setString(1, pattern); ps.setString(2, pattern); ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    clients.add(new Client(
                        rs.getInt("clientid"), rs.getString("name"), rs.getString("email"),
                        rs.getString("contact_number"), rs.getString("company"),
                        rs.getTimestamp("date_creation"), rs.getBoolean("actif")
                    ));
                }
            }
        }
        return clients;
    }

    public static Client getClientById(int clientId) throws SQLException {
        String sql = "SELECT clientid, name, email, contact_number, company, date_creation, actif FROM client WHERE clientid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Client(
                        rs.getInt("clientid"), rs.getString("name"), rs.getString("email"),
                        rs.getString("contact_number"), rs.getString("company"),
                        rs.getTimestamp("date_creation"), rs.getBoolean("actif")
                    );
                }
            }
        }
        return null;
    }

    public static boolean addClient(Client client) throws SQLException {
        String sql = "INSERT INTO client (name, email, contact_number, company, actif) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getName()); ps.setString(2, client.getEmail());
            ps.setString(3, client.getContactNumber()); ps.setString(4, client.getCompany());
            ps.setBoolean(5, client.isActif());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean updateClient(Client client) throws SQLException {
        String sql = "UPDATE client SET name=?, email=?, contact_number=?, company=?, actif=? WHERE clientid=?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getName()); ps.setString(2, client.getEmail());
            ps.setString(3, client.getContactNumber()); ps.setString(4, client.getCompany());
            ps.setBoolean(5, client.isActif()); ps.setInt(6, client.getClientId());
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean deactivateClient(int clientId) throws SQLException {
        String sql = "UPDATE client SET actif = false WHERE clientid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    public static boolean deleteClientPermanently(int clientId) throws SQLException {
        String sql = "DELETE FROM client WHERE clientid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Récupère tous les clients pour l'export Excel.
     */
    public static java.util.List<java.util.Map<String, Object>> getClientsForExport() throws SQLException {
        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        String sql = "SELECT clientid, name, email, contact_number, company, date_creation, actif " +
                     "FROM client ORDER BY name ASC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                row.put("ID", rs.getInt("clientid"));
                row.put("Nom", rs.getString("name"));
                row.put("Email", rs.getString("email"));
                row.put("Téléphone", rs.getString("contact_number"));
                row.put("Société", rs.getString("company"));
                row.put("Création", rs.getString("date_creation"));
                row.put("Actif", rs.getBoolean("actif") ? "Oui" : "Non");
                list.add(row);
            }
        }
        return list;
    }
}
