package pkg.vms;

import pkg.vms.DAO.SettingsDAO;

import javax.swing.*;

/**
 * Gestionnaire du th\u00e8me (clair / sombre) de l'application VMS.
 *
 * Persistance : la pr\u00e9f\u00e9rence est stock\u00e9e dans la table {@code app_settings}
 * sous la cl\u00e9 {@code ui.theme} (valeurs : "light" ou "dark").
 *
 * Application : {@link VMSStyle#applyTheme(boolean)} r\u00e9affecte toutes les
 * couleurs de la palette. Les composants qui ont mis en cache une couleur
 * via {@code setBackground(VMSStyle.X)} lors de leur construction doivent
 * \u00eatre recr\u00e9\u00e9s (dispose+recreate le JFrame). Les composants dont
 * {@code paintComponent} lit les couleurs dynamiquement se mettent \u00e0
 * jour sur un simple {@code repaint}.
 */
public class ThemeManager {

    public static final String KEY = "ui.theme";
    public static final String LIGHT = "light";
    public static final String DARK  = "dark";

    private ThemeManager() {}

    /**
     * Charge le th\u00e8me depuis la BD et l'applique \u00e0 VMSStyle.
     * Appel\u00e9 une fois au d\u00e9marrage de l'application.
     * Silencieux en cas d'erreur BD (reste sur clair par d\u00e9faut).
     */
    public static void loadAndApply() {
        try {
            String v = SettingsDAO.getSetting(KEY);
            boolean dark = DARK.equalsIgnoreCase(v);
            VMSStyle.applyTheme(dark);
        } catch (Exception e) {
            System.err.println("[ThemeManager] Impossible de charger le th\u00e8me : " + e.getMessage());
            VMSStyle.applyTheme(false);
        }
    }

    /**
     * Sauvegarde la pr\u00e9f\u00e9rence de th\u00e8me en BD (sans toucher \u00e0 VMSStyle).
     * @return true si la sauvegarde r\u00e9ussit
     */
    public static boolean savePreference(boolean dark) {
        try {
            return SettingsDAO.updateSetting(KEY, dark ? DARK : LIGHT);
        } catch (Exception e) {
            System.err.println("[ThemeManager] Impossible de sauvegarder le th\u00e8me : " + e.getMessage());
            return false;
        }
    }

    /** Retourne l'\u00e9tat courant (true = sombre). */
    public static boolean isDark() {
        return VMSStyle.isDark();
    }

    /**
     * Bascule le th\u00e8me en mode "live" :
     *   1. Sauvegarde la pr\u00e9f\u00e9rence en BD
     *   2. Applique la nouvelle palette dans VMSStyle
     *   3. Ferme le Dashboard courant et en ouvre un nouveau (m\u00eames credentials)
     *
     * Cette approche "dispose + recreate" garantit que tous les
     * {@code setBackground(VMSStyle.X)} cach\u00e9s sont r\u00e9-appliqu\u00e9s
     * avec les nouvelles valeurs, sans toucher aux 30+ fichiers existants.
     *
     * @param currentDashboard fen\u00eatre active (sera dispos\u00e9e)
     * @param userId identifiant utilisateur (pour recr\u00e9er le Dashboard)
     * @param username nom utilisateur
     * @param role r\u00f4le
     * @param email email
     */
    public static void toggle(JFrame currentDashboard,
                               int userId, String username, String role, String email) {
        boolean newDark = !isDark();
        savePreference(newDark);
        VMSStyle.applyTheme(newDark);

        SwingUtilities.invokeLater(() -> {
            // Fermer toutes les fen\u00eatres enfant (dialogs ouvertes)
            for (java.awt.Window w : java.awt.Window.getWindows()) {
                if (w != currentDashboard && w.isDisplayable()) {
                    w.dispose();
                }
            }
            // Dispose l'ancien dashboard
            if (currentDashboard != null) {
                currentDashboard.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                currentDashboard.dispose();
            }
            // Recr\u00e9er le Dashboard avec le th\u00e8me actif
            Dashboard d = new Dashboard(userId, username, role, email);
            d.setVisible(true);
        });
    }
}
