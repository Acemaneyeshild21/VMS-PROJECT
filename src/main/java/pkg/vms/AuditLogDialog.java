package pkg.vms;

import pkg.vms.DAO.AuditDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Journal d'audit — dialog admin avec filtres (action, utilisateur, table,
 * p\u00e9riode), table paginee et export CSV.
 */
public class AuditLogDialog extends JDialog {

    private static final SimpleDateFormat DF_DT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);

    private final JComboBox<String> cbAction;
    private final JTextField        tfUsername;
    private final JComboBox<String> cbTable;
    private final JComboBox<String> cbPeriode;
    private final JTextField        tfLimit;

    private final DefaultTableModel model;
    private final JTable            table;
    private final JLabel            lblCount;

    public AuditLogDialog(Window owner) {
        super(owner, "Journal d'audit \u2014 \u00c9v\u00e9nements syst\u00e8me", ModalityType.APPLICATION_MODAL);
        setSize(1040, 640);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(VMSStyle.BG_ROOT);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        // ── Header ─────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(VMSStyle.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(16, 22, 16, 22)));
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("\uD83D\uDD10  Journal d'audit");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Tra\u00e7abilit\u00e9 compl\u00e8te des \u00e9v\u00e9nements syst\u00e8me");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(sub);
        header.add(titleBox, BorderLayout.WEST);

        JButton btnExport = UIUtils.buildGhostButton("Exporter CSV", 130, 34);
        btnExport.addActionListener(e -> exportCsv());
        JPanel hRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hRight.setOpaque(false);
        hRight.add(btnExport);
        header.add(hRight, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);

        // ── Filtres + tableau ──────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        cbAction   = buildCombo(new String[]{"Toutes actions"}, 170);
        tfUsername = new JTextField(12);
        tfUsername.putClientProperty("JTextField.placeholderText", "Utilisateur\u2026");
        cbTable    = buildCombo(new String[]{"Toutes tables","demande","bon","utilisateur","client","redemption"}, 140);
        cbPeriode  = buildCombo(new String[]{"7 jours","30 jours","90 jours","Cette ann\u00e9e","Tout"}, 130);
        cbPeriode.setSelectedIndex(1);
        tfLimit    = new JTextField("500", 5);

        loadActions();

        JButton btnSearch = UIUtils.buildPrimaryButton("Rechercher", 130, 34);
        btnSearch.addActionListener(e -> refresh());
        JButton btnReset = UIUtils.buildGhostButton("Reset", 80, 34);
        btnReset.addActionListener(e -> resetFilters());

        filters.add(labeled("Action", cbAction));
        filters.add(labeled("Utilisateur", tfUsername));
        filters.add(labeled("Table", cbTable));
        filters.add(labeled("P\u00e9riode", cbPeriode));
        filters.add(labeled("Limite", tfLimit));
        filters.add(Box.createHorizontalStrut(6));
        filters.add(btnSearch);
        filters.add(btnReset);

        content.add(filters, BorderLayout.NORTH);

        // Tableau
        String[] cols = {"#", "Date / Heure", "Action", "Table", "Record", "Utilisateur", "Contexte"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.setSelectionForeground(VMSStyle.TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(VMSStyle.TEXT_SECONDARY);
        table.getTableHeader().setBackground(VMSStyle.BG_SUBTLE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setCellRenderer(new ActionBadgeRenderer());
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(6).setPreferredWidth(380);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        content.add(sp, BorderLayout.CENTER);

        // Footer count
        lblCount = new JLabel("0 \u00e9v\u00e9nements");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCount.setForeground(VMSStyle.TEXT_MUTED);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setOpaque(false);
        footer.add(lblCount);
        content.add(footer, BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // ── Escape close ───────────────────────────────────────────────────
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        refresh();
    }

    private JComboBox<String> buildCombo(String[] items, int width) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setPreferredSize(new Dimension(width, 32));
        c.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        c.setBackground(VMSStyle.BG_CARD);
        return c;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 9));
        l.setForeground(VMSStyle.TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JTextField) {
            ((JTextField) field).setPreferredSize(new Dimension(140, 32));
        }
        p.add(l);
        p.add(Box.createVerticalStrut(3));
        p.add(field);
        return p;
    }

    private void loadActions() {
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() { return AuditDAO.getDistinctActions(); }
            @Override protected void done() {
                try {
                    List<String> actions = get();
                    String current = (String) cbAction.getSelectedItem();
                    cbAction.removeAllItems();
                    cbAction.addItem("Toutes actions");
                    for (String a : actions) cbAction.addItem(a);
                    if (current != null) cbAction.setSelectedItem(current);
                } catch (Exception ignore) {}
            }
        }.execute();
    }

    private void resetFilters() {
        cbAction.setSelectedIndex(0);
        tfUsername.setText("");
        cbTable.setSelectedIndex(0);
        cbPeriode.setSelectedIndex(1);
        tfLimit.setText("500");
        refresh();
    }

    private void refresh() {
        String action = cbAction.getSelectedIndex() == 0 ? null : (String) cbAction.getSelectedItem();
        String user   = tfUsername.getText().trim();
        String tbl    = cbTable.getSelectedIndex() == 0 ? null : (String) cbTable.getSelectedItem();
        Timestamp dateDebut = computePeriodStart();
        int limit;
        try { limit = Math.max(50, Math.min(10000, Integer.parseInt(tfLimit.getText().trim()))); }
        catch (NumberFormatException ex) { limit = 500; }
        final int lim = limit;

        new SwingWorker<List<AuditDAO.Event>, Void>() {
            @Override protected List<AuditDAO.Event> doInBackground() {
                return AuditDAO.search(action, user, tbl, dateDebut, null, lim);
            }
            @Override protected void done() {
                try {
                    List<AuditDAO.Event> events = get();
                    model.setRowCount(0);
                    int i = 1;
                    for (AuditDAO.Event e : events) {
                        model.addRow(new Object[]{
                                i++,
                                e.dateEvt != null ? DF_DT.format(e.dateEvt) : "\u2013",
                                e.action != null ? e.action : "\u2013",
                                e.tableName != null ? e.tableName : "\u2013",
                                "#" + e.recordId,
                                e.username != null ? e.username : "\u2013",
                                e.contexte != null ? e.contexte : ""
                        });
                    }
                    lblCount.setText(events.size() + " \u00e9v\u00e9nements (limite : " + lim + ")");
                } catch (Exception ex) {
                    ToastManager.error(AuditLogDialog.this, "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Timestamp computePeriodStart() {
        int idx = cbPeriode.getSelectedIndex();
        if (idx == 4) return null; // Tout
        Calendar c = Calendar.getInstance();
        switch (idx) {
            case 0 -> c.add(Calendar.DAY_OF_YEAR, -7);
            case 1 -> c.add(Calendar.DAY_OF_YEAR, -30);
            case 2 -> c.add(Calendar.DAY_OF_YEAR, -90);
            case 3 -> { c.set(Calendar.MONTH, Calendar.JANUARY); c.set(Calendar.DAY_OF_MONTH, 1);
                        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0); }
        }
        return new Timestamp(c.getTimeInMillis());
    }

    private void exportCsv() {
        if (model.getRowCount() == 0) {
            ToastManager.warning(this, "Rien \u00e0 exporter");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("audit_log_" +
                new SimpleDateFormat("yyyyMMdd_HHmm").format(new java.util.Date()) + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try (FileOutputStream fos = new FileOutputStream(fc.getSelectedFile());
             BufferedWriter w = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
            // BOM for Excel UTF-8
            fos.write(0xEF); fos.write(0xBB); fos.write(0xBF);
            w.write("#;Date;Action;Table;Record;Utilisateur;Contexte\n");
            for (int r = 0; r < model.getRowCount(); r++) {
                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < model.getColumnCount(); c++) {
                    if (c > 0) sb.append(';');
                    String val = String.valueOf(model.getValueAt(r, c));
                    val = val.replace("\"", "\"\"");
                    if (val.contains(";") || val.contains("\n") || val.contains("\"")) {
                        val = "\"" + val + "\"";
                    }
                    sb.append(val);
                }
                sb.append('\n');
                w.write(sb.toString());
            }
            ToastManager.info(this, "Export \u00e9crit : " + fc.getSelectedFile().getName());
        } catch (IOException ex) {
            ToastManager.error(this, "Erreur export : " + ex.getMessage());
        }
    }

    // ── Renderer pour la colonne Action ───────────────────────────────────
    private static class ActionBadgeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            String s = v == null ? "" : v.toString();
            Color bg;
            Color fg = Color.WHITE;
            switch (s) {
                case "CREATION"           -> bg = VMSStyle.SUCCESS;
                case "MODIFICATION","UPDATE_EMAIL" -> bg = VMSStyle.ACCENT_BLUE;
                case "SUPPRESSION","REJET","ANNULATION","CONNEXION_ECHOUEE" -> bg = VMSStyle.RED_PRIMARY;
                case "PAIEMENT","APPROBATION","GENERATION" -> bg = new Color(13, 148, 136);
                case "ENVOI","REDEMPTION","UTILISATION_BON" -> bg = VMSStyle.WARNING;
                case "CONNEXION","INSCRIPTION" -> bg = new Color(79, 70, 229);
                case "ARCHIVAGE_MASSIF","CHANGEMENT_STATUT" -> { bg = new Color(156, 163, 175); fg = VMSStyle.TEXT_PRIMARY; }
                default                        -> { bg = VMSStyle.BG_SUBTLE; fg = VMSStyle.TEXT_SECONDARY; }
            }
            if (sel) { bg = VMSStyle.RED_PRIMARY; fg = Color.WHITE; }
            setBackground(bg); setForeground(fg);
            return c;
        }
    }

    // ── API ────────────────────────────────────────────────────────────────
    public static void show(Component owner) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new AuditLogDialog(w).setVisible(true);
    }
}
