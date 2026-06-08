package pkg.vms;

import pkg.vms.DAO.EmailLogDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Historique des emails envoy\u00e9s par le syst\u00e8me (table email_log).
 * Admin uniquement : consultation, filtrage, et d\u00e9tail des erreurs SMTP.
 * Utile pour diagnostiquer les bons non re\u00e7us par les clients.
 */
public class EmailHistoryDialog extends JDialog {

    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH);

    private final DefaultTableModel model;
    private final JTable            table;
    private final JTextField        tfDest;
    private final JComboBox<String> cbStatut;
    private final JComboBox<String> cbFenetre;
    private final JLabel            lblEnvoyes, lblEchecs, lblSim, lblTotal;
    private List<EmailLogDAO.EmailEntry> currentEntries = new ArrayList<>();

    public EmailHistoryDialog(Window owner) {
        super(owner, "Historique emails", ModalityType.APPLICATION_MODAL);
        setSize(980, 620);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(VMSStyle.BG_ROOT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(VMSStyle.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(16, 22, 16, 22)));
        JPanel tb = new JPanel();
        tb.setOpaque(false);
        tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("\u2709\uFE0F  Historique emails");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Journal des envois SMTP : succ\u00e8s, \u00e9checs et simulations");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        tb.add(title);
        tb.add(Box.createVerticalStrut(3));
        tb.add(sub);
        header.add(tb, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(16, 22, 16, 22));

        // Stats ribbon
        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 0));
        stats.setOpaque(false);
        lblTotal   = new JLabel("0");
        lblEnvoyes = new JLabel("0");
        lblEchecs  = new JLabel("0");
        lblSim     = new JLabel("0");
        stats.add(makeStatCard("Total",       lblTotal,   VMSStyle.TEXT_PRIMARY));
        stats.add(makeStatCard("Envoy\u00e9s",    lblEnvoyes, VMSStyle.SUCCESS));
        stats.add(makeStatCard("\u00c9checs",     lblEchecs,  VMSStyle.RED_PRIMARY));
        stats.add(makeStatCard("Simulations", lblSim,     VMSStyle.ACCENT_BLUE));
        body.add(stats, BorderLayout.NORTH);

        // Filters + table wrapper
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        // Filters row
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.setBorder(new EmptyBorder(10, 0, 6, 0));

        tfDest = new JTextField(18);
        tfDest.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tfDest.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(6, 10, 6, 10)));
        tfDest.addActionListener(e -> refresh());

        cbStatut = new JComboBox<>(new String[]{"Tous", "ENVOYE", "ECHEC", "SIMULATION"});
        cbStatut.setPreferredSize(new Dimension(140, 30));
        cbStatut.addActionListener(e -> refresh());

        cbFenetre = new JComboBox<>(new String[]{"24 heures","7 jours","30 jours","Tout"});
        cbFenetre.setSelectedIndex(1);
        cbFenetre.setPreferredSize(new Dimension(130, 30));
        cbFenetre.addActionListener(e -> refresh());

        JButton btnRefresh = UIUtils.buildGhostButton("Actualiser", 110, 32);
        btnRefresh.addActionListener(e -> refresh());

        JButton btnDetail = UIUtils.buildGhostButton("Voir d\u00e9tail", 110, 32);
        btnDetail.addActionListener(e -> showSelectedDetail());

        filters.add(new JLabel("Destinataire :"));
        filters.add(tfDest);
        filters.add(Box.createHorizontalStrut(6));
        filters.add(new JLabel("Statut :"));
        filters.add(cbStatut);
        filters.add(Box.createHorizontalStrut(6));
        filters.add(new JLabel("P\u00e9riode :"));
        filters.add(cbFenetre);
        filters.add(Box.createHorizontalStrut(6));
        filters.add(btnRefresh);
        filters.add(btnDetail);
        center.add(filters, BorderLayout.NORTH);

        // Table
        String[] cols = {"Date", "Destinataire", "Sujet", "PJ", "Statut", "Demande"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 3) ? Integer.class : String.class;
            }
        };
        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(VMSStyle.BG_SUBTLE);
        table.getTableHeader().setReorderingAllowed(false);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // Widths
        table.getColumnModel().getColumn(0).setPreferredWidth(130);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(330);
        table.getColumnModel().getColumn(3).setPreferredWidth(40);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);

        table.getColumnModel().getColumn(4).setCellRenderer(new StatutRenderer());
        table.getColumnModel().getColumn(3).setCellRenderer(new CenteredRenderer());

        // Double-click on row = detail
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) showSelectedDetail();
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        center.add(sp, BorderLayout.CENTER);
        body.add(center, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        // S'assurer que la table existe m\u00eame si schema.sql pas r\u00e9-ex\u00e9cut\u00e9
        EmailLogDAO.ensureSchema();
        refresh();
    }

    private JPanel makeStatCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(VMSStyle.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 14, 10, 14)));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(VMSStyle.TEXT_MUTED);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accent);
        card.add(l, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private int getSelectedHeures() {
        return switch (cbFenetre.getSelectedIndex()) {
            case 0 -> 24;
            case 1 -> 168;   // 7 jours
            case 2 -> 720;   // 30 jours
            default -> 0;    // Tout
        };
    }

    private String getSelectedStatut() {
        Object s = cbStatut.getSelectedItem();
        if (s == null || "Tous".equals(s)) return null;
        return s.toString();
    }

    private void refresh() {
        final int heures = getSelectedHeures();
        final String statut = getSelectedStatut();
        final String dest = tfDest.getText().trim();

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() throws Exception {
                List<EmailLogDAO.EmailEntry> list = EmailLogDAO.search(dest, statut, heures, 500);
                EmailLogDAO.Stats stats = EmailLogDAO.getStats(heures);
                return new Object[]{list, stats};
            }
            @Override protected void done() {
                try {
                    Object[] r = get();
                    @SuppressWarnings("unchecked")
                    List<EmailLogDAO.EmailEntry> list = (List<EmailLogDAO.EmailEntry>) r[0];
                    EmailLogDAO.Stats stats = (EmailLogDAO.Stats) r[1];

                    currentEntries = list;
                    model.setRowCount(0);
                    for (EmailLogDAO.EmailEntry e : list) {
                        model.addRow(new Object[]{
                                e.dateEnvoi != null ? DF.format(e.dateEnvoi) : "\u2013",
                                e.destinataire,
                                truncate(e.sujet, 60),
                                e.nbPiecesJointes,
                                e.statut,
                                e.demandeId != null ? "#" + e.demandeId : "\u2013"
                        });
                    }
                    lblTotal.setText(String.valueOf(stats.total()));
                    lblEnvoyes.setText(String.valueOf(stats.envoyes));
                    lblEchecs.setText(String.valueOf(stats.echecs));
                    lblSim.setText(String.valueOf(stats.simulations));
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof SQLException) {
                        ToastManager.error(EmailHistoryDialog.this, "Erreur BD : " + cause.getMessage());
                    } else {
                        ToastManager.error(EmailHistoryDialog.this, "Erreur : " + cause.getMessage());
                    }
                }
            }
        }.execute();
    }

    private void showSelectedDetail() {
        int row = table.getSelectedRow();
        if (row < 0) {
            ToastManager.info(this, "S\u00e9lectionnez une ligne pour voir le d\u00e9tail");
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        if (modelRow < 0 || modelRow >= currentEntries.size()) return;

        EmailLogDAO.EmailEntry e = currentEntries.get(modelRow);
        String date    = e.dateEnvoi != null ? DF.format(e.dateEnvoi) : "\u2013";
        String erreur  = e.erreur != null ? e.erreur : "\u2013";
        String demande = e.demandeId != null ? "#" + e.demandeId : "\u2013";
        String cc      = e.cc != null && !e.cc.isEmpty() ? e.cc : "\u2013";

        StringBuilder html = new StringBuilder("<html><body style='width:420px;font-family:Segoe UI;'>");
        html.append("<b>Date :</b> ").append(date).append("<br>");
        html.append("<b>Destinataire :</b> ").append(escapeHtml(e.destinataire)).append("<br>");
        html.append("<b>CC :</b> ").append(escapeHtml(cc)).append("<br>");
        html.append("<b>Sujet :</b> ").append(escapeHtml(e.sujet)).append("<br>");
        html.append("<b>Pi\u00e8ces jointes :</b> ").append(e.nbPiecesJointes).append("<br>");
        html.append("<b>Statut :</b> ").append(e.statut).append("<br>");
        html.append("<b>Demande :</b> ").append(demande).append("<br>");
        if (!"ENVOYE".equals(e.statut)) {
            html.append("<br><b>Message :</b><br><span style='color:#D2232D;'>")
                .append(escapeHtml(erreur)).append("</span>");
        }
        html.append("</body></html>");

        int msgType = "ECHEC".equals(e.statut) ? JOptionPane.ERROR_MESSAGE
                : "SIMULATION".equals(e.statut) ? JOptionPane.WARNING_MESSAGE
                : JOptionPane.INFORMATION_MESSAGE;
        JOptionPane.showMessageDialog(this, html.toString(),
                "D\u00e9tail de l'envoi #" + e.emailId, msgType);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    // Renderers

    private static class StatutRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            String s = String.valueOf(v);
            Color color;
            switch (s) {
                case "ENVOYE"     -> color = VMSStyle.SUCCESS;
                case "ECHEC"      -> color = VMSStyle.RED_PRIMARY;
                case "SIMULATION" -> color = VMSStyle.ACCENT_BLUE;
                default           -> color = VMSStyle.TEXT_MUTED;
            }
            setForeground(sel ? Color.WHITE : color);
            return c;
        }
    }

    private static class CenteredRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            return c;
        }
    }

    public static void show(Component owner) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new EmailHistoryDialog(w).setVisible(true);
    }
}
