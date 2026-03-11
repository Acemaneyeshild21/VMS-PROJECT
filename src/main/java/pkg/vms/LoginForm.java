package pkg.vms;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.*;

public class LoginForm extends Application {

    // =====================================================
    //  CHAMPS
    // =====================================================
    private TextField     txtUsername;
    private PasswordField txtPassword;
    private TextField     txtPasswordVisible;
    private Label         lblMessage;
    private boolean       passwordVisible = false;

    // =====================================================
    //  DB
    // =====================================================
    private static final String DB_URL      = "jdbc:postgresql://localhost:5432/VMS_voucher";
    private static final String DB_USER     = "postgres";
    private static final String DB_PASSWORD = "0003";

    // =====================================================
    //  DRAG
    // =====================================================
    private double xOffset = 0;
    private double yOffset = 0;

    public LoginForm() { super(); }

    // =====================================================
    //  STYLES CONSTANTS
    // =====================================================
    // Couleurs Intermart
    private static final String RED     = "#D2232D";
    private static final String RED_DK  = "#AA1420";
    private static final String BG_PAGE = "#F5F6FA";
    private static final String WHITE   = "#FFFFFF";
    private static final String BORDER  = "#E4E6EC";
    private static final String TEXT_PRI = "#161C2D";
    private static final String TEXT_MUT = "#A0A8B9";

    private static final String INPUT_NORMAL =
            "-fx-background-color: #F8F9FD;" +
                    "-fx-text-fill: #161C2D;" +
                    "-fx-prompt-text-fill: #A0A8B9;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-family: 'Trebuchet MS';" +
                    "-fx-padding: 10 14 10 14;" +
                    "-fx-border-color: #E4E6EC;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-width: 1.5;";

    private static final String INPUT_FOCUS =
            "-fx-background-color: #FFFFFF;" +
                    "-fx-text-fill: #161C2D;" +
                    "-fx-prompt-text-fill: #A0A8B9;" +
                    "-fx-font-size: 13px;" +
                    "-fx-font-family: 'Trebuchet MS';" +
                    "-fx-padding: 10 14 10 14;" +
                    "-fx-border-color: #D2232D;" +
                    "-fx-border-radius: 8;" +
                    "-fx-background-radius: 8;" +
                    "-fx-border-width: 1.5;";

    // =====================================================
    //  START
    // =====================================================
    @Override
    public void start(Stage primaryStage) {
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("Connexion — Intermart VMS");

        // ── Root : fond gris clair ─────────────────────────────
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: " + BG_PAGE + ";");

        // ── Layout principal ──────────────────────────────────
        HBox mainLayout = new HBox();
        mainLayout.setPrefSize(900, 600);
        mainLayout.setMaxSize(900, 600);
        mainLayout.setMinSize(900, 600);

        // ── Panneau gauche : branding rouge ───────────────────
        VBox leftPanel = buildLeftPanel();
        leftPanel.setPrefWidth(380);
        leftPanel.setMinWidth(380);
        leftPanel.setMaxWidth(380);

        // ── Panneau droit : formulaire blanc ──────────────────
        VBox rightPanel = buildRightPanel(primaryStage);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);

        mainLayout.getChildren().addAll(leftPanel, rightPanel);

        // Ombre portée sur la carte principale
        DropShadow shadow = new DropShadow();
        shadow.setRadius(40);
        shadow.setOffsetY(8);
        shadow.setColor(Color.rgb(0, 0, 0, 0.12));
        mainLayout.setEffect(shadow);

        // Clip arrondi
        Rectangle clip = new Rectangle(900, 600);
        clip.setArcWidth(20);
        clip.setArcHeight(20);
        mainLayout.setClip(clip);

        // Drag to move
        root.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        root.setOnMouseDragged(e -> {
            primaryStage.setX(e.getScreenX() - xOffset);
            primaryStage.setY(e.getScreenY() - yOffset);
        });

        root.getChildren().add(mainLayout);

