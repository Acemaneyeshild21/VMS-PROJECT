package pkg.vms;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.*;

public class FormulaireUtilisateur extends Application {

    // Composants du formulaire
    private TextField txtUsername;
    private PasswordField txtPassword;
    private TextField txtEmail;
    private ComboBox<String> comboRole;
    private TextArea txtResultat;

    // Informations de connexion à la base de données
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/VMS_voucher";
    private static final String DB_USER = "postgres";  // ✅ CORRIGÉ
    private static final String DB_PASSWORD = "0003";

    public FormulaireUtilisateur() {
        // Constructeur vide requis par JavaFX
    }

    @Override
    public void start(Stage primaryStage) {
        // Configuration de la fenêtre principale
        primaryStage.setTitle("Formulaire d'Insertion - Utilisateur");

        // Layout principal
        VBox root = new VBox(20);
        root.setPadding(new Insets(25));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("-fx-background-color: #f5f5f5;");

        // Titre
        Label lblTitre = new Label("Ajouter un Utilisateur");
        lblTitre.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        lblTitre.setTextFill(Color.web("#2c3e50"));

        // Panel du formulaire
        GridPane formGrid = new GridPane();
        formGrid.setHgap(15);
        formGrid.setVgap(15);
        formGrid.setPadding(new Insets(20));
        formGrid.setAlignment(Pos.CENTER);
        formGrid.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");

        // Username
        Label lblUsername = new Label("Nom d'utilisateur:");
        lblUsername.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        txtUsername = new TextField();
        txtUsername.setPromptText("Entrez le nom d'utilisateur");
        txtUsername.setPrefWidth(300);
        txtUsername.setStyle("-fx-padding: 8;");

        // Password
        Label lblPassword = new Label("Mot de passe:");
        lblPassword.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        txtPassword = new PasswordField();
        txtPassword.setPromptText("Entrez le mot de passe");
        txtPassword.setPrefWidth(300);
        txtPassword.setStyle("-fx-padding: 8;");

        // Email
        Label lblEmail = new Label("Email:");
        lblEmail.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        txtEmail = new TextField();
        txtEmail.setPromptText("exemple@email.com");
        txtEmail.setPrefWidth(300);
        txtEmail.setStyle("-fx-padding: 8;");

        // Role
        Label lblRole = new Label("Rôle:");
        lblRole.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        comboRole = new ComboBox<>();
        comboRole.getItems().addAll("Administrateur", "Utilisateur", "Manager", "Client");
        comboRole.setValue("Utilisateur");
        comboRole.setPrefWidth(300);
        comboRole.setStyle("-fx-padding: 8;");

        // Ajout des composants au GridPane
        formGrid.add(lblUsername, 0, 0);
        formGrid.add(txtUsername, 1, 0);
        formGrid.add(lblPassword, 0, 1);
        formGrid.add(txtPassword, 1, 1);
        formGrid.add(lblEmail, 0, 2);
        formGrid.add(txtEmail, 1, 2);
        formGrid.add(lblRole, 0, 3);
        formGrid.add(comboRole, 1, 3);

        // Panel des boutons
        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnInserer = new Button("Insérer");
        btnInserer.setPrefWidth(120);
        btnInserer.setPrefHeight(40);
        btnInserer.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnInserer.setOnMouseEntered(e -> btnInserer.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;"));
        btnInserer.setOnMouseExited(e -> btnInserer.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5;"));

        Button btnEffacer = new Button("Effacer");
        btnEffacer.setPrefWidth(120);
        btnEffacer.setPrefHeight(40);
        btnEffacer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5;");
        btnEffacer.setOnMouseEntered(e -> btnEffacer.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5; -fx-cursor: hand;"));
        btnEffacer.setOnMouseExited(e -> btnEffacer.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-background-radius: 5;"));

        btnBox.getChildren().addAll(btnInserer, btnEffacer);

        // Zone de résultat
        Label lblResultat = new Label("Résultat:");
        lblResultat.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        txtResultat = new TextArea();
        txtResultat.setEditable(false);
        txtResultat.setPrefHeight(120);
        txtResultat.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; -fx-control-inner-background: #ecf0f1;");

        // Actions des boutons
        btnInserer.setOnAction(e -> insererUtilisateur());
        btnEffacer.setOnAction(e -> effacerChamps());

        // Ajout de tous les composants au layout principal
        root.getChildren().addAll(lblTitre, formGrid, btnBox, lblResultat, txtResultat);

        // Création de la scène
        Scene scene = new Scene(root, 650, 650);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void insererUtilisateur() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText();
        String email = txtEmail.getText().trim();
        String role = comboRole.getValue();

        // Validation des champs
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Tous les champs sont obligatoires!");
            return;
        }

        // Validation email
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", "Format d'email invalide!");
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            // Connexion à la base de données
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // ✅ Requête d'insertion SANS userid (auto-incrémenté)
            String sql = "INSERT INTO utilisateur (username, password, role, email) VALUES (?, ?, ?, ?)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.setString(4, email);

            // Exécution de la requête
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                txtResultat.setText("✓ Utilisateur inséré avec succès!\n\n" +
                        "Username: " + username + "\n" +
                        "Email: " + email + "\n" +
                        "Rôle: " + role);

                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur ajouté avec succès!");
                effacerChamps();
            }

        } catch (SQLException ex) {
            String message = "Erreur lors de l'insertion:\n" + ex.getMessage();
            txtResultat.setText(message);

            if (ex.getMessage().contains("duplicate key")) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Cet email ou username existe déjà!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur SQL", message);
            }

            ex.printStackTrace();
        } finally {
            // Fermeture des ressources
            try {
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void effacerChamps() {
        txtUsername.clear();
        txtPassword.clear();
        txtEmail.clear();
        comboRole.setValue("Utilisateur");
        txtResultat.clear();
        txtUsername.requestFocus();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}