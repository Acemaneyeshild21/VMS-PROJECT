package pkg.vms;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.view.*;

/**
 * Fenêtre principale VMS — sidebar repliable + topbar + zone de contenu.
 *
 * <p>La sidebar peut être réduite en mode « icon-only » (60 px) via le bouton
 * toggle en haut à gauche. Une animation fluide (200 ms) accompagne la transition.</p>
 */
public class MainWindow {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final String C_SIDEBAR = "#0f172a";
    private static final String C_TEXT_S  = "#94a3b8";
    private static final String C_SURFACE = "#f1f5f9";

    // ── Dimensions sidebar ───────────────────────────────────────────────────
    private static final double W_EXPANDED  = 228;
    private static final double W_COLLAPSED = 62;

    // ── Navigation : [emoji, label, rôle requis] ────────────────────────────
    private static final String[][] NAV = {
        {"⬛", "Dashboard",    ""},
        {"📋", "Demandes",     ""},
        {"✅", "Validation",   ""},
        {"🎟", "Bons",         ""},
        {"👥", "Clients",      "Administrateur"},
        {"📊", "Statistiques", "Administrateur"},
        {"🛒", "Rédemption",   ""},
        {"📁", "Archives",     ""},
        {"⚙",  "Paramètres",  "Administrateur"},
    };

    private final Stage               stage;
    private final AuthDAO.UserSession  session;
    private final StackPane            contentArea  = new StackPane();
    private final Label                topbarTitle  = new Label("Dashboard");

    // Sidebar state
    private VBox    sidebar;
    private boolean collapsed = false;
    private Button  activeBtn;

    // Nœuds qui disparaissent en mode collapsed
    private Label   sidebarNavLabel;
    private Label   sidebarSubtitle;
    private Label   sidebarUser;
    private Label   sidebarRole;
    private Button  btnLogout;

    public MainWindow(Stage stage, AuthDAO.UserSession session) {
        this.stage   = stage;
        this.session = session;
    }

    // ── Entrée ───────────────────────────────────────────────────────────────

    public void show() {
        sidebar = buildSidebar();

        BorderPane rightPane = new BorderPane();
        rightPane.setTop(buildTopbar());
        rightPane.setCenter(contentArea);
        contentArea.setStyle("-fx-background-color:" + C_SURFACE + ";");

        BorderPane root = new BorderPane();
        root.setLeft(sidebar);
        root.setCenter(rightPane);

        Scene scene = new Scene(root, 1340, 840);
        try {
            scene.getStylesheets().add(getClass().getResource("/vms.css").toExternalForm());
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

        String initial = session.username.substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:13;-fx-background-radius:20;-fx-alignment:center;"
            + "-fx-min-width:36;-fx-min-height:36;-fx-max-width:36;-fx-max-height:36;");

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
        VBox sb = new VBox(0);
        sb.setPrefWidth(W_EXPANDED);
        sb.setMinWidth(W_EXPANDED);
        sb.setMaxWidth(W_EXPANDED);
        sb.setStyle("-fx-background-color:" + C_SIDEBAR + ";");

        sb.getChildren().addAll(
            buildSidebarHeader(),
            buildNavSection(),
            spacerRegion(),
            buildUserFooter()
        );
        return sb;
    }

