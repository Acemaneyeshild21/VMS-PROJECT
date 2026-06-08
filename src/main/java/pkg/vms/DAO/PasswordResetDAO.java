package pkg.vms.DAO;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.sql.*;

/**
 * DAO pour la r\u00e9initialisation de mot de passe par OTP (code \u00e0 6 chiffres).
 *
 * Flux :
 *   1. {@link #createResetCode(int, String)} g\u00e9n\u00e8re un code al\u00e9atoire 6 chiffres,
 *      le hash via BCrypt, le stocke en BD avec expiration 15 min, et retourne le code en clair
 *      (pour envoi par email).
 *   2. {@link #validateCode(int, String)} v\u00e9rifie qu'un code est valide (non expir\u00e9, non utilis\u00e9,
 *      tentatives < 3). Incr\u00e9mente le compteur de tentatives en cas d'\u00e9chec.
 *   3. {@link #markUsed(int)} invalide le code apr\u00e8s usage r\u00e9ussi.
 *   4. {@link #cleanupExpired()} purge les codes expir\u00e9s (appel\u00e9 p\u00e9riodiquement).
 *
 * S\u00e9curit\u00e9 :
 *   - Code BCrypt\u00e9 en BD (jamais en clair)
 *   - Expiration courte (15 min)
 *   - Max 3 tentatives avant blocage
 *   - Usage unique (flag {@code used})
 */
public class PasswordResetDAO {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 15;
    public static final int MAX_ATTEMPTS = 3;

    /** R\u00e9sultat d'une validation de code. */
    public static class ValidationResult {
        public boolean success;
        public int     resetId;         // si success, l'id \u00e0 passer \u00e0 markUsed
        public String  errorMessage;    // si \u00e9chec, message explicatif
        public int     tentativesRestantes;

        public static ValidationResult ok(int resetId) {
            ValidationResult v = new ValidationResult();
            v.success = true;
            v.resetId = resetId;
            return v;
        }
        public static ValidationResult error(String msg) {
            ValidationResult v = new ValidationResult();
            v.success = false;
            v.errorMessage = msg;
            return v;
        }
        public static ValidationResult wrongCode(int tentativesRestantes) {
            ValidationResult v = new ValidationResult();
            v.success = false;
            v.tentativesRestantes = tentativesRestantes;
            v.errorMessage = tentativesRestantes > 0
                    ? "Code incorrect. " + tentativesRestantes + " tentative(s) restante(s)."
                    : "Trop de tentatives. Demandez un nouveau code.";
            return v;
        }
    }

    /**
     * G\u00e9n\u00e8re un code \u00e0 6 chiffres, l'enregistre (hash BCrypt) et retourne le code en clair.
     * Invalide les pr\u00e9c\u00e9dentes demandes non utilis\u00e9es de cet utilisateur pour \u00e9viter les doublons.
     */
    public static String createResetCode(int userId, String ipAddress) throws SQLException {
        // G\u00e9n\u00e9ration du code
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(RNG.nextInt(10));
        }
        String code = sb.toString();
        String hash = BCrypt.hashpw(code, BCrypt.gensalt(10));

        try (Connection conn = DBconnect.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Invalider les pr\u00e9c\u00e9dentes demandes non-utilis\u00e9es de cet utilisateur
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE password_reset SET used = TRUE WHERE user_id = ? AND used = FALSE")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                // 2. Ins\u00e9rer la nouvelle demande
                String sql = "INSERT INTO password_reset (user_id, code_hash, expires_at, ip_address) " +
                             "VALUES (?, ?, CURRENT_TIMESTAMP + INTERVAL '" + EXPIRATION_MINUTES + " minutes', ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setString(2, hash);
                    ps.setString(3, ipAddress);
                    ps.executeUpdate();
                }

