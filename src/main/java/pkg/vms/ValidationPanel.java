package pkg.vms;

import pkg.vms.DAO.DBconnect;
import pkg.vms.DAO.VoucherDAO;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panneau de validation des demandes de bons cadeau.
 * Deux onglets : Paiements (Comptable/Admin) et Approbations (Approbateur/Manager/Admin).
 */
public class ValidationPanel extends JPanel {

    // ── Palette (Centralisee via VMSStyle) ──────────────────────────────────
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

    // Statuts BD
    private static final String ST_ATTENTE_PAIEMENT = "EN_ATTENTE_PAIEMENT";
    private static final String ST_PAYE             = "PAYE";
    private static final String ST_APPROUVE         = "APPROUVE";
    private static final String ST_REJETE           = "REJETE";

    // ── Fonts (Centralisees via VMSStyle) ────────────────────────────────────
    private static final Font FONT_PAGE_TITLE = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_TABLE_HDR  = VMSStyle.FONT_BADGE.deriveFont(12f);
    private static final Font FONT_TABLE_CELL = new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_BADGE      = new Font("Trebuchet MS", Font.BOLD,  10);
    private static final Font FONT_BTN        = new Font("Trebuchet MS", Font.BOLD,  12);
    private static final Font FONT_TAB        = new Font("Trebuchet MS", Font.BOLD,  13);

    // Colonnes tableau
    private static final String[] COLS = {
            "#", "R\u00e9f\u00e9rence", "Client", "Nb Bons", "Valeur Unit.",
            "Montant Total", "Date", "Actions"
    };
    private static final int COL_ID      = 0;
    private static final int COL_REF     = 1;
    private static final int COL_CLIENT  = 2;
    private static final int COL_NB      = 3;
    private static final int COL_VU      = 4;
    private static final int COL_TOTAL   = 5;
    private static final int COL_DATE    = 6;
    private static final int COL_ACTIONS = 7;

    private final String role;
    private final int    userId;

    private DefaultTableModel tableModel;
    private JTable            table;
    private JLabel            lblTotal;
    private JButton           tabPaiements;
    private JButton           tabApprobations;
    private JLabel            badgePaiements;
    private JLabel            badgeApprobations;
    private String            activeTab; // "PAIEMENTS" ou "APPROBATIONS"

    private boolean canPaiement;
    private boolean canApprobation;

    public ValidationPanel(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        this.canPaiement    = "Comptable".equalsIgnoreCase(role) || "Administrateur".equalsIgnoreCase(role);
        this.canApprobation = "Approbateur".equalsIgnoreCase(role) || "Manager".equalsIgnoreCase(role)
                              || "Administrateur".equalsIgnoreCase(role);

        setLayout(new BorderLayout());
        setOpaque(false);

        if (!canPaiement && !canApprobation) {
            initNoAccess();
        } else {
            activeTab = canPaiement ? "PAIEMENTS" : "APPROBATIONS";
            initComponents();
            chargerDonnees();
        }
    }

