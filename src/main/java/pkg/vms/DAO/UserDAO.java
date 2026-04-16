package pkg.vms.DAO;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.*;

public class UserDAO {

    // ==================== REGISTRATION ====================
    public static boolean registerUser(String username, String email, String password, String role) throws SQLException {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = "INSERT INTO utilisateur (username, email, password, role) VALUES (?, ?, ?, ?) RETURNING userid";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hashedPassword);
            ps.setString(4, role);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int newUserId = rs.getInt(1);
                    AuditDAO.logSimple("utilisateur", newUserId, "INSCRIPTION", -1, "Nouvel utilisateur: " + username);
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== PASSWORD MANAGEMENT ====================
    public static boolean updatePassword(int userId, String oldPassword, String newPassword) throws SQLException {
        String query = "SELECT password FROM utilisateur WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");

                    // Vérifier l'ancien mot de passe
                    boolean passwordMatch = false;
                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                        passwordMatch = BCrypt.checkpw(oldPassword, storedPassword);
                    } else {
                        passwordMatch = storedPassword.equals(oldPassword);
                    }

                    if (passwordMatch) {
                        String hashedNewPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
                        String updateQuery = "UPDATE utilisateur SET password = ? WHERE userid = ?";
                        try (PreparedStatement updatePs = conn.prepareStatement(updateQuery)) {
                            updatePs.setString(1, hashedNewPassword);
                            updatePs.setInt(2, userId);
                            updatePs.executeUpdate();
                            AuditDAO.logSimple("utilisateur", userId, "CHANGE_PASSWORD", userId, "Mot de passe changé");
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // ==================== PROFILE MANAGEMENT ====================
    public static UserProfile getUserProfile(int userId) throws SQLException {
        String query = "SELECT userid, username, email, role FROM utilisateur WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserProfile(
                            rs.getInt("userid"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role")
                    );
                }
            }
        }
        return null;
    }

    public static boolean updateProfile(int userId, String username, String email) throws SQLException {
        String query = "UPDATE utilisateur SET username = ?, email = ? WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setInt(3, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "UPDATE_PROFILE", userId,
                        "Profil mis à jour: username=" + username + ", email=" + email);
                return true;
            }
        }
        return false;
    }

    // ==================== USER MANAGEMENT (ADMIN) ====================
    public static List<UserProfile> getAllUsers() throws SQLException {
        List<UserProfile> users = new ArrayList<>();
        String query = "SELECT userid, username, email, role FROM utilisateur ORDER BY userid";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(new UserProfile(
                        rs.getInt("userid"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role")
                ));
            }
        }
        return users;
    }

    public static boolean deleteUser(int userId) throws SQLException {
        String query = "DELETE FROM utilisateur WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "DELETE", -1, "Utilisateur supprimé");
                return true;
            }
        }
        return false;
    }

    public static boolean updateUserRole(int userId, String newRole) throws SQLException {
        String query = "UPDATE utilisateur SET role = ? WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, newRole);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();

            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "UPDATE_ROLE", -1, "Rôle changé en: " + newRole);
                return true;
            }
        }
        return false;
    }

    // ==================== ROLES ====================
    public static List<String> getAllRoles() throws SQLException {
        List<String> roles = new ArrayList<>();
        String query = "SELECT DISTINCT role FROM utilisateur WHERE role IS NOT NULL";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                roles.add(rs.getString("role"));
            }
        }
        return roles;
    }

    // ==================== MODEL ====================
    public static class UserProfile {
        public int userId;
        public String username;
        public String email;
        public String role;

        public UserProfile(int userId, String username, String email, String role) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.role = role;
        }
    }
}