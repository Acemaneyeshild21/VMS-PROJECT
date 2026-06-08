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
 * GestionBons — Liste légère des bons avec :
 *   Nom client | Email | Valeur unitaire | Statut expiration (rapide)
 */
class GestionBons extends JPanel {

    // ── Palette (Centralisée via VMSStyle) ──────────────────────────────────
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
    private static final Color DANGER       = VMSStyle.RED_PRIMARY;

    private static final Font FONT_TITLE  = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_HDR    = VMSStyle.FONT_BADGE.deriveFont(12f);
    private static final Font FONT_CELL   = VMSStyle.FONT_NAV;
    private static final Font FONT_BADGE  = VMSStyle.FONT_BADGE;
    private static final Font FONT_BTN    = VMSStyle.FONT_BTN_MAIN.deriveFont(12f);
    private static final Font FONT_FILTER = VMSStyle.FONT_NAV;

    // ── Colonnes ────────────────────────────────────────────────────────────
    private static final String[] COLS = {
            "#", "Client", "Email", "Valeur Unit.", "Statut", "Expiration", "Action"
    };
    private static final int COL_ID     = 0;
    private static final int COL_CLIENT = 1;
    private static final int COL_EMAIL  = 2;
    private static final int COL_VALEUR = 3;
    private static final int COL_STATUT = 4;
    private static final int COL_EXPIR  = 5;
    private static final int COL_ACTION = 6;

    private final int    userId;
    private final String role;

    private DefaultTableModel tableModel;
    private JTable            table;
    private JTextField        txtSearch;
    private JLabel            lblTotal;
    private String            filtreExpir = "TOUS";
    private CardLayout        tableCards;
    private JPanel            tableCardHolder;

