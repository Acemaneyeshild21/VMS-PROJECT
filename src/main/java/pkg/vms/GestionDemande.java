package pkg.vms;
import pkg.vms.DAO.DBconnect;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GestionDemande extends JPanel {

    private static final Color BG_ROOT      = new Color(245, 246, 250);
    private static final Color BG_CARD      = new Color(255, 255, 255);
    private static final Color RED_PRIMARY  = new Color(210,  35,  45);
    private static final Color RED_DARK     = new Color(170,  20,  28);
    private static final Color RED_LIGHT    = new Color(255, 235, 236);
    private static final Color BORDER_LIGHT = new Color(228, 230, 236);
    private static final Color TEXT_PRIMARY = new Color( 22,  28,  45);
    private static final Color TEXT_SECOND  = new Color( 90, 100, 120);
    private static final Color TEXT_MUTED   = new Color(160, 168, 185);
    private static final Color SUCCESS      = new Color( 22, 163,  74);
    private static final Color WARNING      = new Color(217, 119,   6);
    private static final Color ACCENT_BLUE  = new Color( 37,  99, 235);
    private static final Color ACCENT_PURP  = new Color(124,  58, 237);

    // Vrais noms de statuts dans la BD (colonne: statuts)
    private static final String ST_ATTENTE_PAIEMENT = "EN_ATTENTE_PAIEMENT";
    private static final String ST_PAYE             = "PAYE";
    private static final String ST_APPROUVE         = "APPROUVE";
    private static final String ST_GENERE           = "GENERE";
    private static final String ST_REJETE           = "REJETE";

    private static final Font FONT_PAGE_TITLE = new Font("Georgia",      Font.BOLD,  24);
    private static final Font FONT_SECTION    = new Font("Trebuchet MS", Font.BOLD,   9);
    private static final Font FONT_TABLE_HDR  = new Font("Trebuchet MS", Font.BOLD,  12);
    private static final Font FONT_TABLE_CELL = new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_BADGE      = new Font("Trebuchet MS", Font.BOLD,  10);
    private static final Font FONT_BTN        = new Font("Trebuchet MS", Font.BOLD,  12);
    private static final Font FONT_FILTER     = new Font("Trebuchet MS", Font.PLAIN, 12);

    // Colonnes tableau
    private static final String[] COLS = {
            "#", "Référence", "Client", "Bons", "Valeur Unit.", "Montant Total",
            "Magasin", "Date création", "Validité (j)", "Statut", "Actions"
    };
    private static final int COL_ID      = 0;
    private static final int COL_REF     = 1;
    private static final int COL_CLIENT  = 2;
    private static final int COL_NB      = 3;
    private static final int COL_VU      = 4;
    private static final int COL_TOTAL   = 5;
    private static final int COL_MAG     = 6;
    private static final int COL_DATE    = 7;
    private static final int COL_EXP     = 8;
    private static final int COL_STATUT  = 9;
    private static final int COL_ACTIONS = 10;

    private final String role;
    private final int    userId;

    private DefaultTableModel tableModel;
    private JTable            table;
    private JTextField        txtSearch;
    private JLabel            lblTotal;
    private String            filtreStatutActif = "TOUTES";

    public GestionDemande(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerDemandes();
    }

    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        mid.add(buildToolbar(),   BorderLayout.NORTH);
        mid.add(buildTableCard(), BorderLayout.CENTER);
        wrapper.add(mid, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── HEADER ──────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(RED_PRIMARY);
                g.fillRoundRect(0, 3, 4, getHeight() - 6, 4, 4);
            }
        };
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        JLabel icon  = new JLabel("\uD83D\uDCCB");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel title = new JLabel("  Gestion des Demandes");
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_PRIMARY);
        titleRow.add(icon); titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Suivi complet du cycle de vie des demandes de bons cadeau");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(5, 14, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(titleRow); left.add(sub);

        JButton btnNew = buildRedButton("+ Nouvelle Demande");
        btnNew.addActionListener(e -> ouvrirNouvelledemande());
        h.add(left,   BorderLayout.CENTER);
        h.add(btnNew, BorderLayout.EAST);
        return h;
    }

    // ── TOOLBAR ─────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(buildChip("Toutes",    null,         "TOUTES"));
        chips.add(buildChip("En attente", WARNING,     ST_ATTENTE_PAIEMENT));
        chips.add(buildChip("Payées",     ACCENT_BLUE, ST_PAYE));
        chips.add(buildChip("Approuvées", ACCENT_PURP, ST_APPROUVE));
        chips.add(buildChip("Générées",   SUCCESS,     ST_GENERE));
        chips.add(buildChip("Rejetées",   RED_PRIMARY, ST_REJETE));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        txtSearch = new JTextField(18) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(FONT_FILTER); g2.setColor(TEXT_MUTED);
                    Insets i = getInsets();
                    g2.drawString("\uD83D\uDD0D  Rechercher...", i.left, getHeight() - i.bottom - 4);
                }
            }
        };
        txtSearch.setFont(FONT_FILTER);
        txtSearch.setForeground(TEXT_PRIMARY);
        txtSearch.setBackground(BG_CARD);
        txtSearch.setCaretColor(RED_PRIMARY);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        txtSearch.setPreferredSize(new Dimension(220, 36));
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrerTable(); }
        });
        JButton btnRefresh = buildIconButton("\u21BB", "Actualiser");
        btnRefresh.addActionListener(e -> chargerDemandes());
        right.add(txtSearch); right.add(btnRefresh);

        bar.add(chips, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JButton buildChip(String label, Color color, String statut) {
        Color chipColor = color != null ? color : TEXT_SECOND;
        JButton chip = new JButton(label) {
            boolean sel = statut.equals("TOUTES");
            {
                setFont(FONT_BADGE);
                setForeground(sel ? Color.WHITE : chipColor);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 24, 30));
                addActionListener(e -> {
                    Container parent = getParent();
                    if (parent != null)
                        for (Component c : parent.getComponents())
                            if (c instanceof JButton) ((JButton) c).repaint();
                    sel = true; repaint();
                    filtrerParStatut(statut);
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isSelected = getForeground().equals(Color.WHITE) || getModel().isPressed();
                if (isSelected || getModel().isRollover()) {
                    g2.setColor(isSelected ? (color != null ? color : TEXT_SECOND)
                            : new Color(chipColor.getRed(), chipColor.getGreen(), chipColor.getBlue(), 18));
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

    // ── TABLE CARD ──────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(BORDER_LIGHT); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == COL_ACTIONS; }
        };

        table = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                return c;
            }
        };
        table.setFont(FONT_TABLE_CELL);
        table.setRowHeight(44);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.setSelectionBackground(RED_LIGHT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_TABLE_HDR);
        header.setBackground(new Color(248, 249, 252));
        header.setForeground(TEXT_SECOND);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        int[] widths = {40, 120, 150, 55, 95, 110, 120, 110, 80, 130, 80};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(40);

        table.getColumnModel().getColumn(COL_STATUT).setCellRenderer(new StatutRenderer());
        table.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(COL_ACTIONS).setCellEditor(new ActionEditor());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(COL_NB).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(COL_VU).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null); scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(BG_CARD);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        lblTotal = new JLabel("Chargement...");
        lblTotal.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblTotal.setForeground(TEXT_MUTED);
        footer.add(lblTotal, BorderLayout.WEST);

        card.add(scroll,  BorderLayout.CENTER);
        card.add(footer,  BorderLayout.SOUTH);
        return card;
    }

    // ── CHARGEMENT BD ───────────────────────────────────────────────────────
    // Colonnes réelles de la table demande :
    // demande_id, email, contact_number, company, statuts, invoice_reference,
    // valeur_et_nombre, client_id, date_creation, date_modification, cree_par,
    // nombre_bons, valeur_unitaire, montant_total, date_paiement, paiement_valide,
    // valide_par, approuve, approuve_par, date_approbation, date_demande,
    // utilisateur_creation_id, description, statut, magasin_id, validite_jours,
    // email_destinataire, reference
    private void chargerDemandes() {
        tableModel.setRowCount(0);
        String sql =
                "SELECT d.demande_id, d.invoice_reference, " +
                        "       c.name AS nom_client, " +
                        "       d.nombre_bons, d.valeur_unitaire, d.montant_total, " +
                        "       m.nom_magasin, d.date_creation, d.validite_jours, d.statuts " +
                        "FROM demande d " +
                        "LEFT JOIN client  c ON d.client_id  = c.client_id " +
                        "LEFT JOIN magazin m ON d.magasin_id = m.magasin_id " +
                        "ORDER BY d.date_creation DESC";

        try (Connection conn = DBconnect.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            int count = 0;
            while (rs.next()) {
                String ref      = rs.getString("invoice_reference");
                String dateRaw  = rs.getString("date_creation");
                String dateAff  = (dateRaw != null && dateRaw.length() >= 10)
                        ? dateRaw.substring(0, 10) : (dateRaw != null ? dateRaw : "—");
                int validite    = rs.getInt("validite_jours");

                tableModel.addRow(new Object[]{
                        rs.getInt("demande_id"),
                        ref != null ? ref : "—",
                        rs.getString("nom_client")  != null ? rs.getString("nom_client")  : "—",
                        rs.getInt("nombre_bons"),
                        String.format("Rs %,.0f", rs.getDouble("valeur_unitaire")),
                        String.format("Rs %,.0f", rs.getDouble("montant_total")),
                        rs.getString("nom_magasin") != null ? rs.getString("nom_magasin") : "—",
                        dateAff,
                        validite > 0 ? validite + " j" : "—",
                        rs.getString("statuts") != null ? rs.getString("statuts") : "—",
                        "actions"
                });
                count++;
            }
            lblTotal.setText(count + " demande" + (count > 1 ? "s" : "") + " au total");

        } catch (SQLException e) {
            e.printStackTrace();
            lblTotal.setText("Erreur : " + e.getMessage());
        }
    }

    // ── FILTRES ─────────────────────────────────────────────────────────────
    private void filtrerParStatut(String statut) {
        filtreStatutActif = statut;
        filtrerTable();
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

        int visible = table.getRowCount();
        lblTotal.setText(visible + " demande" + (visible > 1 ? "s" : "") + " affichée" + (visible > 1 ? "s" : ""));
    }

    // ── RENDERER STATUT ─────────────────────────────────────────────────────
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
            lbl.setFont(FONT_BADGE); lbl.setForeground(badgeFg(statut));
            badge.add(lbl);
            return badge;
        }
        private Color badgeBg(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return new Color(255,237,213);
                case ST_PAYE:            return new Color(219,234,254);
                case ST_APPROUVE:        return new Color(237,233,254);
                case ST_GENERE:          return new Color(220,252,231);
                case ST_REJETE:          return new Color(254,226,226);
                default:                 return new Color(241,245,249);
            }
        }
        private Color badgeFg(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return WARNING;
                case ST_PAYE:            return ACCENT_BLUE;
                case ST_APPROUVE:        return ACCENT_PURP;
                case ST_GENERE:          return SUCCESS;
                case ST_REJETE:          return RED_PRIMARY;
                default:                 return TEXT_MUTED;
            }
        }
        private String badgeLabel(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return "\u23F3  En attente";
                case ST_PAYE:            return "\uD83D\uDCB3  Pay\u00e9e";
                case ST_APPROUVE:        return "\u2705  Approuv\u00e9e";
                case ST_GENERE:          return "\uD83C\uDF9F  G\u00e9n\u00e9r\u00e9e";
                case ST_REJETE:          return "\u274C  Rejet\u00e9e";
                default:                 return s;
            }
        }
    }

    // ── RENDERER ACTIONS ────────────────────────────────────────────────────
    private class ActionRenderer extends DefaultTableCellRenderer {
        private final JPanel   p   = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        private final JButton  btn;
        ActionRenderer() {
            btn = buildVoirBtn(false);
            p.setOpaque(false); p.add(btn);
        }
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            p.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249,250,252)));
            return p;
        }
    }

    // ── EDITOR ACTIONS ──────────────────────────────────────────────────────
    private class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel  p   = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        private final JButton btn;
        private int currentRow = -1;
        ActionEditor() {
            btn = buildVoirBtn(true);
            btn.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0) {
                    int modelRow = table.convertRowIndexToModel(currentRow);
                    int id = (int) tableModel.getValueAt(modelRow, COL_ID);
                    afficherDetailDemande(id);
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

    private JButton buildVoirBtn(boolean interactive) {
        JButton btn = new JButton("Voir") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(interactive && getModel().isRollover() ? RED_PRIMARY : RED_LIGHT);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                setForeground(interactive && getModel().isRollover() ? Color.WHITE : RED_PRIMARY);
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN); btn.setForeground(RED_PRIMARY);
        btn.setOpaque(false); btn.setContentAreaFilled(false);
        btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(64, 28));
        if (interactive) btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── DETAIL DEMANDE ──────────────────────────────────────────────────────
    private void afficherDetailDemande(int demandeId) {
        // Utilise les vraies colonnes de la BD
        String sql =
                "SELECT d.demande_id, d.invoice_reference, d.nombre_bons, " +
                        "       d.valeur_unitaire, d.montant_total, d.validite_jours, " +
                        "       d.statuts, d.date_creation, d.email, d.cree_par, " +
                        "       c.name AS nom_client, c.email AS email_client, " +
                        "       m.nom_magasin, " +
                        "       u.username AS createur " +
                        "FROM demande d " +
                        "LEFT JOIN client      c ON d.client_id   = c.client_id " +
                        "LEFT JOIN magazin     m ON d.magasin_id  = m.magasin_id " +
                        "LEFT JOIN utilisateur u ON d.cree_par    = u.userid " +
                        "WHERE d.demande_id = ?";

        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return;

            String ref      = rs.getString("invoice_reference");
            String client   = rs.getString("nom_client");
            String emailCli = rs.getString("email_client");
            String emailEnv = rs.getString("email");
            int    nbBons   = rs.getInt("nombre_bons");
            double valUnit  = rs.getDouble("valeur_unitaire");
            double total    = rs.getDouble("montant_total");
            int    validite = rs.getInt("validite_jours");
            String magasin  = rs.getString("nom_magasin");
            String dateD    = rs.getString("date_creation");
            String statut   = rs.getString("statuts");
            String createur = rs.getString("createur");

            JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                    "D\u00e9tail \u2014 " + (ref != null ? ref : "#" + demandeId), true);
            dlg.setUndecorated(true);
            dlg.setSize(520, 520);
            dlg.setLocationRelativeTo(this);

            JPanel root = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g); g.setColor(BG_ROOT);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));

            // Header rouge
            JPanel hdr = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(0, 0, RED_PRIMARY, getWidth(), 0, RED_DARK));
                    g2.fillRect(0, 0, getWidth(), getHeight()); g2.dispose();
                }
            };
            hdr.setPreferredSize(new Dimension(0, 72));
            hdr.setLayout(new BorderLayout());
            hdr.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 20));

            JPanel hdrLeft = new JPanel();
            hdrLeft.setOpaque(false);
            hdrLeft.setLayout(new BoxLayout(hdrLeft, BoxLayout.Y_AXIS));
            JLabel lblRef = new JLabel("\uD83D\uDCCB  " + (ref != null ? ref : "#" + demandeId));
            lblRef.setFont(new Font("Georgia", Font.BOLD, 18));
            lblRef.setForeground(Color.WHITE);
            JLabel lblSt = new JLabel(statut != null ? statut : "—");
            lblSt.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
            lblSt.setForeground(new Color(255,255,255,180));
            hdrLeft.add(lblRef); hdrLeft.add(lblSt);
            hdr.add(hdrLeft, BorderLayout.CENTER);
            hdr.add(buildCloseBtn(() -> dlg.dispose()), BorderLayout.EAST);
            root.add(hdr, BorderLayout.NORTH);

            // Body
            JPanel body = new JPanel();
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

            body.add(buildDetailSection("B\u00c9N\u00c9FICIAIRE"));
            body.add(buildDetailRow("Client",       client  != null ? client  : "\u2014"));
            body.add(buildDetailRow("Email client", emailCli != null ? emailCli : "\u2014"));
            body.add(buildDetailRow("Email envoi",  emailEnv != null ? emailEnv : "\u2014"));
            body.add(Box.createVerticalStrut(14));

            body.add(buildDetailSection("BON D'ACHAT"));
            body.add(buildDetailRow("Nombre de bons",  String.valueOf(nbBons)));
            body.add(buildDetailRow("Valeur unitaire",  String.format("Rs %,.2f", valUnit)));
            body.add(buildDetailRow("Montant total",    String.format("Rs %,.2f", total)));
            body.add(buildDetailRow("Validit\u00e9",   validite > 0 ? validite + " jours" : "\u2014"));
            body.add(Box.createVerticalStrut(14));

            body.add(buildDetailSection("POINT DE VENTE & TRA\u00c7ABILIT\u00c9"));
            body.add(buildDetailRow("Magasin",      magasin  != null ? magasin  : "\u2014"));
            body.add(buildDetailRow("Cr\u00e9\u00e9 par", createur != null ? createur : "\u2014"));
            body.add(buildDetailRow("Date cr\u00e9ation",
                    dateD != null && dateD.length() >= 10 ? dateD.substring(0, 10) : "\u2014"));

            JScrollPane sc = new JScrollPane(body);
            sc.setBorder(null); sc.setOpaque(false); sc.getViewport().setOpaque(false);
            root.add(sc, BorderLayout.CENTER);
            root.add(buildDialogActions(demandeId, statut != null ? statut : "", dlg), BorderLayout.SOUTH);

            dlg.add(root);
            dlg.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── ACTIONS DIALOG ──────────────────────────────────────────────────────
    private JPanel buildDialogActions(int demandeId, String statut, JDialog dlg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT));

        JButton btnClose = buildOutlineButton("Fermer");
        btnClose.addActionListener(e -> dlg.dispose());
        p.add(btnClose);

        if (ST_ATTENTE_PAIEMENT.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Comptable"))) {
            JButton btn = buildRedButton("Valider Paiement");
            btn.addActionListener(e -> { changerStatut(demandeId, ST_PAYE); dlg.dispose(); chargerDemandes(); });
            p.add(btn);
        }
        if (ST_PAYE.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Approbateur"))) {
            JButton btn = buildRedButton("Approuver");
            btn.addActionListener(e -> { changerStatut(demandeId, ST_APPROUVE); dlg.dispose(); chargerDemandes(); });
            p.add(btn);
        }
        if (!ST_REJETE.equals(statut) && !ST_GENERE.equals(statut) && role.equalsIgnoreCase("Administrateur")) {
            JButton btn = buildOutlineButton("Rejeter");
            btn.setForeground(RED_PRIMARY);
            btn.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(dlg, "Confirmer le rejet ?",
                        "Rejeter", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (conf == JOptionPane.YES_OPTION) { changerStatut(demandeId, ST_REJETE); dlg.dispose(); chargerDemandes(); }
            });
            p.add(btn);
        }
        return p;
    }

    // ── CHANGER STATUT ──────────────────────────────────────────────────────
    private void changerStatut(int demandeId, String nouveauStatut) {
        // La colonne statut s'appelle "statuts" dans la BD
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE demande SET statuts = ? WHERE demande_id = ?")) {
            ps.setString(1, nouveauStatut);
            ps.setInt(2, demandeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur : " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── NOUVELLE DEMANDE ────────────────────────────────────────────────────
    private void ouvrirNouvelledemande() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        FormulaireCreationBon dlg = new FormulaireCreationBon((JFrame) parent, userId, "", role);
        dlg.setVisible(true);
        chargerDemandes();
    }

    // ── HELPERS UI ──────────────────────────────────────────────────────────
    private JPanel buildDetailSection(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION); lbl.setForeground(RED_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        p.add(lbl, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        return p;
    }

    private JPanel buildDetailRow(String key, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel k = new JLabel(key);
        k.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        k.setForeground(TEXT_SECOND); k.setPreferredSize(new Dimension(130, 20));
        JLabel v = new JLabel(value);
        v.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        v.setForeground(TEXT_PRIMARY);
        row.add(k, BorderLayout.WEST); row.add(v, BorderLayout.CENTER);
        return row;
    }

    private JButton buildRedButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            { setFont(FONT_BTN); setForeground(Color.WHITE);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 28, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? RED_DARK : RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildOutlineButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            { setFont(FONT_BTN); setForeground(TEXT_SECOND);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 24, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? new Color(245,246,250) : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(BORDER_LIGHT); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildIconButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            { setFont(new Font("Trebuchet MS", Font.BOLD, 16)); setForeground(TEXT_SECOND);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setToolTipText(tooltip); setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(36, 36));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0,0,0,8));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildCloseBtn(Runnable action) {
        JButton btn = new JButton("\u2715") {
            { setFont(new Font("Trebuchet MS", Font.BOLD, 13));
                setForeground(new Color(255,255,255,180));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(30, 30));
                addActionListener(e -> action.run());
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { setForeground(Color.WHITE); }
                    public void mouseExited(MouseEvent e)  { setForeground(new Color(255,255,255,180)); }
                });
            }
        };
        return btn;
    }
}