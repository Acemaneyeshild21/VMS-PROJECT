package pkg.vms;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * Système de design VMS — inspiré InvoiceNinja (SaaS moderne).
 * Typographie sans-serif (Segoe UI), palette affinée, tokens unifiés.
 * Fournit aussi des helpers de rendu pour cartes, ombres, et logo.
 */
public class VMSStyle {

    // ── Palette — Fond & Surfaces ─────────────────────────────────────────────
    public static final Color BG_ROOT        = new Color(246, 247, 251);
    public static final Color BG_SIDEBAR     = new Color(255, 255, 255);
    public static final Color BG_TOPBAR      = new Color(255, 255, 255);
    public static final Color BG_CARD        = new Color(255, 255, 255);
    public static final Color BG_CARD_HOVER  = new Color(250, 251, 253);
    public static final Color BG_SUBTLE      = new Color(241, 243, 247);

    // ── Palette — Brand Voucher System ────────────────────────────────────────
    public static final Color RED_PRIMARY    = new Color(210,  35,  45);
    public static final Color RED_DARK       = new Color(170,  20,  28);
    public static final Color RED_LIGHT      = new Color(254, 235, 237);

    // ── Palette — Bordures & Texte (style Tailwind gray) ─────────────────────
    public static final Color BORDER_LIGHT   = new Color(229, 231, 235);
    public static final Color BORDER_SOFT    = new Color(243, 244, 246);
    public static final Color TEXT_PRIMARY   = new Color( 17,  24,  39);
    public static final Color TEXT_SECONDARY = new Color( 75,  85,  99);
    public static final Color TEXT_MUTED     = new Color(156, 163, 175);

    // ── Palette — Accents sémantiques ─────────────────────────────────────────
    public static final Color ACCENT_BLUE    = new Color( 37,  99, 235);
    public static final Color SUCCESS        = new Color( 22, 163,  74);
    public static final Color WARNING        = new Color(217, 119,   6);
    public static final Color DANGER         = RED_PRIMARY;

    // ── Ombres ────────────────────────────────────────────────────────────────
    public static final Color SHADOW_SM      = new Color(17, 24, 39, 14);
    public static final Color SHADOW_MD      = new Color(17, 24, 39, 22);
    public static final Color SHADOW_COLOR   = SHADOW_SM;

    // ── Typographie (Segoe UI — stack moderne) ───────────────────────────────
    private static final String SANS = "Segoe UI";

    public static final Font FONT_BRAND      = new Font(SANS, Font.BOLD,  22);
    public static final Font FONT_SUBTITLE   = new Font(SANS, Font.PLAIN, 12);
    public static final Font FONT_NAV        = new Font(SANS, Font.PLAIN, 13);
    public static final Font FONT_NAV_ACT    = new Font(SANS, Font.BOLD,  13);
    public static final Font FONT_CARD_TTL   = new Font(SANS, Font.BOLD,  15);
    public static final Font FONT_CARD_DSC   = new Font(SANS, Font.PLAIN, 12);
    public static final Font FONT_BTN_MAIN   = new Font(SANS, Font.BOLD,  13);
    public static final Font FONT_BADGE      = new Font(SANS, Font.BOLD,  10);
    public static final Font FONT_USER       = new Font(SANS, Font.BOLD,  13);
    public static final Font FONT_KPI_VAL    = new Font(SANS, Font.BOLD,  26);
    public static final Font FONT_KPI_LBL    = new Font(SANS, Font.PLAIN, 11);
    public static final Font FONT_PAGE_TTL   = new Font(SANS, Font.BOLD,  18);
    public static final Font FONT_SECTION    = new Font(SANS, Font.BOLD,  11);
    public static final Font FONT_INPUT      = new Font(SANS, Font.PLAIN, 13);
    public static final Font FONT_LABEL      = new Font(SANS, Font.BOLD,  11);

    public static Font font(int style, float size) {
        return new Font(SANS, style, 12).deriveFont(size);
    }

