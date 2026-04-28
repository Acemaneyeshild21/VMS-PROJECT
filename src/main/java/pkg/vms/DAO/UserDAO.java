package pkg.vms.DAO;

import org.mindrot.jbcrypt.BCrypt;
import pkg.vms.Roles;

import java.sql.*;
import java.util.*;

public class UserDAO {

    // ── Fix A : ensemble des rôles valides défini une seule fois depuis Roles.java ──
    // Toute valeur hors de cet ensemble est rejetée AVANT d'atteindre la base,
    // ce qui garantit la cohérence même si la contrainte CHECK SQL est désactivée.
    private static final Set<String> ROLES_VALIDES = Set.of(
        Roles.ADMIN_SIEGE,
        Roles.COMPTABLE,
        Roles.APPROBATEUR,
        Roles.SUPERVISEUR_MAGASIN,
        Roles.MANAGER,
        Roles.COLLABORATEUR
    );

    /** Vérifie qu'un rôle est dans la liste blanche. Lève IllegalArgumentException si invalide. */
    private static void validerRole(String role) {
        if (role == null || !ROLES_VALIDES.contains(role)) {
            throw new IllegalArgumentException(
                "Rôle invalide : \"" + role + "\". Valeurs acceptées : " + ROLES_VALIDES
            );
        }
    }

    // ==================== REGISTRATION ====================
    public static boolean registerUser(String username, String email, String password, String role) throws SQLException {
        validerRole(role); // Fix A — rejet avant INSERT
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

    /**
     * Réinitialise le mot de passe directement (flux "mot de passe oublié" avec OTP validé).
     * Ne vérifie PAS l'ancien mot de passe — appeler SEULEMENT après validation OTP.
     */
    public static boolean resetPasswordDirect(int userId, String newPassword) throws SQLException {
        String hash = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql  = "UPDATE utilisateur SET password = ?, tentatives_echec = 0, "
                    + "verrouille_jusqua = NULL WHERE userid = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "RESET_PASSWORD_OK", userId,
                        "Mot de passe réinitialisé via OTP");
                return true;
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
        validerRole(newRole); // Fix A — rejet avant UPDATE
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
    /**
     * Fix C — Retourne la liste des rôles depuis les constantes {@link Roles},
     * et non depuis la base de données.
     *
     * <p>L'ancienne version faisait un {@code SELECT DISTINCT role FROM utilisateur},
     * ce qui permettait à n'importe qui ayant un accès DB direct d'insérer un rôle
     * fictif et de le voir apparaître dans la liste. Désormais la liste est figée
     * par le code source et correspond exactement à la liste blanche {@code ROLES_VALIDES}.</p>
     */
    public static List<String> getAllRoles() {
        // Ordre d'affichage UX : du plus privilégié au moins privilégié
        return List.of(
            Roles.ADMIN_SIEGE,
            Roles.MANAGER,
            Roles.COMPTABLE,
            Roles.APPROBATEUR,
            Roles.SUPERVISEUR_MAGASIN,
            Roles.COLLABORATEUR
        );
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