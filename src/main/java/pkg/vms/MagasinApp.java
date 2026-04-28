package pkg.vms;

import javafx.application.Application;
import javafx.stage.Stage;
import pkg.vms.magasin.MagasinLoginView;

/**
 * Application JavaFX légère — Point de vente / Magasin.
 *
 * <p>Interface simplifiée dédiée à la validation (rédemption) des bons cadeaux.
 * Conçue pour un usage POS : grands éléments, retour visuel immédiat,
 * compatible avec les lecteurs de QR code USB (sortie clavier).</p>
 *
 * <p>Séparée de {@link Main} (siège) pour rester légère et focalisée.</p>
 */
public class MagasinApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        new MagasinLoginView(primaryStage).show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
