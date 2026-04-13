package pkg.vms.DAO;

import pkg.vms.EmailService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VoucherDAO {

    public static class VoucherStats {
        public int activeClients;
        public int totalVouchers;
        public int pendingPayments;
        public int validationRate;
    }

    public static VoucherStats getDashboardStats() throws SQLException {
        VoucherStats stats = new VoucherStats();
        try (Connection conn = DBconnect.getConnection()) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM client WHERE actif = true")) {
                if (rs.next()) stats.activeClients = rs.getInt(1);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM demande")) {
                if (rs.next()) stats.totalVouchers = rs.getInt(1);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM demande WHERE statuts = 'EN_ATTENTE_PAIEMENT'")) {
                if (rs.next()) stats.pendingPayments = rs.getInt(1);
            }
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                         "SELECT COALESCE(ROUND(100.0 * SUM(CASE WHEN statuts IN ('APPROUVE','GENERE','PAYE','ENVOYE') THEN 1 ELSE 0 END) / NULLIF(COUNT(*),0)), 0) FROM demande")) {
                if (rs.next()) stats.validationRate = rs.getInt(1);
            }
        }
        return stats;
    }

    /**
     * Crée une demande avec génération automatique de référence (ex. VR0048-200)
     * et de la facture (ex. INV-VR0048-200).
     */
    public static int createVoucherRequest(int userId, int clientId, int nombreBons,
                                            double valeurUnitaire, String typeBon,
                                            int magasinId, int validiteJours,
                                            String motif, String emailDestinataire) throws SQLException {
        // Générer la référence automatique
        String ref = genererReference(nombreBons);
        String invoiceRef = "INV-" + ref;

        String query = "INSERT INTO demande (reference, invoice_reference, clientid, magasin_id, " +
                       "nombre_bons, valeur_unitaire, type_bon, validite_jours, motif, " +
                       "email_destinataire, statuts, cree_par) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'EN_ATTENTE_PAIEMENT', ?) RETURNING demande_id";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, ref);
            ps.setString(2, invoiceRef);
            ps.setInt(3, clientId);
            ps.setInt(4, magasinId);
            ps.setInt(5, nombreBons);
            ps.setDouble(6, valeurUnitaire);
            ps.setString(7, typeBon);
            ps.setInt(8, validiteJours);
            ps.setString(9, motif);
            ps.setString(10, emailDestinataire);
            ps.setInt(11, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    /**
     * Génère une référence au format VR0048-200 (numéro séquentiel + nombre de bons).
     */
    private static String genererReference(int nombreBons) throws SQLException {
        int seq = 1;
        String sql = "SELECT COALESCE(MAX(demande_id), 0) + 1 FROM demande";
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) seq = rs.getInt(1);
        }
        return String.format("VR%04d-%d", seq, nombreBons);
    }

    public static List<String[]> getVoucherRequests(String role, int userId) throws SQLException {
        List<String[]> data = new ArrayList<>();
        String query = "SELECT d.demande_id, d.reference, c.name, d.montant_total, d.type_bon, d.statuts, " +
                       "d.date_creation, d.invoice_reference, d.valeur_unitaire " +
                       "FROM demande d JOIN client c ON d.clientid = c.clientid ";

        if (!"Administrateur".equalsIgnoreCase(role) && !"Manager".equalsIgnoreCase(role)) {
            query += " WHERE d.cree_par = ?";
        }
        query += " ORDER BY d.date_creation DESC";

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            if (!"Administrateur".equalsIgnoreCase(role) && !"Manager".equalsIgnoreCase(role)) {
                ps.setInt(1, userId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.add(new String[]{
                        String.valueOf(rs.getInt("demande_id")),
                        rs.getString("reference"),
                        rs.getString("name"),
                        String.format("%.2f", rs.getDouble("montant_total")),
                        rs.getString("type_bon"),
                        rs.getString("statuts"),
                        rs.getTimestamp("date_creation") != null ? rs.getTimestamp("date_creation").toString() : "",
                        rs.getString("invoice_reference"),
                        String.valueOf(rs.getDouble("valeur_unitaire"))
                    });
                }
            }
        }
        return data;
    }

    /**
     * Met à jour le statut d'une demande avec traçabilité.
     */
    public static void updateVoucherStatus(int demandeId, String newStatus, int userId) throws SQLException {
        String query;
        boolean useUserId = false;
        
        switch (newStatus) {
            case "PAYE" -> {
                query = "UPDATE demande SET statuts = 'PAYE', paiement_valide = TRUE, " +
                        "date_paiement = CURRENT_TIMESTAMP, valide_par = ? WHERE demande_id = ?";
                useUserId = true;
            }
            case "APPROUVE" -> {
                query = "UPDATE demande SET statuts = 'APPROUVE', approuve = TRUE, " +
                        "date_approbation = CURRENT_TIMESTAMP, approuve_par = ? WHERE demande_id = ?";
                useUserId = true;
            }
            case "REJETE" -> {
                query = "UPDATE demande SET statuts = 'REJETE' WHERE demande_id = ?";
            }
            case "ARCHIVE" -> {
                query = "UPDATE demande SET statuts = 'ARCHIVE' WHERE demande_id = ?";
            }
            case "GENERE" -> {
                query = "UPDATE demande SET statuts = 'GENERE' WHERE demande_id = ?";
            }
            case "ENVOYE" -> {
                query = "UPDATE demande SET statuts = 'ENVOYE' WHERE demande_id = ?";
            }
            default -> {
                query = "UPDATE demande SET statuts = ? WHERE demande_id = ?";
            }
        }

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            if (useUserId) {
                ps.setInt(1, userId);
                ps.setInt(2, demandeId);
            } else if ("REJETE".equals(newStatus) || "ARCHIVE".equals(newStatus) || 
                       "GENERE".equals(newStatus) || "ENVOYE".equals(newStatus)) {
                ps.setInt(1, demandeId);
            } else {
                ps.setString(1, newStatus);
                ps.setInt(2, demandeId);
            }
            ps.executeUpdate();

            // Note : le trigger trg_demande_update enregistre automatiquement
            // l'audit lors du changement de statut — pas de log manuel ici.

            // Notification Email si nécessaire
            try {
                if ("APPROUVE".equals(newStatus) || "REJETE".equals(newStatus)) {
                    String destinataire = getEmailDestinataire(demandeId);
                    if (destinataire != null) {
                        String sujet = "Mise à jour de votre demande VMS — " + newStatus;
                        String corps = "Votre demande #" + demandeId + " a été changée vers le statut : " + newStatus;
                        EmailService.envoyerNotification(destinataire, sujet, corps);
                    }
                }
                
                // Notification à l'approbateur après paiement
                if ("PAYE".equals(newStatus)) {
                    // Pour le BTS, on peut notifier une adresse d'approbation centralisée ou via les rôles
                    EmailService.envoyerNotification(pkg.vms.Config.get("smtp.approver.email", "approbations@intermart.mu"), 
                        "Demande PAYÉE — En attente d'approbation", 
                        "La demande #" + demandeId + " vient d'être payée. Merci de procéder à l'approbation.");
                }
            } catch (Exception e) {
                System.err.println("Erreur notification email: " + e.getMessage());
            }
        }
    }

    private static String getEmailDestinataire(int demandeId) {
        String query = "SELECT email_destinataire FROM demande WHERE demande_id = ?";
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, demandeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("email_destinataire");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Archive les demandes dont les bons sont expirés.
     */
    public static int archiverDemandesExpirees(int userId) throws SQLException {
        String sql = "UPDATE demande SET statuts = 'ARCHIVE' " +
                     "WHERE statuts IN ('GENERE', 'ENVOYE') " +
                     "AND (date_creation + (validite_jours || ' days')::interval) < CURRENT_TIMESTAMP " +
                     "AND statuts != 'ARCHIVE'";
        
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement()) {
            int count = st.executeUpdate(sql);
            if (count > 0) {
                AuditDAO.logSimple("demande", -1, "ARCHIVAGE_MASSIF", userId, count + " demandes archivées automatiquement");
            }
            return count;
        }
    }

    /**
     * Met à jour le statut de la demande à ENVOYE après dispatch email.
     */
    public static void marquerCommeEnvoye(int demandeId, int userId) throws SQLException {
        updateVoucherStatus(demandeId, "ENVOYE", userId);
    }
}
