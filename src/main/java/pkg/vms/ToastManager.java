package pkg.vms;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.*;
import java.util.List;

/**
 * Gestionnaire de notifications "toast" — messages non-bloquants
 * affichés en pile (bas-droite) avec fade-in, auto-dismiss et close.
 *
 * Usage:
 *   ToastManager.success(component, "Client ajouté");
 *   ToastManager.error(component, "Impossible de sauvegarder");
 *   ToastManager.warning(component, "Validation requise");
 *   ToastManager.info(component, "Import terminé");
 */
public final class ToastManager {

    public enum Type {
        SUCCESS(new Color(22, 163, 74),  new Color(220, 252, 231), "✓"),
        ERROR  (new Color(220, 38,  38), new Color(254, 226, 226), "✕"),
        WARNING(new Color(217, 119,  6), new Color(254, 243, 199), "!"),
        INFO   (new Color(37,  99, 235), new Color(219, 234, 254), "i");

        final Color accent;
        final Color bgTint;
        final String glyph;
        Type(Color accent, Color bgTint, String glyph) {
            this.accent = accent; this.bgTint = bgTint; this.glyph = glyph;
        }
    }

    private static final int TOAST_W      = 340;
    private static final int TOAST_H      = 64;
    private static final int MARGIN       = 18;
    private static final int GAP          = 10;
    private static final int DEFAULT_MS   = 3600;

    /** Stack de toasts actifs par fenêtre racine. */
    private static final Map<Window, List<ToastPanel>> ACTIVE = new WeakHashMap<>();

    private ToastManager() {}

    // ── API publique ─────────────────────────────────────────────────────────
    public static void success(Component owner, String msg) { show(owner, Type.SUCCESS, msg, DEFAULT_MS); }
    public static void error  (Component owner, String msg) { show(owner, Type.ERROR,   msg, 5000); }
    public static void warning(Component owner, String msg) { show(owner, Type.WARNING, msg, 4200); }
    public static void info   (Component owner, String msg) { show(owner, Type.INFO,    msg, DEFAULT_MS); }

    public static void show(Component owner, Type type, String message, int durationMs) {
        if (message == null || message.isBlank()) return;
        SwingUtilities.invokeLater(() -> {
            Window root = owner != null ? SwingUtilities.getWindowAncestor(owner) : null;
            if (root == null) root = JOptionPane.getRootFrame();
            if (!(root instanceof RootPaneContainer rpc)) return;

            JLayeredPane layered = rpc.getLayeredPane();
            ToastPanel toast = new ToastPanel(type, message);
            toast.setSize(TOAST_W, TOAST_H);

            layered.add(toast, JLayeredPane.POPUP_LAYER);

            List<ToastPanel> stack = ACTIVE.computeIfAbsent(root, k -> new ArrayList<>());
            stack.add(0, toast);
            relayout(layered, stack);

            Window rootFinal = root;
            toast.startLifecycle(durationMs, () -> {
                layered.remove(toast);
                stack.remove(toast);
                relayout(layered, stack);
                layered.repaint();
                ACTIVE.computeIfPresent(rootFinal, (k, v) -> v.isEmpty() ? null : v);
            });
        });
    }

    private static void relayout(JLayeredPane layered, List<ToastPanel> stack) {
        int x = layered.getWidth() - TOAST_W - MARGIN;
        int y = layered.getHeight() - TOAST_H - MARGIN;
        for (ToastPanel t : stack) {
            t.setLocation(x, y);
            y -= (TOAST_H + GAP);
        }
        layered.repaint();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Composant Toast
    // ═════════════════════════════════════════════════════════════════════════
    private static final class ToastPanel extends JPanel {
        private final Type type;
        private final String message;
        private float alpha = 0f;
        private Timer lifeTimer;

        ToastPanel(Type type, String message) {
            this.type = type;
            this.message = message;
            setOpaque(false);
            setLayout(null);

            JButton close = new JButton("×") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(VMSStyle.TEXT_MUTED);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    FontMetrics fm = g2.getFontMetrics();
                    String s = "×";
                    int tx = (getWidth() - fm.stringWidth(s)) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(s, tx, ty);
                    g2.dispose();
                }
            };
            close.setBorder(null);
            close.setContentAreaFilled(false);
            close.setFocusPainted(false);
            close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            close.setBounds(TOAST_W - 28, 8, 20, 20);
            close.addActionListener(e -> dismiss());
            add(close);
        }

        void startLifecycle(int durationMs, Runnable onDone) {
            Timer fadeIn = new Timer(18, null);
            fadeIn.addActionListener(e -> {
                alpha = Math.min(1f, alpha + 0.12f);
                repaint();
                if (alpha >= 1f) fadeIn.stop();
            });
            fadeIn.start();

            lifeTimer = new Timer(durationMs, e -> {
                Timer fadeOut = new Timer(18, null);
                fadeOut.addActionListener(ev -> {
                    alpha = Math.max(0f, alpha - 0.12f);
                    repaint();
                    if (alpha <= 0f) {
                        fadeOut.stop();
                        onDone.run();
                    }
                });
                fadeOut.start();
            });
            lifeTimer.setRepeats(false);
            lifeTimer.start();
        }

        private void dismiss() {
            if (lifeTimer != null) lifeTimer.stop();
            lifeTimer = new Timer(1, e -> {
                alpha = 0f;
                repaint();
                Container parent = getParent();
                if (parent != null) {
                    parent.remove(this);
                    for (List<ToastPanel> v : ACTIVE.values()) v.remove(this);
                    parent.repaint();
                }
            });
            lifeTimer.setRepeats(false);
            lifeTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

            int w = getWidth(), h = getHeight();

            // Ombre portée
            for (int i = 0; i < 5; i++) {
                g2.setColor(new Color(17, 24, 39, 10 - i * 2));
                g2.fill(new RoundRectangle2D.Double(i, i + 2, w - 2 * i, h - 2 * i, 12, 12));
            }

            // Fond blanc
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, 12, 12));

            // Bordure douce
            g2.setColor(VMSStyle.BORDER_LIGHT);
            g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, 12, 12));

            // Bande accent gauche
            g2.setColor(type.accent);
            g2.fillRect(0, 0, 4, h);

            // Icône pastille
            int iconSize = 28;
            int iconX = 14, iconY = (h - iconSize) / 2;
            g2.setColor(type.bgTint);
            g2.fillOval(iconX, iconY, iconSize, iconSize);
            g2.setColor(type.accent);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();
            int gx = iconX + (iconSize - fm.stringWidth(type.glyph)) / 2;
            int gy = iconY + (iconSize + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(type.glyph, gx, gy);

            // Message
            g2.setColor(VMSStyle.TEXT_PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            FontMetrics mfm = g2.getFontMetrics();
            String displayed = truncate(message, mfm, TOAST_W - 80);
            int mx = iconX + iconSize + 12;
            int my = (h + mfm.getAscent() - mfm.getDescent()) / 2;
            g2.drawString(displayed, mx, my);

            g2.dispose();
        }

        private static String truncate(String s, FontMetrics fm, int maxW) {
            if (fm.stringWidth(s) <= maxW) return s;
            String ell = "…";
            int ellW = fm.stringWidth(ell);
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (fm.stringWidth(sb.toString() + c) + ellW > maxW) break;
                sb.append(c);
            }
            return sb.toString().trim() + ell;
        }
    }
}
