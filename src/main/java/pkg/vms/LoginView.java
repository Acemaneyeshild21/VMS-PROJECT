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
        VBox formPane = buildForm();
        HBox root = new HBox();
        root.getChildren().addAll(buildBrand(), formPane);
        HBox.setHgrow(formPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 980, 620);

        // ── CSS global ────────────────────────────────────────────────────
        try {
            String css = getClass().getResource("/vms.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

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
        btnLogin.getStyleClass().addAll("btn-primary");
        btnLogin.setStyle("-fx-font-size:14;-fx-padding:13 24;");

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
        new ForgotPasswordDialog(stage).show();
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
        // Le CSS global (vms.css) gère le style de base ; on ajoute juste la taille max
        f.setMaxWidth(Double.MAX_VALUE);
    }
}
