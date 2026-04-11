package pkg.vms.DAO;

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
        String query = "SELECT userid, username, role, email FROM utilisateur WHERE username = ? AND password = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setString(1, username);
            ps.setString(2, password);
            
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
}
