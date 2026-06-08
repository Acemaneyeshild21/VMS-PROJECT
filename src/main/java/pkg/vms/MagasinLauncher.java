package pkg.vms;

/**
 * Point d'entrée de l'application légère magasin (POS).
 *
 * <p>Contourne la restriction JavaFX sur les fat-JAR : cette classe
 * n'étend PAS {@code Application}, elle délègue à {@link MagasinApp}.</p>
 *
 * <p>Lancer avec : {@code java -cp vms.jar pkg.vms.MagasinLauncher}</p>
 */
public class MagasinLauncher {
    public static void main(String[] args) {
        MagasinApp.launch(MagasinApp.class, args);
    }
}
