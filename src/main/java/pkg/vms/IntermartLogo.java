package pkg.vms;

import javax.swing.*;
import java.awt.*;

/**
 * Composant Logo Intermart — dessin vectoriel (pas d'emoji, pas de PNG).
 * Dessine un sac cadeau stylisé + le mot "INTERMART" + tagline optionnelle.
 * Taille adaptable, couleurs paramétrables pour support fond clair ou foncé.
 */
public class IntermartLogo extends JPanel {

    public enum Variant {
        /** Logo rouge sur fond blanc (par défaut — sidebar, header). */
        LIGHT,
        /** Logo blanc sur fond rouge / coloré (hero panels). */
        DARK,
        /** Icône seule (sans texte). */
        ICON_ONLY
    }

    private final Variant variant;
    private final int iconSize;
    private final String title;
    private final String tagline;
    private final boolean showTagline;

    public IntermartLogo(Variant variant, int iconSize) {
        this(variant, iconSize, "VMS Voucher", "Gestion des bons cadeau", false);
    }

    public IntermartLogo(Variant variant, int iconSize, String title, String tagline, boolean showTagline) {
        this.variant = variant;
        this.iconSize = iconSize;
        this.title = title;
        this.tagline = tagline;
        this.showTagline = showTagline;
        setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
        if (variant == Variant.ICON_ONLY) return new Dimension(iconSize, iconSize);
        int h = Math.max(iconSize, showTagline ? iconSize + 14 : iconSize);
        // largeur approximative : icône + gap + texte
        int textW = estimateTextWidth();
        return new Dimension(iconSize + 12 + textW, h);
    }

    private int estimateTextWidth() {
        Font f = VMSStyle.FONT_BRAND.deriveFont(iconSize * 0.70f);
        FontMetrics fm = getFontMetrics(f);
        return fm.stringWidth(title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Color fg, bg, textColor, taglineColor;
        switch (variant) {
            case DARK -> {
                fg = Color.WHITE;
                bg = new Color(255, 255, 255, 30);
                textColor = Color.WHITE;
                taglineColor = new Color(255, 255, 255, 180);
            }
            case ICON_ONLY -> {
                fg = Color.WHITE;
                bg = VMSStyle.RED_PRIMARY;
                textColor = VMSStyle.TEXT_PRIMARY;
                taglineColor = VMSStyle.TEXT_MUTED;
            }
            default -> { // LIGHT
                fg = Color.WHITE;
                bg = VMSStyle.RED_PRIMARY;
                textColor = VMSStyle.TEXT_PRIMARY;
                taglineColor = VMSStyle.TEXT_MUTED;
            }
        }

        int y = (getHeight() - iconSize) / 2;
        VMSStyle.paintLogo(g2, 0, y, iconSize, fg, bg);

        if (variant != Variant.ICON_ONLY) {
            int textX = iconSize + 12;
            float titleSize = iconSize * 0.70f;
            Font titleFont = VMSStyle.FONT_BRAND.deriveFont(titleSize);
            g2.setFont(titleFont);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();

            int titleY;
            if (showTagline) {
                int totalH = fm.getAscent() + 16;
                titleY = (getHeight() - totalH) / 2 + fm.getAscent();
                g2.drawString(title, textX, titleY);

                Font tagFont = VMSStyle.FONT_SUBTITLE;
                g2.setFont(tagFont);
                g2.setColor(taglineColor);
                g2.drawString(tagline, textX, titleY + 14);
            } else {
                titleY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(title, textX, titleY);
            }
        }

        g2.dispose();
    }
}