    public GestionBons(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerBons();
    }

    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(buildHeader(), BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 14));
        mid.setOpaque(false);
        mid.add(buildToolbar(),   BorderLayout.NORTH);
        mid.add(buildTableCard(), BorderLayout.CENTER);
        wrapper.add(mid, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── Header (PageLayout) ────────────────────────────────────────────────
    private JPanel buildHeader() {
        JButton btnNew = UIUtils.buildPrimaryButton("+ Nouvelle Demande", 200, 40);
        btnNew.addActionListener(e -> ouvrirNouvelledemande());
        return PageLayout.buildPageHeader(
                "Bons",
                "Liste des bons émis — nom client, email, valeur et statut d'expiration",
                btnNew
        );
    }

    // ── Toolbar (chips + filter bar) ───────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel chipsCard = PageLayout.buildCard(new FlowLayout(FlowLayout.LEFT, 8, 0), 12);
        chipsCard.add(buildChip("Tous",    null,    "TOUS"));
        chipsCard.add(buildChip("Actifs",  SUCCESS, "ACTIF"));
        chipsCard.add(buildChip("Expirés", DANGER,  "EXPIRE"));
        chipsCard.add(buildChip("Ce mois", WARNING, "MOIS"));

        PageLayout.FilterBar fb = PageLayout.buildFilterBar("Rechercher client ou email...");
        txtSearch = fb.search();
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrerTable(); }
        });

        JButton btnRefresh = UIUtils.buildGhostButton("Actualiser", 110, 38);
        btnRefresh.addActionListener(e -> chargerBons());
        fb.addSlot(btnRefresh);

        if ("Administrateur".equalsIgnoreCase(role)) {
            JButton btnArchive = UIUtils.buildGhostButton("Archiver expirés", 150, 38);
            btnArchive.addActionListener(e -> {
                boolean ok = UIUtils.confirmDialog(SwingUtilities.getWindowAncestor(this),
                        "Archivage",
                        "Voulez-vous archiver toutes les demandes dont les bons sont expirés ?",
                        "Archiver", "Annuler");
                if (ok) {
                    try {
                        int count = VoucherDAO.archiverDemandesExpirees(userId);
                        ToastManager.success(this, count + " demande(s) archivée(s)");
                        chargerBons();
                    } catch (SQLException ex) {
                        ToastManager.error(this, "Erreur lors de l'archivage : " + ex.getMessage());
                    }
                }
            });
            fb.addSlot(btnArchive);
        }

        JPanel wrap = new JPanel(new BorderLayout(0, 10));
        wrap.setOpaque(false);
        wrap.add(chipsCard, BorderLayout.NORTH);
        wrap.add(fb.panel(), BorderLayout.CENTER);
        return wrap;
    }

    private JButton buildChip(String label, Color color, String filtre) {
        Color chipColor = color != null ? color : TEXT_SECOND;
        JButton chip = new JButton(label) {
            boolean sel = filtre.equals("TOUS");
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
                            if (c instanceof JButton) ((JButton)c).repaint();
                    sel = true; repaint();
                    filtreExpir = filtre; filtrerTable();
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

    // ── Table Card (ModernTable) ───────────────────────────────────────────
    private JPanel buildTableCard() {
        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel);
        ModernTable.apply(table);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(46);

        int[] widths = {40, 180, 220, 110, 130, 130, 110};
        for (int i = 0; i < widths.length; i++) {
            if (i < table.getColumnCount())
                ModernTable.setColumnWidth(table, i, widths[i]);
        }
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(40);

        // Renderers custom (ceux de la classe pour garder couleurs exactes)
        ModernTable.setColumnRenderer(table, COL_CLIENT, ModernTable.boldRenderer());
        ModernTable.setColumnRenderer(table, COL_VALEUR, ModernTable.moneyRenderer());
        ModernTable.setColumnRenderer(table, COL_STATUT, new StatutRenderer());
        ModernTable.setColumnRenderer(table, COL_EXPIR, new ExpirationRenderer());
        ModernTable.setColumnRenderer(table, COL_ACTION, new ActionRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == COL_ACTION) {
                    Object val = table.getValueAt(row, col);
                    if ("ARCHIVER".equals(val)) {
                        int id = (int) table.getValueAt(row, COL_ID);
                        boolean ok = UIUtils.confirmDialog(SwingUtilities.getWindowAncestor(GestionBons.this),
                                "Confirmation", "Archiver cette demande ?", "Archiver", "Annuler");
                        if (ok) {
                            try {
                                VoucherDAO.updateVoucherStatus(id, "ARCHIVE", userId);
                                chargerBons();
                            } catch (SQLException ex) {
                                ToastManager.error(GestionBons.this, "Erreur : " + ex.getMessage());
                            }
                        }
                    }
                }
            }
        });

        // Carte blanche avec scroll + footer de comptage
        JPanel card = PageLayout.buildCard(new BorderLayout(), 0);
        JScrollPane scroll = ModernTable.wrap(table);
        scroll.setBorder(null);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        lblTotal = new JLabel("Chargement...");
        lblTotal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotal.setForeground(TEXT_MUTED);
        footer.add(lblTotal, BorderLayout.WEST);

        card.add(scroll, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        // Empty states : table / no-data / no-result
        JPanel emptyNoData = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoData.add(PageLayout.buildEmptyState(
                "Aucun bon émis",
                "Les bons apparaîtront ici dès qu'une demande sera générée.",
                "+ Nouvelle Demande",
                this::ouvrirNouvelledemande
        ), BorderLayout.CENTER);

        JPanel emptyNoResult = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoResult.add(PageLayout.buildEmptyState(
                "Aucun résultat",
                "Aucun bon ne correspond au filtre ou à la recherche.",
                "Réinitialiser les filtres",
                () -> { txtSearch.setText(""); filtreExpir = "TOUS"; filtrerTable(); }
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
        if (total == 0) {
            tableCards.show(tableCardHolder, "empty-no-data");
        } else if (visible == 0) {
            tableCards.show(tableCardHolder, "empty-no-result");
        } else {
            tableCards.show(tableCardHolder, "table");
        }
    }

    // ── Chargement BD ───────────────────────────────────────────────────────
    // On charge depuis demande : nom client, email, valeur_unitaire,
    // date_creation + validite_jours pour calculer expiration
    private void chargerBons() {
        tableModel.setRowCount(0);
        String sql =
                "SELECT d.demande_id, c.name AS nom_client, d.email, " +
                        "       d.valeur_unitaire, d.statuts, " +
                        "       d.date_creation, d.validite_jours " +
                        "FROM demande d " +
                        "LEFT JOIN client c ON d.clientid = c.clientid " +
                        "ORDER BY d.date_creation DESC";

        try (Connection conn = DBconnect.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(sql)) {

            int count = 0;
            java.time.LocalDate today = java.time.LocalDate.now();

            while (rs.next()) {
                int    id       = rs.getInt("demande_id");
                String client   = rs.getString("nom_client");
                String email    = rs.getString("email");
                double valeur   = rs.getDouble("valeur_unitaire");
                String statuts  = rs.getString("statuts");
                int    validite = rs.getInt("validite_jours");

                // Calcul date expiration : date_creation + validite_jours
                java.sql.Timestamp ts = rs.getTimestamp("date_creation");
                String expirStr = "—";
                String expirTag = "INCONNU"; // tag interne pour filtre

                if (ts != null && validite > 0) {
                    java.time.LocalDate dateCreation = ts.toLocalDateTime().toLocalDate();
                    java.time.LocalDate dateExpir    = dateCreation.plusDays(validite);
                    expirStr = dateExpir.toString();

                    long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(today, dateExpir);
                    if (joursRestants < 0) {
                        expirTag = "EXPIRE";
                        expirStr = "Expiré (" + Math.abs(joursRestants) + "j)";
                    } else if (joursRestants <= 30) {
                        expirTag = "MOIS";
                        expirStr = joursRestants + " j restants";
                    } else {
                        expirTag = "ACTIF";
                        expirStr = joursRestants + " j restants";
                    }
                }

                tableModel.addRow(new Object[]{
                        id,
                        client  != null ? client : "—",
                        email   != null ? email  : "—",
                        String.format("Rs %,.0f", valeur),
                        statuts != null ? statuts : "—",
                        expirTag + "|" + expirStr,   // tag|texte pour renderer
                        expirTag.equals("EXPIRE") && !"ARCHIVE".equals(statuts) ? "ARCHIVER" : ""
                });
                count++;
            }
            lblTotal.setText(count + " bon" + (count > 1 ? "s" : "") + " au total");
            updateTableView();

        } catch (SQLException e) {
            e.printStackTrace();
            lblTotal.setText("Erreur : " + e.getMessage());
        }
    }

    // ── Filtre ──────────────────────────────────────────────────────────────
    private void filtrerTable() {
        String search = txtSearch.getText().trim().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        if (!filtreExpir.equals("TOUS"))
            filters.add(RowFilter.regexFilter("(?i)^" + filtreExpir, COL_EXPIR));

        if (!search.isEmpty())
            filters.add(RowFilter.regexFilter("(?i)" + search, COL_CLIENT, COL_EMAIL));

        sorter.setRowFilter(filters.isEmpty() ? null : RowFilter.andFilter(filters));

        int visible = table.getRowCount();
        lblTotal.setText(visible + " bon" + (visible > 1 ? "s" : "") + " affiche" + (visible > 1 ? "s" : ""));
        updateTableView();
    }

    // ── Renderer Statut ─────────────────────────────────────────────────────
    private class StatutRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String s = val != null ? val.toString() : "";
            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bgColor(s));
                    g2.fill(new RoundRectangle2D.Double(4, 10, getWidth()-8, getHeight()-20, 20, 20));
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.setBackground(sel ? RED_LIGHT : (row%2==0 ? BG_CARD : new Color(249,250,252)));
            JLabel lbl = new JLabel(labelFor(s));
            lbl.setFont(FONT_BADGE); lbl.setForeground(fgColor(s));
            badge.add(lbl);
            return badge;
        }
        private Color bgColor(String s) {
            switch (s) {
                case "EN_ATTENTE_PAIEMENT": return new Color(255,237,213);
                case "PAYE":               return new Color(219,234,254);
                case "APPROUVE":           return new Color(237,233,254);
                case "GENERE":             return new Color(220,252,231);
                case "REJETE":             return new Color(254,226,226);
                default:                   return new Color(241,245,249);
            }
        }
        private Color fgColor(String s) {
            switch (s) {
                case "EN_ATTENTE_PAIEMENT": return WARNING;
                case "PAYE":               return new Color(37,99,235);
                case "APPROUVE":           return new Color(124,58,237);
                case "GENERE":             return SUCCESS;
                case "REJETE":             return RED_PRIMARY;
                default:                   return TEXT_MUTED;
            }
        }
        private String labelFor(String s) {
            switch (s) {
                case "EN_ATTENTE_PAIEMENT": return "\u23F3 En attente";
                case "PAYE":               return "\uD83D\uDCB3 Pay\u00e9";
                case "APPROUVE":           return "\u2705 Approuv\u00e9";
                case "GENERE":             return "\uD83C\uDF9F G\u00e9n\u00e9r\u00e9";
                case "REJETE":             return "\u274C Rejet\u00e9";
                default:                   return s;
            }
        }
    }

    // ── Renderer Expiration ─────────────────────────────────────────────────
    private class ExpirationRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String raw = val != null ? val.toString() : "INCONNU|—";
            String[] parts = raw.split("\\|", 2);
            String tag  = parts[0];
            String text = parts.length > 1 ? parts[1] : "—";

            Color bg, fg;
            String prefix;
            switch (tag) {
                case "EXPIRE": bg = new Color(254,226,226); fg = DANGER;   prefix = "\uD83D\uDD34 "; break;
                case "MOIS":   bg = new Color(255,237,213); fg = WARNING;  prefix = "\uD83D\uDFE0 "; break;
                case "ACTIF":  bg = new Color(220,252,231); fg = SUCCESS;  prefix = "\uD83D\uDFE2 "; break;
                default:       bg = new Color(241,245,249); fg = TEXT_MUTED; prefix = ""; break;
            }

            JPanel badge = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Double(4, 10, getWidth()-8, getHeight()-20, 20, 20));
                    g2.dispose();
                }
            };
            badge.setOpaque(false);
            badge.setBackground(sel ? RED_LIGHT : (row%2==0 ? BG_CARD : new Color(249,250,252)));
            JLabel lbl = new JLabel(prefix + text);
            lbl.setFont(FONT_BADGE); lbl.setForeground(fg);
            badge.add(lbl);
            return badge;
        }
    }

    private class ActionRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(
                JTable t, Object val, boolean sel, boolean foc, int row, int col) {
            String s = val != null ? val.toString() : "";
            if (s.isEmpty()) return new JLabel("");
            
            JPanel p = new JPanel(new GridBagLayout());
            p.setOpaque(true);
            p.setBackground(sel ? RED_LIGHT : (row % 2 == 0 ? BG_CARD : new Color(249, 250, 252)));
            
            JButton btn = new JButton(s) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 150, 150, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(FONT_BADGE);
            btn.setForeground(TEXT_MUTED);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            p.add(btn);
            return p;
        }
    }

    // ── Actions ─────────────────────────────────────────────────────────────
    private void ouvrirNouvelledemande() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);

        // Créer le panneau FormulaireCreationBon avec les paramètres attendus
        FormulaireCreationBon panel = new FormulaireCreationBon(userId, role);

        // Encapsuler dans un JDialog modal
        JDialog dialog = new JDialog((JFrame) parent, "Nouvelle Demande de Bon", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(650, 550);
        dialog.setLocationRelativeTo(parent);
        dialog.add(panel);
        dialog.setVisible(true);

        // Actualiser la liste après fermeture de la boîte de dialogue
        chargerBons();
    }

    // ── Helpers UI ──────────────────────────────────────────────────────────
    private JButton buildRedButton(String text) {
        return UIUtils.buildRedButton(text);
    }

    private JButton buildIconBtn(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            {
                setFont(new Font("Segoe UI", Font.BOLD, 16));
                setForeground(TEXT_SECOND);
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
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }
}