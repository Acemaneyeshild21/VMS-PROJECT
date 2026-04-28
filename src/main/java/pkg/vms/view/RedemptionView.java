package pkg.vms.view;

import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.controller.RedemptionController;

import java.util.List;

/**
 * Vue Rédemption — saisie d'un code bon + sélection magasin + résultat.
 */
public class RedemptionView {

    private final AuthDAO.UserSession   session;
    private final RedemptionController  ctrl = new RedemptionController();

    private ComboBox<ClientDAO.MagasinInfo> cbMagasin;
    private TextField                       tfCode;
    private Button                          btnValider;
    private VBox                            resultBox;

    public RedemptionView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");
        root.getChildren().addAll(buildHeader(), buildContent());
        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setPadding(new Insets(24, 32, 16, 32));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:#f1f5f9;");

        Label title = new Label("Rédemption de bons");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label sub = new Label("Saisir un code unique pour valider un bon");
        sub.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        VBox titles = new VBox(4, title, sub);
        h.getChildren().add(titles);
        return h;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private ScrollPane buildContent() {
        VBox inner = new VBox(24);
        inner.setPadding(new Insets(0, 32, 32, 32));

        inner.getChildren().addAll(buildSaisieCard(), buildResultatCard());
        loadMagasins();

        ScrollPane sp = new ScrollPane(inner);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private VBox buildSaisieCard() {
        VBox card = card("Valider un bon cadeau");

        // Magasin
        Label lMag = fieldLabel("Point de vente *");
        cbMagasin = new ComboBox<>();
        cbMagasin.setPromptText("Chargement des magasins…");
        cbMagasin.setPrefWidth(360);
        cbMagasin.setStyle("-fx-font-size:13;-fx-padding:4 8;");
        cbMagasin.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClientDAO.MagasinInfo m) {
                return m == null ? "" : m.nom;
            }
            @Override public ClientDAO.MagasinInfo fromString(String s) { return null; }
        });

        // Code bon
        Label lCode = fieldLabel("Code unique du bon *");
        tfCode = new TextField();
        tfCode.setPromptText("Ex: BON-2024-XXXXXXXXX");
        tfCode.setPrefWidth(360);
        tfCode.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-padding:10 14;"
                + "-fx-border-color:#d1d5db;-fx-border-radius:8;-fx-background-radius:8;"
                + "-fx-font-family:monospace;");
        // Valider aussi avec Entrée
        tfCode.setOnAction(e -> valider());

        btnValider = new Button("✓  Valider le bon");
        btnValider.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:14;"
                + "-fx-font-weight:bold;-fx-padding:12 28;-fx-background-radius:10;-fx-cursor:hand;");
        btnValider.setOnAction(e -> valider());

        Button btnReset = new Button("⟳ Réinitialiser");
        btnReset.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:13;"
                + "-fx-padding:11 20;-fx-background-radius:10;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:10;-fx-cursor:hand;");
        btnReset.setOnAction(e -> resetForm());

        HBox btnRow = new HBox(12, btnValider, btnReset);
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setPadding(new Insets(8, 0, 0, 0));

        VBox form = new VBox(16, lMag, cbMagasin, lCode, tfCode, btnRow);
        card.getChildren().add(form);
        return card;
    }

    private VBox buildResultatCard() {
        VBox outer = card("Résultat de la rédemption");

        resultBox = new VBox(16);
        resultBox.setAlignment(Pos.CENTER);
        resultBox.setPadding(new Insets(20, 0, 10, 0));

        Label placeholder = new Label("Saisissez un code et cliquez sur Valider.");
        placeholder.setStyle("-fx-font-size:13;-fx-text-fill:#94a3b8;");
        resultBox.getChildren().add(placeholder);

        outer.getChildren().add(resultBox);
        return outer;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadMagasins() {
        ctrl.chargerMagasins(list -> {
            cbMagasin.setItems(FXCollections.observableArrayList(list));
            cbMagasin.setPromptText(list.isEmpty() ? "Aucun magasin disponible" : "Sélectionner un magasin…");
        }, err -> showErr("Erreur chargement magasins : " + err));
    }

    private void valider() {
        String code = tfCode.getText().trim();
        ClientDAO.MagasinInfo mag = cbMagasin.getValue();

        if (code.isEmpty()) {
            showErrInline("Veuillez saisir le code du bon."); return;
        }
        if (mag == null) {
            showErrInline("Veuillez sélectionner un point de vente."); return;
        }

        btnValider.setDisable(true);
        btnValider.setText("Validation…");
        showPending();

        ctrl.redimerBon(code, mag.id, session.userId, result -> {
            btnValider.setDisable(false);
            btnValider.setText("✓  Valider le bon");
            showResult(result, code);
        }, err -> {
            btnValider.setDisable(false);
            btnValider.setText("✓  Valider le bon");
            showErrInline("Erreur technique : " + err);
        });
    }

    private void resetForm() {
        tfCode.clear();
        cbMagasin.setValue(null);
        resultBox.getChildren().setAll(
            styledLabel("Saisissez un code et cliquez sur Valider.", "#94a3b8", 13, false)
        );
    }

    // ── Result display ────────────────────────────────────────────────────────

    private void showPending() {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(48, 48);
        pi.setStyle("-fx-accent:#dc2626;");
        Label lbl = styledLabel("Vérification en cours…", "#64748b", 13, false);
        resultBox.getChildren().setAll(pi, lbl);
    }

    private void showResult(BonDAO.RedemptionResult r, String code) {
        resultBox.getChildren().clear();

        if (r.succes) {
            // SUCCESS
            VBox box = new VBox(12);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:12;"
                       + "-fx-border-color:#86efac;-fx-border-radius:12;-fx-padding:28 40;");

            Label icon  = styledLabel("✓", "#16a34a", 48, true);
            Label title = styledLabel("Bon validé avec succès !", "#15803d", 18, true);
            Label lCode = styledLabel("Code : " + code, "#374151", 13, false);
            Label lVal  = styledLabel("Valeur : Rs " + String.format("%,.0f", r.valeur), "#1e293b", 26, true);
            Label lMsg  = styledLabel(r.message, "#64748b", 12, false);

            Separator sep = new Separator();
            sep.setMaxWidth(280);

            box.getChildren().addAll(icon, title, sep, lCode, lVal, lMsg);
            resultBox.getChildren().add(box);

        } else {
            // FAILURE
            String bg, border, iconTxt, color;
            switch (r.errorType) {
                case EXPIRED         -> { bg="#fff7ed"; border="#fcd34d"; iconTxt="⏰"; color="#d97706"; }
                case ALREADY_USED    -> { bg="#fef2f2"; border="#fca5a5"; iconTxt="⛔"; color="#dc2626"; }
                case INVALID_CODE    -> { bg="#fef2f2"; border="#fca5a5"; iconTxt="✗";  color="#dc2626"; }
                case CANCELLED       -> { bg="#f8fafc"; border="#cbd5e1"; iconTxt="🚫"; color="#64748b"; }
                case CONNECTION_ERROR-> { bg="#fefce8"; border="#fde68a"; iconTxt="🔌"; color="#ca8a04"; }
                case LOCK_TIMEOUT    -> { bg="#fefce8"; border="#fde68a"; iconTxt="⏳"; color="#ca8a04"; }
                default              -> { bg="#fef2f2"; border="#fca5a5"; iconTxt="!";  color="#dc2626"; }
            }

            VBox box = new VBox(12);
            box.setAlignment(Pos.CENTER);
            box.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:12;"
                       + "-fx-border-color:" + border + ";-fx-border-radius:12;-fx-padding:28 40;");

            Label icon  = styledLabel(iconTxt, color, 40, true);
            Label title = styledLabel("Rédemption refusée", color, 17, true);
            Label lCode = styledLabel("Code : " + code, "#374151", 12, false);
            Label lMsg  = styledLabel(r.message, "#374151", 13, false);
            lMsg.setWrapText(true);
            lMsg.setMaxWidth(400);
            lMsg.setAlignment(Pos.CENTER);

            box.getChildren().addAll(icon, title, lCode, lMsg);
            resultBox.getChildren().add(box);
        }
    }

    private void showErrInline(String msg) {
        resultBox.getChildren().setAll(
            styledLabel("⚠ " + msg, "#dc2626", 13, true)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox card(String title) {
        VBox c = new VBox(16);
        c.setPadding(new Insets(24));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        c.getChildren().add(t);
        return c;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private Label styledLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + ";-fx-text-fill:" + color + ";"
                 + (bold ? "-fx-font-weight:bold;" : ""));
        return l;
    }

    private void showErr(String msg) { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
}
