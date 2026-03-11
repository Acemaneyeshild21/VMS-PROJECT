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

/**
 * GestionBons — Liste légère des bons avec :
 *   Nom client | Email | Valeur unitaire | Statut expiration (rapide)
 */
class GestionBons extends JPanel {

    // ── Palette ─────────────────────────────────────────────────────────────
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
    private static final Color DANGER       = new Color(220,  38,  38);

    private static final Font FONT_TITLE  = new Font("Georgia",      Font.BOLD,  24);
    private static final Font FONT_HDR    = new Font("Trebuchet MS", Font.BOLD,  12);
    private static final Font FONT_CELL   = new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_BADGE  = new Font("Trebuchet MS", Font.BOLD,  10);
    private static final Font FONT_BTN    = new Font("Trebuchet MS", Font.BOLD,  12);
    private static final Font FONT_FILTER = new Font("Trebuchet MS", Font.PLAIN, 12);

    // ── Colonnes ────────────────────────────────────────────────────────────
    private static final String[] COLS = {
            "#", "Client", "Email", "Valeur Unit.", "Statut", "Expiration"
    };
    private static final int COL_ID     = 0;
    private static final int COL_CLIENT = 1;
    private static final int COL_EMAIL  = 2;
    private static final int COL_VALEUR = 3;
    private static final int COL_STATUT = 4;
    private static final int COL_EXPIR  = 5;

    private final int    userId;
    private final String role;

    private DefaultTableModel tableModel;
    private JTable            table;
    private JTextField        txtSearch;
    private JLabel            lblTotal;
    private String            filtreExpir = "TOUS";

    public GestionBons(String role, int userId) {
        this.role   = role;
        this.userId = userId;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
        chargerBons();
    }

    private void initComponents() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createEmptyBorder(28, 32, 28, 32));
        wrapper.add(buildHeader(),    BorderLayout.NORTH);

        JPanel mid = new JPanel(new BorderLayout(0, 12));
        mid.setOpaque(false);
        mid.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        mid.add(buildToolbar(),   BorderLayout.NORTH);
        mid.add(buildTableCard(), BorderLayout.CENTER);
        wrapper.add(mid, BorderLayout.CENTER);
        add(wrapper, BorderLayout.CENTER);
    }

    // ── Header ──────────────────────────────────────────────────────────────
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
        JLabel icon  = new JLabel("\uD83C\uDF81");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel title = new JLabel("  Gestion des Bons");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        titleRow.add(icon); titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Liste des bons émis — nom client, email, valeur et statut d'expiration");
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

    // ── Toolbar ─────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(buildChip("Tous",      null,        "TOUS"));
        chips.add(buildChip("Actifs",    SUCCESS,     "ACTIF"));
        chips.add(buildChip("Expirés",   DANGER,      "EXPIRE"));
        chips.add(buildChip("Ce mois",   WARNING,     "MOIS"));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        txtSearch = new JTextField(18) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(FONT_FILTER); g2.setColor(TEXT_MUTED);
                    Insets i = getInsets();
                    g2.drawString("\uD83D\uDD0D  Rechercher client ou email...", i.left, getHeight() - i.bottom - 4);
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
        txtSearch.setPreferredSize(new Dimension(240, 36));
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { filtrerTable(); }
        });

        JButton btnRefresh = buildIconBtn("\u21BB", "Actualiser");
        btnRefresh.addActionListener(e -> chargerBons());
        right.add(txtSearch); right.add(btnRefresh);

        bar.add(chips, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
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

    // ── Table Card ──────────────────────────────────────────────────────────
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
            @Override public boolean isCellEditable(int r, int c) { return false; }
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
        table.setFont(FONT_CELL);
        table.setRowHeight(46);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.setSelectionBackground(RED_LIGHT);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFocusable(false);
        table.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HDR);
        header.setBackground(new Color(248, 249, 252));
        header.setForeground(TEXT_SECOND);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        // Largeurs
        int[] widths = {40, 180, 220, 110, 130, 130};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getColumnModel().getColumn(COL_ID).setMaxWidth(40);

        // Renderers
        table.getColumnModel().getColumn(COL_STATUT).setCellRenderer(new StatutRenderer());
        table.getColumnModel().getColumn(COL_EXPIR).setCellRenderer(new ExpirationRenderer());

        DefaultTableCellRenderer rightR = new DefaultTableCellRenderer();
        rightR.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(COL_VALEUR).setCellRenderer(rightR);

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
                        "LEFT JOIN client c ON d.client_id = c.client_id " +
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
                        expirTag + "|" + expirStr   // tag|texte pour renderer
                });
                count++;
            }
            lblTotal.setText(count + " bon" + (count > 1 ? "s" : "") + " au total");

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
        lblTotal.setText(visible + " bon" + (visible > 1 ? "s" : "") + " affich\u00e9" + (visible > 1 ? "s" : ""));
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

    // ── Actions ─────────────────────────────────────────────────────────────
    private void ouvrirNouvelledemande() {
        Frame parent = (Frame) SwingUtilities.getWindowAncestor(this);
        FormulaireCreationBon dlg = new FormulaireCreationBon((JFrame) parent, userId, "", role);
        dlg.setVisible(true);
        chargerBons();
    }

    // ── Helpers UI ──────────────────────────────────────────────────────────
    private JButton buildRedButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN); setForeground(Color.WHITE);
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

    private JButton buildIconBtn(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 16));
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