package pkg.vms;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * Stylisateur de tables — look InvoiceNinja.
 * <p>
 * Applique sur un JTable existant :
 *   - header gris clair, label MAJUSCULE petit, gras
 *   - lignes 44px, pas de zebra stripes
 *   - hover subtle, sélection rouge translucide
 *   - bordures horizontales fines seulement (pas de grilles verticales)
 *   - scrollpane bordure arrondie + scrollbar moderne
 *   - renderers utilitaires (badge statut, monétaire, date)
 * <p> <p>
 * Usage :
 *   JTable t = new JTable(model);
 *   ModernTable.apply(t);
 *   JScrollPane sp = ModernTable.wrap(t);
 */
public final class ModernTable {

    public static final int ROW_HEIGHT   = 44;
    public static final int HEADER_HEIGHT = 38;

    private ModernTable() {}

    // ─────────────────────────────────────────────────────────────────────────
    //  APPLY — stylise la JTable en place
    // ─────────────────────────────────────────────────────────────────────────
    public static void apply(JTable table) {
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(VMSStyle.TEXT_PRIMARY);
        table.setBackground(VMSStyle.BG_CARD);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.setSelectionForeground(VMSStyle.RED_DARK);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.setRowMargin(0);
        table.setBorder(BorderFactory.createEmptyBorder());

        // Header
        JTableHeader h = table.getTableHeader();
        h.setFont(new Font("Segoe UI", Font.BOLD, 11));
        h.setForeground(VMSStyle.TEXT_SECONDARY);
        h.setBackground(VMSStyle.BG_SUBTLE);
        h.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT));
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new HeaderRenderer());

        // Default cell renderer (left, vertical center, padding)
        table.setDefaultRenderer(Object.class, new CellRenderer(SwingConstants.LEFT));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  WRAP — englobe la table dans un JScrollPane stylé (bordure/radius)
    // ─────────────────────────────────────────────────────────────────────────
    public static JScrollPane wrap(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        sp.setBackground(VMSStyle.BG_CARD);
        styleScrollBar(sp.getVerticalScrollBar());
        styleScrollBar(sp.getHorizontalScrollBar());
        return sp;
    }

    /** Version avec coins arrondis pour encart dans une carte. */
    public static JPanel wrapRounded(JTable table) {
        JScrollPane sp = wrap(table);
        sp.setBorder(null);

        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(),
                        VMSStyle.CARD_ROUND, VMSStyle.CARD_ROUND));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1,
                        VMSStyle.CARD_ROUND, VMSStyle.CARD_ROUND));
                g2.dispose();
            }
        };
        wrap.setOpaque(false);
        wrap.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  COLONNE — largeur fixe ou préférée
    // ─────────────────────────────────────────────────────────────────────────
    public static void setColumnWidth(JTable table, int col, int pref) {
        if (col >= 0 && col < table.getColumnCount()) {
            TableColumn c = table.getColumnModel().getColumn(col);
            c.setPreferredWidth(pref);
        }
    }

    public static void setColumnWidth(JTable table, int col, int min, int pref, int max) {
        if (col >= 0 && col < table.getColumnCount()) {
            TableColumn c = table.getColumnModel().getColumn(col);
            c.setMinWidth(min);
            c.setPreferredWidth(pref);
            c.setMaxWidth(max);
        }
    }

    public static void setColumnRenderer(JTable table, int col, TableCellRenderer r) {
        if (col >= 0 && col < table.getColumnCount()) {
            table.getColumnModel().getColumn(col).setCellRenderer(r);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  RENDERERS PRÊTS À L'EMPLOI
    // ─────────────────────────────────────────────────────────────────────────

    /** Renderer badge coloré — couleur dérivée du texte (statut). */
    public static TableCellRenderer statusBadgeRenderer() {
        return new BadgeRenderer();
    }

    /** Renderer monétaire — Rs XX,XXX.XX aligné droite, couleur success pour positif. */
    public static TableCellRenderer moneyRenderer() {
        return new MoneyRenderer();
    }

    /** Renderer aligné centre. */
    public static TableCellRenderer centerRenderer() {
        return new CellRenderer(SwingConstants.CENTER);
    }

    /** Renderer cellule en gras (ID ou référence). */
    public static TableCellRenderer boldRenderer() {
        return new CellRenderer(SwingConstants.LEFT) {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                return lbl;
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTERNAL RENDERERS
    // ─────────────────────────────────────────────────────────────────────────
    private static class HeaderRenderer extends DefaultTableCellRenderer {
        public HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(VMSStyle.TEXT_SECONDARY);
            lbl.setBackground(VMSStyle.BG_SUBTLE);
            lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            lbl.setText(v != null ? v.toString().toUpperCase() : "");
            return lbl;
        }
    }

    private static class CellRenderer extends DefaultTableCellRenderer {
        public CellRenderer(int align) {
            setHorizontalAlignment(align);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            if (sel) {
                lbl.setBackground(VMSStyle.RED_LIGHT);
                lbl.setForeground(VMSStyle.RED_DARK);
            } else {
                lbl.setBackground(VMSStyle.BG_CARD);
                lbl.setForeground(VMSStyle.TEXT_PRIMARY);
            }
            return lbl;
        }
    }

    private static class BadgeRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            String text = v != null ? v.toString() : "";
            final Color accent = PageLayout.colorForStatus(text);
            final String displayText = text;
            JPanel p = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    // Fond cellule
                    g2.setColor(sel ? VMSStyle.RED_LIGHT : VMSStyle.BG_CARD);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    // Pilule
                    Font f = new Font("Segoe UI", Font.BOLD, 11);
                    g2.setFont(f);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(displayText) + 20;
                    int th = 22;
                    int x = 14;
                    int y = (getHeight() - th) / 2;
                    g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
                    g2.fill(new RoundRectangle2D.Double(x, y, tw, th, th, th));
                    g2.setColor(accent.darker());
                    g2.drawString(displayText, x + 10, y + fm.getAscent() + (th - fm.getHeight()) / 2);
                    g2.dispose();
                }
            };
            p.setBackground(sel ? VMSStyle.RED_LIGHT : VMSStyle.BG_CARD);
            return p;
        }
    }

    private static class MoneyRenderer extends DefaultTableCellRenderer {
        public MoneyRenderer() {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
        @Override public Component getTableCellRendererComponent(JTable t, Object v,
                boolean sel, boolean foc, int row, int col) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            String formatted;
            try {
                double d = 0;
                if (v instanceof Number) d = ((Number) v).doubleValue();
                else if (v != null) d = Double.parseDouble(v.toString().replaceAll("[^0-9.\\-]", ""));
                formatted = String.format("Rs %,.2f", d);
            } catch (Exception ex) {
                formatted = v != null ? v.toString() : "";
            }
            lbl.setText(formatted);
            if (sel) {
                lbl.setBackground(VMSStyle.RED_LIGHT);
                lbl.setForeground(VMSStyle.RED_DARK);
            } else {
                lbl.setBackground(VMSStyle.BG_CARD);
                lbl.setForeground(VMSStyle.TEXT_PRIMARY);
            }
            return lbl;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SCROLL BAR — look moderne
    // ─────────────────────────────────────────────────────────────────────────
    private static void styleScrollBar(JScrollBar bar) {
        bar.setUI(new BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() {
                thumbColor = VMSStyle.BORDER_LIGHT;
                trackColor = VMSStyle.BG_CARD;
            }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(156, 163, 175, 160));
                g2.fill(new RoundRectangle2D.Double(r.x + 3, r.y + 3, r.width - 6, r.height - 6, 6, 6));
                g2.dispose();
            }
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
                g.setColor(VMSStyle.BG_CARD);
                g.fillRect(r.x, r.y, r.width, r.height);
            }
        });
        bar.setUnitIncrement(14);
        bar.setPreferredSize(new Dimension(10, 10));
    }
}
