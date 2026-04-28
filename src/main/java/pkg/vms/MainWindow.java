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
 * Fenêtre principale VMS — sidebar + zone de contenu.
 * Reçoit la {@link AuthDAO.UserSession} après authentification réussie.
 */
public class MainWindow {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String C_SIDEBAR   = "#0f172a";
    private static final String C_SIDEBAR_H = "#1e293b";
    private static final String C_ACTIVE    = "#dc2626";
    private static final String C_SURFACE   = "#f1f5f9";
    private static final String C_TEXT_S    = "#94a3b8";
    private static final String C_TEXT_W    = "white";

    // ── Navigation items : [icon, label, rôle minimum] ──────────────────────
    private static final String[][] NAV = {
        {"⬛", "Dashboard",      ""},
        {"📋", "Demandes",       ""},
        {"✅", "Validation",     ""},
        {"🎟", "Bons",           ""},
        {"👥", "Clients",        "Administrateur"},
        {"📊", "Statistiques",   "Administrateur"},
        {"🛒", "Rédemption",     ""},
        {"📁", "Archives",       ""},
        {"⚙",  "Paramètres",    "Administrateur"},
    };

    private final Stage              stage;
    private final AuthDAO.UserSession session;
    private final StackPane          contentArea = new StackPane();

    private Button activeBtn;

    public MainWindow(Stage stage, AuthDAO.UserSession session) {
        this.stage   = stage;
        this.session = session;
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.setLeft(buildSidebar());
        root.setCenter(contentArea);

        contentArea.setStyle("-fx-background-color:" + C_SURFACE + ";");

        Scene scene = new Scene(root, 1300, 820);
        stage.setTitle("VoucherManager VMS — " + session.username);
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();

        navigate("Dashboard"); // page d'accueil
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────

    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.setStyle("-fx-background-color:" + C_SIDEBAR + ";");

        // Logo
        VBox logoBox = new VBox(4);
        logoBox.setPadding(new Insets(28, 24, 20, 24));
        logoBox.setStyle("-fx-border-color:#1e293b;-fx-border-width:0 0 1 0;");
        Label lLogo = new Label("VMS");
        lLogo.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:" + C_TEXT_W + ";");
        Label lSub  = new Label("VoucherManager");
        lSub.setStyle("-fx-font-size:11;-fx-text-fill:" + C_TEXT_S + ";");
        logoBox.getChildren().addAll(lLogo, lSub);
        sidebar.getChildren().add(logoBox);

        // Nav section label
        Label lNav = new Label("NAVIGATION");
        lNav.setStyle("-fx-font-size:10;-fx-text-fill:#475569;-fx-font-weight:bold;"
                    + "-fx-padding:18 24 8 24;");
        sidebar.getChildren().add(lNav);

        // Nav buttons
        for (String[] item : NAV) {
            String icon  = item[0];
            String label = item[1];
            String role  = item[2];

            // Visibilité selon rôle
            if (!role.isEmpty() && !role.equals(session.role) && !"Administrateur".equals(session.role)) {
                continue;
            }

            Button btn = navButton(icon, label);
            btn.setOnAction(e -> navigate(label));
            sidebar.getChildren().add(btn);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // User info + logout
        sidebar.getChildren().add(buildUserFooter());
        return sidebar;
    }

    private Button navButton(String icon, String label) {
        Button btn = new Button(icon + "  " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 24, 12, 24));
        btn.setStyle(navStyle(false));
        btn.setOnMouseEntered(e -> {
            if (btn != activeBtn) btn.setStyle(navStyle(true));
        });
        btn.setOnMouseExited(e -> {
            if (btn != activeBtn) btn.setStyle(navStyle(false));
        });
        return btn;
    }

    private VBox buildUserFooter() {
        VBox footer = new VBox(4);
        footer.setPadding(new Insets(16, 24, 24, 24));
        footer.setStyle("-fx-border-color:#1e293b;-fx-border-width:1 0 0 0;");

        Label lUser = new Label(session.username);
        lUser.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:white;");

        Label lRole = new Label(session.role);
        lRole.setStyle("-fx-font-size:11;-fx-text-fill:" + C_TEXT_S + ";");

        Button btnLogout = new Button("⬚  Déconnexion");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setAlignment(Pos.CENTER_LEFT);
        btnLogout.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;"
                + "-fx-font-size:12;-fx-padding:8 0;-fx-cursor:hand;");
        btnLogout.setOnAction(e -> logout());

        footer.getChildren().addAll(lUser, lRole, btnLogout);
        return footer;
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void navigate(String page) {
        // Met à jour le bouton actif
        stage.getScene().getRoot().lookupAll(".nav-btn").forEach(n -> {
            if (n instanceof Button b) b.setStyle(navStyle(false));
        });
        // Cherche le bouton correspondant dans la sidebar
        sidebar().getChildren().stream()
            .filter(n -> n instanceof Button b && b.getText().contains(page))
            .findFirst()
            .ifPresent(n -> {
                activeBtn = (Button) n;
                activeBtn.setStyle(navStyleActive());
            });

        // Charge la vue
        contentArea.getChildren().setAll(loadView(page));
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
            if (r == ButtonType.YES) {
                new LoginView(stage).show();
            }
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Récupère la sidebar (le nœud Left du BorderPane). */
    private VBox sidebar() {
        return (VBox) ((BorderPane) stage.getScene().getRoot()).getLeft();
    }

    private String navStyle(boolean hover) {
        return "-fx-background-color:" + (hover ? C_SIDEBAR_H : "transparent")
             + ";-fx-text-fill:" + C_TEXT_W + ";-fx-font-size:13;"
             + "-fx-alignment:CENTER_LEFT;-fx-cursor:hand;-fx-graphic-text-gap:8;";
    }

    private String navStyleActive() {
        return "-fx-background-color:" + C_ACTIVE + ";-fx-text-fill:white;"
             + "-fx-font-size:13;-fx-font-weight:bold;"
             + "-fx-alignment:CENTER_LEFT;-fx-cursor:hand;";
    }
}