    /** Haut de la sidebar : logo + bouton toggle. */
    private HBox buildSidebarHeader() {
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 12, 16, 20));
        header.setStyle("-fx-border-color:#1e293b;-fx-border-width:0 0 1 0;");
        header.setMinHeight(64);

        // Logo
        Label dot   = new Label("●");
        dot.setStyle("-fx-font-size:9;-fx-text-fill:#dc2626;");
        Label lLogo = new Label("VMS");
        lLogo.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:white;");

        sidebarSubtitle = new Label("VoucherManager");
        sidebarSubtitle.setStyle("-fx-font-size:9;-fx-text-fill:" + C_TEXT_S + ";");
        sidebarSubtitle.setManaged(true);
        sidebarSubtitle.setVisible(true);

        VBox brandBox = new VBox(2, new HBox(6, dot, lLogo), sidebarSubtitle);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(brandBox, Priority.ALWAYS);

        // Bouton toggle ‹ / ›
        Button btnToggle = new Button("‹");
        btnToggle.setStyle(
            "-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;-fx-font-size:14;"
            + "-fx-font-weight:bold;-fx-padding:4 8;-fx-background-radius:6;"
            + "-fx-cursor:hand;-fx-border-width:0;");
        btnToggle.setOnMouseEntered(e ->
            btnToggle.setStyle("-fx-background-color:#334155;-fx-text-fill:white;"
                + "-fx-font-size:14;-fx-font-weight:bold;-fx-padding:4 8;"
                + "-fx-background-radius:6;-fx-cursor:hand;-fx-border-width:0;"));
        btnToggle.setOnMouseExited(e ->
            btnToggle.setStyle("-fx-background-color:#1e293b;-fx-text-fill:#94a3b8;"
                + "-fx-font-size:14;-fx-font-weight:bold;-fx-padding:4 8;"
                + "-fx-background-radius:6;-fx-cursor:hand;-fx-border-width:0;"));
        btnToggle.setOnAction(e -> toggleSidebar(btnToggle));

        header.getChildren().addAll(brandBox, btnToggle);
        return header;
    }

    private VBox buildNavSection() {
        VBox section = new VBox(0);

        sidebarNavLabel = new Label("NAVIGATION");
        sidebarNavLabel.setStyle("-fx-font-size:10;-fx-text-fill:#475569;-fx-font-weight:bold;"
                + "-fx-padding:16 24 6 24;");
        section.getChildren().add(sidebarNavLabel);

        for (String[] item : NAV) {
            String icon  = item[0];
            String label = item[1];
            String role  = item[2];
            boolean adminOnly = !role.isEmpty();
            boolean isAdmin   = "Administrateur".equals(session.role);
            if (adminOnly && !isAdmin) continue;

            Button btn = navButton(icon, label);
            btn.setOnAction(e -> navigate(label));
            btn.setUserData(new String[]{icon, label}); // mémorise icon + label
            section.getChildren().add(btn);
        }
        return section;
    }

    private VBox buildUserFooter() {
        VBox footer = new VBox(6);
        footer.setPadding(new Insets(14, 20, 22, 20));
        footer.setStyle("-fx-border-color:#1e293b;-fx-border-width:1 0 0 0;");

        String initial = session.username.substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle(
            "-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-weight:bold;"
            + "-fx-font-size:12;-fx-background-radius:16;-fx-alignment:center;"
            + "-fx-min-width:30;-fx-min-height:30;-fx-max-width:30;-fx-max-height:30;");

        sidebarUser = new Label(session.username);
        sidebarUser.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-text-fill:white;");
        sidebarRole = new Label(session.role);
        sidebarRole.setStyle("-fx-font-size:10;-fx-text-fill:" + C_TEXT_S + ";");

        VBox nameBox = new VBox(1, sidebarUser, sidebarRole);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        HBox userRow = new HBox(10, avatar, nameBox);
        userRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color:#1e293b;");
        sep.setManaged(true); sep.setVisible(true);

        btnLogout = new Button("↩  Déconnexion");
        btnLogout.setMaxWidth(Double.MAX_VALUE);
        btnLogout.setAlignment(Pos.CENTER_LEFT);
        btnLogout.setStyle("-fx-background-color:transparent;-fx-text-fill:#ef4444;"
            + "-fx-font-size:12;-fx-padding:6 0;-fx-cursor:hand;-fx-border-width:0;");
        btnLogout.setOnAction(e -> logout());
        btnLogout.setOnMouseEntered(e -> btnLogout.setStyle(
            "-fx-background-color:#1e293b;-fx-text-fill:#fca5a5;-fx-font-size:12;"
            + "-fx-padding:6 8;-fx-cursor:hand;-fx-border-width:0;-fx-background-radius:6;"));
        btnLogout.setOnMouseExited(e -> btnLogout.setStyle(
            "-fx-background-color:transparent;-fx-text-fill:#ef4444;-fx-font-size:12;"
            + "-fx-padding:6 0;-fx-cursor:hand;-fx-border-width:0;"));

        footer.getChildren().addAll(userRow, sep, btnLogout);
        return footer;
    }

    // ── Toggle sidebar ───────────────────────────────────────────────────────

    private void toggleSidebar(Button btnToggle) {
        collapsed = !collapsed;

        // Animation de la largeur
        double targetW = collapsed ? W_COLLAPSED : W_EXPANDED;
        Timeline anim = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(sidebar.prefWidthProperty(), sidebar.getPrefWidth())),
            new KeyFrame(Duration.millis(200),
                new KeyValue(sidebar.prefWidthProperty(), targetW, Interpolator.EASE_BOTH))
        );
        anim.setOnFinished(e -> {
            sidebar.setMinWidth(targetW);
            sidebar.setMaxWidth(targetW);
        });
        anim.play();

        // Direction du bouton
        btnToggle.setText(collapsed ? "›" : "‹");

        // Masquer / afficher les nœuds textuels
        sidebarSubtitle.setVisible(!collapsed);
        sidebarSubtitle.setManaged(!collapsed);
        sidebarNavLabel.setVisible(!collapsed);
        sidebarNavLabel.setManaged(!collapsed);
        sidebarUser.setVisible(!collapsed);
        sidebarUser.setManaged(!collapsed);
        sidebarRole.setVisible(!collapsed);
        sidebarRole.setManaged(!collapsed);
        btnLogout.setVisible(!collapsed);
        btnLogout.setManaged(!collapsed);

        // Mettre à jour les boutons nav
        sidebar.getChildren().stream()
            .filter(n -> n instanceof VBox vbox && vbox.getChildren().stream()
                .anyMatch(c -> c instanceof Button b && b.getUserData() instanceof String[]))
            .findFirst()
            .ifPresent(navSection -> {
                ((VBox) navSection).getChildren().stream()
                    .filter(n -> n instanceof Button b && b.getUserData() instanceof String[])
                    .map(n -> (Button) n)
                    .forEach(btn -> {
                        String[] data = (String[]) btn.getUserData();
                        String icon  = data[0];
                        String label = data[1];
                        if (collapsed) {
                            btn.setText(icon);
                            btn.setAlignment(Pos.CENTER);
                            btn.setPadding(new Insets(13, 0, 13, 0));
                            btn.setMaxWidth(W_COLLAPSED);
                            // Tooltip pour le mode icon-only
                            btn.setTooltip(new Tooltip(label));
                        } else {
                            btn.setText(icon + "   " + label);
                            btn.setAlignment(Pos.CENTER_LEFT);
                            btn.setPadding(new Insets(12, 24, 12, 20));
                            btn.setMaxWidth(Double.MAX_VALUE);
                            btn.setTooltip(null);
                        }
                    });
            });
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    private void navigate(String page) {
        topbarTitle.setText(emojiLabel(page));

        // Reset tous les nav buttons
        allNavButtons().forEach(b -> {
            b.getStyleClass().remove("nav-item-active");
            if (!b.getStyleClass().contains("nav-item")) b.getStyleClass().add("nav-item");
        });

        // Active le bouton correspondant
        allNavButtons()
            .filter(b -> b.getUserData() instanceof String[] d && d[1].equals(page))
            .findFirst()
            .ifPresent(b -> {
                activeBtn = b;
                b.getStyleClass().remove("nav-item");
                b.getStyleClass().add("nav-item-active");
            });

        contentArea.getChildren().setAll(loadView(page));
    }

    private java.util.stream.Stream<Button> allNavButtons() {
        return sidebar.getChildren().stream()
            .filter(n -> n instanceof VBox)
            .flatMap(n -> ((VBox) n).getChildren().stream())
            .filter(n -> n instanceof Button b && b.getUserData() instanceof String[])
            .map(n -> (Button) n);
    }

    private String emojiLabel(String page) {
        for (String[] item : NAV)
            if (item[1].equals(page)) return item[0] + "  " + page;
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

    // ── Logout ───────────────────────────────────────────────────────────────

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

    private Button navButton(String icon, String label) {
        Button btn = new Button(icon + "   " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPadding(new Insets(12, 24, 12, 20));
        btn.getStyleClass().add("nav-item");
        return btn;
    }

    private Region spacerRegion() {
        Region r = new Region();
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }
}
