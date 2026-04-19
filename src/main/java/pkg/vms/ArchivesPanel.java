package pkg.vms;

import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.DBconnect;
import pkg.vms.DAO.VoucherDAO;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Page Archives — regroupe les demandes cl\u00f4tur\u00e9es (ENVOYE, REJETE, ARCHIVE).
 * Permet la restauration avec choix de statut, filtres p\u00e9riode et recherche.
 */
public class ArchivesPanel extends JPanel {

    private static final Color BG_CARD      = VMSStyle.BG_CARD;
    private static final Color RED_PRIMARY  = VMSStyle.RED_PRIMARY;
    private static final Color RED_LIGHT    = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED   = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS      = VMSStyle.SUCCESS;
    private static final Color WARNING      = VMSStyle.WARNING;
    private static final Color ACCENT_BLUE  = VMSStyle.ACCENT_BLUE;

    private static final String[] COLS = {
            "#", "R\u00e9f\u00e9rence", "Client", "Bons", "Montant", "Magasin",
            "Date cr\u00e9ation", "Date cl\u00f4ture", "Statut", "Actions"
    };
    private static final int COL_ID      = 0;
    private static final int COL_REF     = 1;
    private static final int COL_CLIENT  = 2;
    private static final int COL_NB      = 3;
    private static final int COL_TOTAL   = 4;
    private static final int COL_MAG     = 5;
    private static final int COL_DATE    = 6;
    private static final int COL_CLOSE   = 7;
    private static final int COL_STATUT  = 8;
    private static final int COL_ACTIONS = 9;

    private final String role;
    private final int    userId;
    private final String username;

    private DefaultTableModel tableModel;
    private JTable            table;
    private JTextField        txtSearch;
    private JLabel            lblTotal;
    private JLabel            lblMontant;
    private JLabel            lblRejets;
    private String            filtreStatutActif = "TOUTES";
    private String            filtrePeriode     = "TOUS";
    private CardLayout        tableCards;
    private JPanel            tableCardHolder;

