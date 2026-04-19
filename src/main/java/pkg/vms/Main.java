package pkg.vms;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Charge le th\u00e8me (clair/sombre) avant d'afficher toute fen\u00eatre
        ThemeManager.loadAndApply();

        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
