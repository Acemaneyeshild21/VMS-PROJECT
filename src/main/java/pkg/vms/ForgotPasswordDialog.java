package pkg.vms;

import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.PasswordResetDAO;
import pkg.vms.DAO.UserDAO;

/**
 * Wizard de réinitialisation de mot de passe en 3 étapes :
 *   Étape 1 — Saisir l'adresse e-mail du compte
 *   Étape 2 — Saisir le code OTP reçu par e-mail
 *   Étape 3 — Saisir et confirmer le nouveau mot de passe
 */
public class ForgotPasswordDialog {

    private final Stage stage = new Stage();

    // State partagé entre étapes
    private AuthDAO.UserSession foundUser;
    private int                 validatedResetId;

    public ForgotPasswordDialog(Stage owner) {
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Mot de passe oublié");
        stage.setResizable(false);
        stage.setWidth(480);
    }

    public void show() {
        showStep1();
        stage.centerOnScreen();
        stage.show();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Étape 1 — Email
    // ════════════════════════════════════════════════════════════════════════

    private void showStep1() {
        VBox root = buildContainer("Réinitialisation du mot de passe",
                "Saisissez l'adresse e-mail associée à votre compte.", "1 / 3");

        Label lbl = fieldLabel("Adresse e-mail *");
        TextField tfEmail = styledField(""); tfEmail.setPromptText("votre@email.mu");

        Label lblErr = errLabel("");

        Button btnSend = primaryBtn("Envoyer le code");
        Button btnCancel = cancelBtn();
        btnCancel.setOnAction(e -> stage.close());

        btnSend.setOnAction(e -> {
            String email = tfEmail.getText().trim();
            if (email.isBlank()) { lblErr.setText("L'adresse e-mail est requise."); return; }

            btnSend.setDisable(true);
            btnSend.setText("Recherche du compte…");
            lblErr.setText("");

            Task<AuthDAO.UserSession> task = new Task<>() {
                @Override protected AuthDAO.UserSession call() throws Exception {
                    return AuthDAO.findByEmail(email);
                }
            };
            task.setOnSucceeded(ev -> {
                AuthDAO.UserSession user = task.getValue();
                if (user == null) {
                    btnSend.setDisable(false);
                    btnSend.setText("Envoyer le code");
                    lblErr.setText("Aucun compte actif trouvé pour cet e-mail.");
                    return;
                }
                foundUser = user;
                sendOTP(email, user.username, user.userId, btnSend, lblErr);
            });
            task.setOnFailed(ev -> {
                btnSend.setDisable(false);
                btnSend.setText("Envoyer le code");
                lblErr.setText("Erreur : " + task.getException().getMessage());
            });
            new Thread(task).start();
        });

        root.getChildren().addAll(lbl, tfEmail, lblErr,
            spacer(8), new HBox(12, btnSend, btnCancel));
        setScene(root);
    }

    private void sendOTP(String email, String nom, int userId, Button btnSend, Label lblErr) {
        // Génération du code + envoi email — tout en arrière-plan (ne jamais bloquer le fil FX)
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                String code    = PasswordResetDAO.createResetCode(userId, "app");
                String sendErr = pkg.vms.EmailService.envoyerCodeReset(email, nom, code);
                if (sendErr != null) {
                    // L'email n'a pas pu être envoyé — on remonte l'erreur
                    throw new RuntimeException("Échec d'envoi de l'e-mail : " + sendErr);
                }
                return code; // code en clair uniquement pour passer à l'étape suivante (jamais loggué)
            }
        };
        task.setOnSucceeded(e -> showStep2());
        task.setOnFailed(e -> {
            btnSend.setDisable(false);
            btnSend.setText("Envoyer le code");
            Throwable cause = task.getException();
            lblErr.setText(cause != null ? cause.getMessage() : "Erreur lors de l'envoi du code.");
        });
        new Thread(task).start();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Étape 2 — Code OTP
    // ════════════════════════════════════════════════════════════════════════

    private void showStep2() {
        VBox root = buildContainer("Code de vérification",
                "Un code à 6 chiffres a été envoyé à votre adresse e-mail.\nIl expire dans 15 minutes.",
                "2 / 3");

        Label lbl = fieldLabel("Code OTP *");
        TextField tfCode = styledField("");
        tfCode.setPromptText("123456");
        tfCode.setStyle(tfCode.getStyle() + "-fx-font-size:20;-fx-font-weight:bold;"
                + "-fx-font-family:monospace;-fx-alignment:CENTER;");
        tfCode.setMaxWidth(180);

        Label lblErr = errLabel("");

        Button btnVerify = primaryBtn("Vérifier le code");
        Button btnBack   = cancelBtn(); btnBack.setText("← Retour");
        btnBack.setOnAction(e -> showStep1());

        btnVerify.setOnAction(e -> {
            String code = tfCode.getText().trim();
            if (code.isBlank()) { lblErr.setText("Saisissez le code OTP."); return; }

            btnVerify.setDisable(true);
            btnVerify.setText("Vérification…");
            lblErr.setText("");

            Task<PasswordResetDAO.ValidationResult> task = new Task<>() {
                @Override protected PasswordResetDAO.ValidationResult call() throws Exception {
                    return PasswordResetDAO.validateCode(foundUser.userId, code);
                }
            };
            task.setOnSucceeded(ev -> {
                PasswordResetDAO.ValidationResult res = task.getValue();
                if (res.success) {
                    validatedResetId = res.resetId;
                    showStep3();
                } else {
                    btnVerify.setDisable(false);
                    btnVerify.setText("Vérifier le code");
                    lblErr.setText(res.errorMessage);
                }
            });
            task.setOnFailed(ev -> {
                btnVerify.setDisable(false);
                btnVerify.setText("Vérifier le code");
                lblErr.setText("Erreur : " + task.getException().getMessage());
            });
            new Thread(task).start();
        });

        // Limiter à 6 caractères numériques
        tfCode.textProperty().addListener((o, ov, nv) -> {
            if (!nv.matches("\\d*")) tfCode.setText(nv.replaceAll("[^\\d]", ""));
            if (nv.length() > 6) tfCode.setText(nv.substring(0, 6));
        });

        root.getChildren().addAll(lbl, tfCode, lblErr,
            spacer(8), new HBox(12, btnVerify, btnBack));
        setScene(root);
    }

