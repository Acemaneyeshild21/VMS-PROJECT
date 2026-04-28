package pkg.vms.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.controller.StatistiquesController;

/**
 * Vue Dashboard — KPIs + activité récente.
 */
public class DashboardView {

    private final AuthDAO.UserSession session;
    private final StatistiquesController ctrl = new StatistiquesController();

    // KPI labels (mis à jour après chargement)
    private Label kpiDemandes  = new Label("—");
    private Label kpiMontant   = new Label("—");
    private Label kipiBonsAct  = new Label("—");
    private Label kpiTaux      = new Label("—");

    private TableView<String[]> tblAudit = new TableView<>();

    public DashboardView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return scroll;
    }

    private VBox buildContent() {
        VBox root = new VBox(24);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color:#f1f5f9;");

        // Header
        root.getChildren().add(buildHeader());

        // KPI cards
        HBox kpiRow = new HBox(16,
            kpiCard("📋 Demandes",   kpiDemandes,  "Total créées",       "#2563eb"),
            kpiCard("💰 Montant",    kpiMontant,   "Rs — Valeur totale", "#16a34a"),
            kpiCard("🎟 Bons actifs",kipiBonsAct,  "En circulation",     "#d97706"),
            kpiCard("📈 Validation", kpiTaux,      "Taux d'approbation", "#7c3aed")
        );
        kpiRow.setFillHeight(true);
        root.getChildren().add(kpiRow);

        // Activité récente
        root.getChildren().add(buildAuditSection());

        // Quick actions
        root.getChildren().add(buildQuickActions());

        // Load data
        loadStats();
        return root;
    }

    private HBox buildHeader() {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4);
        Label title = new Label("Tableau de bord");
        title.setStyle("-fx-font-size:24;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label sub = new Label("Bienvenue, " + session.username + " — " + session.role);
        sub.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");
        text.getChildren().addAll(title, sub);

        h.getChildren().add(text);
        return h;
    }

    @SuppressWarnings("unchecked")
    private VBox buildAuditSection() {
        VBox card = card("Activité récente");

        tblAudit.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblAudit.setPrefHeight(260);
        tblAudit.setStyle("-fx-font-size:12;");
        tblAudit.setPlaceholder(new Label("Chargement…"));

        tblAudit.getColumns().addAll(
            col("Action", 0, 150),
            col("Entité", 1, 120),
            col("Utilisateur", 2, 120),
            col("Contexte", 3, 300)
        );

        card.getChildren().add(tblAudit);
        return card;
    }

    private HBox buildQuickActions() {
        HBox h = new HBox(12);
        h.getChildren().addAll(
            actionCard("📋", "Nouvelle demande",    "Créer une demande de bons", "#2563eb"),
            actionCard("✅", "Valider un paiement", "Changer le statut",         "#16a34a"),
            actionCard("📊", "Exporter rapport",    "Excel ODBC",                "#7c3aed")
        );
        return h;
    }

    private VBox actionCard(String icon, String title, String desc, String color) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(20));
        c.setPrefWidth(240);
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);-fx-cursor:hand;");

        Label ico = new Label(icon);
        ico.setStyle("-fx-font-size:22;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label d = new Label(desc);
        d.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        c.getChildren().addAll(ico, t, d);

        c.setOnMouseEntered(e -> c.setStyle(
            "-fx-background-color:" + color + ";-fx-background-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);-fx-cursor:hand;"));
        c.setOnMouseExited(e -> c.setStyle(
            "-fx-background-color:white;-fx-background-radius:12;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);-fx-cursor:hand;"));
        c.setOnMouseEntered(e -> {
            t.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:white;");
            d.setStyle("-fx-font-size:12;-fx-text-fill:rgba(255,255,255,0.8);");
            c.setStyle("-fx-background-color:" + color + ";-fx-background-radius:12;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);-fx-cursor:hand;");
        });
        c.setOnMouseExited(e -> {
            t.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
            d.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
            c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);-fx-cursor:hand;");
        });

        return c;
    }

    // ── Data loading ─────────────────────────────────────────────────────────

    private void loadStats() {
        ctrl.chargerDonnees(session.role, session.userId,
            data -> {
                kpiDemandes.setText(String.valueOf(data.totalDemandes));
                kpiMontant.setText("Rs " + String.format("%,.0f", data.montantTotal));
                kipiBonsAct.setText(String.valueOf(data.bonsActifs));
                kpiTaux.setText(String.format("%.1f%%", data.tauxRedemption));

                tblAudit.getItems().clear();
                for (Object[] row : data.auditRows) {
                    tblAudit.getItems().add(new String[]{
                        String.valueOf(row[0]), String.valueOf(row[1]),
                        String.valueOf(row[2]), String.valueOf(row[3])
                    });
                }
            },
            err -> showErr("Erreur chargement stats : " + err)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox kpiCard(String title, Label valLbl, String desc, String color) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(20));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        HBox.setHgrow(c, Priority.ALWAYS);

        Label ico = new Label("●");
        ico.setStyle("-fx-font-size:10;-fx-text-fill:" + color + ";");

        Label ttl = new Label(title);
        ttl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        valLbl.setStyle("-fx-font-size:28;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label dsc = new Label(desc);
        dsc.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");

        c.getChildren().addAll(new HBox(6, ico, ttl), valLbl, dsc);
        return c;
    }

    private VBox card(String title) {
        VBox c = new VBox(12);
        c.setPadding(new Insets(20));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        c.getChildren().add(t);
        return c;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<String[], String> col(String title, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        c.setPrefWidth(w);
        return c;
    }

    private void showErr(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }
}
