package pkg.vms.DAO;

import pkg.vms.Config;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DBconnect {

    private static final HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Config.get("db.url", "jdbc:postgresql://localhost:5432/VMS"));
        config.setUsername(Config.get("db.user", "postgres"));
        config.setPassword(Config.get("db.password", "54321"));
        config.setDriverClassName("org.postgresql.Driver");

        // Optimisations pour PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        // Paramètres du pool
        config.setMaximumPoolSize(Config.getInt("db.pool.max_size", 10));
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000);
        config.setConnectionTimeout(30000);

        dataSource = new HikariDataSource(config);
    }

    /**
     * Retourne une connexion du pool.
     * @return Connection
     * @throws SQLException si une erreur de connexion survient
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Ferme le pool de connexions (utile lors de l'arrêt de l'application).
     */
    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * Teste la connexion à la base de données.
     * @return true si la connexion fonctionne, false sinon
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}