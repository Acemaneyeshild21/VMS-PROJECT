package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.BonController;
import pkg.vms.controller.ParametresController;
import pkg.vms.controller.ValidationController;

import java.io.File;
import java.util.List;

/**
 * Vue Archives — consultation et export des demandes archivées.
 */
public class ArchivesView {

    private final AuthDAO.UserSession   session;
    private final ValidationController  vCtrl = new ValidationController();
    private final BonController         bCtrl = new BonController();
    private final ParametresController  pCtrl = new ParametresController();

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private TableView<String[]>            table;
    private Label                          lblCount;
    private TextField                      tfSearch;

    public ArchivesView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");
        root.getChildren().addAll(buildHeader(), buildContent());
        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setPadding(new Insets(24, 32, 16, 32));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:#f1f5f9;");

        VBox titles = new VBox(4);
        Label title = new Label("Archives");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCount = new Label("Chargement…");
        lblCount.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(title, lblCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnExport = outlineBtn("📤 Exporter Excel");
        btnExport.setOnAction(e -> exporterArchives());

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(titles, sp, btnRefresh, btnExport);
        return h;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Barre de recherche + résumé info
        HBox topRow = buildTopRow();
        VBox statsRow = buildStatsRow();
        VBox tableCard = buildTable();

        content.getChildren().addAll(topRow, statsRow, tableCard);
        loadData();
        return content;
    }

    private HBox buildTopRow() {
        tfSearch = new TextField();
        tfSearch.setPromptText("🔍 Rechercher dans les archives…");
        tfSearch.setPrefWidth(360);
        tfSearch.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;");
        tfSearch.textProperty().addListener((o, ov, nv) -> filterRows(nv));

        HBox h = new HBox(12, tfSearch);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox buildStatsRow() {
        VBox card = new VBox(8);
        card.setPadding(new Insets(16, 20, 16, 20));
        card.setStyle("-fx-background-color:#fef3c7;-fx-background-radius:10;"
                    + "-fx-border-color:#fde68a;-fx-border-radius:10;");

        Label info = new Label("📁  Les archives regroupent toutes les demandes avec statut ARCHIVE, "
                + "REJETE ou ANNULE. Elles sont en lecture seule.");
        info.setStyle("-fx-font-size:12;-fx-text-fill:#92400e;");
        info.setWrapText(true);

        card.getChildren().add(info);
        return card;
    }

    @SuppressWarnings("unchecked")
    private VBox buildTable() {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:13;");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(new Label("Aucune entrée archivée."));

        table.getColumns().addAll(
            col("Référence",    1, 140),
            col("Facture",      7, 130),
            col("Client",       2, 160),
            col("Montant (Rs)", 3, 120),
            col("Type",         4,  90),
            col("Date",         6, 150),
            colStatut(5)
        );

        card.getChildren().add(table);
        return card;
    }

    // ── Data ──────────────────────────────────────────────────────────────────

    private void loadData() {
        lblCount.setText("Chargement…");
        // Charger ARCHIVE + REJETE + ANNULE
        vCtrl.chargerDemandes(null, data -> {
            List<String[]> archives = data.stream()
                .filter(r -> r[5] != null && (r[5].equals("ARCHIVE")
                          || r[5].equals("REJETE") || r[5].equals("ANNULE")))
                .toList();
            rows.setAll(archives);
            lblCount.setText(archives.size() + " entrée(s) archivée(s)");
        }, err -> showErr("Erreur chargement : " + err));
    }

    private void filterRows(String q) {
        if (q == null || q.isBlank()) { loadData(); return; }
        String ql = q.toLowerCase();
        vCtrl.chargerDemandes(null, data -> {
            List<String[]> f = data.stream()
                .filter(r -> r[5] != null && (r[5].equals("ARCHIVE")
                          || r[5].equals("REJETE") || r[5].equals("ANNULE")))
                .filter(r -> (r[1] != null && r[1].toLowerCase().contains(ql))
                          || (r[2] != null && r[2].toLowerCase().contains(ql))
                          || (r[7] != null && r[7].toLowerCase().contains(ql)))
                .toList();
            rows.setAll(f);
            lblCount.setText(f.size() + " résultat(s)");
        }, err -> showErr("Erreur : " + err));
    }

    private void exporterArchives() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les archives");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("archives_vms.xlsx");
        File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;

        pCtrl.exporterDemandes(f.getAbsolutePath(),
            () -> showInfo("Export réussi : " + f.getAbsolutePath()),
            err -> showErr("Erreur export : " + err));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TableColumn<String[], String> col(String title, int idx, double w) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        c.setPrefWidth(w);
        return c;
    }

    private TableColumn<String[], String> colStatut(int idx) {
        TableColumn<String[], String> c = new TableColumn<>("Statut");
        c.setPrefWidth(120);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                String bg = switch (v) {
                    case "ARCHIVE" -> "#f1f5f9";
                    case "REJETE"  -> "#fee2e2";
                    case "ANNULE"  -> "#fef3c7";
                    default        -> "#f8fafc";
                };
                setStyle("-fx-background-color:" + bg + ";-fx-background-radius:4;"
                       + "-fx-alignment:CENTER;-fx-font-weight:bold;-fx-font-size:11;");
            }
        });
        return c;
    }

    private Button outlineBtn(String t) {
        Button b = new Button(t);
        b.getStyleClass().add("btn-outline");
        return b;
    }

    private void showErr(String msg)  { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
