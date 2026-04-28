package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import pkg.vms.Client;
import pkg.vms.DAO.VoucherDAO;

import java.sql.Timestamp;
import java.util.List;

/**
 * Fiche Client 360° — historique complet d'un client.
 * Statistiques, demandes, bons, timeline d'activité.
 */
public class FicheClient360Dialog {

    private final Client client;

    public FicheClient360Dialog(Client client) { this.client = client; }

    public void show(Stage owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Fiche Client 360° — " + client.getName());
        stage.setWidth(900);
        stage.setHeight(680);
        stage.setResizable(true);

        ScrollPane scroll = new ScrollPane(buildContent(stage));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f1f5f9;-fx-background:transparent;");

        stage.setScene(new Scene(scroll));
        stage.centerOnScreen();
        stage.show();
    }

    private VBox buildContent(Stage stage) {
        VBox root = new VBox(24);
        root.setPadding(new Insets(32));
        root.setStyle("-fx-background-color:#f1f5f9;");

        root.getChildren().addAll(
            buildClientHeader(stage),
            buildKPIRow(),
            buildTabs()
        );

        return root;
    }

    // ── En-tête client ────────────────────────────────────────────────────────

    private HBox buildClientHeader(Stage stage) {
        // Avatar initiales
        Label avatar = new Label(initials(client.getName()));
        avatar.setMinSize(64, 64);
        avatar.setPrefSize(64, 64);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:22;"
                + "-fx-font-weight:bold;-fx-background-radius:32;");

        Label nom = new Label(client.getName());
        nom.setStyle("-fx-font-size:20;-fx-font-weight:bold;-fx-text-fill:#1e293b;");

        Label email = new Label("📧 " + nvl(client.getEmail()));
        email.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");

        Label tel = new Label("📞 " + nvl(client.getContactNumber()));
        tel.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");

        Label soc = new Label("🏢 " + nvl(client.getCompany()));
        soc.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");

        Label statut = new Label(client.isActif() ? "✓ Actif" : "✗ Inactif");
        statut.setStyle("-fx-font-size:12;-fx-font-weight:bold;-fx-padding:3 10;"
                + "-fx-background-radius:20;-fx-background-color:"
                + (client.isActif() ? "#dcfce7;-fx-text-fill:#16a34a;" : "#fee2e2;-fx-text-fill:#dc2626;"));