    // ── NO ACCESS ──────────────────────────────────────────────────────────
    private void initNoAccess() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JLabel msg = new JLabel("Vous n'avez pas les permissions n\u00e9cessaires");
        msg.setFont(new Font("Trebuchet MS", Font.PLAIN, 15));
        msg.setForeground(TEXT_MUTED);
        center.add(msg);
        add(center, BorderLayout.CENTER);
    }

    // ── INIT ───────────────────────────────────────────────────────────────
    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        mid.add(buildTabBar(),     BorderLayout.NORTH);
        mid.add(buildTableCard(),  BorderLayout.CENTER);
        wrapper.add(mid, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── HEADER ─────────────────────────────────────────────────────────────
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
        JLabel icon  = new JLabel("\u2713");
        icon.setFont(new Font("Trebuchet MS", Font.BOLD, 26));
        icon.setForeground(RED_PRIMARY);
        JLabel title = new JLabel("  Validation");
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_PRIMARY);
        titleRow.add(icon);
        titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        String subtitleText = buildSubtitle();
        JLabel sub = new JLabel(subtitleText);
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(5, 14, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(titleRow);
        left.add(sub);

        JButton btnRefresh = buildIconButton("\u21BB", "Actualiser");
        btnRefresh.addActionListener(e -> chargerDonnees());

        h.add(left,       BorderLayout.CENTER);
        h.add(btnRefresh, BorderLayout.EAST);
        return h;
    }

    private String buildSubtitle() {
        if (canPaiement && canApprobation) {
            return "Validation des paiements et approbation des demandes";
        } else if (canPaiement) {
            return "Validation des paiements en attente";
        } else {
            return "Approbation des demandes pay\u00e9es";
        }
    }

    // ── TAB BAR ────────────────────────────────────────────────────────────
    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);

        if (canPaiement) {
            badgePaiements = buildBadgeLabel("0");
            tabPaiements = buildTabButton("Paiements en attente", badgePaiements,
                    "PAIEMENTS".equals(activeTab));
            tabPaiements.addActionListener(e -> switchTab("PAIEMENTS"));
            bar.add(tabPaiements);
        }

        if (canApprobation) {
            badgeApprobations = buildBadgeLabel("0");
            tabApprobations = buildTabButton("Approbations en attente", badgeApprobations,
                    "APPROBATIONS".equals(activeTab));
            tabApprobations.addActionListener(e -> switchTab("APPROBATIONS"));
            bar.add(tabApprobations);
        }

        return bar;
    }

    private JLabel buildBadgeLabel(String text) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(FONT_BADGE);
        lbl.setForeground(Color.WHITE);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(24, 18));
        lbl.setOpaque(false);
        return lbl;
    }

    private JButton buildTabButton(String text, JLabel badge, boolean selected) {
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        badgeWrap.setOpaque(false);
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_TAB);
        badgeWrap.add(lbl);
        badgeWrap.add(badge);

        JButton btn = new JButton() {
            boolean sel = selected;
            {
                setLayout(new BorderLayout());
                add(badgeWrap, BorderLayout.CENTER);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 40, 36));
                updateColors(sel);
            }
            void updateColors(boolean s) {
                this.sel = s;
                lbl.setForeground(s ? Color.WHITE : TEXT_SECOND);
                repaint();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (sel) {
                    g2.setColor(RED_PRIMARY);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(RED_PRIMARY.getRed(), RED_PRIMARY.getGreen(),
                            RED_PRIMARY.getBlue(), 18));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                } else {
                    g2.setColor(new Color(TEXT_SECOND.getRed(), TEXT_SECOND.getGreen(),
                            TEXT_SECOND.getBlue(), 14));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                    g2.setColor(new Color(TEXT_SECOND.getRed(), TEXT_SECOND.getGreen(),
                            TEXT_SECOND.getBlue(), 60));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 20, 20));
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.putClientProperty("tabLabel", lbl);
        return btn;
    }

    private void switchTab(String tab) {
        activeTab = tab;
        // Update tab button visuals
        if (tabPaiements != null) {
            JLabel lbl = (JLabel) tabPaiements.getClientProperty("tabLabel");
            boolean sel = "PAIEMENTS".equals(tab);
            lbl.setForeground(sel ? Color.WHITE : TEXT_SECOND);
            // Access the anonymous inner class's sel field via reflection-free approach
            tabPaiements.putClientProperty("selected", sel);
            tabPaiements.repaint();
        }
        if (tabApprobations != null) {
            JLabel lbl = (JLabel) tabApprobations.getClientProperty("tabLabel");
            boolean sel = "APPROBATIONS".equals(tab);
            lbl.setForeground(sel ? Color.WHITE : TEXT_SECOND);
            tabApprobations.putClientProperty("selected", sel);
            tabApprobations.repaint();
        }
        chargerDonnees();
    }

    // ── TABLE CARD ─────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
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

        int[] widths = {40, 130, 160, 70, 100, 120, 120, 160};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(40);

        table.getColumnModel().getColumn(COL_ACTIONS).setCellRenderer(new ActionRenderer());
        table.getColumnModel().getColumn(COL_ACTIONS).setCellEditor(new ActionEditor());

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(COL_NB).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(COL_VU).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(rightRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.setOpaque(false);
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

        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    // ── CHARGEMENT BD ──────────────────────────────────────────────────────
    private void chargerDonnees() {
        String statut = "PAIEMENTS".equals(activeTab) ? ST_ATTENTE_PAIEMENT : ST_PAYE;

        new SwingWorker<List<Object[]>, Void>() {
            @Override
            protected List<Object[]> doInBackground() throws Exception {
                List<Object[]> data = new ArrayList<>();
                String sql = "SELECT d.demande_id, d.reference, c.name AS nom_client, d.nombre_bons, " +
                             "d.valeur_unitaire, d.montant_total, d.date_creation, d.statuts " +
                             "FROM demande d LEFT JOIN client c ON d.clientid = c.clientid " +
                             "WHERE d.statuts = ? ORDER BY d.date_creation DESC";
                try (Connection conn = DBconnect.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, statut);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            data.add(new Object[]{
                                rs.getInt("demande_id"),
                                rs.getString("reference"),
                                rs.getString("nom_client"),
                                rs.getInt("nombre_bons"),
                                rs.getDouble("valeur_unitaire"),
                                rs.getDouble("montant_total"),
                                rs.getString("date_creation"),
                                "actions"
                            });
                        }
                    }
                }
                return data;
            }

            @Override
            protected void done() {
                try {
                    tableModel.setRowCount(0);
                    List<Object[]> data = get();
                    for (Object[] row : data) {
                        tableModel.addRow(row);
                    }
                    int count = data.size();
                    String label = "PAIEMENTS".equals(activeTab)
                            ? "paiement" : "approbation";
                    lblTotal.setText(count + " " + label + (count > 1 ? "s" : "")
                            + " en attente");
                    updateBadgeCounts(statut, count);
                } catch (Exception e) {
                    e.printStackTrace();
                    lblTotal.setText("Erreur lors du chargement");
                }
            }
        }.execute();

        // Also update the other tab's badge count in parallel
        chargerBadgeAutreOnglet(statut);
    }

    private void updateBadgeCounts(String loadedStatut, int count) {
        if (ST_ATTENTE_PAIEMENT.equals(loadedStatut) && badgePaiements != null) {
            badgePaiements.setText(String.valueOf(count));
        } else if (ST_PAYE.equals(loadedStatut) && badgeApprobations != null) {
            badgeApprobations.setText(String.valueOf(count));
        }
    }

    private void chargerBadgeAutreOnglet(String loadedStatut) {
        String autreStatut = ST_ATTENTE_PAIEMENT.equals(loadedStatut) ? ST_PAYE : ST_ATTENTE_PAIEMENT;
        boolean needBadge = (ST_ATTENTE_PAIEMENT.equals(autreStatut) && badgePaiements != null)
                         || (ST_PAYE.equals(autreStatut) && badgeApprobations != null);
        if (!needBadge) return;

        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                String sql = "SELECT COUNT(*) FROM demande WHERE statuts = ?";
                try (Connection conn = DBconnect.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, autreStatut);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) return rs.getInt(1);
                    }
                }
                return 0;
            }

            @Override
            protected void done() {
                try {
                    int c = get();
                    if (ST_ATTENTE_PAIEMENT.equals(autreStatut) && badgePaiements != null) {
                        badgePaiements.setText(String.valueOf(c));
                    } else if (ST_PAYE.equals(autreStatut) && badgeApprobations != null) {
                        badgeApprobations.setText(String.valueOf(c));
                    }
                } catch (Exception ignored) { }
            }
        }.execute();
    }

    // ── RENDERER ACTIONS ───────────────────────────────────────────────────
    private class ActionRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 6));
            p.setOpaque(true);
            p.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249, 250, 252)));

            if ("PAIEMENTS".equals(activeTab)) {
                p.add(buildActionBtn("Valider", SUCCESS, false));
            } else {
                p.add(buildActionBtn("Approuver", SUCCESS, false));
                p.add(buildActionBtn("Rejeter", RED_PRIMARY, false));
            }
            return p;
        }
    }

    // ── EDITOR ACTIONS ─────────────────────────────────────────────────────
    private class ActionEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 6));
        private int currentRow = -1;

        @Override public Object getCellEditorValue() { return "actions"; }

        @Override public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col) {
            currentRow = row;
            p.removeAll();
            p.setOpaque(true);
            p.setBackground(RED_LIGHT);

            if ("PAIEMENTS".equals(activeTab)) {
                JButton btnValider = buildActionBtn("Valider", SUCCESS, true);
                btnValider.addActionListener(e -> {
                    fireEditingStopped();
                    validerPaiement();
                });
                p.add(btnValider);
            } else {
                JButton btnApprouver = buildActionBtn("Approuver", SUCCESS, true);
                btnApprouver.addActionListener(e -> {
                    fireEditingStopped();
                    approuverDemande();
                });
                JButton btnRejeter = buildActionBtn("Rejeter", RED_PRIMARY, true);
                btnRejeter.addActionListener(e -> {
                    fireEditingStopped();
                    rejeterDemande();
                });
                p.add(btnApprouver);
                p.add(btnRejeter);
            }
            return p;
        }

        private void validerPaiement() {
            if (currentRow < 0) return;
            int modelRow = table.convertRowIndexToModel(currentRow);
            int demandeId = (int) tableModel.getValueAt(modelRow, COL_ID);
            String ref = String.valueOf(tableModel.getValueAt(modelRow, COL_REF));

            int conf = JOptionPane.showConfirmDialog(ValidationPanel.this,
                    "Confirmer la validation du paiement pour la demande " + ref + " ?",
                    "Valider Paiement", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (conf == JOptionPane.YES_OPTION) {
                try {
                    VoucherDAO.updateVoucherStatus(demandeId, ST_PAYE, userId);
                    chargerDonnees();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ValidationPanel.this,
                            "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void approuverDemande() {
            if (currentRow < 0) return;
            int modelRow = table.convertRowIndexToModel(currentRow);
            int demandeId = (int) tableModel.getValueAt(modelRow, COL_ID);
            String ref = String.valueOf(tableModel.getValueAt(modelRow, COL_REF));

            int conf = JOptionPane.showConfirmDialog(ValidationPanel.this,
                    "Confirmer l'approbation de la demande " + ref + " ?",
                    "Approuver", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (conf == JOptionPane.YES_OPTION) {
                try {
                    VoucherDAO.updateVoucherStatus(demandeId, ST_APPROUVE, userId);
                    chargerDonnees();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ValidationPanel.this,
                            "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        private void rejeterDemande() {
            if (currentRow < 0) return;
            int modelRow = table.convertRowIndexToModel(currentRow);
            int demandeId = (int) tableModel.getValueAt(modelRow, COL_ID);
            String ref = String.valueOf(tableModel.getValueAt(modelRow, COL_REF));

            String raison = (String) JOptionPane.showInputDialog(ValidationPanel.this,
                    "Motif du rejet pour la demande " + ref + " :",
                    "Rejeter la demande", JOptionPane.WARNING_MESSAGE,
                    null, null, "");
            if (raison != null) {
                try {
                    VoucherDAO.updateVoucherStatus(demandeId, ST_REJETE, userId);
                    chargerDonnees();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(ValidationPanel.this,
                            "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // ── HELPERS UI ─────────────────────────────────────────────────────────
    private JButton buildActionBtn(String text, Color color, boolean interactive) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (interactive && getModel().isRollover()) {
                    g2.setColor(color);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    setForeground(Color.WHITE);
                } else {
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    setForeground(color);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(color);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 16, 28));
        if (interactive) btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton buildRedButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN);
                setForeground(Color.WHITE);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 28, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? RED_DARK : RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildOutlineButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN);
                setForeground(TEXT_SECOND);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 24, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? new Color(245, 246, 250) : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

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
