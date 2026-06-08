package pkg.vms.magasin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.controller.RedemptionController;

import java.util.List;

/**
 * Interface principale POS — Validation des bons au point de vente.
 *
 * <p>Optimisée pour un usage caisse : grand champ de saisie compatible
 * avec les lecteurs QR USB (sortie HID/clavier), bouton de validation
 * proéminent, retour visuel immédiat et historique du jour.</p>
 */
public class MagasinMainView {

    private final Stage                stage;
    private final AuthDAO.UserSession  session;
    private final RedemptionController ctrl = new RedemptionController();

    // UI state
    private ComboBox<ClientDAO.MagasinInfo> cbMagasin;
    private TextField                        tfCode;
    private Button                           btnValider;
    private VBox                             resultPane;
    private final ObservableList<BonDAO.RedemptionRecord> histRows =
            FXCollections.observableArrayList();

    public MagasinMainView(Stage stage, AuthDAO.UserSession session) {
        this.stage   = stage;
        this.session = session;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#f1f5f9;");
        root.setTop(buildTopBar());
        root.setCenter(buildCenter());

        Scene scene = new Scene(root, 920, 680);
        try {
            String css = getClass().getResource("/vms.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        stage.setTitle("VMS — Magasin · " + session.username);
        stage.setScene(scene);
        stage.setMinWidth(780);
        stage.setMinHeight(560);
        stage.centerOnScreen();
        stage.show();

        loadMagasins();
        tfCode.requestFocus();
    }

    // ── Top bar ───────────────────────────────────────────────────────────────

    private HBox buildTopBar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 24, 0, 24));
        bar.setPrefHeight(64);
        bar.setStyle("-fx-background-color:#0f172a;");

        // Logo
        Label logo = new Label("VMS");
        logo.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#dc2626;");
        Label appLabel = new Label("Magasin");
        appLabel.setStyle("-fx-font-size:12;-fx-text-fill:#94a3b8;");
        VBox logoBox = new VBox(0, logo, appLabel);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        Separator vs = new Separator(javafx.geometry.Orientation.VERTICAL);
        vs.setStyle("-fx-background-color:#334155;");
        vs.setPrefHeight(36);