        VBox info = new VBox(5, nom, email, tel, soc, statut);
        info.setAlignment(Pos.CENTER_LEFT);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnClose = new Button("✕ Fermer");
        btnClose.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:13;"
                + "-fx-padding:9 18;-fx-background-radius:8;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-cursor:hand;");
        btnClose.setOnAction(e -> stage.close());

        HBox h = new HBox(20, avatar, info, sp, btnClose);
        h.setAlignment(Pos.CENTER_LEFT);
        h.setPadding(new Insets(24));
        h.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        return h;
    }

    // ── KPI Row ───────────────────────────────────────────────────────────────

    private HBox buildKPIRow() {
        Label lblDemandes = kpiLbl();
        Label lblMontant  = kpiLbl();
        Label lblActifs   = kpiLbl();
        Label lblUtilises = kpiLbl();

        HBox row = new HBox(16,
            kpiCard("📋 Demandes",   lblDemandes, "#2563eb"),
            kpiCard("💰 Montant",    lblMontant,  "#16a34a"),
            kpiCard("🎟 Bons actifs", lblActifs,  "#d97706"),
            kpiCard("✓ Utilisés",    lblUtilises, "#7c3aed")
        );
        row.setFillHeight(true);

        // Charger les stats en arrière-plan
        Task<VoucherDAO.ClientStats> task = new Task<>() {
            @Override protected VoucherDAO.ClientStats call() throws Exception {
                return VoucherDAO.getClientStats(client.getClientId());
            }
        };
        task.setOnSucceeded(e -> {
            VoucherDAO.ClientStats s = task.getValue();
            lblDemandes.setText(String.valueOf(s.totalDemandes));
            lblMontant.setText("Rs " + String.format("%,.0f", s.montantTotal));
            lblActifs.setText(String.valueOf(s.bonsActifs));
            lblUtilises.setText(String.valueOf(s.bonsUtilises));
        });
        task.setOnFailed(e -> {});
        new Thread(task).start();

        return row;
    }

    // ── Tabs ──────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TabPane buildTabs() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Tab Demandes
        ObservableList<VoucherDAO.DemandeRow> dRows = FXCollections.observableArrayList();
        TableView<VoucherDAO.DemandeRow> tblD = new TableView<>(dRows);
        tblD.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblD.setStyle("-fx-font-size:12;");
        tblD.getColumns().addAll(
            colD("Référence",  r -> r.reference,                       140),
            colD("Facture",    r -> nvl(r.invoiceReference),           130),
            colD("Nb bons",    r -> String.valueOf(r.nombreBons),       80),
            colD("Montant",    r -> "Rs " + String.format("%,.0f", r.montant), 110),
            colD("Statut",     r -> r.statut,                          120),
            colD("Date",       r -> tsStr(r.dateCreation),             140)
        );
        loadDemandes(dRows);

        // Tab Bons
        ObservableList<VoucherDAO.BonRow> bRows = FXCollections.observableArrayList();
        TableView<VoucherDAO.BonRow> tblB = new TableView<>(bRows);
        tblB.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblB.setStyle("-fx-font-size:12;");
        tblB.getColumns().addAll(
            colB("Code",        r -> r.code,                            200),
            colB("Valeur",      r -> "Rs " + String.format("%,.0f", r.valeur), 100),
            colB("Statut",      r -> r.statut,                          110),
            colB("Émission",    r -> tsStr(r.dateEmission),             130),
            colB("Expiration",  r -> tsStr(r.dateExpiration),           130),
            colB("Réf demande", r -> nvl(r.demandeRef),                 130)
        );
        loadBons(bRows);

        // Tab Timeline
        ObservableList<VoucherDAO.TimelineEvent> tRows = FXCollections.observableArrayList();
        TableView<VoucherDAO.TimelineEvent> tblT = new TableView<>(tRows);
        tblT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tblT.setStyle("-fx-font-size:12;");
        tblT.getColumns().addAll(
            colT("Date",      r -> tsStr(r.dateEvt), 150),
            colT("Action",    r -> r.action,         180),
            colT("Par",       r -> nvl(r.username),  130),
            colT("Détails",   r -> nvl(r.contexte),  250)
        );
        loadTimeline(tRows);

        VBox dCard = wrapTable(tblD);
        VBox bCard = wrapTable(tblB);
        VBox tCard = wrapTable(tblT);

        tp.getTabs().addAll(
            new Tab("📋 Demandes ("  + "…" + ")", dCard),
            new Tab("🎟 Bons",                    bCard),
            new Tab("📅 Timeline",                tCard)
        );

        // Mettre à jour le label des onglets au chargement
        Task<List<VoucherDAO.DemandeRow>> countTask = new Task<>() {
            @Override protected List<VoucherDAO.DemandeRow> call() throws Exception {
                return VoucherDAO.getDemandesByClient(client.getClientId());
            }
        };
        countTask.setOnSucceeded(e -> tp.getTabs().get(0).setText("📋 Demandes (" + countTask.getValue().size() + ")"));
        new Thread(countTask).start();

        return tp;
    }

    // ── Data loaders ──────────────────────────────────────────────────────────

    private void loadDemandes(ObservableList<VoucherDAO.DemandeRow> rows) {
        Task<List<VoucherDAO.DemandeRow>> t = new Task<>() {
            @Override protected List<VoucherDAO.DemandeRow> call() throws Exception {
                return VoucherDAO.getDemandesByClient(client.getClientId());
            }
        };
        t.setOnSucceeded(e -> rows.setAll(t.getValue()));
        new Thread(t).start();
    }

    private void loadBons(ObservableList<VoucherDAO.BonRow> rows) {
        Task<List<VoucherDAO.BonRow>> t = new Task<>() {
            @Override protected List<VoucherDAO.BonRow> call() throws Exception {
                return VoucherDAO.getBonsByClient(client.getClientId());
            }
        };
        t.setOnSucceeded(e -> rows.setAll(t.getValue()));
        new Thread(t).start();
    }

    private void loadTimeline(ObservableList<VoucherDAO.TimelineEvent> rows) {
        Task<List<VoucherDAO.TimelineEvent>> t = new Task<>() {
            @Override protected List<VoucherDAO.TimelineEvent> call() throws Exception {
                return VoucherDAO.getTimelineByClient(client.getClientId(), 50);
            }
        };
        t.setOnSucceeded(e -> rows.setAll(t.getValue()));
        new Thread(t).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private VBox wrapTable(TableView<?> tbl) {
        VBox c = new VBox(0, tbl);
        c.setStyle("-fx-background-color:white;");
        VBox.setVgrow(tbl, Priority.ALWAYS);
        tbl.setPrefHeight(280);
        return c;
    }

    private VBox kpiCard(String title, Label val, String color) {
        Label t = new Label(title);
        t.setStyle("-fx-font-size:11;-fx-text-fill:#64748b;");
        val.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        VBox c = new VBox(6, t, val);
        c.setPadding(new Insets(16));
        c.setStyle("-fx-background-color:white;-fx-background-radius:10;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);");
        HBox.setHgrow(c, Priority.ALWAYS);
        return c;
    }

    private Label kpiLbl() {
        Label l = new Label("…");
        l.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        return l;
    }

    private TableColumn<VoucherDAO.DemandeRow, String> colD(String title,
            java.util.function.Function<VoucherDAO.DemandeRow, String> fn, double w) {
        TableColumn<VoucherDAO.DemandeRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(fn.apply(p.getValue())));
        c.setPrefWidth(w); return c;
    }

    private TableColumn<VoucherDAO.BonRow, String> colB(String title,
            java.util.function.Function<VoucherDAO.BonRow, String> fn, double w) {
        TableColumn<VoucherDAO.BonRow, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(fn.apply(p.getValue())));
        c.setPrefWidth(w); return c;
    }

    private TableColumn<VoucherDAO.TimelineEvent, String> colT(String title,
            java.util.function.Function<VoucherDAO.TimelineEvent, String> fn, double w) {
        TableColumn<VoucherDAO.TimelineEvent, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(fn.apply(p.getValue())));
        c.setPrefWidth(w); return c;
    }

    private String tsStr(Timestamp ts) {
        return ts != null ? ts.toString().substring(0, 16) : "";
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return (parts[0].charAt(0) + "" + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
