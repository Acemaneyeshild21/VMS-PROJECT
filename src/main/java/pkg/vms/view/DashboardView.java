package pkg.vms.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.StatistiquesController;

/**
 * Vue Dashboard — KPIs + activité récente + actions rapides.
 * Design moderne : cartes avec barre de couleur, table audit stylisée.
 */
public class DashboardView {

    private final AuthDAO.UserSession      session;
    private final StatistiquesController   ctrl = new StatistiquesController();

    // KPI labels (mis à jour après chargement async)
    private final Label kpiDemandes      = new Label("—");
    private final Label kpiMontant       = new Label("—");
    private final Label kpiBons          = new Label("—");
    private final Label kpiTaux          = new Label("—");

    // KPI tendances
    private final Label kpiDemandesTrend = new Label("…");
    private final Label kpiMontantTrend  = new Label("…");
    private final Label kpiBonsTrend     = new Label("…");
    private final Label kpiTauxTrend     = new Label("…");

    @SuppressWarnings("unchecked")
    private final TableView<String[]> tblAudit = new TableView<>();

    public DashboardView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        return scroll;
    }

    private VBox buildContent() {
        VBox root = new VBox(28);
        root.setPadding(new Insets(32, 36, 36, 36));
        root.setStyle("-fx-background-color:#f1f5f9;");

        root.getChildren().addAll(
            buildPageHeader(),
            buildKpiRow(),
            buildBottomRow()
        );

        loadStats();
        return root;
    }

    // ── En-tête de page ──────────────────────────────────────────────────────

    private HBox buildPageHeader() {
        HBox h = new HBox();
        h.setAlignment(Pos.CENTER_LEFT);

        VBox text = new VBox(4);
        Label title = new Label("Tableau de bord");
        title.setStyle("-fx-font-size:24;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label sub = new Label("Bienvenue, " + session.username
                + "  ·  " + java.time.LocalDate.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy",
                        java.util.Locale.FRENCH)));
        sub.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        text.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Badge de rôle
        Label badge = new Label(session.role);
        badge.setStyle(
            "-fx-background-color:" + roleColor(session.role) + "22;"
            + "-fx-text-fill:"      + roleColor(session.role) + ";"
            + "-fx-font-size:11;-fx-font-weight:bold;"
            + "-fx-padding:4 12;-fx-background-radius:20;");

        h.getChildren().addAll(text, spacer, badge);
        return h;
    }

    // ── Ligne KPIs ───────────────────────────────────────────────────────────

    private HBox buildKpiRow() {
        HBox row = new HBox(16,
            kpiCard(kpiDemandes, kpiDemandesTrend, "Demandes",      "Total créées",         "#2563eb"),
            kpiCard(kpiMontant,  kpiMontantTrend,  "Montant total", "Rs — Valeur émise",    "#16a34a"),
            kpiCard(kpiBons,     kpiBonsTrend,      "Bons actifs",   "En circulation",       "#d97706"),
            kpiCard(kpiTaux,     kpiTauxTrend,      "Taux valid.",   "Bons rédemptés / émis","#7c3aed")
        );
        row.setFillHeight(true);
        return row;
    }

    private VBox kpiCard(Label valLbl, Label trendLbl, String title, String desc, String color) {
        VBox card = new VBox(0);
        card.setStyle(
            "-fx-background-color:white;-fx-background-radius:12;"
            + "-fx-border-radius:12;-fx-border-color:#f1f5f9;-fx-border-width:1;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.055),8,0,0,2);");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);

        // Barre de couleur en haut
        Region bar = new Region();
        bar.setPrefHeight(4);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setStyle("-fx-background-color:" + color + ";"
                   + "-fx-background-radius:12 12 0 0;");

        // Contenu
        VBox body = new VBox(6);
        body.setPadding(new Insets(16, 20, 18, 20));

        Label ttl = new Label(title.toUpperCase());
        ttl.setStyle("-fx-font-size:10;-fx-font-weight:bold;-fx-text-fill:#94a3b8;"
                   + "-fx-letter-spacing:0.08em;");

        valLbl.setStyle("-fx-font-size:30;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label dsc = new Label(desc);
        dsc.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");

        // Tendance (ex: "↑ 12 ce mois" ou "⬇ Expirent bientôt")
        trendLbl.setStyle("-fx-font-size:11;-fx-text-fill:" + color + ";-fx-font-weight:bold;");

        body.getChildren().addAll(ttl, valLbl, dsc, trendLbl);
        card.getChildren().addAll(bar, body);
        return card;
    }

    // ── Ligne inférieure : audit + actions ───────────────────────────────────

    private HBox buildBottomRow() {
        HBox row = new HBox(20);
        row.setFillHeight(true);

        VBox auditCard = buildAuditCard();
        HBox.setHgrow(auditCard, Priority.ALWAYS);

        VBox actionsCard = buildActionsCard();
        actionsCard.setPrefWidth(300);
        actionsCard.setMinWidth(260);
        actionsCard.setMaxWidth(300);

        row.getChildren().addAll(auditCard, actionsCard);
        return row;
    }

    @SuppressWarnings("unchecked")
    private VBox buildAuditCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color:white;-fx-background-radius:12;"
            + "-fx-border-radius:12;-fx-border-color:#f1f5f9;-fx-border-width:1;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.055),8,0,0,2);");

        HBox header = new HBox();
        Label t = new Label("Activité récente");
        t.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label sub = new Label("Dernières 50 entrées");
        sub.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");
        header.getChildren().addAll(t, sp, sub);

        tblAudit.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblAudit.setPrefHeight(260);
        tblAudit.setPlaceholder(new Label("Chargement de l'activité…"));
        VBox.setVgrow(tblAudit, Priority.ALWAYS);

        tblAudit.getColumns().addAll(
            col("Action",      0, 160),
            col("Entité",      1, 120),
            col("Utilisateur", 2, 120),
            col("Détail",      3, 0)    // 0 = croît librement
        );

        card.getChildren().addAll(header, tblAudit);
        return card;
    }

    private VBox buildActionsCard() {
        VBox card = new VBox(16);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color:white;-fx-background-radius:12;"
            + "-fx-border-radius:12;-fx-border-color:#f1f5f9;-fx-border-width:1;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.055),8,0,0,2);");

        Label t = new Label("Actions rapides");
        t.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        card.getChildren().addAll(
            t,
            quickAction("📋", "Nouvelle demande",    "Créer une demande de bons",  "#2563eb"),
            quickAction("✅", "Valider paiement",    "Changer le statut",          "#16a34a"),
            quickAction("📊", "Exporter rapport",    "Excel / ODBC",               "#7c3aed"),
            quickAction("🔍", "Vérifier un bon",     "Contrôle anti-fraude",       "#d97706")
        );
        return card;
    }

    private HBox quickAction(String icon, String title, String desc, String color) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 12, 10, 12));
        row.setStyle(
            "-fx-background-color:#f8fafc;-fx-background-radius:8;-fx-cursor:hand;");
        row.setMaxWidth(Double.MAX_VALUE);

        // Icône colorée
        Label ico = new Label(icon);
        ico.setStyle(
            "-fx-font-size:16;-fx-background-color:" + color + "22;"
            + "-fx-background-radius:8;-fx-padding:6 8;");

        VBox text = new VBox(2);
        Label ttl = new Label(title);
        ttl.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label dsc = new Label(desc);
        dsc.setStyle("-fx-font-size:11;-fx-text-fill:#64748b;");
        text.getChildren().addAll(ttl, dsc);
        HBox.setHgrow(text, Priority.ALWAYS);

        Label arrow = new Label("›");
        arrow.setStyle("-fx-font-size:16;-fx-text-fill:#94a3b8;");

        row.getChildren().addAll(ico, text, arrow);

        // Hover
        row.setOnMouseEntered(e ->
            row.setStyle("-fx-background-color:" + color + "11;-fx-background-radius:8;-fx-cursor:hand;"));
        row.setOnMouseExited(e ->
            row.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;-fx-cursor:hand;"));

        return row;
    }

    // ── Chargement ───────────────────────────────────────────────────────────

    private void loadStats() {
        ctrl.chargerDonnees(session.role, session.userId,
            data -> {
                kpiDemandes.setText(String.valueOf(data.totalDemandes));
                kpiMontant.setText("Rs " + String.format("%,.0f", data.montantTotal));
                kpiBons.setText(String.valueOf(data.bonsActifs));
                kpiTaux.setText(String.format("%.1f%%", data.tauxRedemption));

                // Tendances (données complémentaires)
                kpiDemandesTrend.setText("↗ " + data.demandesAttente + " en attente");
                kpiMontantTrend.setText("↗ " + data.demandesApprouvees + " approuvées");
                kpiBonsTrend.setText("⚠ " + data.bonsExpirant30j + " expirent dans 30j");
                kpiTauxTrend.setText("✓ " + data.redemptionsToday + " validées aujourd'hui");

                tblAudit.getItems().clear();
                for (Object[] row : data.auditRows) {
                    tblAudit.getItems().add(new String[]{
                        String.valueOf(row[0]), String.valueOf(row[1]),
                        String.valueOf(row[2]), String.valueOf(row[3])
                    });
                }
                tblAudit.setPlaceholder(new Label("Aucune activité récente."));
            },
            err -> tblAudit.setPlaceholder(new Label("Erreur : " + err))
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableColumn<String[], String> col(String title, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        if (w > 0) c.setPrefWidth(w);
        return c;
    }

    private String roleColor(String role) {
        return switch (role) {
            case "Administrateur"       -> "#dc2626";
            case "Superviseur_Magasin"  -> "#d97706";
            case "Valideur"             -> "#2563eb";
            default                     -> "#64748b";
        };
    }
}