                conn.commit();
                AuditDAO.logSimple("utilisateur", userId, "RESET_PASSWORD_DEMANDE", userId,
                        "Code OTP g\u00e9n\u00e9r\u00e9 (expire dans " + EXPIRATION_MINUTES + " min)");
                return code;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * V\u00e9rifie un code fourni par l'utilisateur contre le dernier code non-utilis\u00e9 et non-expir\u00e9.
     * Incr\u00e9mente {@code tentatives} en cas d'\u00e9chec. Bloque si {@code tentatives >= MAX_ATTEMPTS}.
     */
    public static ValidationResult validateCode(int userId, String code) throws SQLException {
        String sql = "SELECT reset_id, code_hash, expires_at, used, tentatives " +
                     "FROM password_reset " +
                     "WHERE user_id = ? " +
                     "  AND used = FALSE " +
                     "ORDER BY date_demande DESC LIMIT 1";

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return ValidationResult.error("Aucun code actif. Demandez un nouveau code.");
                }
                int resetId = rs.getInt("reset_id");
                String hash = rs.getString("code_hash");
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                int tentatives = rs.getInt("tentatives");

                // V\u00e9rifier expiration
                if (expiresAt.before(new Timestamp(System.currentTimeMillis()))) {
                    return ValidationResult.error("Ce code a expir\u00e9. Demandez un nouveau code.");
                }

                // V\u00e9rifier blocage pour abus
                if (tentatives >= MAX_ATTEMPTS) {
                    return ValidationResult.error("Trop de tentatives \u00e9chou\u00e9es. Demandez un nouveau code.");
                }

                // V\u00e9rifier le code
                if (BCrypt.checkpw(code, hash)) {
                    return ValidationResult.ok(resetId);
                } else {
                    // Incr\u00e9menter les tentatives
                    try (PreparedStatement up = conn.prepareStatement(
                            "UPDATE password_reset SET tentatives = tentatives + 1 WHERE reset_id = ?")) {
                        up.setInt(1, resetId);
                        up.executeUpdate();
                    }
                    int restantes = MAX_ATTEMPTS - (tentatives + 1);
                    AuditDAO.logSimple("utilisateur", userId, "RESET_PASSWORD_ECHEC", userId,
                            "Code incorrect (tentative " + (tentatives + 1) + "/" + MAX_ATTEMPTS + ")");
                    return ValidationResult.wrongCode(restantes);
                }
            }
        }
    }

    /** Marque un code comme utilis\u00e9 (apr\u00e8s changement de mot de passe r\u00e9ussi). */
    public static void markUsed(int resetId) throws SQLException {
        String sql = "UPDATE password_reset SET used = TRUE WHERE reset_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, resetId);
            ps.executeUpdate();
        }
    }

    /** Purge les codes expir\u00e9s depuis plus de 24h (appel optionnel, p.ex. au d\u00e9marrage). */
    public static int cleanupExpired() throws SQLException {
        String sql = "DELETE FROM password_reset " +
                     "WHERE expires_at < CURRENT_TIMESTAMP - INTERVAL '24 hours'";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    /** Cr\u00e9e la table si elle n'existe pas (pour d\u00e9ploiement \u00e0 chaud). */
    public static void ensureSchema() {
        String ddl =
            "CREATE TABLE IF NOT EXISTS password_reset (" +
            "  reset_id     SERIAL PRIMARY KEY," +
            "  user_id      INT NOT NULL," +
            "  code_hash    VARCHAR(255) NOT NULL," +
            "  expires_at   TIMESTAMP NOT NULL," +
            "  used         BOOLEAN DEFAULT FALSE," +
            "  tentatives   INT DEFAULT 0," +
            "  date_demande TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "  ip_address   VARCHAR(45)" +
            ")";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement()) {
            st.execute(ddl);
            st.execute("CREATE INDEX IF NOT EXISTS idx_pwdreset_user ON password_reset(user_id, used, expires_at)");
        } catch (SQLException e) {
            System.err.println("[PasswordResetDAO] ensureSchema : " + e.getMessage());
        }
    }
}
