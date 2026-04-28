package pkg.vms;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.view.*;

/**
 * Fenêtre principale VMS — sidebar + topbar + zone de contenu.
 * Structure inspirée Invoice Ninja : sidebar gauche fixe, topbar en-tête, contenu à droite.
 */
public class MainWindow {

    // ── Palette sidebar ──────────────────────────────────────────────────────
    private static final String C_SIDEBAR  = "#0f172a";
    private static final String C_TEXT_S   = "#94a3b8";
    private static final String C_TEXT_W   = "white";
    private static final String C_SURFACE  = "#f1f5f9";
    private static final String C_ACTIVE   = "#dc2626";

    // ── Navigation items : [icon, label, rôle requis (vide = tous)] ─────────
    private static final String[][] NAV = {
        {"⬛", "Dashboard",     ""},
        {"📋", "Demandes",      ""},
        {"✅", "Validation",    ""},
        {"🎟", "Bons",          ""},
        {"👥", "Clients",       "Administrateur"},
        {"📊", "Statistiques",  "Administrateur"},
        {"🛒", "Rédemption",    ""},
        {"📁", "Archives",      ""},
        {"⚙",  "Paramètres",   "Administrateur"},
    };

    private final Stage              stage;
    private final AuthDAO.UserSession session;
    private final StackPane          contentArea = new StackPane();

    /** Titre affiché dans la topbar, mis à jour à chaque navigation. */
    private final Label topbarTitle = new Label("Dashboard");

    private Button activeBtn;

    public MainWindow(Stage stage, AuthDAO.UserSession session) {
        this.stage   = stage;
        this.session = session;
    }

    // ── Entrée ───────────────────────────────────────────────────────────────

