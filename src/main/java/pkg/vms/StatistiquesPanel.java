package pkg.vms;

import pkg.vms.controller.StatistiquesController;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Panneau Statistiques & Rapports du VMS Intermart Maurice.
 * Affiche les KPI, la repartition par statut, l'activite recente et le top clients.
 */
public class StatistiquesPanel extends JPanel {

    // ── Palette (Centralisee via VMSStyle) ──────────────────────────────────
    private static final Color BG_ROOT      = VMSStyle.BG_ROOT;
    private static final Color BG_CARD      = VMSStyle.BG_CARD;
    private static final Color BG_CARD_HOVER = VMSStyle.BG_CARD_HOVER;
    private static final Color RED_PRIMARY  = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK     = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT    = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED   = VMSStyle.TEXT_MUTED;
    private static final Color ACCENT_BLUE  = VMSStyle.ACCENT_BLUE;
    private static final Color SUCCESS      = VMSStyle.SUCCESS;
    private static final Color WARNING      = VMSStyle.WARNING;
    private static final Color SHADOW_COLOR = VMSStyle.SHADOW_COLOR;
    private static final Color ACCENT_PURP  = new Color(124, 58, 237);

    // ── Fonts (Centralisees via VMSStyle) ────────────────────────────────────
    private static final Font FONT_PAGE_TITLE = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_SECTION    = new Font("Georgia", Font.BOLD, 15);
    private static final Font FONT_TABLE_HDR  = new Font("Trebuchet MS", Font.BOLD, 12);
    private static final Font FONT_TABLE_CELL = new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_BADGE      = new Font("Trebuchet MS", Font.BOLD, 10);
    private static final Font FONT_KPI_VAL    = VMSStyle.FONT_KPI_VAL;
    private static final Font FONT_KPI_LBL    = VMSStyle.FONT_KPI_LBL;
    private static final Font FONT_BTN        = new Font("Trebuchet MS", Font.BOLD, 12);

    private static final DecimalFormat FMT_MONTANT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat FMT_PERCENT = new DecimalFormat("0.0");

    // Statuts connus
    private static final String ST_ATTENTE_PAIEMENT = "EN_ATTENTE_PAIEMENT";
    private static final String ST_PAYE             = "PAYE";
    private static final String ST_APPROUVE         = "APPROUVE";
    private static final String ST_GENERE           = "GENERE";
    private static final String ST_REJETE           = "REJETE";
    private static final String ST_ENVOYE           = "ENVOYE";

    // KPI labels (dynamiques)
    private JLabel kpiTotalDemandesVal;
    private JLabel kpiMontantTotalVal;
    private JLabel kpiBonsActifsVal;
    private JLabel kpiTauxRedemptionVal;

    // Tables
    private DefaultTableModel statutTableModel;
    private DefaultTableModel auditTableModel;
    private DefaultTableModel topClientsTableModel;
    private DefaultTableModel bonsExpirationTableModel;

    private final String role;
    private final int    userId;
    private final StatistiquesController controller = new StatistiquesController();

    public StatistiquesPanel(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerDonneesAsync();
    }

