package pkg.vms.DAO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    public static class ClientInfo {
        public int id;
        public String name;
        public String email;

        public ClientInfo(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }

        @Override
        public String toString() { return name + " (" + email + ")"; }
    }

    public static class MagasinInfo {
        public int id;
        public String nom;

        public MagasinInfo(int id, String nom) {
            this.id = id;
            this.nom = nom;
        }

        @Override
        public String toString() { return nom; }
    }

    public static List<ClientInfo> getActiveClients() throws SQLException {
        List<ClientInfo> clients = new ArrayList<>();
        String query = "SELECT clientid, name, email FROM client WHERE actif = true ORDER BY name ASC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                clients.add(new ClientInfo(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
        }
        return clients;
    }

    public static List<MagasinInfo> getAllMagasins() throws SQLException {
        List<MagasinInfo> magasins = new ArrayList<>();
        String query = "SELECT magasin_id, nom_magasin FROM magasin ORDER BY nom_magasin ASC";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                magasins.add(new MagasinInfo(rs.getInt(1), rs.getString(2)));
            }
        }
        return magasins;
    }
}