    public void show() {
        // Zone droite : topbar + contenu
        BorderPane rightPane = new BorderPane();
        rightPane.setTop(buildTopbar());
        rightPane.setCenter(contentArea);

        contentArea.setStyle("-fx-background-color:" + C_SURFACE + ";");

        // Racine : sidebar gauche + zone droite
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(rightPane);

        Scene scene = new Scene(root, 1340, 840);

        // ── Chargement du CSS global ──────────────────────────────────────
        try {
            String css = getClass().getResource("/vms.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}

        stage.setTitle("VoucherManager VMS — " + session.username);
        stage.setScene(scene);
        stage.setMinWidth(1050);
        stage.setMinHeight(680);
        stage.show();

        navigate("Dashboard");
    }

    // ── Topbar ───────────────────────────────────────────────────────────────

    private HBox buildTopbar() {
        HBox bar = new HBox();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");

        topbarTitle.setStyle("-fx-font-size:18;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Avatar initiales
        String initial = session.username.substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:13;-fx-background-radius:20;-fx-alignment:center;"
            + "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;");

        // Infos utilisateur
        VBox userInfo = new VBox(1);
        Label lUser = new Label(session.username);
        lUser.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label lRole = new Label(session.role);
        lRole.setStyle("-fx-font-size:11;-fx-text-fill:#64748b;");
        userInfo.getChildren().addAll(lUser, lRole);
        userInfo.setAlignment(Pos.CENTER_LEFT);

        HBox userBox = new HBox(10, avatar, userInfo);
        userBox.setAlignment(Pos.CENTER);

        bar.getChildren().addAll(topbarTitle, spacer, userBox);
        return bar;
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(228);
        sidebar.setMinWidth(228);
        sidebar.setMaxWidth(228);
        sidebar.setStyle("-fx-background-color:" + C_SIDEBAR + ";");

        // Logo VMS
        sidebar.getChildren().add(buildLogoBox());

        // Section NAVIGATION
        Label lNav = new Label("NAVIGATION");
        lNav.setStyle("-fx-font-size:10;-fx-text-fill:#475569;-fx-font-weight:bold;"
                    + "-fx-padding:18 24 6 24;-fx-letter-spacing:0.08em;");
        sidebar.getChildren().add(lNav);

        // Boutons nav
        for (String[] item : NAV) {
            String icon  = item[0];
            String label = item[1];
            String role  = item[2];

            boolean adminOnly = !role.isEmpty();
            boolean isAdmin   = "Administrateur".equals(session.role);

            if (adminOnly && !isAdmin) continue;

            Button btn = navButton(icon, label);
            btn.setOnAction(e -> navigate(label));
            sidebar.getChildren().add(btn);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Footer utilisateur
        sidebar.getChildren().add(buildUserFooter());
        return sidebar;
    }

    private VBox buildLogoBox() {
        VBox box = new VBox(3);
        box.setPadding(new Insets(24, 24, 20, 24));
        box.setStyle("-fx-border-color:#1e293b;-fx-border-width:0 0 1 0;");

        // Marque rouge
        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER_LEFT);

        Label dot = new Label("●");
        dot.setStyle("-fx-font-size:10;-fx-text-fill:#dc2626;");

        Label lLogo = new Label("VMS");
        lLogo.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");

        brand.getChildren().addAll(dot, lLogo);

        Label lSub = new Label("VoucherManager");
        lSub.setStyle("-fx-font-size:10;-fx-text-fill:" + C_TEXT_S + ";-fx-padding:0 0 0 18;");

        box.getChildren().addAll(brand, lSub);
        return box;
    }

    private VBox buildUserFooter() {
        VBox footer = new VBox(6);
        footer.setPadding(new Insets(16, 24, 24, 24));
        footer.setStyle("-fx-border-color:#1e293b;-fx-border-width:1 0 0 0;");

        // Mini-carte utilisateur
        String initial = session.username.substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:12;-fx-background-radius:16;-fx-alignment:center;"
            + "-fx-min-width:30;-fx-min-height:30;-fx-max-width:30;-fx-max-height:30;");

        Label lUser = new Label(session.username);
        lUser.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");

        Label lRole = new Label(session.role);
        lRole.setStyle("-fx-font-size:10;-fx-text-fill:" + C_TEXT_S + ";");

        HBox userRow = new HBox(10, avatar, new VBox(1, lUser, lRole));
        userRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#1e293b;");

        Button btnLogout = new Button("↩  Déconnexion");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setAlignment(Pos.CENTER_LEFT);
        btnLogout.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#ef4444;-fx-font-size:12;"
            + "-fx-padding:6 0;-fx-cursor:hand;-fx-border-width:0;");
        btnLogout.setOnAction(e -> logout());
        btnLogout.setOnMouseEntered(e ->
            btnLogout.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#fca5a5;"
                + "-fx-font-size:12;-fx-padding:6 8;-fx-cursor:hand;-fx-border-width:0;"
                + "-fx-background-radius:6;"));
        btnLogout.setOnMouseExited(e ->
            btnLogout.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;"
                + "-fx-font-size:12;-fx-padding:6 0;-fx-cursor:hand;-fx-border-width:0;"));

        footer.getChildren().addAll(userRow, sep, btnLogout);
        return footer;
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void navigate(String page) {
        // Mise à jour titre topbar
        topbarTitle.setText(emojiLabel(page));

        // Réinitialise tous les boutons nav
        sidebar().getChildren().stream()
            .filter(n -> n instanceof Button)
            .map(n -> (Button) n)
            .forEach(b -> {
                b.getStyleClass().remove("nav-item-active");
                b.getStyleClass().add("nav-item");
                b.setStyle("");
            });

        // Active le bon bouton
        sidebar().getChildren().stream()
            .filter(n -> n instanceof Button b && b.getText().contains(page))
            .findFirst()
            .ifPresent(n -> {
                activeBtn = (Button) n;
                activeBtn.getStyleClass().remove("nav-item");
                activeBtn.getStyleClass().add("nav-item-active");
                activeBtn.setStyle(""); // laisser le CSS gérer
            });

        contentArea.getChildren().setAll(loadView(page));
    }

    private String emojiLabel(String page) {
        for (String[] item : NAV) {
            if (item[1].equals(page)) return item[0] + "  " + page;
        }
        return page;
    }

    private Region loadView(String page) {
        return switch (page) {
            case "Dashboard"    -> new DashboardView(session).build();
            case "Demandes"     -> new GestionDemandeView(session).build();
            case "Validation"   -> new ValidationView(session).build();
            case "Bons"         -> new GestionBonsView(session).build();
            case "Clients"      -> new GestionClientsView(session).build();
            case "Statistiques" -> new StatistiquesView(session).build();
            case "Rédemption"   -> new RedemptionView(session).build();
            case "Archives"     -> new ArchivesView(session).build();
            case "Paramètres"   -> new ParametresView(session).build();
            default             -> new DashboardView(session).build();
        };
    }

    private void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Voulez-vous vous déconnecter ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Déconnexion");
        alert.setHeaderText("Confirmer la déconnexion");
        alert.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) new LoginView(stage).show();
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox sidebar() {
        BorderPane root  = (BorderPane) stage.getScene().getRoot();
        return (VBox) root.getLeft();
    }

    private Button navButton(String icon, String label) {
        Button btn = new Button(icon + "   " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("nav-item");
        return btn;
    }
}
