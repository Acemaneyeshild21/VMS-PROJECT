package pkg.vms.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog de progression réutilisable pour les opérations longues.
 */
public class ProgressDialog {

    private final Stage  stage = new Stage();
    private final Label  lblStatus;
    private final ProgressBar bar;

    public ProgressDialog(String title) {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(32, 40, 32, 40));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                    + "-fx-border-color:#e2e8f0;-fx-border-radius:12;");
        root.setPrefWidth(420);

        Label lTitle = new Label(title);
        lTitle.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        bar = new ProgressBar(0);
        bar.setPrefWidth(340);
        bar.setStyle("-fx-accent:#dc2626;");

        lblStatus = new Label("Initialisation…");
        lblStatus.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        root.getChildren().addAll(lTitle, bar, lblStatus);
        stage.setScene(new Scene(root));
    }

    public void show() { stage.show(); }

    public void update(String pct, String message) {
        try {
            double v = Double.parseDouble(pct) / 100.0;
            bar.setProgress(v);
        } catch (NumberFormatException ignored) {
            bar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
        }
        lblStatus.setText(message);
    }

    public void close() { stage.close(); }
}
