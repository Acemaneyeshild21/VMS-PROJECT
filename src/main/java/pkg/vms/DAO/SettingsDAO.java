package pkg.vms.DAO;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsDAO {

    public static class EmailSettings {
        public String smtpServer;
        public int smtpPort;
        public String smtpUsername;
        public String smtpPassword;
        public boolean tlsEnabled;
        public String fromEmail;

        public EmailSettings(String smtpServer, int smtpPort, String smtpUsername,
                             String smtpPassword, boolean tlsEnabled, String fromEmail) {
            this.smtpServer = smtpServer;
            this.smtpPort = smtpPort;
            this.smtpUsername = smtpUsername;
            this.smtpPassword = smtpPassword;
            this.tlsEnabled = tlsEnabled;
            this.fromEmail = fromEmail;
        }
    }

    public static EmailSettings getEmailSettings() throws SQLException {
        String query = "SELECT smtp_server, smtp_port, smtp_username, smtp_password, tls_enabled, from_email " +
                "FROM app_settings WHERE setting_key = 'email'";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return new EmailSettings(
                        rs.getString("smtp_server"),
                        rs.getInt("smtp_port"),
                        rs.getString("smtp_username"),
                        rs.getString("smtp_password"),
                        rs.getBoolean("tls_enabled"),
                        rs.getString("from_email")
                );
            }
        }
        return null;
    }

    public static boolean updateEmailSettings(EmailSettings settings) throws SQLException {
        String query = "UPDATE app_settings SET smtp_server = ?, smtp_port = ?, smtp_username = ?, " +
                "smtp_password = ?, tls_enabled = ?, from_email = ? WHERE setting_key = 'email'";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, settings.smtpServer);
            ps.setInt(2, settings.smtpPort);
            ps.setString(3, settings.smtpUsername);
            ps.setString(4, settings.smtpPassword);
            ps.setBoolean(5, settings.tlsEnabled);
            ps.setString(6, settings.fromEmail);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("app_settings", -1, "UPDATE_EMAIL", -1, "Paramètres email mis à jour");
                return true;
            }
        }
        return false;
    }

    public static Map<String, String> getAllSettings() throws SQLException {
        Map<String, String> settings = new HashMap<>();
        String query = "SELECT setting_key, setting_value FROM app_settings";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return settings;
    }

    public static String getSetting(String key) throws SQLException {
        String query = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        }
        return null;
    }

    public static boolean updateSetting(String key, String value) throws SQLException {
        String query = "UPDATE app_settings SET setting_value = ? WHERE setting_key = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, value);
            ps.setString(2, key);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }
}