package pkg.vms;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Panneau de chargement "skeleton" (barres grises animées).
 *
 * <p>Remplace le spinner ou le texte "Chargement…" pendant les appels BD.
 * Donne l'impression que le contenu est sur le point d'apparaître.
 *
 * <pre>
 *   ████████████████████████████   ← titre fictif
 *   ████████████  ██████  ██████   ← ligne 1
 *   ████████████  ██████  ██████   ← ligne 2
 *   ████████████  ██████  ██████   ← ligne 3
 * </pre>
 *
 * Usage :
 * <pre>
 *   SkeletonPane sk = new SkeletonPane(5, 4);  // 5 lignes, 4 colonnes
 *   contentCard.getChildren().setAll(sk);       // show
 *   sk.stop();
 *   contentCard.getChildren().setAll(table);    // replace when data arrives
 * </pre>
 */
public class SkeletonPane extends VBox {

    private final Timeline pulse;

    /**
     * @param rows     nombre de lignes skeleton à afficher
     * @param cols     nombre de "cellules" par ligne
     */
    public SkeletonPane(int rows, int cols) {
        super(0);
        setStyle("-fx-background-color:white;-fx-padding:0;");

        // Ligne d'en-tête (une seule barre large)
        getChildren().add(headerRow());

        for (int r = 0; r < rows; r++) {
            getChildren().add(dataRow(r, cols));
        }

        // Animation shimmer : opacité 0.5 → 1.0 → 0.5 en boucle
        pulse = new Timeline(
            new KeyFrame(Duration.ZERO,   new KeyValue(opacityProperty(), 0.5)),
            new KeyFrame(Duration.millis(700), new KeyValue(opacityProperty(), 1.0)),
            new KeyFrame(Duration.millis(1400), new KeyValue(opacityProperty(), 0.5))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    public void stop() { pulse.stop(); }

    // ── Construction ─────────────────────────────────────────────────────────

    private HBox headerRow() {
        HBox h = new HBox();
        h.setPadding(new Insets(12, 16, 12, 16));
        h.setStyle("-fx-background-color:#f8fafc;"
                 + "-fx-border-color:transparent transparent #e2e8f0 transparent;"
                 + "-fx-border-width:0 0 1 0;");
        Rectangle bar = bar(220, 12);
        h.getChildren().add(bar);
        return h;
    }

    private HBox dataRow(int rowIndex, int cols) {
        HBox h = new HBox(12);
        h.setPadding(new Insets(14, 16, 14, 16));
        h.setStyle(
            "-fx-background-color:" + (rowIndex % 2 == 0 ? "white" : "#fafbfd") + ";"
            + "-fx-border-color:transparent transparent #f1f5f9 transparent;"
            + "-fx-border-width:0 0 1 0;");
        HBox.setHgrow(h, Priority.ALWAYS);

        double[] widths = {140, 110, 160, 100, 90, 150}; // largeurs variables par colonne

        for (int c = 0; c < Math.min(cols, widths.length); c++) {
            double w = widths[c] * (0.6 + Math.random() * 0.4); // légère variation
            Rectangle r = bar((int) w, 12);
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);
            h.getChildren().addAll(r, sp);
        }
        return h;
    }

    private static Rectangle bar(int w, int h) {
        Rectangle r = new Rectangle(w, h);
        r.setFill(Color.web("#e2e8f0"));
        r.setArcWidth(6);
        r.setArcHeight(6);
        return r;
    }
}
