package pkg.vms;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Centralisation des styles, couleurs et polices pour le projet VMS.
 * Garantit une cohérence visuelle sur toute l'application.
 */
public class VMSStyle {

    // ── Palette de Couleurs ──────────────────────────────────────────────────
    public static final Color BG_ROOT        = new Color(245, 246, 250);
    public static final Color BG_SIDEBAR     = new Color(255, 255, 255);
    public static final Color BG_TOPBAR      = new Color(255, 255, 255);
    public static final Color BG_CARD        = new Color(255, 255, 255);
    public static final Color BG_CARD_HOVER  = new Color(255, 248, 248);
    
    public static final Color RED_PRIMARY    = new Color(210,  35,  45);
    public static final Color RED_DARK       = new Color(170,  20,  28);
    public static final Color RED_LIGHT      = new Color(255, 235, 236);
    
    public static final Color BORDER_LIGHT   = new Color(228, 230, 236);
    public static final Color TEXT_PRIMARY   = new Color( 22,  28,  45);
    public static final Color TEXT_SECONDARY = new Color( 90, 100, 120);
    public static final Color TEXT_MUTED     = new Color(160, 168, 185);
    
    public static final Color ACCENT_BLUE    = new Color( 37, 120, 220);
    public static final Color SUCCESS        = new Color( 34, 177, 110);
    public static final Color WARNING        = new Color(240, 150,  40);
    public static final Color SHADOW_COLOR   = new Color(0, 0, 0, 18);

    // ── Polices ──────────────────────────────────────────────────────────────
    public static final Font FONT_BRAND      = new Font("Georgia",      Font.BOLD,  30);
    public static final Font FONT_SUBTITLE   = new Font("Georgia",      Font.ITALIC,12);
    public static final Font FONT_NAV        = new Font("Trebuchet MS", Font.PLAIN, 13);
    public static final Font FONT_NAV_ACT    = new Font("Trebuchet MS", Font.BOLD,  13);
    public static final Font FONT_CARD_TTL   = new Font("Georgia",      Font.BOLD,  16);
    public static final Font FONT_CARD_DSC   = new Font("Trebuchet MS", Font.PLAIN, 12);
    public static final Font FONT_BTN_MAIN   = new Font("Trebuchet MS", Font.BOLD,  14);
    public static final Font FONT_BADGE      = new Font("Trebuchet MS", Font.BOLD,  10);
    public static final Font FONT_USER       = new Font("Trebuchet MS", Font.BOLD,  13);
    public static final Font FONT_KPI_VAL    = new Font("Georgia",      Font.BOLD,  26);
    public static final Font FONT_KPI_LBL    = new Font("Trebuchet MS", Font.PLAIN, 11);

    // ── Paramètres de Design ────────────────────────────────────────────────
    public static final int CARD_ROUND = 15; // Arrondi standard des cartes

    /**
     * Applique un rendu de carte standard avec arrondis et bordure légère.
     */
    public static void paintStandardCard(Graphics2D g2, int w, int h, boolean hover) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hover ? BG_CARD_HOVER : BG_CARD);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, CARD_ROUND, CARD_ROUND));
        g2.setColor(BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, CARD_ROUND, CARD_ROUND));
    }
}
