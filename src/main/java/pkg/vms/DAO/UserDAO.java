package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public static boolean registerUser(String username, String email, String password, String role) throws SQLException {
        String query = "INSERT INTO utilisateur (username, email, password, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.setString(4, role);
            return ps.executeUpdate() > 0;
        }
    }

    public static List<String> getAllRoles() throws SQLException {
        List<String> roles = new ArrayList<>();
        String query = "SELECT DISTINCT role FROM utilisateur";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                roles.add(rs.getString("role"));
            }
        }
        return roles;
    }
}
