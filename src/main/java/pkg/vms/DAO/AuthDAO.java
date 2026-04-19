package pkg.vms.DAO;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthDAO {

    public static class UserSession {
        public int userId;
        public String username;
        public String role;
        public String email;

        public UserSession(int userId, String username, String role, String email) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.email = email;
        }
    }

    public static UserSession authenticate(String username, String password) throws SQLException {
        String query = "SELECT userid, username, role, email, password FROM utilisateur WHERE username = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    
                    // Vérification du mot de passe (gestion temporaire du clair pour migration)
                    boolean passwordMatch = false;
                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                        passwordMatch = BCrypt.checkpw(password, storedPassword);
                    } else {
                        passwordMatch = storedPassword.equals(password);
                    }

                    if (passwordMatch) {
                        UserSession session = new UserSession(
                            rs.getInt("userid"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("email")
                        );
                        
                        // Si le mot de passe était en clair, on le hash automatiquement à la prochaine connexion
                        if (!storedPassword.startsWith("$2")) {
                            updateToHashedPassword(session.userId, password);
                        }

                        AuditDAO.logSimple("utilisateur", session.userId, "CONNEXION", session.userId, "Utilisateur connecté");
                        return session;
                    } else {
                        AuditDAO.logSimple("utilisateur", -1, "CONNEXION_ECHOUEE", -1, "Tentative échouée pour : " + username);
                    }
                }
            }
        }
        return null;
    }

    private static void updateToHashedPassword(int userId, String clearPassword) {
        String hashed = BCrypt.hashpw(clearPassword, BCrypt.gensalt());
        String sql = "UPDATE utilisateur SET password = ? WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("Auto-migration du mot de passe vers BCrypt effectuée pour l'utilisateur ID: " + userId);
        } catch (SQLException e) {
            System.err.println("Erreur migration mot de passe : " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESET MOT DE PASSE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Recherche un utilisateur par email (pour le flux de réinitialisation).
     * Retourne null si aucun compte ne correspond OU si le compte est inactif.
     */
    public static UserSession findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) return null;
        String sql = "SELECT userid, username, role, email FROM utilisateur " +
                     "WHERE LOWER(email) = LOWER(?) AND actif = TRUE";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserSession(
                            rs.getInt("userid"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getString("email"));
                }
            }
        }
        return null;
    }

    /**
     * Met à jour le mot de passe d'un utilisateur (BCrypt).
     * Utilisé par le flux de réinitialisation après validation du code OTP.
     * Log audit RESET_PASSWORD_SUCCES.
     */
    public static boolean updatePassword(int userId, String newPassword) throws SQLException {
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères");
        }
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        String sql = "UPDATE utilisateur SET password = ? WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "RESET_PASSWORD_SUCCES", userId,
                        "Mot de passe réinitialisé via code OTP");
                return true;
            }
        }
        return false;
    }
}
