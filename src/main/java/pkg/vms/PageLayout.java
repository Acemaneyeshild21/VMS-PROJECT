package pkg.vms;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Kit de composition de pages — style InvoiceNinja.
 * Briques réutilisables : header, cartes, filtres, état vide, stats, badges.
 */
public final class PageLayout {

    private PageLayout() {}

    public static JPanel buildPageHeader(String title, String subtitle, JComponent right) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(VMSStyle.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(lblTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            texts.add(Box.createVerticalStrut(4));
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            lblSub.setForeground(VMSStyle.TEXT_SECONDARY);
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            texts.add(lblSub);
        }

        header.add(texts, BorderLayout.WEST);
        if (right != null) {
            JPanel rightWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            rightWrap.setOpaque(false);
            rightWrap.add(right);
            header.add(rightWrap, BorderLayout.EAST);
        }
        return header;
    }

    public static JPanel buildCard(LayoutManager layout, int padding) {
        JPanel card = new JPanel(layout) {
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
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return card;
    }

    // ── Barre de filtres ─────────────────────────────────────────────────────
    public record FilterBar(JPanel panel, JTextField search, JPanel rightSlots) {
        public void addSlot(JComponent c) { rightSlots.add(c); }
        public void addSlots(JComponent... cs) { for (JComponent c : cs) rightSlots.add(c); }
    }

    public static FilterBar buildFilterBar(String searchPlaceholder) {
        JPanel bar = buildCard(new BorderLayout(12, 0), 14);

        JTextField search = new JTextField();
        UIUtils.styleModernField(search, searchPlaceholder);
        JPanel searchWrap = UIUtils.wrapModernField(search);
        searchWrap.setPreferredSize(new Dimension(320, 38));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(searchWrap, BorderLayout.CENTER);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        bar.add(right, BorderLayout.EAST);

        return new FilterBar(bar, search, right);
    }

    // ── État vide ────────────────────────────────────────────────────────────
    public static JPanel buildEmptyState(String title, String message, String ctaLabel, Runnable ctaAction) {
        JPanel root = new JPanel(new GridBagLayout());
        root.setOpaque(false);

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        JComponent illustration = new JComponent() {
            { setPreferredSize(new Dimension(120, 120)); setMaximumSize(new Dimension(120, 120)); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                g2.setColor(VMSStyle.BG_SUBTLE);
                g2.fill(new Ellipse2D.Double(10, 10, w - 20, h - 20));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(10, 10, w - 20, h - 20));

                int bw = 48, bh = 36;
                int bx = (w - bw) / 2, by = (h - bh) / 2 + 4;
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, 6, 6));
                g2.setColor(VMSStyle.TEXT_MUTED);
                g2.draw(new RoundRectangle2D.Double(bx, by, bw, bh, 6, 6));
                g2.drawLine(bx + 8, by + 12, bx + bw - 8, by + 12);
                g2.drawLine(bx + 8, by + 20, bx + bw - 12, by + 20);
                g2.drawLine(bx + 8, by + 28, bx + bw - 18, by + 28);
                g2.dispose();
            }
        };
        illustration.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(illustration);

        stack.add(Box.createVerticalStrut(18));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(VMSStyle.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(lblTitle);

        if (message != null && !message.isEmpty()) {
            stack.add(Box.createVerticalStrut(6));
            JLabel lblMsg = new JLabel(message);
            lblMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblMsg.setForeground(VMSStyle.TEXT_SECONDARY);
            lblMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            stack.add(lblMsg);
        }

        if (ctaLabel != null && ctaAction != null) {
            stack.add(Box.createVerticalStrut(16));
            JButton cta = UIUtils.buildPrimaryButton(ctaLabel, 180, 38);
            cta.setAlignmentX(Component.CENTER_ALIGNMENT);
            cta.addActionListener(e -> ctaAction.run());
            stack.add(cta);
        }

        root.add(stack);
        return root;
    }

    // ── Couleur par statut métier ────────────────────────────────────────────
    public static Color colorForStatus(String status) {
        if (status == null) return VMSStyle.TEXT_MUTED;
        String s = status.toLowerCase();
        if (s.contains("actif") || s.contains("valid") || s.contains("approuv") || s.contains("succ"))
            return VMSStyle.SUCCESS;
        if (s.contains("attente") || s.contains("pend") || s.contains("cours") || s.contains("brouillon"))
            return VMSStyle.WARNING;
        if (s.contains("refus") || s.contains("annul") || s.contains("expir") || s.contains("rejet"))
            return VMSStyle.DANGER;
        if (s.contains("utilis") || s.contains("redempt") || s.contains("consum"))
            return VMSStyle.ACCENT_BLUE;
        return VMSStyle.TEXT_SECONDARY;
    }
}
