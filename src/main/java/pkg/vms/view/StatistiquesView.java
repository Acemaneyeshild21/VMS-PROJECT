package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.RapportController;
import pkg.vms.controller.StatistiquesController;

import java.io.File;
import java.util.List;

/**
 * Vue Statistiques — KPIs, tableaux de bord, export Excel.
 */
public class StatistiquesView {

    private final AuthDAO.UserSession      session;
    private final StatistiquesController   ctrl  = new StatistiquesController();
    private final RapportController        rCtrl = new RapportController();

    private Label lblDemandes, lblMontant, lblBons, lblTaux;
    private TableView<Object[]> tblStatuts;
    private TableView<Object[]> tblTopClients;
    private TableView<Object[]> tblExpiration;

    public StatistiquesView(AuthDAO.UserSession session) { this.session = session; }

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

        root.getChildren().addAll(
            buildHeader(),
            buildKPIRow(),
            buildTablesRow(),
            buildExpirationCard(),
            buildExportButtons()
        );

        loadData();
        return root;
    }

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Statistiques & Rapports");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRefresh = new Button("↻ Actualiser");
        btnRefresh.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:13;"
                + "-fx-padding:9 18;-fx-background-radius:8;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-cursor:hand;");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(title, sp, btnRefresh);
        return h;
    }

    private HBox buildKPIRow() {
        lblDemandes = kpiLabel();
        lblMontant  = kpiLabel();
        lblBons     = kpiLabel();
        lblTaux     = kpiLabel();

        HBox row = new HBox(16,
            kpiCard("📋 Demandes",    lblDemandes, "Total créées",       "#2563eb"),
            kpiCard("💰 Montant",     lblMontant,  "Rs — Valeur totale", "#16a34a"),
            kpiCard("🎟 Bons actifs", lblBons,     "En circulation",     "#d97706"),
            kpiCard("📈 Rédemption",  lblTaux,     "Taux global %",      "#7c3aed")
        );
        row.setFillHeight(true);
        return row;
    }

    private HBox buildTablesRow() {
        HBox h = new HBox(16);
        h.setFillHeight(true);

        // Table statuts
        VBox statCard = buildTableCard("Répartition par statut");
        tblStatuts = new TableView<>();
        tblStatuts.setPrefHeight(220);
        tblStatuts.setStyle("-fx-font-size:12;");
        tblStatuts.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblStatuts.getColumns().addAll(
            colObj("Statut", 0, 160),
            colObj("Nb demandes", 1, 100),
            colObj("Montant total", 2, 130)
        );
        statCard.getChildren().add(tblStatuts);
        HBox.setHgrow(statCard, Priority.ALWAYS);

        // Table top clients
        VBox topCard = buildTableCard("Top 5 Clients");
        tblTopClients = new TableView<>();
        tblTopClients.setPrefHeight(220);
        tblTopClients.setStyle("-fx-font-size:12;");
        tblTopClients.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblTopClients.getColumns().addAll(
            colObj("Client", 0, 180),
            colObj("Nb demandes", 1, 100),
            colObj("Montant", 2, 130)
        );
        topCard.getChildren().add(tblTopClients);
        HBox.setHgrow(topCard, Priority.ALWAYS);

        h.getChildren().addAll(statCard, topCard);
        return h;
    }

    private VBox buildExpirationCard() {
        VBox card = buildTableCard("Bons proches d'expiration (≤30 jours)");

        tblExpiration = new TableView<>();
        tblExpiration.setPrefHeight(220);
        tblExpiration.setStyle("-fx-font-size:12;");
        tblExpiration.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblExpiration.setPlaceholder(new Label("Aucun bon en cours d'expiration."));
        tblExpiration.getColumns().addAll(
            colObj("Code unique",    0, 200),
            colObj("Valeur (Rs)",    1, 100),
            colObj("Client",         2, 160),
            colObj("Expiration",     3, 140),
            colObj("Jours restants", 4, 110)
        );
        card.getChildren().add(tblExpiration);
        return card;
    }

    private HBox buildExportButtons() {
        Label lbl = new Label("Exporter vers Excel :");
        lbl.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        HBox h = new HBox(12, lbl,
            exportBtn("📊 Rapport complet", "rapport_complet_vms"),
            exportBtn("📋 Demandes",        "rapport_demandes_vms"),
            exportBtn("🎟 Bons",            "rapport_bons_vms"),
            exportBtn("👥 Clients",         "rapport_clients_vms")
        );
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(20));
        h.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        return h;
    }

    private Button exportBtn(String label, String baseName) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color:#f1f5f9;-fx-text-fill:#1e293b;-fx-font-size:12;"
                 + "-fx-padding:8 14;-fx-background-radius:8;-fx-border-color:#d1d5db;"
                 + "-fx-border-radius:8;-fx-cursor:hand;");
        b.setOnAction(e -> exportRapport(baseName));
        return b;
    }

    private void exportRapport(String baseName) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le rapport Excel");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName(baseName + ".xlsx");
        File f = fc.showSaveDialog(lblDemandes.getScene().getWindow());
        if (f == null) return;

        if (baseName.contains("complet")) {
            rCtrl.genererRapportComplet(f.getAbsolutePath(),
                path -> showInfo("Rapport généré : " + path),
                err  -> showErr("Erreur : " + err));
        } else if (baseName.contains("demandes")) {
            rCtrl.genererRapportDemandes(f.getAbsolutePath(),
                path -> showInfo("Rapport généré : " + path),
                err  -> showErr("Erreur : " + err));
        } else if (baseName.contains("bons")) {
            rCtrl.genererRapportBons(f.getAbsolutePath(),
                path -> showInfo("Rapport généré : " + path),
                err  -> showErr("Erreur : " + err));
        } else {
            rCtrl.genererRapportClients(f.getAbsolutePath(),
                path -> showInfo("Rapport généré : " + path),
                err  -> showErr("Erreur : " + err));
        }
    }

    private void loadData() {
        lblDemandes.setText("…");
        lblMontant.setText("…");
        lblBons.setText("…");
        lblTaux.setText("…");

        ctrl.chargerDonnees(session.role, session.userId,
            data -> {
                lblDemandes.setText(String.valueOf(data.totalDemandes));
                lblMontant.setText("Rs " + String.format("%,.0f", data.montantTotal));
                lblBons.setText(String.valueOf(data.bonsActifs));
                lblTaux.setText(String.format("%.1f%%", data.tauxRedemption));

                ObservableList<Object[]> statutData = FXCollections.observableArrayList(data.statutRows);
                tblStatuts.setItems(statutData);

                ObservableList<Object[]> topData = FXCollections.observableArrayList(data.topClientsRows);
                tblTopClients.setItems(topData);

                ObservableList<Object[]> expData = FXCollections.observableArrayList(data.bonsExpirationRows);
                tblExpiration.setItems(expData);
            },
            err -> showErr("Erreur chargement : " + err)
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox kpiCard(String title, Label valLbl, String desc, String color) {
        VBox c = new VBox(8);
        c.setPadding(new Insets(20));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        HBox.setHgrow(c, Priority.ALWAYS);
        Label ttl = new Label(title);
        ttl.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        valLbl.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label dsc = new Label(desc);
        dsc.setStyle("-fx-font-size:11;-fx-text-fill:#94a3b8;");
        c.getChildren().addAll(ttl, valLbl, dsc);
        return c;
    }

    private Label kpiLabel() {
        Label l = new Label("—");
        l.setStyle("-fx-font-size:26;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        return l;
    }

    private VBox buildTableCard(String title) {
        VBox c = new VBox(12);
        c.setPadding(new Insets(20));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:14;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        c.getChildren().add(t);
        return c;
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Object[], String> colObj(String title, int idx, double w) {
        TableColumn<Object[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> {
            Object[] arr = p.getValue();
            String v = (arr != null && arr.length > idx && arr[idx] != null)
                     ? arr[idx].toString() : "";
            return new SimpleStringProperty(v);
        });
        c.setPrefWidth(w);
        return c;
    }

    private void showErr(String msg)  { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