    // ── INIT ────────────────────────────────────────────────────────────────
    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);

        // Scrollable content
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(22));
        content.add(buildKpiStrip());
        content.add(Box.createVerticalStrut(22));
        content.add(buildMiddleSection());
        content.add(Box.createVerticalStrut(22));
        content.add(buildTopClientsSection());
        content.add(Box.createVerticalStrut(22));
        content.add(buildBonsExpirationSection());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        wrapper.add(scroll, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── HEADER ──────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

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
        JLabel icon = new JLabel("≡");
        icon.setFont(new Font("Georgia", Font.BOLD, 26));
        icon.setForeground(RED_PRIMARY);
        JLabel title = new JLabel("  Statistiques & Rapports");
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_PRIMARY);
        titleRow.add(icon);
        titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Vue d'ensemble des indicateurs et de l'activité du système VMS");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(5, 14, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titleRow);
        left.add(sub);

        JButton btnRefresh = buildIconButton("↻", "Actualiser les données");
        btnRefresh.addActionListener(e -> chargerDonneesAsync());

        h.add(left, BorderLayout.CENTER);
        h.add(btnRefresh, BorderLayout.EAST);
        return h;
    }

    // ── KPI STRIP ───────────────────────────────────────────────────────────
    private JPanel buildKpiStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 4, 14, 0));
        strip.setOpaque(false);
        strip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        strip.setPreferredSize(new Dimension(0, 110));

        kpiTotalDemandesVal = new JLabel("...");
        kpiMontantTotalVal  = new JLabel("...");
        kpiBonsActifsVal    = new JLabel("...");
        kpiTauxRedemptionVal = new JLabel("...");

        strip.add(buildKpiCard(ACCENT_BLUE, "⌂", "Total Demandes", kpiTotalDemandesVal));
        strip.add(buildKpiCard(SUCCESS, "Rs", "Montant Total Bons", kpiMontantTotalVal));
        strip.add(buildKpiCard(WARNING, "◈", "Bons Actifs", kpiBonsActifsVal));
        strip.add(buildKpiCard(RED_PRIMARY, "%", "Taux Rédemption", kpiTauxRedemptionVal));

        return strip;
    }

    private JPanel buildKpiCard(Color accent, String iconText, String label, JLabel valueLabel) {
        valueLabel.setFont(FONT_KPI_VAL);
        valueLabel.setForeground(accent);

        JLabel lblL = new JLabel(label);
        lblL.setFont(FONT_KPI_LBL);
        lblL.setForeground(TEXT_SECOND);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        left.add(valueLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(lblL);

        // Icon bubble
        JPanel iconBubble = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(44, 44); }
        };
        iconBubble.setOpaque(false);
        JLabel iconL = new JLabel(iconText);
        iconL.setFont(new Font("Georgia", Font.BOLD, 18));
        iconL.setForeground(accent);
        iconBubble.add(iconL);

        JPanel kpi = new JPanel() {
            boolean h = false;
            {
                setOpaque(false);
                setLayout(new BorderLayout());
                setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(SHADOW_COLOR);
                g2.fill(new RoundRectangle2D.Double(2, 3, getWidth() - 4, getHeight() - 2, 12, 12));
                // Card body
                g2.setColor(h ? BG_CARD_HOVER : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 2, getHeight() - 2, 12, 12));
                // Accent bar at top
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 2, 3, 3, 3));
                // Border
                g2.setColor(h ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 50) : BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 2, getHeight() - 2, 12, 12));
            }
        };
        kpi.add(left, BorderLayout.CENTER);
        kpi.add(iconBubble, BorderLayout.EAST);
        return kpi;
    }

    // ── MIDDLE SECTION (two columns) ────────────────────────────────────────
    private JPanel buildMiddleSection() {
        JPanel mid = new JPanel(new GridLayout(1, 2, 16, 0));
        mid.setOpaque(false);
        mid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 340));
        mid.setPreferredSize(new Dimension(0, 340));

        mid.add(buildStatutCard());
        mid.add(buildAuditCard());
        return mid;
    }

    // ── Repartition par Statut ──────────────────────────────────────────────
    private JPanel buildStatutCard() {
        JPanel card = buildCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel titleL = new JLabel("Répartition par Statut");
        titleL.setFont(FONT_SECTION);
        titleL.setForeground(TEXT_PRIMARY);
        card.add(titleL, BorderLayout.NORTH);

        String[] cols = {"Statut", "Nombre", "Montant (Rs)"};
        statutTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(statutTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                return c;
            }
        };
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);

        // Status badge renderer for column 0
        table.getColumnModel().getColumn(0).setCellRenderer(new StatutBadgeRenderer());

        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── Activite Recente ────────────────────────────────────────────────────
    private JPanel buildAuditCard() {
        JPanel card = buildCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel titleL = new JLabel("Activité Récente");
        titleL.setFont(FONT_SECTION);
        titleL.setForeground(TEXT_PRIMARY);
        card.add(titleL, BorderLayout.NORTH);

        String[] cols = {"Action", "Description", "Date", "Utilisateur"};
        auditTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(auditTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                return c;
            }
        };
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── Top Clients ─────────────────────────────────────────────────────────
    private JPanel buildTopClientsSection() {
        JPanel card = buildCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        card.setPreferredSize(new Dimension(0, 260));

        JLabel titleL = new JLabel("Top Clients");
        titleL.setFont(FONT_SECTION);
        titleL.setForeground(TEXT_PRIMARY);

        JLabel subtitleL = new JLabel("  (par montant total des demandes)");
        subtitleL.setFont(new Font("Trebuchet MS", Font.ITALIC, 11));
        subtitleL.setForeground(TEXT_MUTED);

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        headerRow.setOpaque(false);
        headerRow.add(titleL);
        headerRow.add(subtitleL);
        card.add(headerRow, BorderLayout.NORTH);

        String[] cols = {"#", "Client", "Nb Demandes", "Montant Total (Rs)"};
        topClientsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(topClientsTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                return c;
            }
        };
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Right-align numeric columns
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);

        // Center-align rank column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── ASYNC DATA LOADING ──────────────────────────────────────────────────
    private void chargerDonneesAsync() {
        controller.chargerDonnees(role, userId,
            data -> {
                kpiTotalDemandesVal.setText(String.valueOf(data.totalDemandes));
                kpiMontantTotalVal.setText(FMT_MONTANT.format(data.montantTotal));
                kpiBonsActifsVal.setText(String.valueOf(data.bonsActifs));
                kpiTauxRedemptionVal.setText(FMT_PERCENT.format(data.tauxRedemption) + " %");

                statutTableModel.setRowCount(0);
                for (Object[] row : data.statutRows) {
                    statutTableModel.addRow(new Object[]{
                        row[0], row[1], FMT_MONTANT.format((Double) row[2])
                    });
                }

                auditTableModel.setRowCount(0);
                for (Object[] row : data.auditRows) {
                    auditTableModel.addRow(row);
                }

                topClientsTableModel.setRowCount(0);
                for (Object[] row : data.topClientsRows) {
                    topClientsTableModel.addRow(new Object[]{
                        row[0], row[1], row[2], FMT_MONTANT.format((Double) row[3])
                    });
                }

                bonsExpirationTableModel.setRowCount(0);
                for (Object[] row : data.bonsExpirationRows) {
                    bonsExpirationTableModel.addRow(row);
                }
            },
            err -> System.err.println("Erreur chargement stats: " + err)
        );
    }

    // ── Bons Proches Expiration ─────────────────────────────────────────────
    private JPanel buildBonsExpirationSection() {
        JPanel card = buildCardPanel();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        card.setPreferredSize(new Dimension(0, 280));

        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleBlock.setOpaque(false);
        JLabel titleL = new JLabel("Bons Proches d'Expiration");
        titleL.setFont(FONT_SECTION);
        titleL.setForeground(TEXT_PRIMARY);
        JLabel subtitleL = new JLabel("  (seuil configurable dans Paramètres → Bons Cadeau)");
        subtitleL.setFont(new Font("Trebuchet MS", Font.ITALIC, 11));
        subtitleL.setForeground(TEXT_MUTED);
        titleBlock.add(titleL);
        titleBlock.add(subtitleL);
        card.add(titleBlock, BorderLayout.NORTH);

        String[] cols = {"Code Unique", "Valeur (Rs)", "Client", "Expiration", "Jours restants"};
        bonsExpirationTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(bonsExpirationTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                // Rouge pale si < 7 jours restants
                try {
                    Object joursObj = getValueAt(row, 4);
                    if (joursObj != null && !isRowSelected(row)) {
                        double jours = Double.parseDouble(joursObj.toString());
                        if (jours < 7) c.setBackground(new Color(254, 226, 226));
                    }
                } catch (NumberFormatException ignored) {}
                return c;
            }
        };
        styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(160);
        table.getColumnModel().getColumn(3).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(90);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getViewport().setBackground(BG_CARD);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ── STYLE HELPERS ───────────────────────────────────────────────────────

    /** Builds a standard card panel with rounded corners, shadow, and border. */
    private JPanel buildCardPanel() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(SHADOW_COLOR);
                g2.fill(new RoundRectangle2D.Double(2, 3, getWidth() - 4, getHeight() - 2, 14, 14));
                // Card body
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth() - 2, getHeight() - 2, 14, 14));
                // Border
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 2, getHeight() - 2, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    /** Apply standard table styling matching GestionDemande. */
    private void styleTable(JTable table) {
        table.setFont(FONT_TABLE_CELL);
        table.setRowHeight(40);
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
        header.setPreferredSize(new Dimension(0, 38));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));
    }

    /** Statut badge renderer (colored pills). */
    private class StatutBadgeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String statut = val != null ? val.toString() : "";
            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(badgeBg(statut));
                    g2.fill(new RoundRectangle2D.Double(4, 8, getWidth() - 8, getHeight() - 16, 20, 20));
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249, 250, 252)));
            JLabel lbl = new JLabel(badgeLabel(statut));
            lbl.setFont(FONT_BADGE);
            lbl.setForeground(badgeFg(statut));
            badge.add(lbl);
            return badge;
        }

        private Color badgeBg(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return new Color(255, 237, 213);
                case ST_PAYE:             return new Color(219, 234, 254);
                case ST_APPROUVE:         return new Color(237, 233, 254);
                case ST_GENERE:           return new Color(220, 252, 231);
                case ST_ENVOYE:           return new Color(209, 250, 246);
                case ST_REJETE:           return new Color(254, 226, 226);
                case "ARCHIVE":           return new Color(241, 245, 249);
                default:                  return new Color(241, 245, 249);
            }
        }

        private Color badgeFg(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return WARNING;
                case ST_PAYE:             return ACCENT_BLUE;
                case ST_APPROUVE:         return ACCENT_PURP;
                case ST_GENERE:           return SUCCESS;
                case ST_ENVOYE:           return new Color(20, 170, 190);
                case ST_REJETE:           return RED_PRIMARY;
                case "ARCHIVE":           return new Color(100, 116, 139);
                default:                  return TEXT_MUTED;
            }
        }

        private String badgeLabel(String s) {
            switch (s) {
                case ST_ATTENTE_PAIEMENT: return "⏳  En attente";
                case ST_PAYE:             return "✓  Payée";
                case ST_APPROUVE:         return "✓  Approuvée";
                case ST_GENERE:           return "◈  Générée";
                case ST_ENVOYE:           return "→  Envoyée";
                case ST_REJETE:           return "✕  Rejetée";
                default:                  return s;
            }
        }
    }

    /** Icon button matching the GestionDemande style. */
    private JButton buildIconButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 16));
                setForeground(TEXT_SECOND);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setToolTipText(tooltip);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(36, 36));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0, 0, 0, 8));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }
}
