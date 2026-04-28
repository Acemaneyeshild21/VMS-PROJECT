package pkg.vms.DAO;

import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Authentification et gestion du verrouillage de compte.
 *
 * <h3>Fix B — Lockout après 5 tentatives échouées</h3>
 * <ul>
 *   <li>La colonne {@code tentatives_echec} est incrémentée à chaque mauvais mot de passe.</li>
 *   <li>À la 5ᵉ tentative, {@code verrouille_jusqua} est positionné à {@code NOW() + 15 min}.</li>
 *   <li>Tant que ce timestamp est dans le futur, toute tentative est rejetée immédiatement
 *       avec un message indiquant l'heure de déverrouillage.</li>
 *   <li>Après une connexion réussie, le compteur et le verrou sont remis à zéro.</li>
 * </ul>
 *
 * <p>Les colonnes sont ajoutées via {@code ADD COLUMN IF NOT EXISTS} dans schema.sql —
 * migration douce, aucun risque sur une base existante.</p>
 */
public class AuthDAO {

    /** Nombre de tentatives consécutives avant verrouillage. */
    private static final int MAX_TENTATIVES    = 5;
    /** Durée du verrou en minutes. */
    private static final int LOCK_DURATION_MIN = 15;

    // ── Modèle session ────────────────────────────────────────────────────────

    public static class UserSession {
        public int    userId;
        public String username;
        public String role;
        public String email;

        public UserSession(int userId, String username, String role, String email) {
            this.userId   = userId;
            this.username = username;
            this.role     = role;
            this.email    = email;
        }
    }

    // ── Exception spécialisée pour le verrouillage ───────────────────────────

    /**
     * Lancée quand un compte est temporairement verrouillé ou définitivement désactivé.
     * Le message est directement affichable dans LoginForm.
     */
    public static class CompteBloqueException extends Exception {
        public final LocalDateTime deblocageAt;

        /** Constructeur standard : compte verrouillé temporairement. */
        public CompteBloqueException(Timestamp verrouille) {
            super(buildMessage(verrouille));
            this.deblocageAt = verrouille.toLocalDateTime();
        }

        /** Constructeur pour compte désactivé (pas de déverrouillage automatique). */
        public CompteBloqueException(String message) {
            super(message);
            this.deblocageAt = null;
        }

