package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.controller.BonController;
import pkg.vms.controller.ValidationController;

import java.util.List;

/**
 * Vue Gestion des Bons — liste des bons générés, archivage.
 */
public class GestionBonsView {

    private final AuthDAO.UserSession session;
    private final BonController       ctrl  = new BonController();
    private final ValidationController vCtrl = new ValidationController();

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private TableView<String[]>            table;
    private Label                          lblCount;
    private TextField                      tfSearch;

    public GestionBonsView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");
        root.getChildren().addAll(buildHeader(), buildContent());
        return root;
    }

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setPadding(new Insets(24, 32, 16, 32));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:#f1f5f9;");

        VBox titles = new VBox(4);
        Label title = new Label("Bons cadeaux");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCount = new Label("Chargement…");
        lblCount.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(title, lblCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnArchiveAll = new Button("📁 Archiver expirés");
        btnArchiveAll.setStyle("-fx-background-color:#d97706;-fx-text-fill:white;-fx-font-size:13;"
                + "-fx-font-weight:bold;-fx-padding:9 16;-fx-background-radius:8;-fx-cursor:hand;");
        btnArchiveAll.setOnAction(e -> archiverExpires());

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(titles, sp, btnRefresh, btnArchiveAll);
        return h;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Filtre statut
        HBox filters = buildFilters();
        VBox tableCard = buildTable();

        content.getChildren().addAll(filters, tableCard);
        loadData();
        return content;
    }

    private HBox buildFilters() {
        HBox h = new HBox(12);
        h.setAlignment(Pos.CENTER_LEFT);

        tfSearch = new TextField();
        tfSearch.setPromptText("🔍 Rechercher par référence ou client…");
        tfSearch.setPrefWidth(300);
        tfSearch.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;");
        tfSearch.textProperty().addListener((o, ov, nv) -> filterRows(nv));

        ComboBox<String> cbStatut = new ComboBox<>();
        cbStatut.getItems().addAll("Toutes demandes", "GENERE", "ENVOYE", "ARCHIVE");
        cbStatut.setValue("Toutes demandes");
        cbStatut.setOnAction(e -> {
            String v = cbStatut.getValue();
            loadData("Toutes demandes".equals(v) ? null : v);
        });

        h.getChildren().addAll(new Label("Statut :"), cbStatut, tfSearch);
        return h;
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
        table.setPlaceholder(new Label("Aucun bon trouvé."));

        table.getColumns().addAll(
            col("Référence",    1, 140),
            col("Facture",      7, 130),
            col("Client",       2, 160),
            col("Montant (Rs)", 3, 120),
            col("Type",         4, 90),
            col("Date",         6, 150),
            colStatut(5),
            colActions()
        );

        card.getChildren().add(table);
        return card;
    }

    private void loadData() { loadData(null); }

    private void loadData(String statut) {
        lblCount.setText("Chargement…");
        vCtrl.chargerDemandes(statut, data -> {
            // On ne montre que les demandes qui ont des bons (GENERE, ENVOYE, ARCHIVE)
            List<String[]> bons = data.stream()
                .filter(r -> r[5] != null && (r[5].equals("GENERE") || r[5].equals("ENVOYE")
                          || r[5].equals("ARCHIVE") || statut != null))
                .toList();
            rows.setAll(bons);
            lblCount.setText(bons.size() + " demande(s) avec bons");
        }, err -> showErr(err));
    }

    private void filterRows(String q) {
        if (q == null || q.isBlank()) { loadData(); return; }
        String ql = q.toLowerCase();
        vCtrl.chargerDemandes(null, data -> {
            List<String[]> f = data.stream()
                .filter(r -> (r[1] != null && r[1].toLowerCase().contains(ql))
                          || (r[2] != null && r[2].toLowerCase().contains(ql)))
                .toList();
            rows.setAll(f);
            lblCount.setText(f.size() + " résultat(s)");
        }, err -> showErr(err));
    }

    private void archiverExpires() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Archiver toutes les demandes expirées ?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Archivage groupé");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            ctrl.archiverDemandesExpirees(session.userId,
                nb -> { showInfo(nb + " demande(s) archivée(s)."); loadData(); },
                err -> showErr("Erreur : " + err));
        });
    }

    private void archiverDemande(int id, String ref) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Archiver la demande " + ref + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            ctrl.archiverDemande(id, session.userId,
                () -> { showInfo(ref + " archivée."); loadData(); },
                err -> showErr("Erreur : " + err));
        });
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
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
                setText(null); setStyle("");
                setGraphic(empty || v == null ? null : pkg.vms.VmsUI.badgeCell(v));
            }
        });
        return c;
    }

    private TableColumn<String[], String> colActions() {
        TableColumn<String[], String> c = new TableColumn<>("Actions");
        c.setPrefWidth(110);
        c.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("📁 Archiver");
            { btn.setStyle("-fx-font-size:11;-fx-padding:5 10;-fx-background-color:#64748b;"
                         + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> {
                  String[] row = getTableView().getItems().get(getIndex());
                  archiverDemande(Integer.parseInt(row[0]), row[1]);
              });
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
        return c;
    }

    private Button outlineBtn(String t) {
        Button b = new Button(t);
        b.getStyleClass().add("btn-outline");
        return b;
    }

    private void showErr(String msg) { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
