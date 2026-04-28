package pkg.vms;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextFieldUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Utilitaires UI — composants stylisés réutilisables (inputs, boutons, dialogs).
 * Style aligné sur le système InvoiceNinja-like (VMSStyle).
 */
public class UIUtils {

    // ═════════════════════════════════════════════════════════════════════════
    //  BOUTONS
    // ═════════════════════════════════════════════════════════════════════════

    public static JButton buildRedButton(String text) {
        return buildRedButton(text, 160, 38);
    }

    public static JButton buildRedButton(String text, int w, int h) {
        return buildPrimaryButton(text, w, h);
    }

    /** Bouton primaire Intermart — rouge plein, coins arrondis. */
    public static JButton buildPrimaryButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            boolean pressed = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    public void mousePressed(MouseEvent e) { pressed = true;  repaint(); }
                    public void mouseReleased(MouseEvent e){ pressed = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color base = pressed ? VMSStyle.RED_DARK
                          : hovered ? new Color(190, 28, 38)
                          : VMSStyle.RED_PRIMARY;
                g2.setColor(base);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.BTN_ROUND, VMSStyle.BTN_ROUND));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setForeground(Color.WHITE);
        btn.setFont(VMSStyle.FONT_BTN_MAIN);
        return btn;
    }

    public static JButton buildOutlineButton(String text) {
        return buildOutlineButton(text, 160, 38);
    }

    /** Bouton secondaire — bordure rouge, fond transparent. */
    public static JButton buildOutlineButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered) {
                    g2.setColor(VMSStyle.RED_LIGHT);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.BTN_ROUND, VMSStyle.BTN_ROUND));
                }
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth()-2, getHeight()-2, VMSStyle.BTN_ROUND, VMSStyle.BTN_ROUND));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        styleButton(btn);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setForeground(VMSStyle.RED_PRIMARY);
        btn.setFont(VMSStyle.FONT_BTN_MAIN);
        return btn;
    }

    /** Bouton discret — texte seul, hover léger fond gris. */
    public static JButton buildGhostButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                if (hovered) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(VMSStyle.BG_SUBTLE);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.BTN_ROUND, VMSStyle.BTN_ROUND));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        styleButton(btn);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setForeground(VMSStyle.TEXT_SECONDARY);
        btn.setFont(VMSStyle.FONT_BTN_MAIN);
        return btn;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INPUTS MODERNES
    // ═════════════════════════════════════════════════════════════════════════

    /** Style d'input moderne : police, placeholder, caret rouge. */
    public static void styleModernField(JTextField field, String placeholder) {
        field.setFont(VMSStyle.FONT_INPUT);
        field.setForeground(VMSStyle.TEXT_PRIMARY);
        field.setCaretColor(VMSStyle.RED_PRIMARY);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        field.setPreferredSize(new Dimension(0, 42));

        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { field.repaint(); if (field.getParent()!=null) field.getParent().repaint(); }
            public void focusLost(FocusEvent e)   { field.repaint(); if (field.getParent()!=null) field.getParent().repaint(); }
        });

        field.setUI(new BasicTextFieldUI() {
            @Override protected void paintSafely(Graphics g) {
                super.paintSafely(g);
                if (field.getText().isEmpty() && !field.hasFocus() && placeholder != null) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setFont(VMSStyle.FONT_INPUT);
                    g2.setColor(VMSStyle.TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    Insets i = field.getInsets();
                    g2.drawString(placeholder, i.left, (field.getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                }
            }
        });
    }

    /** Wrapper autour d'un JTextField avec bordure arrondie et focus highlight. */
    public static JPanel wrapModernField(JTextField field) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = field.hasFocus();
                g2.setColor(focused ? Color.WHITE : VMSStyle.BG_SUBTLE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.setColor(focused ? VMSStyle.RED_PRIMARY : VMSStyle.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(0, 42));
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    /** Wrapper password avec bouton œil pour afficher/masquer. */
    public static JPanel wrapModernPasswordField(JPasswordField field) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = field.hasFocus();
                g2.setColor(focused ? Color.WHITE : VMSStyle.BG_SUBTLE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.setColor(focused ? VMSStyle.RED_PRIMARY : VMSStyle.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(0, 42));

        field.setFont(VMSStyle.FONT_INPUT);
        field.setForeground(VMSStyle.TEXT_PRIMARY);
        field.setCaretColor(VMSStyle.RED_PRIMARY);
        field.setOpaque(false);
        field.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 8));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { wrapper.repaint(); }
            public void focusLost(FocusEvent e)   { wrapper.repaint(); }
        });

        JButton toggle = buildEyeToggle(field);

        wrapper.add(field,  BorderLayout.CENTER);
        wrapper.add(toggle, BorderLayout.EAST);
        return wrapper;
    }

    /** Bouton œil (vectoriel) pour afficher/masquer password. */
    public static JButton buildEyeToggle(JPasswordField field) {
        final boolean[] shown = { false };
        JButton btn = new JButton() {
            boolean hovered = false;
            {
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(40, 42));
                setToolTipText("Afficher / masquer le mot de passe");
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                });
                addActionListener(e -> {
                    shown[0] = !shown[0];
                    field.setEchoChar(shown[0] ? (char) 0 : '\u2022');
                    repaint();
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.setColor(hovered ? VMSStyle.TEXT_PRIMARY : VMSStyle.TEXT_MUTED);
                g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Double(cx - 9, cy - 5, 18, 10, 0, 180, Arc2D.OPEN));
                g2.draw(new Arc2D.Double(cx - 9, cy - 5, 18, 10, 180, 180, Arc2D.OPEN));
                g2.fill(new Ellipse2D.Double(cx - 2.5, cy - 2.5, 5, 5));
                if (!shown[0]) {
                    g2.setColor(hovered ? VMSStyle.RED_PRIMARY : VMSStyle.TEXT_MUTED);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 10, cy + 6, cx + 10, cy - 6);
                }
                g2.dispose();
            }
        };
        return btn;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  LABELS
    // ═════════════════════════════════════════════════════════════════════════

    public static JLabel buildFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(VMSStyle.FONT_LABEL);
        lbl.setForeground(VMSStyle.TEXT_SECONDARY);
        return lbl;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private static void styleButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    /**
     * Dialogue de confirmation stylisé (remplace JOptionPane basique).
     * @return true si l'utilisateur clique OUI
     */
    public static boolean confirmDialog(Window parent, String title, String message, String yesLabel, String noLabel) {
        JDialog dlg = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setUndecorated(true);
        dlg.setSize(420, 220);
        dlg.setLocationRelativeTo(parent);

        final boolean[] result = { false };

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.CARD_ROUND, VMSStyle.CARD_ROUND));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, VMSStyle.CARD_ROUND, VMSStyle.CARD_ROUND));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(28, 32, 22, 32));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        header.setOpaque(false);
        JPanel warnIcon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.RED_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2;
                g2.drawLine(cx, 12, cx, getHeight() - 14);
                g2.fillOval(cx - 2, getHeight() - 10, 4, 4);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(40, 40); }
        };
        warnIcon.setOpaque(false);
        JLabel titleL = new JLabel(title);
        titleL.setFont(VMSStyle.FONT_PAGE_TTL);
        titleL.setForeground(VMSStyle.TEXT_PRIMARY);
        header.add(warnIcon);
        header.add(titleL);

        JLabel msgL = new JLabel("<html><body style='width:320px'>" + message + "</body></html>");
        msgL.setFont(VMSStyle.FONT_CARD_DSC);
        msgL.setForeground(VMSStyle.TEXT_SECONDARY);
        msgL.setBorder(BorderFactory.createEmptyBorder(12, 54, 16, 0));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(header, BorderLayout.NORTH);
        center.add(msgL,   BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setOpaque(false);
        JButton btnNo  = buildGhostButton(noLabel,  110, 36);
        JButton btnYes = buildPrimaryButton(yesLabel, 130, 36);
        btnNo.addActionListener(e -> dlg.dispose());
        btnYes.addActionListener(e -> { result[0] = true; dlg.dispose(); });
        btns.add(btnNo);
        btns.add(btnYes);

        root.add(center, BorderLayout.CENTER);
        root.add(btns,   BorderLayout.SOUTH);
        dlg.setContentPane(root);
        dlg.setBackground(new Color(0, 0, 0, 0));
        dlg.setVisible(true);
        return result[0];
    }
}