    public ArchivesPanel(String role, int userId, String username) {
        this.role     = role;
        this.userId   = userId;
        this.username = username;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerArchives();
    }

    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 14));
        mid.setOpaque(false);
        mid.add(buildStatsRow(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(buildToolbar(),   BorderLayout.NORTH);
        body.add(buildTableCard(), BorderLayout.CENTER);
        mid.add(body, BorderLayout.CENTER);

        wrapper.add(mid, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JButton btnAutoArchive = UIUtils.buildOutlineButton("Archiver bons expir\u00e9s", 210, 40);
        btnAutoArchive.addActionListener(e -> archiverExpires());

        JButton btnExport = UIUtils.buildGhostButton("Exporter CSV", 140, 40);
        btnExport.addActionListener(e -> exporterCSV());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnExport);
        right.add(btnAutoArchive);

        return PageLayout.buildPageHeader(
                "Archives",
                "Demandes cl\u00f4tur\u00e9es \u2014 envoy\u00e9es, rejet\u00e9es ou archiv\u00e9es manuellement",
                right
        );
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        lblTotal    = new JLabel("\u2026");
        lblMontant  = new JLabel("\u2026");
        lblRejets   = new JLabel("\u2026");

        row.add(buildStatCard("TOTAL ARCHIV\u00c9ES", lblTotal,   ACCENT_BLUE));
        row.add(buildStatCard("MONTANT CL\u00d4TUR\u00c9",   lblMontant, SUCCESS));
        row.add(buildStatCard("REJET\u00c9ES",         lblRejets,  RED_PRIMARY));
        return row;
    }

    private JPanel buildStatCard(String label, JLabel valRef, Color accent) {
        JPanel card = PageLayout.buildCard(new BorderLayout(14, 0), 18);

        JPanel t = new JPanel();
        t.setOpaque(false);
        t.setLayout(new BoxLayout(t, BoxLayout.Y_AXIS));

        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        valRef.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valRef.setForeground(TEXT_PRIMARY);
        valRef.setAlignmentX(Component.LEFT_ALIGNMENT);

        t.add(l);
        t.add(Box.createVerticalStrut(6));
        t.add(valRef);

        JPanel strip = new JPanel();
        strip.setOpaque(true);
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(4, 0));

        card.add(strip, BorderLayout.WEST);
        card.add(t, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildToolbar() {
        JPanel chipsCard = PageLayout.buildCard(new FlowLayout(FlowLayout.LEFT, 8, 0), 12);
        chipsCard.add(buildChip("Toutes",    null,        "TOUTES"));
        chipsCard.add(buildChip("Envoy\u00e9es",  new Color(20,170,190), "ENVOYE"));
        chipsCard.add(buildChip("Rejet\u00e9es",  RED_PRIMARY, "REJETE"));
        chipsCard.add(buildChip("Archiv\u00e9es", TEXT_MUTED,  "ARCHIVE"));

        PageLayout.FilterBar fb = PageLayout.buildFilterBar("Rechercher dans les archives (r\u00e9f\u00e9rence, client\u2026)");
        txtSearch = fb.search();
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrerTable(); }
        });

        JComboBox<String> cbPeriode = new JComboBox<>(new String[]{
                "Toute p\u00e9riode", "7 derniers jours", "30 derniers jours", "90 derniers jours", "Cette ann\u00e9e"
        });
        cbPeriode.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPeriode.setPreferredSize(new Dimension(160, 38));
        cbPeriode.addActionListener(e -> {
            switch (cbPeriode.getSelectedIndex()) {
                case 1 -> filtrePeriode = "7J";
                case 2 -> filtrePeriode = "30J";
                case 3 -> filtrePeriode = "90J";
                case 4 -> filtrePeriode = "ANNEE";
                default -> filtrePeriode = "TOUS";
            }
            chargerArchives();
        });
        fb.addSlot(cbPeriode);

        JButton btnReset = UIUtils.buildGhostButton("R\u00e9initialiser", 120, 38);
        btnReset.addActionListener(e -> reinitialiser());
        fb.addSlot(btnReset);

        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.add(chipsCard, BorderLayout.NORTH);
        wrap.add(fb.panel(), BorderLayout.CENTER);
        return wrap;
    }

    private void reinitialiser() {
        txtSearch.setText("");
        filtreStatutActif = "TOUTES";
        filtrePeriode = "TOUS";
        chargerArchives();
        ToastManager.info(this, "Filtres r\u00e9initialis\u00e9s");
    }

    private JButton buildChip(String label, Color color, String statut) {
        Color chipColor = color != null ? color : TEXT_SECOND;
        JButton chip = new JButton(label) {
            boolean sel = statut.equals("TOUTES");
            {
                setFont(new Font("Segoe UI", Font.BOLD, 10));
                setForeground(sel ? Color.WHITE : chipColor);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 26, 30));
                addActionListener(e -> {
                    Container parent = getParent();
                    if (parent != null)
                        for (Component c : parent.getComponents())
                            if (c instanceof JButton) ((JButton) c).repaint();
                    sel = true; repaint();
                    filtreStatutActif = statut;
                    filtrerTable();
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isSelected = getForeground().equals(Color.WHITE) || getModel().isPressed();
                if (isSelected) {
                    g2.setColor(color != null ? color : TEXT_SECOND);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                } else {
                    g2.setColor(new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 14));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                    g2.setColor(new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 20, 20));
                }
                g2.dispose(); super.paintComponent(g);
            }
        };
        return chip;
    }

    private JPanel buildTableCard() {
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == COL_ACTIONS; }
        };

        table = new JTable(tableModel);
        ModernTable.apply(table);
        table.setAutoCreateRowSorter(false);

        int[] widths = {40, 130, 160, 55, 110, 130, 110, 110, 120, 130};
        for (int i = 0; i < widths.length; i++)
            ModernTable.setColumnWidth(table, i, widths[i]);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(40);

        ModernTable.setColumnRenderer(table, COL_REF, ModernTable.boldRenderer());
        ModernTable.setColumnRenderer(table, COL_STATUT, new StatutRenderer());
        ModernTable.setColumnRenderer(table, COL_NB, ModernTable.centerRenderer());
        ModernTable.setColumnRenderer(table, COL_TOTAL, ModernTable.moneyRenderer());
        table.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(COL_ACTIONS).setCellEditor(new ActionEditor());

        JPanel card = PageLayout.buildCard(new BorderLayout(), 0);
        JScrollPane scroll = ModernTable.wrap(table);
        scroll.setBorder(null);
        card.add(scroll, BorderLayout.CENTER);

        JPanel emptyNoData = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoData.add(PageLayout.buildEmptyState(
                "Aucune archive",
                "Les demandes cl\u00f4tur\u00e9es (envoy\u00e9es, rejet\u00e9es, archiv\u00e9es) appara\u00eetront ici.",
                null, null
        ), BorderLayout.CENTER);

        JPanel emptyNoResult = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoResult.add(PageLayout.buildEmptyState(
                "Aucun r\u00e9sultat",
                "Aucune archive ne correspond aux filtres s\u00e9lectionn\u00e9s.",
                "R\u00e9initialiser",
                this::reinitialiser
        ), BorderLayout.CENTER);

        tableCards = new CardLayout();
        tableCardHolder = new JPanel(tableCards);
        tableCardHolder.setOpaque(false);
        tableCardHolder.add(card, "table");
        tableCardHolder.add(emptyNoData, "empty-no-data");
        tableCardHolder.add(emptyNoResult, "empty-no-result");
        return tableCardHolder;
    }

    private void updateTableView() {
        if (tableCards == null) return;
        int total = tableModel.getRowCount();
        int visible = table.getRowCount();
        if (total == 0) tableCards.show(tableCardHolder, "empty-no-data");
        else if (visible == 0) tableCards.show(tableCardHolder, "empty-no-result");
        else tableCards.show(tableCardHolder, "table");
    }

    private void chargerArchives() {
        new SwingWorker<LoadResult, Void>() {
            @Override
            protected LoadResult doInBackground() throws Exception {
                LoadResult r = new LoadResult();
                String periodClause = switch (filtrePeriode) {
                    case "7J"    -> " AND d.date_modification >= CURRENT_DATE - INTERVAL '7 days'";
                    case "30J"   -> " AND d.date_modification >= CURRENT_DATE - INTERVAL '30 days'";
                    case "90J"   -> " AND d.date_modification >= CURRENT_DATE - INTERVAL '90 days'";
                    case "ANNEE" -> " AND EXTRACT(YEAR FROM d.date_modification) = EXTRACT(YEAR FROM CURRENT_DATE)";
                    default      -> "";
                };
                String sql = "SELECT d.demande_id, d.invoice_reference, c.name AS nom_client, " +
                             "d.nombre_bons, d.montant_total, m.nom_magasin, d.date_creation, " +
                             "d.date_modification, d.statuts " +
                             "FROM demande d LEFT JOIN client c ON d.clientid = c.clientid " +
                             "LEFT JOIN magasin m ON d.magasin_id = m.magasin_id " +
                             "WHERE d.statuts IN ('ENVOYE','REJETE','ARCHIVE')" + periodClause + " " +
                             "ORDER BY d.date_modification DESC NULLS LAST, d.date_creation DESC";

                try (Connection conn = DBconnect.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) {
                        String ts = rs.getTimestamp("date_modification") != null
                                ? rs.getTimestamp("date_modification").toString().substring(0, 10) : "\u2014";
                        String st2 = rs.getString("statuts");
                        double total = rs.getDouble("montant_total");
                        r.rows.add(new Object[]{
                                rs.getInt("demande_id"),
                                rs.getString("invoice_reference"),
                                rs.getString("nom_client"),
                                rs.getInt("nombre_bons"),
                                total,
                                rs.getString("nom_magasin"),
                                rs.getTimestamp("date_creation") != null
                                        ? rs.getTimestamp("date_creation").toString().substring(0, 10) : "\u2014",
                                ts,
                                st2,
                                "actions"
                        });
                        r.montantTotal += total;
                        if ("REJETE".equals(st2)) r.nbRejets++;
                    }
                }
                return r;
            }

            @Override
            protected void done() {
                try {
                    LoadResult r = get();
                    tableModel.setRowCount(0);
                    for (Object[] row : r.rows) tableModel.addRow(row);
                    lblTotal.setText(String.valueOf(r.rows.size()));
                    lblMontant.setText(String.format("Rs %,.0f", r.montantTotal));
                    lblRejets.setText(String.valueOf(r.nbRejets));
                    filtrerTable();
                } catch (Exception e) {
                    e.printStackTrace();
                    ToastManager.error(ArchivesPanel.this, "Erreur chargement : " + e.getMessage());
                }
            }
        }.execute();
    }

    private static class LoadResult {
        List<Object[]> rows = new ArrayList<>();
        double montantTotal = 0;
        int nbRejets = 0;
    }

    private void filtrerTable() {
        String search = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (!filtreStatutActif.equals("TOUTES"))
            filters.add(RowFilter.regexFilter("(?i)" + filtreStatutActif, COL_STATUT));
        if (!search.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + search, COL_REF, COL_CLIENT, COL_MAG));

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));
        updateTableView();
    }

    // ── RESTAURATION ────────────────────────────────────────────────────────
    private void demanderRestauration(int demandeId, String ancienStatut) {
        // Vérifier si déjà redimé (protection double rédemption)
        if ("ENVOYE".equals(ancienStatut)) {
            try (Connection conn = DBconnect.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT COUNT(*) FROM bon b WHERE b.demande_id = ? AND b.statut = 'UTILISE'")) {
                ps.setInt(1, demandeId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        ToastManager.warning(this,
                                "Impossible de restaurer : certains bons sont d\u00e9j\u00e0 utilis\u00e9s");
                        return;
                    }
                }
            } catch (SQLException e) {
                ToastManager.error(this, "Erreur v\u00e9rification : " + e.getMessage());
                return;
            }
        }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Restaurer la demande", true);
        dlg.setUndecorated(true);
        dlg.setSize(420, 380);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g); g.setColor(BG_CARD);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));

        JLabel title = new JLabel("Restaurer la demande #" + demandeId);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(20, 24, 6, 24));

        JLabel sub = new JLabel("<html>S\u00e9lectionnez le statut de destination.<br>"
                + "Un journal d'audit sera cr\u00e9\u00e9.</html>");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(0, 24, 16, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(sub);
        root.add(header, BorderLayout.NORTH);

        JPanel choices = new JPanel();
        choices.setOpaque(false);
        choices.setLayout(new BoxLayout(choices, BoxLayout.Y_AXIS));
        choices.setBorder(BorderFactory.createEmptyBorder(0, 24, 10, 24));

        ButtonGroup grp = new ButtonGroup();
        JRadioButton rbAttente  = buildRadio("En attente paiement", "EN_ATTENTE_PAIEMENT", true);
        JRadioButton rbPaye     = buildRadio("Pay\u00e9e",          "PAYE",                false);
        JRadioButton rbApprouve = buildRadio("Approuv\u00e9e",      "APPROUVE",            false);
        JRadioButton rbGenere   = buildRadio("G\u00e9n\u00e9r\u00e9e", "GENERE",            false);
        grp.add(rbAttente); grp.add(rbPaye); grp.add(rbApprouve); grp.add(rbGenere);
        choices.add(rbAttente); choices.add(Box.createVerticalStrut(6));
        choices.add(rbPaye);    choices.add(Box.createVerticalStrut(6));
        choices.add(rbApprouve);choices.add(Box.createVerticalStrut(6));
        choices.add(rbGenere);
        root.add(choices, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        btns.setOpaque(false);
        btns.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT));
        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 100, 36);
        btnCancel.addActionListener(e -> dlg.dispose());
        JButton btnOk = UIUtils.buildRedButton("Restaurer", 120, 36);
        btnOk.addActionListener(e -> {
            String target = rbAttente.isSelected() ? "EN_ATTENTE_PAIEMENT"
                          : rbPaye.isSelected()    ? "PAYE"
                          : rbApprouve.isSelected()? "APPROUVE"
                          : "GENERE";
            effectuerRestauration(demandeId, ancienStatut, target);
            dlg.dispose();
        });
        btns.add(btnCancel);
        btns.add(btnOk);
        root.add(btns, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    private JRadioButton buildRadio(String label, String value, boolean selected) {
        JRadioButton rb = new JRadioButton(label, selected);
        rb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        rb.setForeground(TEXT_PRIMARY);
        rb.setOpaque(false);
        rb.setFocusPainted(false);
        rb.setAlignmentX(Component.LEFT_ALIGNMENT);
        rb.putClientProperty("value", value);
        return rb;
    }

    private void effectuerRestauration(int demandeId, String ancienStatut, String nouveauStatut) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                VoucherDAO.updateVoucherStatus(demandeId, nouveauStatut, userId);
                AuditDAO.log("demande", demandeId, "MODIFICATION",
                        "{\"statut\":\"" + ancienStatut + "\"}",
                        "{\"statut\":\"" + nouveauStatut + "\"}",
                        userId, username,
                        "Restauration depuis archives : " + ancienStatut + " \u2192 " + nouveauStatut);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    ToastManager.success(ArchivesPanel.this,
                            "Demande #" + demandeId + " restaur\u00e9e vers " + nouveauStatut);
                    chargerArchives();
                } catch (Exception e) {
                    ToastManager.error(ArchivesPanel.this, "Erreur : " + e.getMessage());
                }
            }
        }.execute();
    }

    private void archiverExpires() {
        Window w = SwingUtilities.getWindowAncestor(this);
        boolean ok = UIUtils.confirmDialog(w,
                "Archiver les bons expir\u00e9s",
                "Cette op\u00e9ration archivera automatiquement toutes les demandes dont la validit\u00e9 est d\u00e9pass\u00e9e. "
                + "Continuer ?", "Archiver", "Annuler");
        if (!ok) return;

        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return VoucherDAO.archiverDemandesExpirees(userId);
            }
            @Override protected void done() {
                try {
                    int n = get();
                    if (n > 0) ToastManager.success(ArchivesPanel.this, n + " demande" + (n>1?"s":"") + " archiv\u00e9e" + (n>1?"s":""));
                    else ToastManager.info(ArchivesPanel.this, "Aucune demande \u00e0 archiver");
                    chargerArchives();
                } catch (Exception e) {
                    ToastManager.error(ArchivesPanel.this, "Erreur : " + e.getMessage());
                }
            }
        }.execute();
    }

    private void exporterCSV() {
        try {
            StringBuilder sb = new StringBuilder();
            for (String col : COLS) { if (!col.equals("Actions")) sb.append(col).append(";"); }
            sb.setLength(sb.length() - 1); sb.append("\n");

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                for (int j = 0; j < COLS.length; j++) {
                    if (j == COL_ACTIONS) continue;
                    Object v = tableModel.getValueAt(i, j);
                    sb.append(v != null ? v.toString().replace(";", ",") : "").append(";");
                }
                sb.setLength(sb.length() - 1); sb.append("\n");
            }

            java.io.File f = new java.io.File(System.getProperty("user.home"),
                    "archives_vms_" + System.currentTimeMillis() + ".csv");
            try (java.io.FileWriter fw = new java.io.FileWriter(f, java.nio.charset.StandardCharsets.UTF_8)) {
                fw.write('\ufeff'); fw.write(sb.toString());
            }
            ToastManager.success(this, "Export \u00e9crit : " + f.getName());
        } catch (Exception e) {
            ToastManager.error(this, "Erreur export : " + e.getMessage());
        }
    }

    // ── RENDERERS ───────────────────────────────────────────────────────────
    private class StatutRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String statut = val != null ? val.toString() : "";
            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(badgeBg(statut));
                    g2.fill(new RoundRectangle2D.Double(4, 8, getWidth()-8, getHeight()-16, 20, 20));
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249,250,252)));
            JLabel lbl = new JLabel(badgeLabel(statut));
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
            lbl.setForeground(badgeFg(statut));
            badge.add(lbl);
            return badge;
        }
        private Color badgeBg(String s) {
            return switch (s) {
                case "ENVOYE"  -> new Color(209,250,246);
                case "REJETE"  -> new Color(254,226,226);
                case "ARCHIVE" -> new Color(243,244,246);
                default -> new Color(241,245,249);
            };
        }
        private Color badgeFg(String s) {
            return switch (s) {
                case "ENVOYE"  -> new Color(20,170,190);
                case "REJETE"  -> RED_PRIMARY;
                case "ARCHIVE" -> TEXT_SECOND;
                default -> TEXT_MUTED;
            };
        }
        private String badgeLabel(String s) {
            return switch (s) {
                case "ENVOYE"  -> "\uD83D\uDCE8  Envoy\u00e9e";
                case "REJETE"  -> "\u274C  Rejet\u00e9e";
                case "ARCHIVE" -> "\uD83D\uDCE6  Archiv\u00e9e";
                default -> s;
            };
        }
    }

    private class ActionRenderer extends DefaultTableCellRenderer {
        private final JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        private final JButton btn;
        ActionRenderer() {
            btn = buildRestoreBtn(false);
            p.setOpaque(false); p.add(btn);
        }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            p.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249,250,252)));
            return p;
        }
    }

    private class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        private final JButton btn;
        private int currentRow = -1;
        ActionEditor() {
            btn = buildRestoreBtn(true);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(currentRow);
                    int id = (int) tableModel.getValueAt(modelRow, COL_ID);
                    String st = (String) tableModel.getValueAt(modelRow, COL_STATUT);
                    demanderRestauration(id, st);
                }
            });
            p.setOpaque(false); p.add(btn);
        }
        @Override public Object getCellEditorValue() { return "actions"; }
        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col) {
            currentRow = row; p.setBackground(RED_LIGHT); return p;
        }
    }

    private JButton buildRestoreBtn(boolean interactive) {
        JButton btn = new JButton("\u21BA Restaurer") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(interactive && getModel().isRollover() ? SUCCESS : new Color(220,252,231));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                setForeground(interactive && getModel().isRollover() ? Color.WHITE : SUCCESS);
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setForeground(SUCCESS);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(110, 28));
        if (interactive) btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
