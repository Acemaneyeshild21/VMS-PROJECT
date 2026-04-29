package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.controller.DemandeController;
import pkg.vms.controller.ValidationController;

import java.util.List;

/**
 * Vue Gestion des Demandes — liste + création + workflow.
 * [0]=id [1]=ref [2]=client [3]=montant [4]=type [5]=statut [6]=date [7]=invoice [8]=val_unit
 */
public class GestionDemandeView {

    private final AuthDAO.UserSession    session;
    private final ValidationController  vCtrl = new ValidationController();
    private final DemandeController     dCtrl = new DemandeController();

    private TableView<String[]>       table;
    private pkg.vms.Pager<String[]>   pager;
    private ComboBox<String>          cbFilter;
    private TextField                 tfSearch;
    private Label                     lblCount;

    public GestionDemandeView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");
        // IMPORTANT : buildHeader() DOIT être appelé en premier — il initialise lblCount.
        // buildContent() appelle loadData() qui fait lblCount.setText(…) ; si lblCount est
        // encore null (buildHeader() pas encore appelé) → NullPointerException → vue vide.
        HBox header  = buildHeader();
        VBox content = buildContent();
        root.getChildren().addAll(header, content);
        return root;
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setPadding(new Insets(24, 32, 16, 32));
        h.setAlignment(Pos.CENTER_LEFT);
        h.setStyle("-fx-background-color:#f1f5f9;");

