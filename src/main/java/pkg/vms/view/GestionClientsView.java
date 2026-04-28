package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import pkg.vms.Client;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.ClientController;

import java.io.File;
import java.util.List;

/**
 * Vue Gestion des Clients — CRUD complet.
 */
public class GestionClientsView {

    private final AuthDAO.UserSession session;
    private final ClientController    ctrl = new ClientController();

    private final ObservableList<Client> rows = FXCollections.observableArrayList();
    private TableView<Client>            table;
    private Label                        lblCount;
    private TextField                    tfSearch;

    public GestionClientsView(AuthDAO.UserSession session) { this.session = session; }

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
        Label title = new Label("Clients");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCount = new Label("Chargement…");
        lblCount.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(title, lblCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnExport = outlineBtn("📤 Exporter Excel");
        btnExport.setOnAction(e -> exporterClients());

        Button btnNew = primaryBtn("+ Nouveau client");
        btnNew.setOnAction(e -> showFormDialog(null));

        Button btnRefresh = outlineBtn("↻");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(titles, sp, btnRefresh, btnExport, btnNew);
        return h;
    }

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        tfSearch = new TextField();
        tfSearch.setPromptText("🔍 Rechercher par nom, email ou société…");
        tfSearch.setPrefWidth(350);
        tfSearch.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                + "-fx-border-radius:8;-fx-background-radius:8;");
        tfSearch.textProperty().addListener((o, ov, nv) -> rechercherClients(nv));

        VBox tableCard = buildTable();
        content.getChildren().addAll(tfSearch, tableCard);
        loadData();
        return content;
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
        table.setPlaceholder(new Label("Aucun client trouvé."));

        TableColumn<Client, String> cNom = colStr("Nom", c -> c.getName(), 160);
        TableColumn<Client, String> cEmail = colStr("Email", c -> c.getEmail(), 200);
        TableColumn<Client, String> cTel = colStr("Téléphone", c -> c.getContactNumber(), 130);
        TableColumn<Client, String> cSoc = colStr("Société", c -> c.getCompany(), 150);
        TableColumn<Client, String> cDate = colStr("Création",
            c -> c.getDateCreation() != null ? c.getDateCreation().toString().substring(0,10) : "", 110);
        TableColumn<Client, String> cActif = colStr("Actif",
            c -> c.isActif() ? "✓ Actif" : "✗ Inactif", 80);

        cActif.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || v == null) { setText(null); setStyle(""); return; }
                setText(v);
                boolean actif = v.startsWith("✓");
                setStyle("-fx-text-fill:" + (actif ? "#16a34a" : "#dc2626") + ";"
                       + "-fx-font-weight:bold;-fx-font-size:12;");
            }
        });

        TableColumn<Client, String> cActions = new TableColumn<>("Actions");
        cActions.setPrefWidth(150);
        cActions.setCellFactory(col -> new TableCell<>() {
            final HBox btns = new HBox(6,
                actionBtn("✏ Modifier", "#2563eb"),
                actionBtn("🗑 Supprimer", "#dc2626")
            );
            {
                ((Button)btns.getChildren().get(0)).setOnAction(e ->
                    showFormDialog(getTableView().getItems().get(getIndex())));
                ((Button)btns.getChildren().get(1)).setOnAction(e ->
                    supprimerClient(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btns);
            }
        });

        table.getColumns().addAll(cNom, cEmail, cTel, cSoc, cDate, cActif, cActions);
        return card;
    }

    private void loadData() {
        lblCount.setText("Chargement…");
        ctrl.chargerClients(data -> {
            rows.setAll(data);
            lblCount.setText(data.size() + " client(s)");
        }, err -> showErr(err));
    }

    private void rechercherClients(String q) {
        if (q == null || q.isBlank()) { loadData(); return; }
        ctrl.rechercherClients(q, data -> {
            rows.setAll(data);
            lblCount.setText(data.size() + " résultat(s)");
        }, err -> showErr(err));
    }

    private void showFormDialog(Client existing) {
        boolean isNew = existing == null;
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle(isNew ? "Nouveau client" : "Modifier le client");
        dlg.setHeaderText(isNew ? "Créer un nouveau client" : "Modifier : " + existing.getName());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12); grid.setPadding(new Insets(20));

        TextField tfNom    = field(isNew ? "" : existing.getName());
        TextField tfEmail  = field(isNew ? "" : existing.getEmail());
        TextField tfTel    = field(isNew ? "" : (existing.getContactNumber() != null ? existing.getContactNumber() : ""));
        TextField tfSoc    = field(isNew ? "" : (existing.getCompany()       != null ? existing.getCompany()       : ""));

        grid.add(lbl("Nom *"),       0, 0); grid.add(tfNom,   1, 0);
        grid.add(lbl("Email *"),     0, 1); grid.add(tfEmail, 1, 1);
        grid.add(lbl("Téléphone"),   0, 2); grid.add(tfTel,   1, 2);
        grid.add(lbl("Société"),     0, 3); grid.add(tfSoc,   1, 3);

        dlg.getDialogPane().setContent(grid);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            if (tfNom.getText().isBlank() || tfEmail.getText().isBlank()) {
                showErr("Nom et Email sont obligatoires."); return;
            }
            Client c = isNew ? new Client(tfNom.getText(), tfEmail.getText(), tfTel.getText(), tfSoc.getText())
                             : existing;
            if (!isNew) {
                c.setName(tfNom.getText());
                c.setEmail(tfEmail.getText());
                c.setContactNumber(tfTel.getText());
                c.setCompany(tfSoc.getText());
            }
            if (isNew) {
                ctrl.creerClient(c, ok -> { if (ok) { showInfo("Client créé."); loadData(); }
                                            else showErr("Erreur création."); },
                    err -> showErr(err));
            } else {
                ctrl.modifierClient(c, ok -> { if (ok) { showInfo("Client mis à jour."); loadData(); }
                                               else showErr("Erreur modification."); },
                    err -> showErr(err));
            }
        });
    }

    private void supprimerClient(Client c) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Supprimer définitivement " + c.getName() + " ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            ctrl.supprimerClient(c.getClientId(),
                ok -> { showInfo("Client supprimé."); loadData(); },
                err -> showErr(err));
        });
    }

    private void exporterClients() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exporter les clients");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
        fc.setInitialFileName("clients_vms.xlsx");
        File f = fc.showSaveDialog(table.getScene().getWindow());
        if (f == null) return;
        ctrl.exporterClients(f.getAbsolutePath(),
            () -> showInfo("Export réussi : " + f.getAbsolutePath()),
            err -> showErr("Erreur export : " + err));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private <T> TableColumn<Client, T> colStr(String title,
            java.util.function.Function<Client, T> fn, double w) {
        TableColumn<Client, T> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new javafx.beans.property.SimpleObjectProperty<>(fn.apply(p.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private Button actionBtn(String t, String color) {
        Button b = new Button(t);
        b.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:" + color + ";"
                 + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
        return b;
    }

    private TextField field(String val) {
        TextField tf = new TextField(val);
        tf.setPrefWidth(260);
        tf.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                  + "-fx-border-radius:8;-fx-background-radius:8;");
        return tf;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private Button primaryBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:#dc2626;-fx-text-fill:white;-fx-font-size:13;"
                 + "-fx-font-weight:bold;-fx-padding:9 18;-fx-background-radius:8;-fx-cursor:hand;");
        return b;
    }

    private Button outlineBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color:white;-fx-text-fill:#374151;-fx-font-size:13;"
                 + "-fx-padding:9 18;-fx-background-radius:8;-fx-border-color:#d1d5db;"
                 + "-fx-border-radius:8;-fx-cursor:hand;");
        return b;
    }

    private void showErr(String msg)  { new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait(); }
    private void showInfo(String msg) { new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait(); }
}
