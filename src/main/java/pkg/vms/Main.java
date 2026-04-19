package pkg.vms;

import pkg.vms.DAO.EmailLogDAO;
import pkg.vms.DAO.PasswordResetDAO;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Charge le th\u00e8me (clair/sombre) avant d'afficher toute fen\u00eatre
        ThemeManager.loadAndApply();

        // Cr\u00e9e les tables auxiliaires si elles n'existent pas encore
        // (d\u00e9ploiement \u00e0 chaud sans r\u00e9-ex\u00e9cuter schema.sql)
        EmailLogDAO.ensureSchema();
        PasswordResetDAO.ensureSchema();

        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