        Scene scene = new Scene(root, 900, 600);
        scene.setFill(Color.TRANSPARENT);
        primaryStage.setScene(scene);
        primaryStage.show();
        txtUsername.requestFocus();
    }

    // =====================================================
    //  PANNEAU GAUCHE — branding Intermart
    // =====================================================
    private VBox buildLeftPanel() {
        VBox panel = new VBox();
        panel.setAlignment(Pos.TOP_LEFT);
        panel.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, #D2232D, #AA1420);"
        );

        // Stripes décoratives
        StackPane brandArea = new StackPane();
        brandArea.setPrefHeight(600);
        VBox.setVgrow(brandArea, Priority.ALWAYS);

        // Pattern diagonal (simulé avec des rectangles semi-transparents)
        VBox stripes = new VBox();
        stripes.setStyle("-fx-background-color: transparent;");
        for (int i = 0; i < 20; i++) {
            Region stripe = new Region();
            stripe.setPrefHeight(3);
            stripe.setPrefWidth(600);
            stripe.setStyle("-fx-background-color: rgba(255,255,255,0.04);");
            stripe.setTranslateY(i * 32);
            stripe.setRotate(20);
            stripes.getChildren().add(stripe);
        }

        // Contenu branding centré
        VBox brand = new VBox(20);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(60, 40, 60, 48));

        // Logo / icône
        Label logoIcon = new Label("🛒");
        logoIcon.setFont(Font.font("Segoe UI Emoji", 52));

        // Nom
        Label lblName = new Label("INTERMART");
        lblName.setFont(Font.font("Georgia", FontWeight.BOLD, 36));
        lblName.setTextFill(Color.WHITE);

        // Tagline
        Label lblTagline = new Label("Voucher Management System");
        lblTagline.setFont(Font.font("Trebuchet MS", FontPosture.ITALIC, 14));
        lblTagline.setTextFill(Color.rgb(255, 255, 255, 0.75));

        // Séparateur
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setPrefWidth(60);
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.4);");
        VBox.setMargin(sep, new Insets(8, 0, 8, 0));

        // Points forts
        VBox features = new VBox(10);
        features.setAlignment(Pos.CENTER_LEFT);
        for (String f : new String[]{
                "✓  Gestion des bons cadeau",
                "✓  Suivi des demandes en temps réel",
                "✓  Multi-magasins & multi-rôles",
                "✓  Rapports Excel automatisés"
        }) {
            Label lbl = new Label(f);
            lbl.setFont(Font.font("Trebuchet MS", 12));
            lbl.setTextFill(Color.rgb(255, 255, 255, 0.80));
            features.getChildren().add(lbl);
        }

        // Version
        Label lblVersion = new Label("v1.0.0  •  Maurice");
        lblVersion.setFont(Font.font("Trebuchet MS", 11));
        lblVersion.setTextFill(Color.rgb(255, 255, 255, 0.45));
        VBox.setMargin(lblVersion, new Insets(28, 0, 0, 0));

        brand.getChildren().addAll(logoIcon, lblName, lblTagline, sep, features, lblVersion);

        brandArea.getChildren().addAll(brand);
        panel.getChildren().add(brandArea);
        return panel;
    }

    // =====================================================
    //  PANNEAU DROIT — formulaire
    // =====================================================
    private VBox buildRightPanel(Stage primaryStage) {
        VBox panel = new VBox();
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: " + WHITE + ";");
        panel.setPadding(new Insets(0, 56, 0, 56));

        // Bouton fermer en haut à droite
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.TOP_RIGHT);
        topBar.setPadding(new Insets(18, 0, 0, 0));
        Button btnClose = buildCloseButton(primaryStage);
        topBar.getChildren().add(btnClose);

        // Spacer haut
        Region topSpacer = new Region();
        VBox.setVgrow(topSpacer, Priority.ALWAYS);

        // ── Titre ─────────────────────────────────────────────
        VBox titleBlock = new VBox(6);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        Label lblWelcome = new Label("Bienvenue");
        lblWelcome.setFont(Font.font("Georgia", FontWeight.BOLD, 30));
        lblWelcome.setTextFill(Color.web(TEXT_PRI));

        Label lblSub = new Label("Connectez-vous à votre espace VMS");
        lblSub.setFont(Font.font("Trebuchet MS", 13));
        lblSub.setTextFill(Color.web(TEXT_MUT));

        // Barre rouge décorative sous le titre
        Region redBar = new Region();
        redBar.setPrefSize(40, 3);
        redBar.setMaxWidth(40);
        redBar.setStyle("-fx-background-color: " + RED + "; -fx-background-radius: 2;");
        VBox.setMargin(redBar, new Insets(8, 0, 0, 0));

        titleBlock.getChildren().addAll(lblWelcome, lblSub, redBar);

        // ── Champs ────────────────────────────────────────────
        VBox formBlock = new VBox(16);
        formBlock.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(formBlock, new Insets(28, 0, 0, 0));

        // Username
        VBox usernameField = new VBox(6);
        Label lblUser = buildFieldLabel("Nom d'utilisateur");
        txtUsername = new TextField();
        txtUsername.setPromptText("ex: jdupont");
        txtUsername.setPrefHeight(42);
        txtUsername.setMaxWidth(Double.MAX_VALUE);
        txtUsername.setStyle(INPUT_NORMAL);
        txtUsername.focusedProperty().addListener((obs, old, focused) ->
                txtUsername.setStyle(focused ? INPUT_FOCUS : INPUT_NORMAL));
        usernameField.getChildren().addAll(lblUser, txtUsername);

        // Password
        VBox passwordField = new VBox(6);
        Label lblPwd = buildFieldLabel("Mot de passe");

        StackPane pwdWrapper = new StackPane();
        pwdWrapper.setMaxWidth(Double.MAX_VALUE);

        txtPassword = new PasswordField();
        txtPassword.setPromptText("••••••••");
        txtPassword.setPrefHeight(42);
        txtPassword.setMaxWidth(Double.MAX_VALUE);
        txtPassword.setStyle(INPUT_NORMAL);
        txtPassword.focusedProperty().addListener((obs, old, f) ->
                txtPassword.setStyle(f ? INPUT_FOCUS : INPUT_NORMAL));
        txtPassword.setOnAction(e -> authentifier(primaryStage));

        txtPasswordVisible = new TextField();
        txtPasswordVisible.setPromptText("••••••••");
        txtPasswordVisible.setPrefHeight(42);
        txtPasswordVisible.setMaxWidth(Double.MAX_VALUE);
        txtPasswordVisible.setVisible(false);
        txtPasswordVisible.setStyle(INPUT_NORMAL);
        txtPasswordVisible.focusedProperty().addListener((obs, old, f) ->
                txtPasswordVisible.setStyle(f ? INPUT_FOCUS : INPUT_NORMAL));
        txtPasswordVisible.setOnAction(e -> authentifier(primaryStage));

        Button btnToggle = new Button("👁");
        btnToggle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUT + ";" +
                        "-fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 4 10 4 10;");
        btnToggle.setOnMouseEntered(e -> btnToggle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + RED + ";" +
                        "-fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 4 10 4 10;"));
        btnToggle.setOnMouseExited(e -> btnToggle.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUT + ";" +
                        "-fx-cursor: hand; -fx-font-size: 15px; -fx-padding: 4 10 4 10;"));
        btnToggle.setOnAction(e -> togglePasswordVisibility(btnToggle));
        StackPane.setAlignment(btnToggle, Pos.CENTER_RIGHT);

        pwdWrapper.getChildren().addAll(txtPassword, txtPasswordVisible, btnToggle);
        passwordField.getChildren().addAll(lblPwd, pwdWrapper);

        formBlock.getChildren().addAll(usernameField, passwordField);

        // ── Message statut ────────────────────────────────────
        lblMessage = new Label("");
        lblMessage.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 12));
        lblMessage.setWrapText(true);
        lblMessage.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(lblMessage, new Insets(4, 0, 0, 0));

        // ── Bouton connexion ──────────────────────────────────
        Button btnLogin = new Button("Se connecter");
        btnLogin.setPrefHeight(44);
        btnLogin.setMaxWidth(Double.MAX_VALUE);
        btnLogin.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 14));
        btnLogin.setStyle(
                "-fx-background-color: " + RED + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;");
        btnLogin.setOnMouseEntered(e -> btnLogin.setStyle(
                "-fx-background-color: " + RED_DK + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"));
        btnLogin.setOnMouseExited(e -> btnLogin.setStyle(
                "-fx-background-color: " + RED + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"));
        btnLogin.setOnAction(e -> authentifier(primaryStage));
        VBox.setMargin(btnLogin, new Insets(10, 0, 0, 0));

        // ── Lien inscription ──────────────────────────────────
        HBox signupRow = new HBox(6);
        signupRow.setAlignment(Pos.CENTER);
        VBox.setMargin(signupRow, new Insets(12, 0, 0, 0));
        Label lblSignup = new Label("Pas encore de compte ?");
        lblSignup.setFont(Font.font("Trebuchet MS", 12));
        lblSignup.setTextFill(Color.web(TEXT_MUT));
        Hyperlink linkSignup = new Hyperlink("S'inscrire");
        linkSignup.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 12));
        linkSignup.setTextFill(Color.web(RED));
        linkSignup.setUnderline(false);
        linkSignup.setStyle("-fx-border-color: transparent;");
        linkSignup.setOnMouseEntered(e -> linkSignup.setTextFill(Color.web(RED_DK)));
        linkSignup.setOnMouseExited(e -> linkSignup.setTextFill(Color.web(RED)));
        linkSignup.setOnAction(e -> ouvrirFormulaireInscription());
        signupRow.getChildren().addAll(lblSignup, linkSignup);

        // Spacer bas
        Region botSpacer = new Region();
        VBox.setVgrow(botSpacer, Priority.ALWAYS);

        panel.getChildren().addAll(
                topBar, topSpacer,
                titleBlock, formBlock, lblMessage, btnLogin, signupRow,
                botSpacer
        );

        return panel;
    }

    // =====================================================
    //  HELPERS UI
    // =====================================================
    private Label buildFieldLabel(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 12));
        lbl.setTextFill(Color.web("#37415A"));
        return lbl;
    }

    private Button buildCloseButton(Stage stage) {
        Button btn = new Button("✕");
        btn.setFont(Font.font("Trebuchet MS", FontWeight.BOLD, 13));
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUT + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 30px; -fx-min-height: 30px;" +
                        "-fx-max-width: 30px; -fx-max-height: 30px;");
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #FEE2E2;" +
                        "-fx-text-fill: " + RED + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 30px; -fx-min-height: 30px;" +
                        "-fx-max-width: 30px; -fx-max-height: 30px;"));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + TEXT_MUT + ";" +
                        "-fx-cursor: hand;" +
                        "-fx-background-radius: 50%;" +
                        "-fx-min-width: 30px; -fx-min-height: 30px;" +
                        "-fx-max-width: 30px; -fx-max-height: 30px;"));
        btn.setOnAction(e -> stage.close());
        return btn;
    }

    // =====================================================
    //  TOGGLE PASSWORD
    // =====================================================
    private void togglePasswordVisibility(Button btnToggle) {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            txtPasswordVisible.setText(txtPassword.getText());
            txtPassword.setVisible(false);
            txtPasswordVisible.setVisible(true);
            btnToggle.setText("🙈");
            txtPasswordVisible.requestFocus();
            txtPasswordVisible.positionCaret(txtPasswordVisible.getText().length());
        } else {
            txtPassword.setText(txtPasswordVisible.getText());
            txtPasswordVisible.setVisible(false);
            txtPassword.setVisible(true);
            btnToggle.setText("👁");
            txtPassword.requestFocus();
            txtPassword.positionCaret(txtPassword.getText().length());
        }
    }

    // =====================================================
    //  AUTHENTIFICATION
    // =====================================================
    private void authentifier(Stage primaryStage) {
        String username = txtUsername.getText().trim();
        String password = passwordVisible ? txtPasswordVisible.getText() : txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            afficherMessage("⚠  Veuillez remplir tous les champs.", "error");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT userid, username, role, email FROM utilisateur WHERE username = ? AND password = ?")) {

            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int    userId = rs.getInt("userid");
                String role   = rs.getString("role");
                String email  = rs.getString("email");

                afficherMessage("✓  Connexion réussie — bienvenue " + username, "success");

                new Thread(() -> {
                    try {
                        Thread.sleep(400);
                        javafx.application.Platform.runLater(() ->
                                ouvrirDashboard(userId, username, role, email, primaryStage));
                    } catch (InterruptedException ignored) {}
                }).start();

            } else {
                afficherMessage("✗  Identifiants incorrects.", "error");
                if (passwordVisible) { txtPasswordVisible.clear(); txtPasswordVisible.requestFocus(); }
                else                 { txtPassword.clear();        txtPassword.requestFocus(); }
            }

        } catch (SQLException ex) {
            afficherMessage("⚠  Erreur de connexion à la base de données.", "error");
            showAlert(Alert.AlertType.ERROR, "Erreur SQL",
                    "Impossible de se connecter :\n" + ex.getMessage());
        }
    }

    // =====================================================
    //  AFFICHER MESSAGE
    // =====================================================
    private void afficherMessage(String message, String type) {
        lblMessage.setText(message);
        switch (type) {
            case "error"   -> lblMessage.setTextFill(Color.web(RED));
            case "success" -> lblMessage.setTextFill(Color.web("#16A34A"));
            default        -> lblMessage.setTextFill(Color.web(TEXT_MUT));
        }
    }

    // =====================================================
    //  OUVRIR DASHBOARD
    // =====================================================
    private void ouvrirDashboard(int userId, String username, String role, String email, Stage loginStage) {
        try {
            Dashboard dashboard = new Dashboard(userId, username, role, email);
            Stage dashboardStage = new Stage();
            dashboard.start(dashboardStage);
            loginStage.close();
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le Dashboard :\n" + ex.getMessage());
        }
    }

    // =====================================================
    //  OUVRIR INSCRIPTION
    // =====================================================
    private void ouvrirFormulaireInscription() {
        try {
            FormulaireUtilisateur formulaire = new FormulaireUtilisateur();
            Stage stage = new Stage();
            formulaire.start(stage);
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Erreur",
                    "Impossible d'ouvrir le formulaire :\n" + ex.getMessage());
        }
    }

    // =====================================================
    //  ALERT
    // =====================================================
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}