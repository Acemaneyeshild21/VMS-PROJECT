package pkg.vms;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.controller.DemandeController;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class GestionDemande extends JPanel {

    // ── Palette (Centralisée via VMSStyle) ───────────────────────────────────────
    private static final Color BG_ROOT      = VMSStyle.BG_ROOT;
    private static final Color BG_CARD      = VMSStyle.BG_CARD;
    private static final Color RED_PRIMARY  = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK     = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT    = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED   = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS      = VMSStyle.SUCCESS;
    private static final Color WARNING      = VMSStyle.WARNING;
    private static final Color ACCENT_BLUE  = VMSStyle.ACCENT_BLUE;
    private static final Color ACCENT_PURP  = new Color(124,  58, 237);

    // Vrais noms de statuts dans la BD (colonne: statuts)
    private static final String ST_ATTENTE_PAIEMENT = "EN_ATTENTE_PAIEMENT";
    private static final String ST_PAYE             = "PAYE";
    private static final String ST_APPROUVE         = "APPROUVE";
    private static final String ST_GENERE           = "GENERE";
    private static final String ST_REJETE           = "REJETE";
    private static final String ST_ENVOYE           = "ENVOYE";

    // ── Fonts (Centralisées via VMSStyle) ────────────────────────────────────────────
    private static final Font FONT_PAGE_TITLE = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_SECTION    = VMSStyle.FONT_BADGE.deriveFont(9f);
    private static final Font FONT_TABLE_HDR  = VMSStyle.FONT_BADGE.deriveFont(12f);
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
    private final DemandeController controller = new DemandeController();

    private JButton           btnNouveau;
    private DefaultTableModel tableModel;
    private JTable            table;
    private JTextField        txtSearch;
    private JLabel            lblTotal;
    private String            filtreStatutActif = "TOUTES";

    public GestionDemande(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        boolean canCreate = "Administrateur".equalsIgnoreCase(role) || "Manager".equalsIgnoreCase(role)
                            || "Collaborateur".equalsIgnoreCase(role);
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerDemandes();

        if (!canCreate && btnNouveau != null) {
            btnNouveau.setVisible(false);
        }
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

    // ── HEADER ──────────────────────────────────────────────────────────────────────
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
        JLabel icon  = new JLabel("📋");
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

        btnNouveau = UIUtils.buildRedButton("+ Nouvelle Demande");
        btnNouveau.addActionListener(e -> ouvrirNouvelledemande());
        h.add(left,   BorderLayout.CENTER);
        h.add(btnNouveau, BorderLayout.EAST);
        return h;
    }

    // ── TOOLBAR ───────────────────────────────────────────────────────────────────────
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
        chips.add(buildChip("Envoyées",   new Color(20,170,190), ST_ENVOYE));
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
                    g2.drawString("🔍  Rechercher...", i.left, getHeight() - i.bottom - 4);
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
        JButton btnRefresh = buildIconButton("↻", "Actualiser");
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

    // ── TABLE CARD ───────────────────────────────────────────────────────────────────
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

    private void chargerDemandes() {
        controller.chargerDemandes(
            rows -> {
                tableModel.setRowCount(0);
                for (VoucherDAO.DemandeComplet dc : rows) {
                    tableModel.addRow(new Object[]{
                        dc.id, dc.reference, dc.client, dc.nbBons,
                        dc.valeurUnit, dc.montantTotal, dc.magasin,
                        dc.dateCreation, dc.validiteJours, dc.statut, "actions"
                    });
                }
                lblTotal.setText(rows.size() + " demande" + (rows.size() > 1 ? "s" : "") + " au total");
            },
            err -> lblTotal.setText("Erreur chargement : " + err)
        );
    }

    // ── FILTRES ───────────────────────────────────────────────────────────────────────────
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

    // ── RENDERER STATUT ───────────────────────────────────────────────────────────────────
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
                case ST_ENVOYE:          return new Color(209,250,246);
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
                case ST_ENVOYE:          return new Color(20,170,190);
                case ST_REJETE:          return RED_PRIMARY;
                default:                 return TEXT_MUTED;
            }
        }
        private String badgeLabel(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return "⏳  En attente";
                case ST_PAYE:            return "💳  Payée";
                case ST_APPROUVE:        return "✅  Approuvée";
                case ST_GENERE:          return "🎟  Générée";
                case ST_ENVOYE:          return "📨  Envoyée";
                case ST_REJETE:          return "❌  Rejetée";
                default:                 return s;
            }
        }
    }

    // ── RENDERER ACTIONS ────────────────────────────────────────────────────────────────────
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

    // ── EDITOR ACTIONS ─────────────────────────────────────────────────────────────────────
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

    // ── DETAIL DEMANDE ────────────────────────────────────────────────────────────────────
    private void afficherDetailDemande(int demandeId) {
        controller.chargerDetail(demandeId,
            detail -> buildAndShowDetailDialog(demandeId, detail),
            err -> JOptionPane.showMessageDialog(this,
                "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE)
        );
    }

    private void buildAndShowDetailDialog(int demandeId, VoucherDAO.DemandeDetail detail) {
        String ref      = detail.reference;
        String client   = detail.client;
        String emailCli = detail.emailClient;
        String emailEnv = detail.emailEnvoi;
        int    nbBons   = detail.nbBons;
        double valUnit  = detail.valeurUnit;
        double total    = detail.total;
        int    validite = detail.validite;
        String magasin  = detail.magasin;
        String dateD    = detail.dateCreation;
        String statut   = detail.statut;
        String createur = detail.createur;

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Détail — " + (ref != null ? ref : "#" + demandeId), true);
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
        JLabel lblRef = new JLabel("📋  " + (ref != null ? ref : "#" + demandeId));
        lblRef.setFont(new Font("Georgia", Font.BOLD, 18));
        lblRef.setForeground(Color.WHITE);
        JLabel lblSt = new JLabel(statut != null ? statut : "—");
        lblSt.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblSt.setForeground(new Color(255,255,255,180));
        hdrLeft.add(lblRef); hdrLeft.add(lblSt);
        hdr.add(hdrLeft, BorderLayout.CENTER);
        hdr.add(buildCloseBtn(() -> dlg.dispose()), BorderLayout.EAST);
        root.add(hdr, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        body.add(buildDetailSection("BÉNÉFICIAIRE"));
        body.add(buildDetailRow("Client",       client   != null ? client   : "—"));
        body.add(buildDetailRow("Email client", emailCli != null ? emailCli : "—"));
        body.add(buildDetailRow("Email envoi",  emailEnv != null ? emailEnv : "—"));
        body.add(Box.createVerticalStrut(14));

        body.add(buildDetailSection("BON D'ACHAT"));
        body.add(buildDetailRow("Nombre de bons",  String.valueOf(nbBons)));
        body.add(buildDetailRow("Valeur unitaire",  String.format("Rs %,.2f", valUnit)));
        body.add(buildDetailRow("Montant total",    String.format("Rs %,.2f", total)));
        body.add(buildDetailRow("Validité",   validite > 0 ? validite + " jours" : "—"));
        body.add(Box.createVerticalStrut(14));

        body.add(buildDetailSection("POINT DE VENTE & TRAÇABILITÉ"));
        body.add(buildDetailRow("Magasin",         magasin  != null ? magasin  : "—"));
        body.add(buildDetailRow("Créé par", createur != null ? createur : "—"));
        body.add(buildDetailRow("Date création",
                dateD != null && dateD.length() >= 10 ? dateD.substring(0, 10) : "—"));

        JScrollPane sc = new JScrollPane(body);
        sc.setBorder(null); sc.setOpaque(false); sc.getViewport().setOpaque(false);
        root.add(sc, BorderLayout.CENTER);
        root.add(buildDialogActions(demandeId, statut != null ? statut : "", dlg), BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── ACTIONS DIALOG ────────────────────────────────────────────────────────────────────
    private JPanel buildDialogActions(int demandeId, String statut, JDialog dlg) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT));

        JButton btnClose = UIUtils.buildOutlineButton("Fermer");
        btnClose.addActionListener(e -> dlg.dispose());
        p.add(btnClose);

        if (ST_ATTENTE_PAIEMENT.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Comptable") || role.equalsIgnoreCase("Manager"))) {
            JButton btn = UIUtils.buildRedButton("Valider Paiement");
            btn.addActionListener(e -> { dlg.dispose(); changerStatut(demandeId, ST_PAYE); });
            p.add(btn);
        }

        if (ST_PAYE.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Approbateur") || role.equalsIgnoreCase("Manager"))) {
            JButton btn = UIUtils.buildRedButton("Approuver");
            btn.addActionListener(e -> { dlg.dispose(); changerStatut(demandeId, ST_APPROUVE); });
            p.add(btn);
        }

        if (ST_APPROUVE.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Manager") || role.equalsIgnoreCase("Approbateur"))) {
            JButton btn = UIUtils.buildRedButton("🎟 Générer les Bons");
            btn.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(dlg,
                        "Générer les bons PDF pour cette demande ?\nLes bons seront créés et envoyés par email.",
                        "Génération des bons", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (conf == JOptionPane.YES_OPTION) {
                    genererBonsDemande(demandeId, dlg);
                }
            });
            p.add(btn);
        }

        if (ST_GENERE.equals(statut) &&
                (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Manager"))) {
            JButton btn = UIUtils.buildOutlineButton("↺ Renvoyer Email");
            btn.setForeground(ACCENT_BLUE);
            btn.addActionListener(e -> { dlg.dispose(); renvoyerEmail(demandeId); });
            p.add(btn);
        }

        if (!ST_REJETE.equals(statut) && !ST_GENERE.equals(statut) && !ST_ENVOYE.equals(statut)
                && (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Manager") || role.equalsIgnoreCase("Approbateur"))) {
            JButton btn = UIUtils.buildOutlineButton("Rejeter");
            btn.setForeground(RED_PRIMARY);
            btn.addActionListener(e -> {
                int conf = JOptionPane.showConfirmDialog(dlg, "Confirmer le rejet ?",
                        "Rejeter", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (conf == JOptionPane.YES_OPTION) { dlg.dispose(); changerStatut(demandeId, ST_REJETE); }
            });
            p.add(btn);
        }
        return p;
    }

    // ── CHANGER STATUT ──────────────────────────────────────────────────────────────────────
    private void changerStatut(int demandeId, String nouveauStatut) {
        controller.changerStatut(demandeId, nouveauStatut, userId,
            () -> chargerDemandes(),
            err -> JOptionPane.showMessageDialog(this,
                "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE)
        );
    }

    // ── GENERER LES BONS ───────────────────────────────────────────────────────────────────
    private void genererBonsDemande(int demandeId, JDialog dlg) {
        dlg.dispose();

        JDialog progressDlg = new JDialog(
            (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
            "Génération en cours…", true);
        progressDlg.setUndecorated(true);
        progressDlg.setSize(420, 130);
        progressDlg.setLocationRelativeTo(this);
        progressDlg.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        JPanel pRoot = new JPanel(new BorderLayout(0, 0));
        pRoot.setBackground(BG_CARD);
        pRoot.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_LIGHT, 1),
            BorderFactory.createEmptyBorder(20, 24, 20, 24)));

        JLabel lblStep = new JLabel("Initialisation…");
        lblStep.setFont(FONT_TABLE_CELL);
        lblStep.setForeground(TEXT_PRIMARY);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setForeground(RED_PRIMARY);
        bar.setBackground(new Color(240, 242, 246));
        bar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        pRoot.add(lblStep, BorderLayout.NORTH);
        pRoot.add(bar, BorderLayout.CENTER);
        progressDlg.add(pRoot);

        controller.genererBons(demandeId, userId,
            progress -> {
                bar.setValue(Integer.parseInt(progress[0]));
                bar.setString(progress[0] + " %");
                lblStep.setText(progress[1]);
            },
            nb -> {
                progressDlg.dispose();
                chargerDemandes();
                JOptionPane.showMessageDialog(GestionDemande.this,
                    nb + " bon(s) générés et envoyés avec succès !",
                    "Génération terminée", JOptionPane.INFORMATION_MESSAGE);
            },
            err -> {
                progressDlg.dispose();
                JOptionPane.showMessageDialog(GestionDemande.this,
                    "Erreur lors de la génération : " + err,
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            },
            nb -> {
                progressDlg.dispose();
                chargerDemandes();
                afficherAlerteEmailEchec(demandeId, nb);
            }
        );

        progressDlg.setVisible(true);
    }

    // ── ALERTE ECHEC EMAIL ───────────────────────────────────────────────────────────────────
    private void afficherAlerteEmailEchec(int demandeId, int nbBons) {
        JDialog errDlg = new JDialog(
            (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
            "Échec envoi email", true);
        errDlg.setUndecorated(true);
        errDlg.setSize(460, 200);
        errDlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.setBackground(BG_CARD);

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setBackground(WARNING);
        hdr.setPreferredSize(new Dimension(0, 52));
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        JLabel hdrLbl = new JLabel("⚠  Échec de l'envoi email");
        hdrLbl.setFont(new Font("Georgia", Font.BOLD, 15));
        hdrLbl.setForeground(Color.WHITE);
        hdr.add(hdrLbl, BorderLayout.CENTER);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(16, 22, 12, 22));
        JLabel l1 = new JLabel(nbBons + " bon(s) générés avec succès — l'envoi email a échoué.");
        l1.setFont(FONT_TABLE_CELL); l1.setForeground(TEXT_PRIMARY);
        JLabel l2 = new JLabel("L'erreur est enregistrée dans email_errors. Vous pouvez relancer.");
        l2.setFont(new Font("Trebuchet MS", Font.PLAIN, 11)); l2.setForeground(TEXT_SECOND);
        body.add(l1); body.add(Box.createVerticalStrut(5)); body.add(l2);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        btns.setOpaque(false);
        JButton btnFermer = UIUtils.buildOutlineButton("Fermer");
        btnFermer.addActionListener(e -> errDlg.dispose());
        JButton btnRenvoyer = UIUtils.buildRedButton("↺ Renvoyer");
        btnRenvoyer.addActionListener(e -> { errDlg.dispose(); renvoyerEmail(demandeId); });
        btns.add(btnFermer); btns.add(btnRenvoyer);

        root.add(hdr, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        errDlg.add(root);
        errDlg.setVisible(true);
    }

    // ── RELANCE EMAIL ─────────────────────────────────────────────────────────────────────────
    private void renvoyerEmail(int demandeId) {
        controller.renvoyerEmail(demandeId, userId,
            () -> {
                chargerDemandes();
                JOptionPane.showMessageDialog(GestionDemande.this,
                    "Emails renvoyés avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
            },
            err -> JOptionPane.showMessageDialog(GestionDemande.this,
                "Échec du renvoi : " + err + "\n\nL'erreur a été enregistrée (tentative supplémentaire).",
                "Erreur", JOptionPane.ERROR_MESSAGE)
        );
    }

    // ── NOUVELLE DEMANDE ────────────────────────────────────────────────────────────────────
    private void ouvrirNouvelledemande() {
        Dashboard db = (Dashboard) SwingUtilities.getWindowAncestor(this);
        if (db != null) {
            db.showPanel(new FormulaireCreationBon(userId, ""));
        }
    }

    // ── HELPERS UI ───────────────────────────────────────────────────────────────────────────
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
        JButton btn = new JButton("✕") {
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
