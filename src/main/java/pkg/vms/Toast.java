package pkg.vms;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Notification Toast non-bloquante — remplace les Alert.INFORMATION / Alert.ERROR.
 *
 * <p>Usage :
 * <pre>
 *   Toast.success(stage, "Client créé avec succès");
 *   Toast.error(stage, "Connexion impossible");
 *   Toast.info(stage, "Données rechargées");
 *   Toast.warn(stage, "Aucun résultat trouvé");
 * </pre>
 * </p>
 */
public final class Toast {

    public enum Type { SUCCESS, ERROR, INFO, WARN }

    private Toast() {}

    // ── API publique ─────────────────────────────────────────────────────────

    // Avec Stage explicite
    public static void success(Stage owner, String msg) { show(owner, msg, Type.SUCCESS, 3000); }
    public static void error  (Stage owner, String msg) { show(owner, msg, Type.ERROR,   4500); }
    public static void info   (Stage owner, String msg) { show(owner, msg, Type.INFO,    3000); }
    public static void warn   (Stage owner, String msg) { show(owner, msg, Type.WARN,    3500); }

    // Sans Stage — trouve le stage principal automatiquement (usage depuis les vues)
    public static void success(String msg) { show(findStage(), msg, Type.SUCCESS, 3000); }
    public static void error  (String msg) { show(findStage(), msg, Type.ERROR,   4500); }
    public static void info   (String msg) { show(findStage(), msg, Type.INFO,    3000); }
    public static void warn   (String msg) { show(findStage(), msg, Type.WARN,    3500); }

    /** Trouve le Stage principal parmi les fenêtres JavaFX actuellement affichées. */
    private static Stage findStage() {
        return (Stage) javafx.stage.Window.getWindows().stream()
            .filter(w -> w instanceof Stage s && s.isShowing())
            .findFirst().orElse(null);
    }

    // ── Implémentation ───────────────────────────────────────────────────────

    private static void show(Stage owner, String msg, Type type, int durationMs) {
        // Icône + couleur selon le type
        String icon  = switch (type) {
            case SUCCESS -> "✓";
            case ERROR   -> "✕";
            case WARN    -> "⚠";
            case INFO    -> "ℹ";
        };
        String bgColor = switch (type) {
            case SUCCESS -> "#15803d";
            case ERROR   -> "#dc2626";
            case WARN    -> "#d97706";
            case INFO    -> "#2563eb";
        };

        // ── Construction de la card ──────────────────────────────────────────
        Label ico = new Label(icon);
        ico.setStyle(
            "-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;"
            + "-fx-background-color:rgba(255,255,255,0.2);"
            + "-fx-background-radius:20;-fx-min-width:26;-fx-min-height:26;"
            + "-fx-alignment:center;");

        Label text = new Label(msg);
        text.setStyle("-fx-font-size:13;-fx-text-fill:white;-fx-wrap-text:true;");
        text.setMaxWidth(340);
        text.setWrapText(true);

        HBox card = new HBox(12, ico, text);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14, 20, 14, 16));
        card.setStyle(
            "-fx-background-color:" + bgColor + ";"
            + "-fx-background-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.28),16,0,0,4);");
        card.setMinWidth(280);

        // ── Popup ────────────────────────────────────────────────────────────
        Popup popup = new Popup();
        popup.getContent().add(card);
        popup.setAutoFix(true);

        // Position : coin bas-droit de la fenêtre
        popup.show(owner);
        double x = owner.getX() + owner.getWidth()  - card.prefWidth(-1)  - 24;
        double y = owner.getY() + owner.getHeight() - card.prefHeight(-1) - 48;
        popup.setX(x);
        popup.setY(y);

        // ── Animation : slide-in depuis la droite ────────────────────────────
        card.setTranslateX(80);
        card.setOpacity(0);

        Timeline slideIn = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.translateXProperty(), 80),
                new KeyValue(card.opacityProperty(),    0)),
            new KeyFrame(Duration.millis(220),
                new KeyValue(card.translateXProperty(), 0,   Interpolator.EASE_OUT),
                new KeyValue(card.opacityProperty(),    1.0, Interpolator.EASE_OUT))
        );

        // ── Animation : fade-out après délai ────────────────────────────────
        PauseTransition pause = new PauseTransition(Duration.millis(durationMs));

        Timeline fadeOut = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(card.opacityProperty(),    1.0)),
            new KeyFrame(Duration.millis(300),
                new KeyValue(card.opacityProperty(),    0,  Interpolator.EASE_IN),
                new KeyValue(card.translateXProperty(), 40, Interpolator.EASE_IN))
        );
        fadeOut.setOnFinished(e -> popup.hide());

        SequentialTransition seq = new SequentialTransition(slideIn, pause, fadeOut);
        seq.play();
    }
}
