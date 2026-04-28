package pkg.vms;

/**
 * Classe de lancement — ne doit PAS étendre {@code Application}.
 *
 * <p>Ce contournement est nécessaire pour les fat-JARs (maven-shade) :
 * la JVM JavaFX exige que la classe principale ne soit pas dans le module path
 * si elle étend {@code Application}. {@link Main} étend {@code Application} ;
 * {@code Launcher} délègue sans l'étendre.</p>
 */
public class Launcher {
    public static void main(String[] args) {
        Main.launch(Main.class, args);
    }
}
