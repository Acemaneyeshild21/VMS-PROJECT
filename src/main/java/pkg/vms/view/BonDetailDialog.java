package pkg.vms.view;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pkg.vms.DAO.BonDAO;

/**
 * Dialog de détail d'un bon — affiche les informations complètes
 * et le QR code généré à la volée avec ZXing.
 */
public class BonDetailDialog {

    private final BonDAO.BonInfo bon;

    public BonDetailDialog(BonDAO.BonInfo bon) { this.bon = bon; }

    public void show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Détail du bon — " + bon.codeUnique);
        stage.setResizable(false);

        VBox root = buildContent(stage);
        stage.setScene(new Scene(root));
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildContent(Stage stage) {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");

        // ── Header coloré selon statut ──────────────────────────────────────
        VBox header = buildHeader();

        // ── Corps en deux colonnes ──────────────────────────────────────────
        HBox body = new HBox(24);
        body.setPadding(new Insets(24, 32, 24, 32));
        body.setAlignment(Pos.TOP_LEFT);

        VBox details = buildDetails();
        HBox.setHgrow(details, Priority.ALWAYS);

        VBox qrBox = buildQRBox();

        body.getChildren().addAll(details, qrBox);

        // ── Footer ──────────────────────────────────────────────────────────
        HBox footer = buildFooter(stage);

        root.getChildren().addAll(header, body, footer);
        return root;
    }

    private VBox buildHeader() {
        String[] colors = statutColors(bon.statut);

        VBox h = new VBox(6);
        h.setPadding(new Insets(24, 32, 20, 32));
        h.setStyle("-fx-background-color:" + colors[0] + ";");

        Label code = new Label(bon.codeUnique);
        code.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:white;"
                + "-fx-font-family:monospace;");

        Label statutLbl = new Label(bon.statut);
        statutLbl.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-padding:3 10;"
                + "-fx-background-radius:20;-fx-background-color:" + colors[1]
                + ";-fx-text-fill:" + colors[0] + ";");

        Label valLbl = new Label("Rs " + String.format("%,.2f", bon.valeur));
        valLbl.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:white;");

        h.getChildren().addAll(code, new HBox(8, valLbl, statutLbl));
        HBox.setHgrow(valLbl, Priority.ALWAYS);
        return h;
    }

    private VBox buildDetails() {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        card.setPadding(new Insets(0));

        Label title = new Label("Informations");
        title.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;"
                + "-fx-padding:16 20 8 20;");

        Separator sep = new Separator();

        VBox rows = new VBox(0);
        rows.getChildren().addAll(
            detailRow("Client",           nvl(bon.clientNom),    true),
            detailRow("Email client",     nvl(bon.clientEmail),  false),
            detailRow("Réf. demande",     nvl(bon.reference),    true),
            detailRow("Date d'émission",  nvl(bon.dateEmission), false),
            detailRow("Date d'expiration",nvl(bon.dateExpiration),true),
            detailRow("ID bon",           String.valueOf(bon.bonId), false)
        );

        card.getChildren().addAll(title, sep, rows);
        card.setMinWidth(320);
        return card;
    }

    private HBox detailRow(String label, String value, boolean shaded) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;-fx-min-width:130;");
        Label val = new Label(value.isBlank() ? "—" : value);
        val.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        val.setWrapText(true);

        HBox row = new HBox(12, lbl, val);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 20, 10, 20));
        if (shaded) row.setStyle("-fx-background-color:#f8fafc;");
        return row;
    }

    private VBox buildQRBox() {
        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        box.setMinWidth(230);

        Label title = new Label("QR Code");
        title.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        ImageView iv = new ImageView(generateQR(bon.codeUnique, 200));
        iv.setSmooth(false); // pixel-parfait
        iv.setFitWidth(200);
        iv.setFitHeight(200);

        Label hint = new Label("Scanner pour valider");
        hint.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");

        box.getChildren().addAll(title, iv, hint);
        return box;
    }

    private HBox buildFooter(Stage stage) {
        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-background-color:#1e293b;-fx-text-fill:white;-fx-font-size:13;"
                + "-fx-font-weight:bold;-fx-padding:9 24;-fx-background-radius:8;-fx-cursor:hand;");
        btnClose.setOnAction(e -> stage.close());

        HBox h = new HBox(btnClose);
        h.setAlignment(Pos.CENTER_RIGHT);
        h.setPadding(new Insets(0, 32, 24, 32));
        return h;
    }

    // ── QR Generation ────────────────────────────────────────────────────────

    private WritableImage generateQR(String content, int size) {
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();

        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    pw.setColor(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
        } catch (WriterException ex) {
            // QR non générable : fond gris avec message
            for (int x = 0; x < size; x++)
                for (int y = 0; y < size; y++)
                    pw.setColor(x, y, Color.LIGHTGRAY);
        }
        return img;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** @return [backgroundColor, badgeBackground] */
    private String[] statutColors(String statut) {
        return switch (statut != null ? statut : "") {
            case "GENERE"  -> new String[]{"#2563eb", "#dbeafe"};
            case "ENVOYE"  -> new String[]{"#16a34a", "#dcfce7"};
            case "UTILISE" -> new String[]{"#7c3aed", "#ede9fe"};
            case "EXPIRE"  -> new String[]{"#d97706", "#fef3c7"};
            case "ANNULE"  -> new String[]{"#64748b", "#f1f5f9"};
            default        -> new String[]{"#1e293b", "#f1f5f9"};
        };
    }

    private String nvl(String s) { return s != null ? s : ""; }
}
