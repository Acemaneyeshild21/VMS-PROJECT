package pkg.vms.DAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBconnect {

    // Configuration de la connexion
    private static final String URL = "jdbc:postgresql://localhost:5432/VMS";
    private static final String USER = "postgres";
    private static final String PASSWORD = "54321";

    private static Connection connection;

    /**
     * Retourne la connexion active (singleton-like)
     * @return Connection
     */
    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                // Chargement explicite du driver au besoin
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connexion à la base de données établie !");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL non trouvé !");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la connexion à la base de données !");
            e.printStackTrace();
        }
        return connection;
    }

    /**
     * Ferme la connexion à la base de données
     */
    public static void closeConnection() {
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
    public static boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed();
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