        // Magasin selector
        Label lblMag = new Label("Point de vente :");
        lblMag.setStyle("-fx-font-size:12;-fx-text-fill:#94a3b8;");
        cbMagasin = new ComboBox<>();
        cbMagasin.setPromptText("Sélectionner…");
        cbMagasin.setPrefWidth(220);
        cbMagasin.setStyle("-fx-font-size:12;");
        cbMagasin.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClientDAO.MagasinInfo m) {
                return m == null ? "" : m.nom;
            }
            @Override public ClientDAO.MagasinInfo fromString(String s) { return null; }
        });
        cbMagasin.valueProperty().addListener((o, ov, nv) -> {
            if (nv != null) refreshHistory(nv.id);
        });

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // User info
        Label userLbl = new Label("👤 " + session.username);
        userLbl.setStyle("-fx-font-size:12;-fx-text-fill:#cbd5e1;");
        Label roleLbl = new Label(session.role);
        roleLbl.setStyle("-fx-font-size:10;-fx-text-fill:#64748b;");
        VBox userBox = new VBox(1, userLbl, roleLbl);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnLogout = new Button("Déconnexion");
        btnLogout.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-font-size:12;"
                + "-fx-padding:6 14;-fx-background-radius:8;-fx-border-color:#334155;"
                + "-fx-border-radius:8;-fx-cursor:hand;");
        btnLogout.setOnAction(e -> logout());

        bar.getChildren().addAll(logoBox, vs, lblMag, cbMagasin, sp, userBox, btnLogout);
        return bar;
    }

    // ── Centre ────────────────────────────────────────────────────────────────

    private HBox buildCenter() {
        HBox center = new HBox(20);
        center.setPadding(new Insets(24));
        center.setFillHeight(true);

        VBox leftPanel = buildValidationPanel();
        HBox.setHgrow(leftPanel, Priority.ALWAYS);

        VBox rightPanel = buildHistoryPanel();
        rightPanel.setPrefWidth(320);
        rightPanel.setMinWidth(280);

        center.getChildren().addAll(leftPanel, rightPanel);
        return center;
    }

    // ── Panneau de validation (gauche) ────────────────────────────────────────

    private VBox buildValidationPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.TOP_CENTER);

        // Titre
        Label title = new Label("VALIDATION BON CADEAU");
        title.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#1e293b;"
                + "-fx-letter-spacing:2;");

        Label hint = new Label("Scannez le QR code ou saisissez le code manuellement");
        hint.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        // Code input
        VBox inputCard = buildInputCard();

        // Result pane
        resultPane = new VBox(12);
        resultPane.setAlignment(Pos.CENTER);
        resultPane.setPadding(new Insets(20));
        resultPane.setMinHeight(160);
        resultPane.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-border-color:#e2e8f0;-fx-border-radius:16;");
        showResultPlaceholder();

        panel.getChildren().addAll(title, hint, inputCard, resultPane);
        return panel;
    }

    private VBox buildInputCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(24));
        card.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");

        Label scanIcon = new Label("📷");
        scanIcon.setStyle("-fx-font-size:36;");
        scanIcon.setAlignment(Pos.CENTER);

        tfCode = new TextField();
        tfCode.setPromptText("Code bon — scanner ou saisir…");
        tfCode.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-padding:14 18;"
                + "-fx-border-color:#d1d5db;-fx-border-radius:10;-fx-background-radius:10;"
                + "-fx-font-family:monospace;-fx-background-color:#f8fafc;");
        tfCode.setMaxWidth(Double.MAX_VALUE);
        // Valider automatiquement si le code semble complet (lecteur USB finit par Enter)
        tfCode.setOnAction(e -> valider());

        btnValider = new Button("✓   VALIDER LE BON");
        btnValider.setMaxWidth(Double.MAX_VALUE);
        btnValider.setStyle("-fx-background-color:#16a34a;-fx-text-fill:white;-fx-font-size:16;"
                + "-fx-font-weight:bold;-fx-padding:16 32;-fx-background-radius:12;"
                + "-fx-cursor:hand;");
        btnValider.setOnAction(e -> valider());

        Button btnClear = new Button("⟳  Nouveau scan");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:13;"
                + "-fx-padding:8;-fx-cursor:hand;");
        btnClear.setOnAction(e -> resetScan());

        VBox.setVgrow(tfCode, Priority.NEVER);
        card.getChildren().addAll(scanIcon, tfCode, btnValider, btnClear);
        card.setAlignment(Pos.CENTER);
        return card;
    }

    // ── Panneau historique (droite) ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private VBox buildHistoryPanel() {
        VBox panel = new VBox(12);
        panel.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),10,0,0,3);");
        panel.setPadding(new Insets(20));

        Label title = new Label("📋 Activité du jour");
        title.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Button btnRefresh = new Button("↻");
        btnRefresh.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:14;"
                + "-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> {
            if (cbMagasin.getValue() != null) refreshHistory(cbMagasin.getValue().id);
        });

        HBox header = new HBox(title, new Region(), btnRefresh);
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        TableView<BonDAO.RedemptionRecord> tbl = new TableView<>(histRows);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setStyle("-fx-font-size:12;");
        tbl.setPlaceholder(new Label("Aucune rédemption aujourd'hui."));
        VBox.setVgrow(tbl, Priority.ALWAYS);

        TableColumn<BonDAO.RedemptionRecord, String> cHeure = new TableColumn<>("Heure");
        cHeure.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().heure));
        cHeure.setPrefWidth(60);

        TableColumn<BonDAO.RedemptionRecord, String> cCode = new TableColumn<>("Code");
        cCode.setCellValueFactory(p -> new SimpleStringProperty(
            shortenCode(p.getValue().codeUnique)));
        cCode.setPrefWidth(110);

        TableColumn<BonDAO.RedemptionRecord, String> cVal = new TableColumn<>("Rs");
        cVal.setCellValueFactory(p -> new SimpleStringProperty(
            String.format("%,.0f", p.getValue().valeur)));
        cVal.setPrefWidth(70);

        TableColumn<BonDAO.RedemptionRecord, String> cStat = new TableColumn<>("✓");
        cStat.setPrefWidth(36);
        cStat.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().statut));
        cStat.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText("OK".equals(v) ? "✓" : "✗");
                setStyle("-fx-text-fill:" + ("OK".equals(v) ? "#16a34a" : "#dc2626")
                       + ";-fx-font-weight:bold;-fx-alignment:CENTER;");
            }
        });

        tbl.getColumns().addAll(cHeure, cCode, cVal, cStat);

        // Compteur
        Label lblCount = new Label("0 rédemption(s)");
        lblCount.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");
        histRows.addListener((javafx.collections.ListChangeListener<BonDAO.RedemptionRecord>) c ->
            lblCount.setText(histRows.size() + " rédemption(s) aujourd'hui"));

        panel.getChildren().addAll(header, lblCount, tbl);
        return panel;
    }

    // ── Logic ─────────────────────────────────────────────────────────────────

    private void loadMagasins() {
        ctrl.chargerMagasins(list -> {
            cbMagasin.setItems(FXCollections.observableArrayList(list));
            if (!list.isEmpty()) {
                cbMagasin.setValue(list.get(0));  // sélectionner le premier par défaut
                refreshHistory(list.get(0).id);
            }
        }, err -> showAlert("Erreur chargement magasins : " + err));
    }

    private void valider() {
        String code = tfCode.getText().trim();
        if (code.isEmpty()) {
            showResultError("Saisissez ou scannez un code bon.", null); return;
        }
        ClientDAO.MagasinInfo mag = cbMagasin.getValue();
        if (mag == null) {
            showResultError("Sélectionnez un point de vente.", null); return;
        }

        btnValider.setDisable(true);
        btnValider.setText("Validation en cours…");
        btnValider.setStyle(btnValider.getStyle().replace("#16a34a", "#64748b"));

        showResultLoading();

        ctrl.redimerBon(code, mag.id, session.userId, result -> {
            resetBtn();
            if (result.succes) {
                showResultSuccess(code, result.valeur);
                refreshHistory(mag.id);
                // Prêt pour le prochain scan
                tfCode.clear();
                tfCode.requestFocus();
            } else {
                showResultError(result.message, result.errorType);
            }
        }, err -> {
            resetBtn();
            showResultError("Erreur technique : " + err, BonDAO.RedemptionResult.ErrorType.UNKNOWN);
        });
    }

    private void resetScan() {
        tfCode.clear();
        showResultPlaceholder();
        tfCode.requestFocus();
    }

    private void resetBtn() {
        btnValider.setDisable(false);
        btnValider.setText("✓   VALIDER LE BON");
        btnValider.setStyle(btnValider.getStyle().replace("#64748b", "#16a34a"));
    }

    private void refreshHistory(int magasinId) {
        Task<List<BonDAO.RedemptionRecord>> task = new Task<>() {
            @Override protected List<BonDAO.RedemptionRecord> call() throws Exception {
                return BonDAO.getRedemptionsAujourdhui(magasinId);
            }
        };
        task.setOnSucceeded(e -> histRows.setAll(task.getValue()));
        task.setOnFailed(e -> {}); // silencieux
        new Thread(task).start();
    }

    private void logout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Se déconnecter de l'application magasin ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Déconnexion");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) new MagasinLoginView(stage).show();
        });
    }

    // ── Result display ────────────────────────────────────────────────────────

    private void showResultPlaceholder() {
        resultPane.setStyle(resultPane.getStyle().replace("-fx-border-color:#16a34a;",
            "-fx-border-color:#e2e8f0;"));
        resultPane.getChildren().setAll(
            styledLabel("En attente de scan…", "#94a3b8", 14, false)
        );
    }

    private void showResultLoading() {
        ProgressIndicator pi = new ProgressIndicator();
        pi.setPrefSize(40, 40);
        pi.setStyle("-fx-accent:#16a34a;");
        resultPane.getChildren().setAll(pi,
            styledLabel("Validation en cours…", "#64748b", 13, false));
        resultPane.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-border-color:#e2e8f0;-fx-border-radius:16;");
    }

    private void showResultSuccess(String code, double valeur) {
        resultPane.setStyle("-fx-background-color:#f0fdf4;-fx-background-radius:16;"
                + "-fx-border-color:#86efac;-fx-border-radius:16;");

        Label iconLbl  = styledLabel("✓", "#16a34a", 52, true);
        Label titleLbl = styledLabel("BON VALIDÉ !", "#15803d", 20, true);
        Label valLbl   = styledLabel("Rs " + String.format("%,.2f", valeur), "#1e293b", 30, true);
        Label codeLbl  = styledLabel(shortenCode(code), "#64748b", 12, false);

        resultPane.getChildren().setAll(iconLbl, titleLbl, valLbl, codeLbl);
    }

    private void showResultError(String message, BonDAO.RedemptionResult.ErrorType type) {
        String icon, color, bg;
        if (type == null || type == BonDAO.RedemptionResult.ErrorType.UNKNOWN
                || type == BonDAO.RedemptionResult.ErrorType.INVALID_CODE) {
            icon = "✗"; color = "#dc2626"; bg = "#fef2f2";
        } else if (type == BonDAO.RedemptionResult.ErrorType.ALREADY_USED) {
            icon = "⛔"; color = "#dc2626"; bg = "#fef2f2";
        } else if (type == BonDAO.RedemptionResult.ErrorType.EXPIRED) {
            icon = "⏰"; color = "#d97706"; bg = "#fffbeb";
        } else if (type == BonDAO.RedemptionResult.ErrorType.CONNECTION_ERROR) {
            icon = "🔌"; color = "#d97706"; bg = "#fffbeb";
        } else {
            icon = "!"; color = "#dc2626"; bg = "#fef2f2";
        }

        resultPane.setStyle("-fx-background-color:" + bg + ";-fx-background-radius:16;"
                + "-fx-border-color:#fca5a5;-fx-border-radius:16;");

        Label iconLbl  = styledLabel(icon, color, 44, true);
        Label titleLbl = styledLabel("Rédemption refusée", color, 16, true);
        Label msgLbl   = styledLabel(message, "#374151", 13, false);
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(380);
        msgLbl.setAlignment(Pos.CENTER);

        resultPane.getChildren().setAll(iconLbl, titleLbl, msgLbl);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Label styledLabel(String text, String color, int size, boolean bold) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:" + size + ";-fx-text-fill:" + color + ";"
                + (bold ? "-fx-font-weight:bold;" : ""));
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private String shortenCode(String code) {
        if (code == null) return "";
        // Affiche les 16 derniers caractères pour les codes longs
        return code.length() > 20 ? "…" + code.substring(code.length() - 16) : code;
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