    // ── Paramètres de Design ─────────────────────────────────────────────────
    public static final int CARD_ROUND  = 12;
    public static final int INPUT_ROUND = 8;
    public static final int BTN_ROUND   = 8;

    public static void paintStandardCard(Graphics2D g2, int w, int h, boolean hover) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(hover ? BG_CARD_HOVER : BG_CARD);
        g2.fill(new RoundRectangle2D.Double(0, 0, w, h, CARD_ROUND, CARD_ROUND));
        g2.setColor(BORDER_LIGHT);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(0, 0, w - 1, h - 1, CARD_ROUND, CARD_ROUND));
    }

    public static void paintDropShadow(Graphics2D g2, int w, int h, int radius) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 0; i < 4; i++) {
            g2.setColor(new Color(17, 24, 39, 6 - i));
            g2.fill(new RoundRectangle2D.Double(i, i + 2, w - 2 * i, h - 2 * i, radius, radius));
        }
    }

    /**
     * Dessine le logo VMS Voucher vectoriel.
     * Ticket/bon rouge avec encoches latérales (perforations), ruban décoratif
     * en haut, et grand "V" central — inspiré d'un voucher cadeau.
     * @param fg couleur principale du corps du voucher (ex. Color.WHITE)
     * @param bg couleur du cercle derrière + couleur du "V" (null = pas de cercle)
     */
    public static void paintLogo(Graphics2D g2, int x, int y, int size, Color fg, Color bg) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (bg != null) {
            g2.setColor(bg);
            g2.fillRoundRect(x, y, size, size, size / 4, size / 4);
        }

        Color contrastColor = (bg != null) ? bg : RED_PRIMARY;

        // ── Ruban / nœud décoratif en haut ────────────────────────────────
        double knotW = size * 0.40;
        double knotH = size * 0.20;
        double knotX = x + (size - knotW) / 2.0;
        double knotY = y + size * 0.10;
        g2.setColor(fg);
        g2.setStroke(new BasicStroke((float)(size * 0.075), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(knotX, knotY, knotW * 0.55, knotH, 30, 180, Arc2D.OPEN));
        g2.draw(new Arc2D.Double(knotX + knotW * 0.45, knotY, knotW * 0.55, knotH, -30, -180, Arc2D.OPEN));
        // Petit nœud central (pastille)
        double knotR = size * 0.045;
        g2.fill(new Ellipse2D.Double(x + size / 2.0 - knotR, knotY + knotH / 2.0 - knotR, knotR * 2, knotR * 2));

        // ── Corps du voucher (rectangle arrondi) ──────────────────────────
        double pad = size * 0.13;
        double vx = x + pad;
        double vy = y + size * 0.34;
        double vw = size - 2 * pad;
        double vh = size * 0.52;
        double corner = size * 0.10;

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(fg);
        g2.fill(new RoundRectangle2D.Double(vx, vy, vw, vh, corner, corner));

        // ── Encoches latérales (style ticket perforé) ─────────────────────
        double notchR = size * 0.055;
        g2.setColor(bg != null ? bg : Color.WHITE);
        g2.fill(new Ellipse2D.Double(vx - notchR, vy + vh / 2.0 - notchR, notchR * 2, notchR * 2));
        g2.fill(new Ellipse2D.Double(vx + vw - notchR, vy + vh / 2.0 - notchR, notchR * 2, notchR * 2));

        // ── Grand "V" au centre du voucher ────────────────────────────────
        Font vFont = new Font(SANS, Font.BOLD, Math.max(8, (int)(size * 0.42)));
        g2.setFont(vFont);
        FontMetrics fm = g2.getFontMetrics();
        String letter = "V";
        int letterW = fm.stringWidth(letter);
        int letterY = (int)(vy + (vh + fm.getAscent() - fm.getDescent()) / 2.0);
        g2.setColor(contrastColor);
        g2.drawString(letter, (int)(vx + (vw - letterW) / 2.0), letterY);
    }
}
