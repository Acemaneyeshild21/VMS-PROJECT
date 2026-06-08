package pkg.vms;

import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;

/**
 * Fabrique d'icônes vectorielles FontAwesome (via Ikonli) pour remplacer les emoji
 * Unicode qui ne s'affichent pas correctement sur la JVM Windows (carrés vides).
 *
 * <p>Utilisation typique :
 * <pre>
 *     // Icône simple dans un bouton
 *     JButton btn = new JButton(IconUtil.icon(FontAwesomeSolid.BELL, 16, VMSStyle.TEXT_PRIMARY));
 *
 *     // Label cliquable (pour les cellules de palette, cartes KPI, etc.)
 *     JLabel lbl = IconUtil.label(FontAwesomeSolid.HOME, 18, VMSStyle.RED_PRIMARY);
 *
 *     // Avec tooltip explicatif
 *     IconUtil.withTooltip(lbl, "Retour à l'accueil");
 * </pre>
 *
 * <p>Avantages par rapport aux emoji :
 * <ul>
 *   <li>Rendu vectoriel → net à toutes les tailles et en HiDPI</li>
 *   <li>Rendu 100 % garanti (la police est embarquée dans le JAR Ikonli)</li>
 *   <li>Couleur personnalisable (utile pour le mode sombre)</li>
 *   <li>+1500 icônes disponibles dans le pack FontAwesome 5</li>
 * </ul>
 *
 * <p>Les constantes à utiliser sont dans :
 * {@link FontAwesomeSolid} (style plein, recommandé pour les UI denses) ou
 * {@link FontAwesomeRegular} (style fin, pour les contextes légers).
 */
public final class IconUtil {

    private IconUtil() { /* utilitaire statique */ }

    // ═════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION D'ICÔNES (Icon / FontIcon)
    // ═════════════════════════════════════════════════════════════════════

    /** Icône FontAwesome de taille donnée, couleur = texte primaire du thème courant. */
    public static FontIcon icon(Ikon glyph, int size) {
        return icon(glyph, size, VMSStyle.TEXT_PRIMARY);
    }

    /** Icône FontAwesome de taille et couleur données. */
    public static FontIcon icon(Ikon glyph, int size, Color color) {
        FontIcon ic = FontIcon.of(glyph);
        ic.setIconSize(size);
        ic.setIconColor(color);
        return ic;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DE LABELS (JLabel prêt à coller dans un layout)
    // ═════════════════════════════════════════════════════════════════════

    /** JLabel affichant une icône FontAwesome (couleur = texte primaire). */
    public static JLabel label(Ikon glyph, int size) {
        return label(glyph, size, VMSStyle.TEXT_PRIMARY);
    }

    /** JLabel affichant une icône FontAwesome colorée. */
    public static JLabel label(Ikon glyph, int size, Color color) {
        JLabel l = new JLabel(icon(glyph, size, color));
        l.setOpaque(false);
        return l;
    }

    /** JLabel "icône + texte à droite" — utile pour les cartes KPI, les boutons custom, etc. */
    public static JLabel labelWithText(Ikon glyph, int size, Color color, String text) {
        JLabel l = new JLabel(text, icon(glyph, size, color), JLabel.LEADING);
        l.setIconTextGap(8);
        l.setOpaque(false);
        return l;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  BOUTONS RONDS / CARRÉS AVEC ICÔNE (hover + tooltip)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Bouton circulaire transparent qui affiche une icône centrée.
     * Parfait pour remplacer les petits carrés du top bar (cloche, thème, menu…).
     *
     * @param glyph   icône FontAwesome
     * @param size    taille de l'icône en pixels
     * @param color   couleur de l'icône (au repos)
     * @param tooltip texte explicatif affiché au survol (ne jamais laisser null !)
     */
    public static JButton toolbarButton(Ikon glyph, int size, Color color, String tooltip) {
        JButton b = new JButton(icon(glyph, size, color));
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(size + 16, size + 16));
        // Hover : fond léger
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setContentAreaFilled(true);
                b.setBackground(new Color(0, 0, 0, 20));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                b.setContentAreaFilled(false);
            }
        });
        return b;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═════════════════════════════════════════════════════════════════════

    /** Ajoute un tooltip à n'importe quel JComponent et retourne le composant (chaînage). */
    @SuppressWarnings("unchecked")
    public static <T extends JComponent> T withTooltip(T comp, String tooltip) {
        comp.setToolTipText(tooltip);
        return comp;
    }
}