        VBox titles = new VBox(4);
        Label title = new Label("Demandes de bons");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        lblCount = new Label("Chargement…");
        lblCount.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");
        titles.getChildren().addAll(title, lblCount);

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Button btnNew = primaryBtn("+ Nouvelle demande");
        btnNew.setOnAction(e -> showCreateDialog());

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> loadData());

        h.getChildren().addAll(titles, sp, btnRefresh, btnNew);
        return h;
    }

    // ── Content ──────────────────────────────────────────────────────────────

    private VBox buildContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(0, 32, 32, 32));
        VBox.setVgrow(content, Priority.ALWAYS);

        content.getChildren().addAll(buildFilters(), buildTable());
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
        tfSearch.textProperty().addListener((o, ov, nv) -> filterTable(nv));

        cbFilter = new ComboBox<>();
        cbFilter.getItems().addAll("Tous", "EN_ATTENTE_PAIEMENT", "PAYE", "APPROUVE",
                                   "GENERE", "ENVOYE", "REJETE", "ANNULE");
        cbFilter.setValue("Tous");
        cbFilter.setStyle("-fx-font-size:13;");
        cbFilter.setOnAction(e -> {
            String v = cbFilter.getValue();
            loadData("Tous".equals(v) ? null : v);
        });

        h.getChildren().addAll(new Label("Filtre :"), cbFilter, tfSearch);
        return h;
    }

    @SuppressWarnings("unchecked")
    private VBox buildTable() {
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                    + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        VBox.setVgrow(card, Priority.ALWAYS);

        table = new TableView<>();
        pager = new pkg.vms.Pager<>(table);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle("-fx-font-size:13;");
        VBox.setVgrow(table, Priority.ALWAYS);
        table.setPlaceholder(pkg.vms.VmsUI.emptyState("📋", "Aucune demande", "Créez votre première demande de bons cadeaux."));

        table.getColumns().addAll(
            col("Référence",     1, 140),
            col("Facture",       7, 140),
            col("Client",        2, 160),
            col("Val. Unitaire", 8, 100),
            col("Montant Total", 3, 120),
            col("Type",          4, 100),
            col("Date création", 6, 160),
            colStatut("Statut",  5),
            colActions()
        );

        card.getChildren().addAll(table, pager.getBar());
        return card;
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadData() { loadData(null); }

    private void loadData(String statut) {
        lblCount.setText("Chargement…");
        vCtrl.chargerDemandes(statut, data -> {
            pager.setData(data);
            lblCount.setText(data.size() + " demande(s)");
        }, err -> showErr(err));
    }

    private void filterTable(String query) {
        if (query == null || query.isBlank()) {
            loadData();
            return;
        }
        String q = query.toLowerCase();
        vCtrl.chargerDemandes(null, data -> {
            List<String[]> filtered = data.stream()
                .filter(r -> (r[1] != null && r[1].toLowerCase().contains(q))
                          || (r[2] != null && r[2].toLowerCase().contains(q)))
                .toList();
            pager.setData(filtered);
            lblCount.setText(filtered.size() + " résultat(s)");
        }, err -> showErr(err));
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    private void showCreateDialog() {
        dCtrl.chargerDonneesFormulaire(data -> {
            Dialog<ButtonType> dlg = new Dialog<>();
            dlg.setTitle("Nouvelle demande de bons");
            dlg.setHeaderText("Créer une demande");
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(12);
            grid.setVgap(12);
            grid.setPadding(new Insets(20));

            ComboBox<ClientDAO.ClientInfo> cbClient = new ComboBox<>();
            cbClient.getItems().addAll(data.clients);
            cbClient.setPromptText("Sélectionnez un client");
            cbClient.setPrefWidth(280);

            ComboBox<ClientDAO.MagasinInfo> cbMag = new ComboBox<>();
            cbMag.getItems().addAll(data.magasins);
            cbMag.setPromptText("Sélectionnez un magasin");
            cbMag.setPrefWidth(280);

            Spinner<Integer> spNb = new Spinner<>(1, 9999, 10);
            spNb.setEditable(true);
            spNb.setPrefWidth(140);

            TextField tfVal = new TextField("500");
            tfVal.setPrefWidth(140);

            ComboBox<String> cbType = new ComboBox<>();
            cbType.getItems().addAll("Standard", "Premium", "Anniversaire", "Fidélité");
            cbType.setValue("Standard");

            Spinner<Integer> spValid = new Spinner<>(30, 730, 365);
            spValid.setEditable(true);
            spValid.setPrefWidth(140);

            TextField tfMotif = new TextField();
            tfMotif.setPromptText("Motif de la demande (optionnel)");
            tfMotif.setPrefWidth(280);

            TextField tfEmail = new TextField();
            tfEmail.setPromptText("Email destinataire (optionnel)");
            tfEmail.setPrefWidth(280);

            grid.add(lbl("Client *"),          0, 0); grid.add(cbClient,  1, 0);
            grid.add(lbl("Magasin *"),         0, 1); grid.add(cbMag,     1, 1);
            grid.add(lbl("Nb de bons *"),      0, 2); grid.add(spNb,      1, 2);
            grid.add(lbl("Valeur unitaire *"), 0, 3); grid.add(tfVal,     1, 3);
            grid.add(lbl("Type"),              0, 4); grid.add(cbType,    1, 4);
            grid.add(lbl("Validité (jours)"),  0, 5); grid.add(spValid,   1, 5);
            grid.add(lbl("Motif"),             0, 6); grid.add(tfMotif,   1, 6);
            grid.add(lbl("Email destinat."),   0, 7); grid.add(tfEmail,   1, 7);

            dlg.getDialogPane().setContent(grid);

            dlg.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.OK) return;
                if (cbClient.getValue() == null || cbMag.getValue() == null) {
                    showErr("Client et magasin obligatoires.");
                    return;
                }
                double val;
                try { val = Double.parseDouble(tfVal.getText().replace(",", ".")); }
                catch (NumberFormatException ex) { showErr("Valeur unitaire invalide."); return; }

                dCtrl.creerDemande(
                    session.userId,
                    cbClient.getValue().id,
                    spNb.getValue(),
                    val,
                    cbType.getValue(),
                    cbMag.getValue().id,
                    spValid.getValue(),
                    tfMotif.getText(),
                    tfEmail.getText(),
                    id -> { showInfo("Demande créée avec succès ! (ID: " + id + ")"); loadData(); },
                    err -> showErr("Erreur création : " + err)
                );
            });
        }, err -> showErr("Erreur chargement formulaire : " + err));
    }

    private void showActionsMenu(String[] row) {
        int id      = Integer.parseInt(row[0]);
        String statut = row[5];
        String ref  = row[1];

        ContextMenu menu = new ContextMenu();

        switch (statut) {
            case "EN_ATTENTE_PAIEMENT" -> {
                MenuItem mPayer = new MenuItem("✅ Valider le paiement → PAYÉ");
                mPayer.setOnAction(e -> changerStatut(id, "PAYE", ref));
                MenuItem mRejet = new MenuItem("❌ Rejeter");
                mRejet.setOnAction(e -> changerStatut(id, "REJETE", ref));
                menu.getItems().addAll(mPayer, mRejet);
            }
            case "PAYE" -> {
                MenuItem mApp = new MenuItem("✅ Approuver → APPROUVÉ");
                mApp.setOnAction(e -> changerStatut(id, "APPROUVE", ref));
                menu.getItems().add(mApp);
            }
            case "APPROUVE" -> {
                MenuItem mGen = new MenuItem("🎟 Générer les bons");
                mGen.setOnAction(e -> genererBons(id, ref));
                menu.getItems().add(mGen);
            }
            case "GENERE", "ENVOYE" -> {
                // GENERE = bons générés mais email échoué ; ENVOYE = tout OK
                MenuItem mResend = new MenuItem("📧 " + ("GENERE".equals(statut)
                        ? "Envoyer par email (non envoyé)"
                        : "Renvoyer par email"));
                mResend.setOnAction(e -> renvoyerEmail(id, ref));
                menu.getItems().add(mResend);
            }
        }

        // Voir les bons — disponible si bons générés
        if ("GENERE".equals(statut) || "ENVOYE".equals(statut) || "ARCHIVE".equals(statut)) {
            if (!menu.getItems().isEmpty()) menu.getItems().add(new SeparatorMenuItem());
            MenuItem mBons = new MenuItem("🎟 Voir les bons");
            mBons.setOnAction(e -> showBonsDialog(id, ref));
            menu.getItems().add(mBons);
        }

        if (!menu.getItems().isEmpty()) {
            menu.show(table, javafx.geometry.Side.RIGHT, 0, 0);
        }
    }

    private void showBonsDialog(int demandeId, String ref) {
        javafx.concurrent.Task<java.util.List<pkg.vms.DAO.BonDAO.BonInfo>> task = new javafx.concurrent.Task<>() {
            @Override protected java.util.List<pkg.vms.DAO.BonDAO.BonInfo> call() throws Exception {
                return pkg.vms.DAO.BonDAO.getBonsByDemande(demandeId);
            }
        };
        task.setOnSucceeded(ev -> {
            java.util.List<pkg.vms.DAO.BonDAO.BonInfo> bons = task.getValue();
            if (bons.isEmpty()) { showInfo("Aucun bon trouvé pour " + ref + "."); return; }

            javafx.stage.Stage s = new javafx.stage.Stage();
            s.initModality(javafx.stage.Modality.WINDOW_MODAL);
            s.initOwner(table.getScene().getWindow());
            s.setTitle("Bons de la demande " + ref + " (" + bons.size() + " bon(s))");
            s.setWidth(640); s.setHeight(400);

            javafx.collections.ObservableList<pkg.vms.DAO.BonDAO.BonInfo> rows =
                javafx.collections.FXCollections.observableArrayList(bons);
            @SuppressWarnings("unchecked")
            TableView<pkg.vms.DAO.BonDAO.BonInfo> tbl = new TableView<>(rows);
            tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

            TableColumn<pkg.vms.DAO.BonDAO.BonInfo, String> cCode = new TableColumn<>("Code");
            cCode.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().codeUnique));
            cCode.setPrefWidth(200);

            TableColumn<pkg.vms.DAO.BonDAO.BonInfo, String> cVal = new TableColumn<>("Valeur");
            cVal.setCellValueFactory(p -> new SimpleStringProperty("Rs " + String.format("%,.2f", p.getValue().valeur)));
            cVal.setPrefWidth(100);

            TableColumn<pkg.vms.DAO.BonDAO.BonInfo, String> cStat = new TableColumn<>("Statut");
            cStat.setCellValueFactory(p -> new SimpleStringProperty(p.getValue().statut));
            cStat.setPrefWidth(110);

            TableColumn<pkg.vms.DAO.BonDAO.BonInfo, String> cExpir = new TableColumn<>("Expiration");
            cExpir.setCellValueFactory(p -> new SimpleStringProperty(nvl(p.getValue().dateExpiration)));
            cExpir.setPrefWidth(130);

            TableColumn<pkg.vms.DAO.BonDAO.BonInfo, String> cDetail = new TableColumn<>("Détail");
            cDetail.setPrefWidth(90);
            cDetail.setCellFactory(col -> new TableCell<>() {
                final Button btn = new Button("🔍 QR");
                { btn.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:#7c3aed;"
                             + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
                  btn.setOnAction(e -> new BonDetailDialog(getTableView().getItems().get(getIndex())).show(s));
                }
                @Override protected void updateItem(String v, boolean empty) {
                    super.updateItem(v, empty);
                    setGraphic(empty ? null : btn);
                }
            });

            tbl.getColumns().addAll(cCode, cVal, cStat, cExpir, cDetail);

            s.setScene(new javafx.scene.Scene(new BorderPane(tbl)));
            s.centerOnScreen();
            s.show();
        });
        task.setOnFailed(ev -> showErr("Erreur chargement bons : " + task.getException().getMessage()));
        new Thread(task).start();
    }

    private String nvl(String x) { return x != null ? x : ""; }

    private void changerStatut(int id, String statut, String ref) {
        vCtrl.changerStatut(id, statut, session.userId,
            () -> { showInfo("Statut de " + ref + " → " + statut); loadData(); },
            err -> showErr("Erreur : " + err)
        );
    }

    private void genererBons(int demandeId, String ref) {
        ProgressDialog prog = new ProgressDialog("Génération des bons — " + ref);
        prog.show();

        dCtrl.genererBons(demandeId, session.userId,
            arr -> prog.update(arr[0] + "%", arr[1]),
            nb  -> { prog.close(); showInfo(nb + " bons générés et envoyés pour " + ref + "."); loadData(); },
            err -> { prog.close(); showErr("Erreur génération : " + err); },
            nb  -> { prog.close();
                     showInfo(nb + " bons générés pour " + ref + ".\nL'envoi email a échoué — vérifiez la config SMTP."); loadData(); }
        );
    }

    private void renvoyerEmail(int demandeId, String ref) {
        dCtrl.renvoyerEmail(demandeId, session.userId,
            () -> showInfo("Email renvoyé pour " + ref),
            err -> showErr("Erreur renvoi : " + err)
        );
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

    private TableColumn<String[], String> colStatut(String title, int idx) {
        TableColumn<String[], String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > idx ? p.getValue()[idx] : ""));
        c.setPrefWidth(130);
        c.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setText(null);
                setStyle("");
                setGraphic(empty || v == null || v.isBlank() ? null : pkg.vms.VmsUI.badgeCell(v));
            }
        });
        return c;
    }

    private TableColumn<String[], String> colActions() {
        TableColumn<String[], String> c = new TableColumn<>("Actions");
        c.setPrefWidth(90);
        c.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("⋯ Actions");
            { btn.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:#e2e8f0;"
                         + "-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> {
                  String[] row = getTableView().getItems().get(getIndex());
                  showActionsMenu(row);
              });
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });
        return c;
    }

    private Label lbl(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private Button primaryBtn(String t) {
        Button b = new Button(t);
        b.getStyleClass().add("btn-primary");
        return b;
    }

    private Button outlineBtn(String t) {
        Button b = new Button(t);
        b.getStyleClass().add("btn-outline");
        return b;
    }

    private void showErr(String msg)  { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
