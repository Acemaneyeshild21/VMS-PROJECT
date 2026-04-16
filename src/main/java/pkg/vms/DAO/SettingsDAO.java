package pkg.vms.DAO;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class SettingsDAO {

    /**
     * Récupère un paramètre applicatif par sa clé
     */
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

    /**
     * Met à jour un paramètre applicatif par sa clé (UPSERT)
     */
    public static boolean updateSetting(String key, String value) throws SQLException {
        // UPSERT : INSERT ... ON CONFLICT UPDATE
        String query = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON CONFLICT (setting_key) DO UPDATE SET setting_value = ?, date_modification = CURRENT_TIMESTAMP";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setString(3, value);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditDAO.logSimple("app_settings", -1, "MODIFICATION", -1, "Parametre '" + key + "' mis a jour");
                return true;
            }
        }
        return false;
    }

    public static class EmailSettings {
        public String smtpServer;
        public int smtpPort;
        public String smtpUsername;
        public String smtpPassword;
        public boolean tlsEnabled;
        public String fromEmail;
        public String fromName;
        public String adminEmail;

        public EmailSettings(String smtpServer, int smtpPort, String smtpUsername,
                             String smtpPassword, boolean tlsEnabled, String fromEmail,
                             String fromName, String adminEmail) {
            this.smtpServer = smtpServer;
            this.smtpPort = smtpPort;
            this.smtpUsername = smtpUsername;
            this.smtpPassword = smtpPassword;
            this.tlsEnabled = tlsEnabled;
            this.fromEmail = fromEmail;
            this.fromName = fromName;
            this.adminEmail = adminEmail;
        }
    }

    /**
     * Recupere les parametres email de la BD
     */
    public static EmailSettings getEmailSettings() throws SQLException {
        String query = "SELECT smtp_server, smtp_port, smtp_username, smtp_password, tls_enabled, from_email, from_name, admin_email " +
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
                        rs.getString("from_email"),
                        rs.getString("from_name"),
                        rs.getString("admin_email")
                );
            }
        }
        return null;
    }

    /**
     * Met a jour les parametres email
     */
    public static boolean updateEmailSettings(EmailSettings settings) throws SQLException {
        String query = "UPDATE app_settings SET smtp_server = ?, smtp_port = ?, smtp_username = ?, " +
                "smtp_password = ?, tls_enabled = ?, from_email = ?, from_name = ?, admin_email = ? " +
                "WHERE setting_key = 'email'";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, settings.smtpServer);
            ps.setInt(2, settings.smtpPort);
            ps.setString(3, settings.smtpUsername);
            ps.setString(4, settings.smtpPassword);
            ps.setBoolean(5, settings.tlsEnabled);
            ps.setString(6, settings.fromEmail);
            ps.setString(7, settings.fromName);
            ps.setString(8, settings.adminEmail);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("app_settings", -1, "UPDATE_EMAIL", -1, "Parametres email mis a jour");
                return true;
            }
        }
        return false;
    }

    /**
     * Teste la connexion SMTP
     */
    public static boolean testSmtpConnection(EmailSettings settings) {
        try {
            java.util.Properties props = new java.util.Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", settings.tlsEnabled ? "true" : "false");
            props.put("mail.smtp.host", settings.smtpServer);
            props.put("mail.smtp.port", settings.smtpPort);
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");

            jakarta.mail.Session session = jakarta.mail.Session.getInstance(props, new jakarta.mail.Authenticator() {
                @Override
                protected jakarta.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new jakarta.mail.PasswordAuthentication(settings.smtpUsername, settings.smtpPassword);
                }
            });

            jakarta.mail.Transport transport = session.getTransport("smtp");
            transport.connect(settings.smtpServer, settings.smtpPort, settings.smtpUsername, settings.smtpPassword);
            transport.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Recupere tous les parametres
     */
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
}