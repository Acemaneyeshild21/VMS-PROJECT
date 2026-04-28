package pkg.vms;

import javafx.application.Application;
import javafx.stage.Stage;
import pkg.vms.DAO.EmailLogDAO;
import pkg.vms.DAO.PasswordResetDAO;

/**
 * Point d'entrée JavaFX de l'application VoucherManager (VMS).
 *
 * <p>La classe {@link Launcher} appelle {@code Main.launch()} pour contourner
 * la restriction du module system JavaFX sur les fat-JARs.</p>
 *
 * <p>Phase 2 : {@code start()} instanciera {@code LoginView} (JavaFX).</p>
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Initialisation des tables auxiliaires (hot-deploy sans re-exécuter schema.sql)
        EmailLogDAO.ensureSchema();
        PasswordResetDAO.ensureSchema();

        // Phase 2 : LoginView JavaFX sera créée ici
        // new LoginView(primaryStage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
