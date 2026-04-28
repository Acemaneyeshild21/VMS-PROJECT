package pkg.vms.view;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import pkg.vms.DAO.*;
import pkg.vms.controller.ParametresController;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Vue Paramètres — profil, utilisateurs, email, bons, BD, sociétés, magasins, logs.
 */
public class ParametresView {

    private final AuthDAO.UserSession  session;
    private final ParametresController ctrl = new ParametresController();

    public ParametresView(AuthDAO.UserSession session) { this.session = session; }

    public Region build() {
        ScrollPane sp = new ScrollPane(buildContent());
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        return sp;
    }

    private VBox buildContent() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color:#f1f5f9;");
        root.getChildren().addAll(buildHeader(), buildTabs());
        return root;
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        HBox h = new HBox(12);
        h.setPadding(new Insets(24, 32, 16, 32));
        h.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Paramètres");
        title.setStyle("-fx-font-size:22;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        Label sub = new Label("Configuration de l'application VMS");
        sub.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        h.getChildren().add(new VBox(4, title, sub));
        return h;
    }

    // ── Tab pane ──────────────────────────────────────────────────────────────

    private TabPane buildTabs() {
        TabPane tp = new TabPane();
        tp.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tp.setStyle("-fx-background-color:#f1f5f9;");
        tp.setPadding(new Insets(0, 32, 32, 32));

        tp.getTabs().add(tab("👤 Mon Profil",    buildProfilTab()));
        tp.getTabs().add(tab("📧 Email SMTP",     buildEmailTab()));
        tp.getTabs().add(tab("🎟 Bons",           buildBonsTab()));

        boolean isAdmin = "Administrateur".equals(session.role);
        if (isAdmin) {
            tp.getTabs().add(tab("👥 Utilisateurs",  buildUsersTab()));
            tp.getTabs().add(tab("🏢 Sociétés",       buildSocietesTab()));
            tp.getTabs().add(tab("🏪 Magasins",       buildMagasinsTab()));
            tp.getTabs().add(tab("🗄 Base de données", buildDBTab()));
        }

        tp.getTabs().add(tab("📋 Logs d'audit",  buildLogsTab()));
        tp.getTabs().add(tab("📤 Exports",        buildExportsTab()));

        return tp;
    }

    private Tab tab(String title, Region content) {
        Tab t = new Tab(title, content);
        t.setStyle("-fx-font-size:13;");
        return t;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 1 — Profil
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildProfilTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        // Profil info
        VBox profilCard = sectionCard("Informations du compte");

        TextField tfUser  = styledField("");
        TextField tfEmail = styledField("");

        profilCard.getChildren().addAll(
            formRow("Nom d'utilisateur", tfUser),
            formRow("Adresse e-mail",    tfEmail),
            buildRoleBadge()
        );

        Button btnSave = primaryBtn("💾 Sauvegarder le profil");
        btnSave.setOnAction(e -> ctrl.mettreAJourProfil(session.userId,
            tfUser.getText(), tfEmail.getText(),
            ok -> { if (ok) showInfo("Profil mis à jour."); else showErr("Erreur mise à jour."); },
            err -> showErr(err)));

        profilCard.getChildren().add(btnSave);

        // Charger les données
        ctrl.chargerProfil(session.userId, p -> {
            tfUser.setText(p.username  != null ? p.username : "");
            tfEmail.setText(p.email   != null ? p.email    : "");
        }, err -> showErr("Erreur chargement profil : " + err));

        // Changer mot de passe
        VBox pwdCard = sectionCard("Changer le mot de passe");
        PasswordField pfAncien  = pwdField("Mot de passe actuel");
        PasswordField pfNouveau = pwdField("Nouveau mot de passe");
        PasswordField pfConfirm = pwdField("Confirmer le nouveau mot de passe");

        Button btnPwd = outlineBtn("🔑 Changer le mot de passe");
        btnPwd.setOnAction(e -> {
            if (!pfNouveau.getText().equals(pfConfirm.getText())) {
                showErr("Les mots de passe ne correspondent pas."); return;
            }
            if (pfNouveau.getText().length() < 6) {
                showErr("Le mot de passe doit contenir au moins 6 caractères."); return;
            }
            ctrl.changerMotDePasse(session.userId, pfAncien.getText(), pfNouveau.getText(),
                ok -> {
                    if (ok) { showInfo("Mot de passe modifié."); pfAncien.clear(); pfNouveau.clear(); pfConfirm.clear(); }
                    else showErr("Mot de passe actuel incorrect.");
                },
                err -> showErr(err));
        });

        pwdCard.getChildren().addAll(
            formRow("Mot de passe actuel",  pfAncien),
            formRow("Nouveau mot de passe", pfNouveau),
            formRow("Confirmer",            pfConfirm),
            btnPwd
        );

        root.getChildren().addAll(profilCard, pwdCard);
        return root;
    }