        private static String buildMessage(Timestamp ts) {
            String heure = ts.toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("HH:mm"));
            return "Compte temporairement bloqué après " + MAX_TENTATIVES +
                   " tentatives échouées.\nRéessayez après " + heure + ".";
        }
    }

    // ── Authentification principale ───────────────────────────────────────────

    /**
     * Tente d'authentifier un utilisateur par son nom d'utilisateur et mot de passe.
     *
     * @return La session si succès, {@code null} si mot de passe incorrect.
     * @throws CompteBloqueException si le compte est verrouillé ou désactivé.
     * @throws SQLException          en cas d'erreur base de données.
     */
    public static UserSession authenticate(String username, String password)
            throws SQLException, CompteBloqueException {

        String query = """
            SELECT userid, username, role, email, password,
                   tentatives_echec, verrouille_jusqua, actif
              FROM utilisateur
             WHERE username = ?
            """;

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    // Utilisateur inconnu — log sans incrémenter (pas de compte à bloquer)
                    AuditDAO.logSimple("utilisateur", -1, "CONNEXION_ECHOUEE", -1,
                            "Utilisateur inconnu : " + username);
                    return null;
                }

                int       userId         = rs.getInt("userid");
                String    storedPassword = rs.getString("password");
                int       tentatives     = rs.getInt("tentatives_echec");
                Timestamp verrouille     = rs.getTimestamp("verrouille_jusqua");
                boolean   actif          = rs.getBoolean("actif");

                // ── Compte désactivé ─────────────────────────────────────────
                if (!actif) {
                    AuditDAO.logSimple("utilisateur", userId, "CONNEXION_ECHOUEE", userId,
                            "Tentative sur compte désactivé : " + username);
                    throw new CompteBloqueException("Ce compte a été désactivé.\nContactez votre administrateur.");
                }

                // ── Fix B : verrou temporaire encore actif ? ─────────────────
                if (verrouille != null && verrouille.after(new java.util.Date())) {
                    AuditDAO.logSimple("utilisateur", userId, "CONNEXION_BLOQUEE", userId,
                            "Tentative bloquée — compte verrouillé jusqu'à " + verrouille);
                    throw new CompteBloqueException(verrouille);
                }

                // ── Vérification du mot de passe ─────────────────────────────
                boolean match = checkPassword(password, storedPassword);

                if (!match) {
                    int nouvellesTentatives = tentatives + 1;
                    enregistrerEchec(conn, userId, nouvellesTentatives);
                    AuditDAO.logSimple("utilisateur", userId, "CONNEXION_ECHOUEE", userId,
                            "Mot de passe incorrect — tentative " +
                            nouvellesTentatives + "/" + MAX_TENTATIVES);
                    return null;
                }

                // ── Connexion réussie ─────────────────────────────────────────
                reinitialiserVerrou(conn, userId);

                // Migration auto clair → BCrypt (anciens comptes)
                if (!storedPassword.startsWith("$2")) {
                    migrerVersBCrypt(userId, password);
                }

                UserSession session = new UserSession(
                        userId,
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("email")
                );
                AuditDAO.logSimple("utilisateur", userId, "CONNEXION", userId,
                        "Connexion réussie — " + username);
                return session;
            }
        }
    }

    // ── Helpers privés ───────────────────────────────────────────────────────

    private static boolean checkPassword(String plain, String stored) {
        // BCrypt (nouveau) — ou clair (héritage migration)
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            return BCrypt.checkpw(plain, stored);
        }
        return stored.equals(plain);
    }

    /**
     * Incrémente {@code tentatives_echec}.
     * Verrouille le compte si le seuil {@link #MAX_TENTATIVES} est atteint.
     */
    private static void enregistrerEchec(Connection conn, int userId, int nouvellesTentatives)
            throws SQLException {
        String sql;
        if (nouvellesTentatives >= MAX_TENTATIVES) {
            sql = """
                UPDATE utilisateur
                   SET tentatives_echec  = ?,
                       verrouille_jusqua = NOW() + INTERVAL '%d minutes'
                 WHERE userid = ?
                """.formatted(LOCK_DURATION_MIN);
            AuditDAO.logSimple("utilisateur", userId, "COMPTE_VERROUILLE", userId,
                    "Verrouillage " + LOCK_DURATION_MIN + " min après " +
                    MAX_TENTATIVES + " tentatives consécutives");
        } else {
            sql = "UPDATE utilisateur SET tentatives_echec = ? WHERE userid = ?";
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nouvellesTentatives);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Réinitialise le compteur et annule le verrou après connexion réussie. */
    private static void reinitialiserVerrou(Connection conn, int userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE utilisateur SET tentatives_echec = 0, verrouille_jusqua = NULL WHERE userid = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    /** Migration silencieuse : hash BCrypt pour les mots de passe stockés en clair. */
    private static void migrerVersBCrypt(int userId, String clearPassword) {
        String hashed = BCrypt.hashpw(clearPassword, BCrypt.gensalt());
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE utilisateur SET password = ? WHERE userid = ?")) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            ps.executeUpdate();
            System.out.println("[AuthDAO] Migration BCrypt effectuée pour userid=" + userId);
        } catch (SQLException e) {
            System.err.println("[AuthDAO] Erreur migration BCrypt : " + e.getMessage());
        }
    }

    // ── Actions admin ────────────────────────────────────────────────────────

    /**
     * Déverrouille manuellement un compte depuis ParametresPanel (admin uniquement).
     *
     * @param userId  ID du compte à déverrouiller
     * @param adminId ID de l'administrateur qui effectue l'action
     */
    public static void deverrouillerCompte(int userId, int adminId) throws SQLException {
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE utilisateur SET tentatives_echec = 0, verrouille_jusqua = NULL WHERE userid = ?")) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
        AuditDAO.logSimple("utilisateur", userId, "DEVERROUILLAGE", adminId,
                "Compte déverrouillé manuellement par admin #" + adminId);
    }

    /**
     * Recherche un utilisateur par son email.
     * Utilisé dans le flux de réinitialisation de mot de passe (ResetPasswordDialog).
     *
     * @return UserSession si trouvé, {@code null} sinon
     */
    public static UserSession findByEmail(String email) throws SQLException {
        String query = "SELECT userid, username, role, email FROM utilisateur WHERE email = ? AND actif = TRUE";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UserSession(
                        rs.getInt("userid"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getString("email")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Réinitialise directement le mot de passe d'un utilisateur (flux OTP reset).
     * Ne demande PAS l'ancien mot de passe — à appeler uniquement après validation OTP.
     *
     * @return {@code true} si la mise à jour a réussi
     */
    public static boolean updatePassword(int userId, String newPassword) throws SQLException {
        String hashed = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE utilisateur SET password = ? WHERE userid = ?")) {
            ps.setString(1, hashed);
            ps.setInt(2, userId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                AuditDAO.logSimple("utilisateur", userId, "RESET_PASSWORD", userId,
                        "Mot de passe réinitialisé via OTP");
                return true;
            }
        }
        return false;
    }

    /**
     * Retourne le nombre de tentatives échouées en cours pour un utilisateur.
     * Utilisé dans la vue de gestion des utilisateurs (ParametresPanel).
     */
    public static int getTentativesEchec(int userId) throws SQLException {
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT tentatives_echec FROM utilisateur WHERE userid = ?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}
