package pkg.vms;

import pkg.vms.DAO.DBconnect;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;

/**
 * Panneau de tableau de bord avec statistiques et activité récente
 */
public class StatisticsPanel extends JPanel {

    private static final Color BG_ROOT = VMSStyle.BG_ROOT;
    private static final Color BG_CARD = VMSStyle.BG_CARD;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED = VMSStyle.TEXT_MUTED;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;

    public StatisticsPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        // ═══ TITRE ═══
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel("📊");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("  Tableau de Bord");
        title.setFont(VMSStyle.FONT_BRAND.deriveFont(26f));
        title.setForeground(TEXT_PRIMARY);

        titleRow.add(icon);
        titleRow.add(title);
        content.add(titleRow);
        content.add(Box.createVerticalStrut(8));

        JLabel subtitle = new JLabel("Statistiques en temps réel et activité récente");
        subtitle.setFont(VMSStyle.FONT_NAV.deriveFont(14f));
        subtitle.setForeground(TEXT_SECOND);
        subtitle.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(subtitle);
        content.add(Box.createVerticalStrut(28));

        // ═══ CARTES DE STATISTIQUES ═══
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 16, 16));
        statsPanel.setOpaque(false);
        statsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        statsPanel.add(buildStatCard("📋", "Total Demandes", loadTotalDemandes(), new Color(59, 130, 246)));
        statsPanel.add(buildStatCard("🎁", "Bons Générés", loadTotalBons(), new Color(16, 185, 129)));
        statsPanel.add(buildStatCard("✅", "Bons Rédemptés", loadBonsRedimes(), new Color(139, 92, 246)));
        statsPanel.add(buildStatCard("💰", "Montant Total", loadMontantTotal(), new Color(245, 158, 11)));
        statsPanel.add(buildStatCard("⏰", "Bons Expirés", loadBonsExpires(), new Color(239, 68, 68)));
        statsPanel.add(buildStatCard("👥", "Clients Actifs", loadClientsActifs(), new Color(6, 182, 212)));

        content.add(statsPanel);
        content.add(Box.createVerticalStrut(28));

        // ═══ TABLEAU ACTIVITÉ RÉCENTE ══��
        JLabel activityLabel = new JLabel("Activité Récente");
        activityLabel.setFont(VMSStyle.FONT_BRAND.deriveFont(14f));
        activityLabel.setForeground(TEXT_PRIMARY);
        activityLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        activityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(activityLabel);

        JPanel tablePanel = buildActivityTable();
        tablePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tablePanel);

        // ═══ SCROLL ═══
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Construit une carte de statistique
     */
    private JPanel buildStatCard(String emoji, String label, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Emoji
        JLabel emojiL = new JLabel(emoji);
        emojiL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        emojiL.setHorizontalAlignment(SwingConstants.CENTER);
        emojiL.setPreferredSize(new Dimension(48, 48));
        card.add(emojiL, BorderLayout.WEST);

        // Texte (label + value)
        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel labelL = new JLabel(label);
        labelL.setFont(VMSStyle.FONT_NAV.deriveFont(12f));
        labelL.setForeground(TEXT_MUTED);
        labelL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueL = new JLabel(value);
        valueL.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueL.setForeground(color);
        valueL.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(labelL);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(valueL);

        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    /**
     * Construit le tableau d'activité récente
     */
    private JPanel buildActivityTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        String[] cols = {"Date/Heure", "Action", "Description", "Utilisateur"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        // Charger les données en arrière-plan
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try (Connection conn = DBconnect.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                             "SELECT date_action, action, contexte, username FROM audit_log ORDER BY date_action DESC LIMIT 15")) {

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("date_action");
                        String dateStr = ts != null ? sdf.format(ts) : "";
                        String username = rs.getString("username") != null ? rs.getString("username") : "—";

                        model.addRow(new Object[]{
                                dateStr,
                                rs.getString("action"),
                                rs.getString("contexte"),
                                username
                        });
                    }
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement activité: " + e.getMessage());
                }
                return null;
            }
        }.execute();

        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(28);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));
        table.getTableHeader().setForeground(TEXT_SECOND);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));

        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    // ═══ MÉTHODES DE CHARGEMENT DES DONNÉES ═══

    private String loadTotalDemandes() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM demande")) {
            if (rs.next()) {
                return String.valueOf(rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    private String loadTotalBons() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM bon")) {
            if (rs.next()) {
                return String.valueOf(rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    private String loadBonsRedimes() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM bon WHERE statut = 'REDIME'")) {
            if (rs.next()) {
                return String.valueOf(rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    private String loadMontantTotal() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(SUM(montant_total), 0) as total FROM demande")) {
            if (rs.next()) {
                double total = rs.getDouble("total");
                return "Rs " + String.format("%,.2f", total);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Rs 0";
    }

    private String loadBonsExpires() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM bon WHERE date_expiration < CURRENT_TIMESTAMP AND statut != 'REDIME'")) {
            if (rs.next()) {
                return String.valueOf(rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }

    private String loadClientsActifs() {
        try (Connection conn = DBconnect.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) as count FROM client WHERE actif = true")) {
            if (rs.next()) {
                return String.valueOf(rs.getInt("count"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0";
    }
}