    private HBox buildRoleBadge() {
        Label lbl  = fieldLabel("Rôle :");
        Label role = new Label(session.role);
        role.setStyle("-fx-background-color:#dbeafe;-fx-text-fill:#1d4ed8;-fx-font-size:12;"
                    + "-fx-font-weight:bold;-fx-padding:3 10;-fx-background-radius:20;");
        HBox h = new HBox(12, lbl, role);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 2 — Email SMTP
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildEmailTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Configuration SMTP");

        TextField tfServer = styledField("smtp.gmail.com");
        TextField tfPort   = styledField("587");
        TextField tfUser   = styledField("");
        PasswordField pfPwd = pwdField("Mot de passe / App password");
        CheckBox cbTls = new CheckBox("Activer TLS/STARTTLS");
        cbTls.setStyle("-fx-font-size:13;");

        // Charger config
        ctrl.chargerConfigEmail(cfg -> {
            if (cfg.smtpServer   != null) tfServer.setText(cfg.smtpServer);
            tfPort.setText(String.valueOf(cfg.smtpPort > 0 ? cfg.smtpPort : 587));
            if (cfg.smtpUsername != null) tfUser.setText(cfg.smtpUsername);
            if (cfg.smtpPassword != null) pfPwd.setText(cfg.smtpPassword);
            cbTls.setSelected(cfg.tlsEnabled);
        }, err -> showErr("Erreur config email : " + err));

        Button btnSave = primaryBtn("💾 Sauvegarder");
        Button btnTest = outlineBtn("📡 Tester la connexion");

        btnSave.setOnAction(e -> {
            SettingsDAO.EmailSettings s = new SettingsDAO.EmailSettings(
                tfServer.getText().trim(),
                parseIntSafe(tfPort.getText(), 587),
                tfUser.getText().trim(),
                pfPwd.getText(),
                cbTls.isSelected(),
                "", "", ""   // fromEmail, fromName, adminEmail — gérés dans config.properties
            );
            ctrl.sauvegarderConfigEmail(s,
                ok -> { if (ok) showInfo("Config email sauvegardée."); else showErr("Erreur sauvegarde."); },
                err -> showErr(err));
        });

        btnTest.setOnAction(e -> ctrl.testerConnexionEmail(
            ok -> showInfo(ok ? "✓ Connexion SMTP réussie." : "✗ Connexion SMTP échouée."),
            err -> showErr("Erreur test email : " + err)));

        HBox btns = new HBox(12, btnSave, btnTest);

        card.getChildren().addAll(
            formRow("Serveur SMTP",    tfServer),
            formRow("Port",            tfPort),
            formRow("Utilisateur",     tfUser),
            formRow("Mot de passe",    pfPwd),
            cbTls,
            btns
        );

        root.getChildren().add(card);
        return root;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 3 — Config Bons
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildBonsTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Paramètres des bons cadeaux");

        Map<String, TextField> fields = new LinkedHashMap<>();
        String[] keys = {"bon_validite_defaut", "bon_type_defaut", "bon_entreprise", "bon_format_qr", "bon_signature"};
        String[] labels = {"Validité par défaut (jours)", "Type par défaut", "Entreprise / nom d'émission",
                           "Format QR code", "Signature bons"};

        for (int i = 0; i < keys.length; i++) {
            TextField tf = styledField("");
            fields.put(keys[i], tf);
            card.getChildren().add(formRow(labels[i], tf));
        }

        ctrl.chargerConfigBons(cfg -> {
            for (Map.Entry<String, String> e : cfg.entrySet()) {
                TextField tf = fields.get(e.getKey());
                if (tf != null) tf.setText(e.getValue());
            }
        }, err -> showErr("Erreur chargement config bons : " + err));

        Button btnSave = primaryBtn("💾 Sauvegarder");
        btnSave.setOnAction(e -> {
            Map<String, String> m = new LinkedHashMap<>();
            fields.forEach((k, tf) -> m.put(k, tf.getText()));
            ctrl.sauvegarderConfigBons(m,
                () -> showInfo("Configuration bons sauvegardée."),
                err -> showErr(err));
        });

        card.getChildren().add(btnSave);
        root.getChildren().add(card);
        return root;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 4 — Utilisateurs (Admin only)
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildUsersTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Gestion des utilisateurs");

        ObservableList<UserDAO.UserProfile> userRows = FXCollections.observableArrayList();
        TableView<UserDAO.UserProfile> tbl = new TableView<>(userRows);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPrefHeight(260);
        tbl.setStyle("-fx-font-size:12;");

        TableColumn<UserDAO.UserProfile, String> cUser  = colUser("Nom",   u -> u.username,  160);
        TableColumn<UserDAO.UserProfile, String> cEmail = colUser("Email", u -> u.email,     200);
        TableColumn<UserDAO.UserProfile, String> cRole  = colUser("Rôle",  u -> u.role,      140);
        TableColumn<UserDAO.UserProfile, String> cAct   = colUser("ID", u -> String.valueOf(u.userId), 50);

        TableColumn<UserDAO.UserProfile, String> cActions = new TableColumn<>("Actions");
        cActions.setPrefWidth(120);
        cActions.setCellFactory(col -> new TableCell<>() {
            final ComboBox<String> cbRole = new ComboBox<>();
            final Button btn = new Button("Changer rôle");
            { btn.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:#2563eb;"
                         + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> {
                  UserDAO.UserProfile u = getTableView().getItems().get(getIndex());
                  String newRole = cbRole.getValue();
                  if (newRole == null) return;
                  ctrl.changerRoleUtilisateur(u.userId, newRole,
                      () -> { showInfo("Rôle mis à jour."); refreshUsers(userRows); },
                      err -> showErr(err));
              });
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty) { setGraphic(null); return; }
                setGraphic(new HBox(6, cbRole, btn));
                ctrl.chargerRolesDisponibles(roles -> cbRole.setItems(FXCollections.observableArrayList(roles)),
                    err -> {});
            }
        });

        tbl.getColumns().addAll(cUser, cEmail, cRole, cAct, cActions);

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> refreshUsers(userRows));

        Button btnNew = primaryBtn("+ Ajouter un utilisateur");
        btnNew.setOnAction(e -> showNewUserDialog(userRows));

        HBox topRow = new HBox(12, btnRefresh, btnNew);

        card.getChildren().addAll(topRow, tbl);
        root.getChildren().add(card);

        refreshUsers(userRows);
        return root;
    }

    private void refreshUsers(ObservableList<UserDAO.UserProfile> rows) {
        ctrl.chargerUtilisateurs(rows::setAll, err -> showErr(err));
    }

    private void showNewUserDialog(ObservableList<UserDAO.UserProfile> rows) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Nouvel utilisateur");
        dlg.setHeaderText("Créer un compte utilisateur");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfUser  = styledField(""); tfUser.setPromptText("Nom d'utilisateur");
        TextField tfEmail = styledField(""); tfEmail.setPromptText("Email");
        PasswordField pfPwd = pwdField("Mot de passe");
        ComboBox<String> cbRole = new ComboBox<>();
        ctrl.chargerRolesDisponibles(cbRole.getItems()::setAll, err -> {});

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20));
        g.add(fieldLabel("Nom *"),     0, 0); g.add(tfUser,  1, 0);
        g.add(fieldLabel("Email *"),   0, 1); g.add(tfEmail, 1, 1);
        g.add(fieldLabel("Mot de passe *"), 0, 2); g.add(pfPwd, 1, 2);
        g.add(fieldLabel("Rôle *"),    0, 3); g.add(cbRole,  1, 3);

        dlg.getDialogPane().setContent(g);
        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            if (tfUser.getText().isBlank() || tfEmail.getText().isBlank() || pfPwd.getText().isBlank()) {
                showErr("Tous les champs sont obligatoires."); return;
            }
            ctrl.creerUtilisateur(tfUser.getText(), tfEmail.getText(), pfPwd.getText(),
                cbRole.getValue() != null ? cbRole.getValue() : "Employe",
                ok -> { if (ok) { showInfo("Utilisateur créé."); refreshUsers(rows); }
                        else showErr("Erreur création (login ou email déjà pris ?)."); },
                err -> showErr(err));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 5 — Sociétés
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildSocietesTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Gestion des sociétés");

        ObservableList<SocieteDAO.Societe> societeRows = FXCollections.observableArrayList();
        TableView<SocieteDAO.Societe> tbl = new TableView<>(societeRows);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPrefHeight(220);
        tbl.setStyle("-fx-font-size:12;");

        TableColumn<SocieteDAO.Societe, String> cNom   = colSoc("Nom",      s -> s.nom,       180);
        TableColumn<SocieteDAO.Societe, String> cAdr   = colSoc("Adresse",  s -> s.adresse,   180);
        TableColumn<SocieteDAO.Societe, String> cTel   = colSoc("Tél",      s -> s.telephone, 120);
        TableColumn<SocieteDAO.Societe, String> cEmail = colSoc("Email",    s -> s.email,     160);
        TableColumn<SocieteDAO.Societe, String> cDel = new TableColumn<>("Action");
        cDel.setPrefWidth(100);
        cDel.setCellFactory(col -> new TableCell<>() {
            final Button btn = new Button("🗑 Supprimer");
            { btn.setStyle("-fx-font-size:11;-fx-padding:4 8;-fx-background-color:#dc2626;"
                         + "-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
              btn.setOnAction(e -> {
                  SocieteDAO.Societe s = getTableView().getItems().get(getIndex());
                  ctrl.supprimerSociete(s.societeId,
                      () -> refreshSocietes(societeRows),
                      err -> showErr(err));
              });
            }
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        tbl.getColumns().addAll(cNom, cAdr, cTel, cEmail, cDel);

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> refreshSocietes(societeRows));

        Button btnAdd = primaryBtn("+ Ajouter société");
        btnAdd.setOnAction(e -> showNewSocieteDialog(societeRows));

        card.getChildren().addAll(new HBox(12, btnRefresh, btnAdd), tbl);
        root.getChildren().add(card);

        refreshSocietes(societeRows);
        return root;
    }

    private void refreshSocietes(ObservableList<SocieteDAO.Societe> rows) {
        ctrl.chargerSocietes(rows::setAll, err -> showErr(err));
    }

    private void showNewSocieteDialog(ObservableList<SocieteDAO.Societe> rows) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Nouvelle société");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfNom = styledField(""); tfNom.setPromptText("Nom");
        TextField tfAdr = styledField(""); tfAdr.setPromptText("Adresse");
        TextField tfTel = styledField(""); tfTel.setPromptText("Téléphone");
        TextField tfEmail = styledField(""); tfEmail.setPromptText("Email");

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20));
        g.add(fieldLabel("Nom *"),    0, 0); g.add(tfNom,   1, 0);
        g.add(fieldLabel("Adresse"),  0, 1); g.add(tfAdr,   1, 1);
        g.add(fieldLabel("Tél"),      0, 2); g.add(tfTel,   1, 2);
        g.add(fieldLabel("Email"),    0, 3); g.add(tfEmail, 1, 3);
        dlg.getDialogPane().setContent(g);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK || tfNom.getText().isBlank()) return;
            ctrl.ajouterSociete(tfNom.getText(), tfAdr.getText(), tfTel.getText(), tfEmail.getText(),
                () -> { showInfo("Société ajoutée."); refreshSocietes(rows); },
                err -> showErr(err));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 6 — Magasins
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildMagasinsTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Points de vente / Magasins");

        ObservableList<MagasinDAO.Magasin> magRows = FXCollections.observableArrayList();
        TableView<MagasinDAO.Magasin> tbl = new TableView<>(magRows);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPrefHeight(220);
        tbl.setStyle("-fx-font-size:12;");

        TableColumn<MagasinDAO.Magasin, String> cNom = colMag("Nom magasin", m -> m.nomMagasin, 220);
        TableColumn<MagasinDAO.Magasin, String> cAdr = colMag("Adresse",     m -> m.adresse,    240);
        tbl.getColumns().addAll(cNom, cAdr);

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> refreshMagasins(magRows));

        Button btnAdd = primaryBtn("+ Ajouter un magasin");
        btnAdd.setOnAction(e -> showNewMagasinDialog(magRows));

        card.getChildren().addAll(new HBox(12, btnRefresh, btnAdd), tbl);
        root.getChildren().add(card);

        refreshMagasins(magRows);
        return root;
    }

    private void refreshMagasins(ObservableList<MagasinDAO.Magasin> rows) {
        ctrl.chargerMagasins(rows::setAll, err -> showErr(err));
    }

    private void showNewMagasinDialog(ObservableList<MagasinDAO.Magasin> rows) {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Nouveau point de vente");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfNom = styledField(""); tfNom.setPromptText("Nom");
        TextField tfAdr = styledField(""); tfAdr.setPromptText("Adresse");

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(12); g.setPadding(new Insets(20));
        g.add(fieldLabel("Nom *"),   0, 0); g.add(tfNom, 1, 0);
        g.add(fieldLabel("Adresse"), 0, 1); g.add(tfAdr, 1, 1);
        dlg.getDialogPane().setContent(g);

        dlg.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK || tfNom.getText().isBlank()) return;
            ctrl.ajouterMagasin(tfNom.getText(), tfAdr.getText(),
                ok -> { showInfo("Magasin ajouté."); refreshMagasins(rows); },
                err -> showErr(err));
        });
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 7 — Base de données
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildDBTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Monitoring base de données");

        Label lblInfo = new Label("Chargement…");
        lblInfo.setStyle("-fx-font-size:12;-fx-text-fill:#64748b;");

        ObservableList<Object[]> dbRows = FXCollections.observableArrayList();
        TableView<Object[]> tbl = new TableView<>(dbRows);
        tbl.setPrefHeight(200);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setStyle("-fx-font-size:12;");

        TableColumn<Object[], String> cTable = new TableColumn<>("Table");
        cTable.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > 0 ? String.valueOf(p.getValue()[0]) : ""));
        cTable.setPrefWidth(200);

        TableColumn<Object[], String> cNb = new TableColumn<>("Lignes");
        cNb.setCellValueFactory(p -> new SimpleStringProperty(
            p.getValue().length > 1 ? String.valueOf(p.getValue()[1]) : ""));
        cNb.setPrefWidth(100);

        tbl.getColumns().addAll(cTable, cNb);

        Runnable loadDB = () -> ctrl.chargerStatsDB(stats -> {
            lblInfo.setText(stats.info);
            dbRows.setAll(stats.rows);
        }, err -> showErr(err));

        Button btnRefresh = outlineBtn("↻ Actualiser");
        btnRefresh.setOnAction(e -> loadDB.run());

        Button btnTest = outlineBtn("🔌 Tester connexion");
        btnTest.setOnAction(e -> ctrl.testerConnexionDB(
            ok -> showInfo(ok ? "✓ Connexion PostgreSQL OK." : "✗ Connexion échouée."),
            err -> showErr("Erreur connexion : " + err)));

        Button btnVacuum = new Button("🗑 VACUUM (optimiser)");
        btnVacuum.setStyle("-fx-background-color:#d97706;-fx-text-fill:white;-fx-font-size:13;"
                + "-fx-padding:9 16;-fx-background-radius:8;-fx-cursor:hand;");
        btnVacuum.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Lancer VACUUM ANALYZE ? (peut prendre quelques secondes)", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn != ButtonType.YES) return;
                ctrl.vacuumDB(() -> showInfo("VACUUM terminé."), err -> showErr(err));
            });
        });

        HBox btnRow = new HBox(12, btnRefresh, btnTest, btnVacuum);

        card.getChildren().addAll(lblInfo, btnRow, tbl);
        root.getChildren().add(card);

        loadDB.run();
        return root;
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 8 — Logs d'audit
    // ════════════════════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private VBox buildLogsTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Journal d'audit");

        ObservableList<String[]> logRows = FXCollections.observableArrayList();
        TableView<String[]> tbl = new TableView<>(logRows);
        tbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tbl.setPrefHeight(380);
        tbl.setStyle("-fx-font-size:12;");
        tbl.setPlaceholder(new Label("Aucun log disponible."));

        String[] cols = {"Date/Heure", "Action", "Table", "Utilisateur", "Détails"};
        int[]    idxs = {0, 1, 2, 3, 4};
        double[] wids = {150, 150, 100, 120, 250};

        for (int i = 0; i < cols.length; i++) {
            final int idx = idxs[i];
            TableColumn<String[], String> c = new TableColumn<>(cols[i]);
            c.setCellValueFactory(p -> new SimpleStringProperty(
                p.getValue().length > idx ? p.getValue()[idx] : ""));
            c.setPrefWidth(wids[i]);
            tbl.getColumns().add(c);
        }

        Button btnRefresh = outlineBtn("↻ Actualiser les logs");
        btnRefresh.setOnAction(e -> loadLogs(logRows));

        card.getChildren().addAll(btnRefresh, tbl);
        root.getChildren().add(card);

        loadLogs(logRows);
        return root;
    }

    private void loadLogs(ObservableList<String[]> rows) {
        ctrl.chargerLogs(session.role, session.userId, rows::setAll, err -> showErr(err));
    }

    // ════════════════════════════════════════════════════════════════════════
    // TAB 9 — Exports
    // ════════════════════════════════════════════════════════════════════════

    private VBox buildExportsTab() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(24, 0, 0, 0));

        VBox card = sectionCard("Export des données en Excel");
        card.setSpacing(16);

        card.getChildren().addAll(
            exportRow("📋 Demandes",            "demandes_vms",    "demandes"),
            exportRow("🎟 Bons",                 "bons_vms",        "bons"),
            exportRow("👥 Clients",              "clients_vms",     "clients"),
            exportRow("⏰ Bons proche expiration","expiration_vms",  "expiration")
        );

        root.getChildren().add(card);
        return root;
    }

    private HBox exportRow(String label, String baseName, String type) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size:13;-fx-text-fill:#374151;");
        lbl.setMinWidth(240);

        Button btn = outlineBtn("📤 Exporter");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Enregistrer " + label);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            fc.setInitialFileName(baseName + ".xlsx");
            // We need a window reference — use a new stage if not available
            File f;
            try { f = fc.showSaveDialog(btn.getScene().getWindow()); }
            catch (Exception ex) { showErr("Impossible d'ouvrir le sélecteur de fichier."); return; }
            if (f == null) return;

            switch (type) {
                case "demandes"   -> ctrl.exporterDemandes(f.getAbsolutePath(),
                    () -> showInfo("Export réussi : " + f.getName()), err -> showErr(err));
                case "bons"       -> ctrl.exporterBons(f.getAbsolutePath(),
                    () -> showInfo("Export réussi : " + f.getName()), err -> showErr(err));
                case "clients"    -> ctrl.exporterClients(f.getAbsolutePath(),
                    () -> showInfo("Export réussi : " + f.getName()), err -> showErr(err));
                case "expiration" -> ctrl.exporterBonsExpiration(f.getAbsolutePath(),
                    () -> showInfo("Export réussi : " + f.getName()), err -> showErr(err));
            }
        });

        HBox row = new HBox(16, lbl, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 16));
        row.setStyle("-fx-background-color:#f8fafc;-fx-background-radius:8;"
                   + "-fx-border-color:#e2e8f0;-fx-border-radius:8;");
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    // Helpers
    // ════════════════════════════════════════════════════════════════════════

    private VBox sectionCard(String title) {
        VBox c = new VBox(16);
        c.setPadding(new Insets(24));
        c.setStyle("-fx-background-color:white;-fx-background-radius:12;"
                 + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label t = new Label(title);
        t.setStyle("-fx-font-size:15;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        c.getChildren().add(t);
        return c;
    }

    private HBox formRow(String label, Control field) {
        Label l = fieldLabel(label);
        l.setMinWidth(200);
        HBox row = new HBox(12, l, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Label fieldLabel(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size:13;-fx-font-weight:bold;-fx-text-fill:#374151;");
        return l;
    }

    private TextField styledField(String val) {
        TextField tf = new TextField(val);
        tf.setPrefWidth(280);
        tf.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                  + "-fx-border-radius:8;-fx-background-radius:8;");
        return tf;
    }

    private PasswordField pwdField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefWidth(280);
        pf.setStyle("-fx-font-size:13;-fx-padding:8 12;-fx-border-color:#d1d5db;"
                  + "-fx-border-radius:8;-fx-background-radius:8;");
        return pf;
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

    private <T> TableColumn<T, String> colUser(String title,
            java.util.function.Function<UserDAO.UserProfile, String> fn, double w) {
        TableColumn<UserDAO.UserProfile, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            fn.apply(p.getValue()) != null ? fn.apply(p.getValue()) : ""));
        c.setPrefWidth(w);
        @SuppressWarnings("unchecked")
        TableColumn<T, String> cast = (TableColumn<T, String>) c;
        return cast;
    }

    private <T> TableColumn<T, String> colSoc(String title,
            java.util.function.Function<SocieteDAO.Societe, String> fn, double w) {
        TableColumn<SocieteDAO.Societe, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            fn.apply(p.getValue()) != null ? fn.apply(p.getValue()) : ""));
        c.setPrefWidth(w);
        @SuppressWarnings("unchecked")
        TableColumn<T, String> cast = (TableColumn<T, String>) c;
        return cast;
    }

    private <T> TableColumn<T, String> colMag(String title,
            java.util.function.Function<MagasinDAO.Magasin, String> fn, double w) {
        TableColumn<MagasinDAO.Magasin, String> c = new TableColumn<>(title);
        c.setCellValueFactory(p -> new SimpleStringProperty(
            fn.apply(p.getValue()) != null ? fn.apply(p.getValue()) : ""));
        c.setPrefWidth(w);
        @SuppressWarnings("unchecked")
        TableColumn<T, String> cast = (TableColumn<T, String>) c;
        return cast;
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private void showErr(String msg)  { pkg.vms.Toast.error(msg); }
    private void showInfo(String msg) { pkg.vms.Toast.success(msg); }
}
