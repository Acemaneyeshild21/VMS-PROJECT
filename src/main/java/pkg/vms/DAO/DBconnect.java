package pkg.vms.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnect {

    // Configuration de la connexion
    private static final String URL = "jdbc:postgresql://localhost:5432/VMS_voucher";
    private static final String USER = "postgres";
    private static final String PASSWORD = "0003";

    private static Connection connection;

    /**
     * Constructeur - établit la connexion à la base de données
     */
    public DBconnect() {
        try {
            // Chargement du driver PostgreSQL
            Class.forName("org.postgresql.Driver");

            // Établissement de la connexion
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base de données réussie !");

        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trouvé !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la connexion à la base de données !");
            e.printStackTrace();
        }
    }

    /**
     * Retourne la connexion active
     * @return Connection
     */
    public static Connection getConnection() {
        try {
            // Vérifier si la connexion est toujours valide
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Ferme la connexion à la base de données
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion fermée avec succès.");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion !");
            e.printStackTrace();
        }
    }

    /**
     * Teste la connexion à la base de données
     * @return true si la connexion fonctionne, false sinon
     */
    public boolean testConnection() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Méthode statique pour obtenir une connexion rapide
     * @return Connection
     */
    public static Connection getStaticConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}