package pkg.vms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Utilitaires pour l'interface utilisateur (UI).
 * Centralise la création de composants stylisés pour éviter la duplication.
 */
public class UIUtils {

    /**
     * Construit un bouton rouge plein (style primaire Intermart).
     */
    public static JButton buildRedButton(String text) {
        return buildRedButton(text, 160, 38);
    }

    public static JButton buildRedButton(String text, int w, int h) {
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
                g2.setColor(hovered ? VMSStyle.RED_DARK : VMSStyle.RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                super.paintComponent(g2);
            }
        };
        styleButton(btn);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setForeground(Color.WHITE);
        btn.setFont(VMSStyle.FONT_BTN_MAIN);
        return btn;
    }

    /**
     * Construit un bouton avec bordure rouge (style secondaire).
     */
    public static JButton buildOutlineButton(String text) {
        return buildOutlineButton(text, 160, 38);
    }

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
                    g2.setColor(new Color(210, 35, 45, 15));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                }
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth()-2, getHeight()-2, 10, 10));
                super.paintComponent(g2);
            }
        };
        styleButton(btn);
        btn.setPreferredSize(new Dimension(w, h));
        btn.setForeground(VMSStyle.RED_PRIMARY);
        btn.setFont(VMSStyle.FONT_BTN_MAIN.deriveFont(13f));
        return btn;
    }

    private static void styleButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
