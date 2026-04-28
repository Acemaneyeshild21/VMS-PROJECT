package pkg.vms.magasin;

import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pkg.vms.controller.LoginController;

/**
 * Écran de connexion pour l'application magasin.
 * Design full-dark POS : simple, lisible, optimisé pour un écran de caisse.
 */
public class MagasinLoginView {

    private final Stage           stage;
    private final LoginController ctrl = new LoginController();

    private TextField     tfUsername;
    private PasswordField tfPassword;
    private Label         lblError;
    private Button        btnLogin;

    public MagasinLoginView(Stage stage) { this.stage = stage; }

    public void show() {
        StackPane root = new StackPane(buildCard());
        root.setStyle("-fx-background-color:linear-gradient(to bottom right,#0f172a,#1e293b);");
        root.setPadding(new Insets(40));

        Scene scene = new Scene(root, 480, 580);
        stage.setTitle("VMS — Application Magasin");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.centerOnScreen();
        stage.show();

        tfUsername.requestFocus();
    }

    private VBox buildCard() {
        VBox card = new VBox(20);
        card.setMaxWidth(380);
        card.setMaxHeight(Double.MAX_VALUE);
        card.setPadding(new Insets(40));
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color:white;-fx-background-radius:16;"
                + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),24,0,0,8);");

        // Logo / branding
        Label logo = new Label("VMS");
        logo.setStyle("-fx-font-size:32;-fx-font-weight:bold;-fx-text-fill:#dc2626;");

        Label tagline = new Label("Application Magasin");
        tagline.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;-fx-font-weight:bold;");

        Label subtitle = new Label("Validation des bons cadeaux");
        subtitle.setStyle("-fx-font-size:12;-fx-text-fill:#94a3b8;");

        Separator sep = new Separator();
        sep.setPadding(new Insets(4, 0, 4, 0));

        // Form
        Label lUser = fieldLabel("Identifiant");
        tfUsername = new TextField();
        tfUsername.setPromptText("Votre nom d'utilisateur");
        styleInput(tfUsername);
        tfUsername.setOnAction(e -> tfPassword.requestFocus());

        Label lPass = fieldLabel("Mot de passe");
        tfPassword = new PasswordField();
        tfPassword.setPromptText("••••••••");
        styleInput(tfPassword);
        tfPassword.setOnAction(e -> doLogin());

        lblError = new Label();
        lblError.setStyle("-fx-font-size:12;-fx-text-fill:#dc2626;");
        lblError.setVisible(false);
        lblError.setWrapText(true);

        btnLogin = new Button("Se connecter");
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:14;"
                + "-fx-font-weight:bold;-fx-padding:13 24;-fx-background-radius:10;"
                + "-fx-cursor:hand;");
        btnLogin.setOnAction(e -> doLogin());

        Label version = new Label("VoucherManager VMS — v1.0 Magasin");
        version.setStyle("-fx-font-size:10;-fx-text-fill:#cbd5e1;");

        card.getChildren().addAll(
            logo, tagline, subtitle, sep,
            lUser, tfUsername, lPass, tfPassword,
            lblError, btnLogin, version
        );
        return card;
    }

    private void doLogin() {
        String user = tfUsername.getText().trim();
        String pass = tfPassword.getText();
        if (user.isEmpty() || pass.isEmpty()) {
            showErr("Veuillez remplir tous les champs."); return;
        }
        btnLogin.setDisable(true);
        btnLogin.setText("Connexion…");
        lblError.setVisible(false);

        ctrl.authenticate(user, pass,
            session -> new MagasinMainView(stage, session).show(),
            msg -> { showErr(msg); resetBtn(); },
            msg -> { showErr(msg); resetBtn(); }
        );
    }

    private void resetBtn() {
        btnLogin.setDisable(false);
        btnLogin.setText("Se connecter");
    }

    private void showErr(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    private Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private void styleInput(Control c) {
        c.setStyle("-fx-font-size:13;-fx-padding:10 14;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;-fx-background-color:#f9fafb;");
        c.setMaxWidth(Double.MAX_VALUE);
    }
}
