package pkg.vms;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pkg.vms.controller.LoginController;

/**
 * Écran de connexion JavaFX — split hero/form.
 * Appelé depuis {@link Main#start(Stage)}.
 */
public class LoginView {

    private final Stage           stage;
    private final LoginController ctrl = new LoginController();

    private TextField     tfUsername;
    private PasswordField tfPassword;
    private Label         lblError;
    private Button        btnLogin;

    public LoginView(Stage stage) { this.stage = stage; }

    public void show() {
        HBox root = new HBox();
        root.getChildren().addAll(buildBrand(), buildForm());
        HBox.setHgrow(buildForm(), Priority.ALWAYS);

        Scene scene = new Scene(root, 980, 620);
        stage.setTitle("VoucherManager VMS — Connexion");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        tfUsername.requestFocus();
    }

    // ── Panel gauche (branding) ──────────────────────────────────────────────

    private VBox buildBrand() {
        VBox p = new VBox(20);
        p.setPrefWidth(420);
        p.setPadding(new Insets(52, 48, 40, 48));
        p.setStyle("-fx-background-color: linear-gradient(to bottom right, #dc2626, #7f1d1d);");

        Label logo = new Label("VMS");
        logo.setStyle("-fx-font-size:34;-fx-font-weight:bold;-fx-text-fill:white;");

        Label logoSub = new Label("VoucherManager");
        logoSub.setStyle("-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.75);");

        Label heroTitle = new Label("Gérez vos bons cadeau\nen toute simplicité.");
        heroTitle.setWrapText(true);
        heroTitle.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:white;-fx-wrap-text:true;");

        Label heroSub = new Label(
            "Plateforme complète : émission, validation, suivi " +
            "et rédemption des bons cadeau en temps réel.");
        heroSub.setWrapText(true);
        heroSub.setMaxWidth(340);
        heroSub.setStyle("-fx-font-size:13;-fx-text-fill:rgba(255,255,255,0.75);-fx-wrap-text:true;");

        HBox chips = new HBox(10, chip("✓ Multi-sociétés"), chip("✓ Audit trail"), chip("✓ Anti-fraude"));

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:rgba(255,255,255,0.2);");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Label ver = new Label("BTS SIO SLAM RP2 — Session 2026");
        ver.setStyle("-fx-font-size:10;-fx-text-fill:rgba(255,255,255,0.45);");

        p.getChildren().addAll(logo, logoSub, sep, heroTitle, heroSub, spacer, chips, ver);
        return p;
    }

    private Label chip(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-background-color:rgba(255,255,255,0.15);-fx-text-fill:white;"
                + "-fx-padding:4 10;-fx-background-radius:20;-fx-font-size:11;");
        return l;
    }

    // ── Panel droit (formulaire) ─────────────────────────────────────────────

    private VBox buildForm() {
        VBox p = new VBox(16);
        p.setAlignment(Pos.CENTER);
        p.setPadding(new Insets(0, 72, 0, 72));
        p.setStyle("-fx-background-color:white;");
        p.setMaxWidth(540);

        Label title = new Label("Connexion");
        title.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label subtitle = new Label("Accédez à votre espace de gestion");
        subtitle.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");

        Separator sep = new Separator();

        Label lUser = mkLabel("Nom d'utilisateur");
        tfUsername = new TextField();
        tfUsername.setPromptText("Entrez votre identifiant");
        styleField(tfUsername);

        Label lPass = mkLabel("Mot de passe");
        tfPassword = new PasswordField();
        tfPassword.setPromptText("••••••••");
        styleField(tfPassword);

        Hyperlink forgot = new Hyperlink("Mot de passe oublié ?");
        forgot.setStyle("-fx-font-size:12;-fx-text-fill:#dc2626;-fx-border-color:transparent;");
        HBox forgotBox = new HBox(forgot);
        forgotBox.setAlignment(Pos.CENTER_RIGHT);

        lblError = new Label();
        lblError.setWrapText(true);
        lblError.setVisible(false);
        lblError.setStyle("-fx-font-size:12;-fx-text-fill:#dc2626;-fx-wrap-text:true;");

        btnLogin = new Button("Se connecter");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:14;"
                + "-fx-font-weight:bold;-fx-padding:12 24;-fx-background-radius:8;-fx-cursor:hand;");

        btnLogin.setOnAction(e -> doLogin());
        tfPassword.setOnAction(e -> doLogin());
        tfUsername.setOnAction(e -> tfPassword.requestFocus());
        forgot.setOnAction(e -> showResetDialog());

        p.getChildren().addAll(title, subtitle, sep, lUser, tfUsername, lPass, tfPassword,
                forgotBox, lblError, btnLogin);
        return p;
    }

    // ── Actions ──────────────────────────────────────────────────────────────

    private void doLogin() {
        String user = tfUsername.getText().trim();
        String pass = tfPassword.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            showErr("Veuillez remplir tous les champs.", false);
            return;
        }
        btnLogin.setDisable(true);
        btnLogin.setText("Connexion…");
        lblError.setVisible(false);

        ctrl.authenticate(user, pass,
            session -> {
                new MainWindow(stage, session).show();
            },
            msg -> {
                showErr(msg, false);
                btnLogin.setDisable(false);
                btnLogin.setText("Se connecter");
            },
            msg -> {
                showErr(msg, true);
                btnLogin.setDisable(false);
                btnLogin.setText("Se connecter");
            }
        );
    }

    private void showResetDialog() {
        Alert a = new Alert(Alert.AlertType.INFORMATION,
            "La réinitialisation par OTP sera disponible prochainement.", ButtonType.OK);
        a.setTitle("Mot de passe oublié");
        a.setHeaderText("Réinitialisation");
        a.showAndWait();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void showErr(String msg, boolean orange) {
        lblError.setText(msg);
        lblError.setStyle("-fx-font-size:12;-fx-text-fill:" +
                (orange ? "#d97706" : "#dc2626") + ";-fx-wrap-text:true;");
        lblError.setVisible(true);
    }

    private Label mkLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private void styleField(Control f) {
        f.setStyle("-fx-font-size:13;-fx-padding:10 14;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-background-color:#f9fafb;");
        f.setMaxWidth(Double.MAX_VALUE);
    }
}
