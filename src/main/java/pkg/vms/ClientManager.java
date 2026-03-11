package pkg.vms;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de base de données pour les clients
 * Gère toutes les opérations CRUD (Create, Read, Update, Delete)
 */
public class ClientManager {

    // Configuration de la base de données
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/VMS_voucher";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "0003";

    /**
     * Obtenir une connexion à la base de données
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * CRÉER un nouveau client
     * @param client Le client à créer
     * @return true si succès, false sinon
     */
    public boolean ajouterClient(Client client) {
        String sql = "INSERT INTO client (name, email, contactnumber, company, actif) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getEmail());
            pstmt.setString(3, client.getContactNumber());
            pstmt.setString(4, client.getCompany());
            pstmt.setBoolean(5, client.isActif());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * LIRE tous les clients
     * @return Liste de tous les clients
     */
    public List<Client> obtenirTousLesClients() {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM client ORDER BY client_id DESC";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("contactnumber"),
                        rs.getString("company"),
                        rs.getTimestamp("date_creation"),
                        rs.getBoolean("actif")
                );
                clients.add(client);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clients;
    }

    /**
     * LIRE un client par son ID
     * @param clientId L'ID du client
     * @return Le client trouvé, ou null si non trouvé
     */
    public Client obtenirClientParId(int clientId) {
        String sql = "SELECT * FROM client WHERE client_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new Client(
                        rs.getInt("client_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("contactnumber"),
                        rs.getString("company"),
                        rs.getTimestamp("date_creation"),
                        rs.getBoolean("actif")
                );
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * RECHERCHER des clients par nom, email ou société
     * @param recherche Le terme de recherche
     * @return Liste des clients correspondants
     */
    public List<Client> rechercherClients(String recherche) {
        List<Client> clients = new ArrayList<>();
        String sql = "SELECT * FROM client WHERE " +
                "LOWER(name) LIKE LOWER(?) OR " +
                "LOWER(email) LIKE LOWER(?) OR " +
                "LOWER(company) LIKE LOWER(?) " +
                "ORDER BY client_id DESC";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + recherche + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Client client = new Client(
                        rs.getInt("client_id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("contactnumber"),
                        rs.getString("company"),
                        rs.getTimestamp("date_creation"),
                        rs.getBoolean("actif")
                );
                clients.add(client);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return clients;
    }

    /**
     * METTRE À JOUR un client existant
     * @param client Le client avec les nouvelles informations
     * @return true si succès, false sinon
     */
    public boolean modifierClient(Client client) {
        String sql = "UPDATE client SET name = ?, email = ?, contactnumber = ?, company = ?, actif = ? WHERE client_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, client.getName());
            pstmt.setString(2, client.getEmail());
            pstmt.setString(3, client.getContactNumber());
            pstmt.setString(4, client.getCompany());
            pstmt.setBoolean(5, client.isActif());
            pstmt.setInt(6, client.getClientId());

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * SUPPRIMER un client (désactivation logique)
     * @param clientId L'ID du client à supprimer
     * @return true si succès, false sinon
     */
    public boolean supprimerClient(int clientId) {
        // Désactivation logique au lieu de suppression physique
        String sql = "UPDATE client SET actif = false WHERE client_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * SUPPRIMER définitivement un client de la base de données
     * @param clientId L'ID du client à supprimer
     * @return true si succès, false sinon
     */
    public boolean supprimerClientDefinitivement(int clientId) {
        String sql = "DELETE FROM client WHERE client_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * VÉRIFIER si un email existe déjà
     * @param email L'email à vérifier
     * @param clientIdExclure ID du client à exclure de la vérification (pour modification)
     * @return true si l'email existe déjà, false sinon
     */
    public boolean emailExiste(String email, int clientIdExclure) {
        String sql = "SELECT COUNT(*) FROM client WHERE email = ? AND client_id != ?";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setInt(2, clientIdExclure);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * COMPTER le nombre total de clients actifs
     * @return Nombre de clients actifs
     */
    public int compterClientsActifs() {
        String sql = "SELECT COUNT(*) FROM client WHERE actif = true";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return 0;
    }
}