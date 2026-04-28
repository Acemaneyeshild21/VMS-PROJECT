package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.ValidationController;

/**
 * Vue Validation — approbation et changement de statut des demandes.
 */
public class ValidationView {

    private final AuthDAO.UserSession   session;
    private final ValidationController ctrl = new ValidationController();

    private final ObservableList<String[]> rows = FXCollections.observableArrayList();
    private TableView<String[]>            table;
    private Label                          lblCount;
    private ComboBox<String>               cbStatut;

    public ValidationView(AuthDAO.UserSession session) { this.session = session; }

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
        Label title = new Label("Validation des demandes");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCount = new Label("Chargement…");
        lblCount.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(title, lblCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(titles, sp, btnRefresh);
        return h;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Onglets de statut
        HBox tabs = buildStatutTabs();

        // Table
        VBox tableCard = buildTable();

        content.getChildren().addAll(tabs, tableCard);
        loadData();
        return content;
    }

    private HBox buildStatutTabs() {
        HBox h = new HBox(8);

        String[] statuts = {"Tous", "EN_ATTENTE_PAIEMENT", "PAYE", "APPROUVE", "ENVOYE", "REJETE"};
        String[] labels  = {"Tout", "En attente", "Payé", "Approuvé", "Envoyé", "Rejeté"};
        String[] colors  = {"#1e293b","#d97706","#2563eb","#16a34a","#7c3aed","#dc2626"};

        ToggleGroup tg = new ToggleGroup();
        for (int i = 0; i < statuts.length; i++) {
            final String s = statuts[i];
            ToggleButton tb = new ToggleButton(labels[i]);
            tb.setToggleGroup(tg);
            tb.setStyle("-fx-background-color:white;-fx-border-color:#d1d5db;-fx-border-radius:8;"
                      + "-fx-background-radius:8;-fx-font-size:12;-fx-padding:6 14;-fx-cursor:hand;");
            if (i == 0) {
                tb.setSelected(true);
                tb.setStyle("-fx-background-color:#1e293b;-fx-text-fill:white;-fx-border-radius:8;"
                          + "-fx-background-radius:8;-fx-font-size:12;-fx-padding:6 14;-fx-cursor:hand;");
            }
            final String color = colors[i];
            tb.selectedProperty().addListener((o, ov, nv) -> {
                if (nv) {
                    tb.setStyle("-fx-background-color:" + color + ";-fx-text-fill:white;-fx-border-radius:8;"
                              + "-fx-background-radius:8;-fx-font-size:12;-fx-padding:6 14;-fx-cursor:hand;");
                    loadData("Tous".equals(s) ? null : s);
                } else {
                    tb.setStyle("-fx-background-color:white;-fx-border-color:#d1d5db;-fx-border-radius:8;"
                              + "-fx-background-radius:8;-fx-font-size:12;-fx-padding:6 14;-fx-cursor:hand;");
                }
            });
            h.getChildren().add(tb);
        }
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
        table.setPlaceholder(new Label("Aucune demande pour ce filtre."));

        table.getColumns().addAll(
            col("Référence",    1, 140),
            col("Facture",      7, 130),
            col("Client",       2, 150),
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
        ctrl.chargerDemandes(statut, data -> {
            rows.setAll(data);
            lblCount.setText(data.size() + " demande(s)");
        }, err -> showErr(err));
    }

    private void showActions(String[] row) {
        int    id     = Integer.parseInt(row[0]);
        String statut = row[5];
        String ref    = row[1];

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Action sur " + ref);
        confirm.setHeaderText("Changer le statut de la demande");

        ComboBox<String> cbNext = new ComboBox<>();
        switch (statut) {
            case "EN_ATTENTE_PAIEMENT" -> cbNext.getItems().addAll("PAYE", "REJETE", "ANNULE");
            case "PAYE"                -> cbNext.getItems().addAll("APPROUVE", "REJETE");
            case "APPROUVE"            -> cbNext.getItems().addAll("GENERE");
            default                    -> { showInfo("Aucune action disponible pour : " + statut); return; }
        }
        cbNext.getSelectionModel().selectFirst();

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(20));
        g.add(new Label("Référence : " + ref), 0, 0, 2, 1);
        g.add(new Label("Statut actuel : "), 0, 1);
        g.add(new Label(statut), 1, 1);
        g.add(new Label("Nouveau statut :"), 0, 2);
        g.add(cbNext, 1, 2);

        confirm.getDialogPane().setContent(g);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String next = cbNext.getValue();
            ctrl.changerStatut(id, next, session.userId,
                () -> { showInfo("Statut de " + ref + " → " + next); loadData(); },
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
        c.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        c.setPrefWidth(140);
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
            final Button btn = new Button("Changer statut");
            { btn.setStyle("-fx-font-size:11;-fx-padding:5 10;-fx-background-color:#2563eb;"
                         + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> showActions(getTableView().getItems().get(getIndex())));
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

    private void showErr(String msg)  { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