    // ════════════════════════════════════════════════════════════════════════
    // Étape 3 — Nouveau mot de passe
    // ════════════════════════════════════════════════════════════════════════

    private void showStep3() {
        VBox root = buildContainer("Nouveau mot de passe",
                "Créez un nouveau mot de passe sécurisé pour le compte\n"
                        + (foundUser != null ? foundUser.username : "") + ".",
                "3 / 3");

        Label l1 = fieldLabel("Nouveau mot de passe *");
        PasswordField pf1 = pwdField("Au moins 6 caractères");

        Label l2 = fieldLabel("Confirmer le mot de passe *");
        PasswordField pf2 = pwdField("Répétez le mot de passe");

        Label lblErr = errLabel("");

        Button btnSave   = primaryBtn("Réinitialiser le mot de passe");
        Button btnCancel = cancelBtn();
        btnCancel.setOnAction(e -> stage.close());

        btnSave.setOnAction(e -> {
            String p1 = pf1.getText(), p2 = pf2.getText();
            if (p1.length() < 6) { lblErr.setText("Le mot de passe doit contenir au moins 6 caractères."); return; }
            if (!p1.equals(p2)) { lblErr.setText("Les mots de passe ne correspondent pas."); return; }

            btnSave.setDisable(true);
            btnSave.setText("Enregistrement…");
            lblErr.setText("");

            Task<Boolean> task = new Task<>() {
                @Override protected Boolean call() throws Exception {
                    boolean ok = UserDAO.resetPasswordDirect(foundUser.userId, p1);
                    if (ok) PasswordResetDAO.markUsed(validatedResetId);
                    return ok;
                }
            };
            task.setOnSucceeded(ev -> {
                if (task.getValue()) {
                    showSuccess();
                } else {
                    btnSave.setDisable(false);
                    btnSave.setText("Réinitialiser le mot de passe");
                    lblErr.setText("Erreur lors de la réinitialisation. Veuillez réessayer.");
                }
            });
            task.setOnFailed(ev -> {
                btnSave.setDisable(false);
                btnSave.setText("Réinitialiser le mot de passe");
                lblErr.setText("Erreur : " + task.getException().getMessage());
            });
            new Thread(task).start();
        });

        root.getChildren().addAll(l1, pf1, l2, pf2, lblErr,
            spacer(8), new HBox(12, btnSave, btnCancel));
        setScene(root);
    }

    // ── Succès ────────────────────────────────────────────────────────────────

    private void showSuccess() {
        VBox root = buildContainer("Mot de passe réinitialisé !", "", "✓");

        Label icon = new Label("✓");
        icon.setStyle("-fx-font-size:52;-fx-text-fill:#16a34a;");
        icon.setAlignment(Pos.CENTER);

        Label msg = new Label("Votre mot de passe a été modifié avec succès.\nVous pouvez maintenant vous connecter.");
        msg.setStyle("-fx-font-size:13;-fx-text-fill:#374151;-fx-text-alignment:center;");
        msg.setWrapText(true);
        msg.setAlignment(Pos.CENTER);

        Button btnOk = primaryBtn("Se connecter");
        btnOk.setOnAction(e -> stage.close());

        root.setAlignment(Pos.CENTER);
        root.getChildren().addAll(icon, msg, spacer(8), btnOk);
        setScene(root);
    }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private VBox buildContainer(String title, String subtitle, String step) {
        VBox root = new VBox(16);
        root.setPadding(new Insets(40, 40, 32, 40));
        root.setStyle("-fx-background-color:white;");

        // Step indicator
        Label stepLbl = new Label("Étape " + step);
        stepLbl.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;-fx-font-weight:bold;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        if (!subtitle.isBlank()) {
            Label sub = new Label(subtitle);
            sub.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
            sub.setWrapText(true);
            root.getChildren().addAll(stepLbl, titleLbl, sub, new Separator());
        } else {
            root.getChildren().addAll(stepLbl, titleLbl, new Separator());
        }

        return root;
    }

    private void setScene(VBox root) {
        stage.setScene(new Scene(root));
    }

    private Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private TextField styledField(String val) {
        TextField tf = new TextField(val);
        tf.setStyle("-fx-font-size:13;-fx-padding:10 14;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;");
        return tf;
    }

    private PasswordField pwdField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle("-fx-font-size:13;-fx-padding:10 14;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;");
        return pf;
    }

    private Label errLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:12;-fx-text-fill:#dc2626;");
        l.setWrapText(true);
        return l;
    }

    private Button primaryBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:13;"
                + "-fx-font-weight:bold;-fx-padding:10 22;-fx-background-radius:8;-fx-cursor:hand;");
        return b;
    }

    private Button cancelBtn() {
        Button b = new Button("Annuler");
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:#64748b;-fx-font-size:13;"
                + "-fx-padding:10 16;-fx-cursor:hand;");
        return b;
    }

    private Region spacer(double h) {
        Region r = new Region(); r.setMinHeight(h); return r;
    }
}
