package pkg.vms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Panel de gestion complète des clients avec interface moderne
 */
public class GestionClientsPanel extends JPanel {

    // ── Palette (Centralisée via VMSStyle) ──────────────────────────────────
    private static final Color BG_ROOT      = VMSStyle.BG_ROOT;
    private static final Color BG_CARD      = VMSStyle.BG_CARD;
    private static final Color RED_PRIMARY  = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK     = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT    = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED   = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS      = VMSStyle.SUCCESS;
    private static final Color WARNING      = VMSStyle.WARNING;

    // ── Fonts (Centralisées via VMSStyle) ────────────────────────────────────
    private static final Font FONT_TITLE     = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_SUBTITLE  = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_NAV       = VMSStyle.FONT_NAV;
    private static final Font FONT_BTN       = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_TABLE     = VMSStyle.FONT_NAV;
    private static final Font FONT_TABLE_HDR = VMSStyle.FONT_BADGE.deriveFont(12f);
    private static final Font FONT_TABLE_CELL= new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_FILTER    = new Font("Segoe UI", Font.PLAIN, 12);

    private ClientManager clientManager;
    private JTable tableClients;
    private DefaultTableModel tableModel;
    private JTextField txtRecherche;
    private JLabel lblTotalClients;
    private CardLayout tableCards;
    private JPanel tableCardHolder;
    private boolean isSearchMode = false;

    public GestionClientsPanel() {
        clientManager = new ClientManager();
        initializeUI();
        chargerClients();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // Bloc central : header + filter bar + table (style InvoiceNinja)
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        content.add(createHeaderPanel(), BorderLayout.NORTH);
        content.add(createCenterPanel(), BorderLayout.CENTER);
        content.add(createFooterPanel(), BorderLayout.SOUTH);

        add(content, BorderLayout.CENTER);
    }

    // ==================== CREATION DU HEADER (InvoiceNinja style) ====================
    private JPanel createHeaderPanel() {
        // Boutons d'action à droite du header
        JButton btnNouveau = UIUtils.buildPrimaryButton("+ Nouveau Client", 170, 40);
        btnNouveau.addActionListener(e -> ouvrirFormulaireNouveauClient());

        JButton btnExport = UIUtils.buildGhostButton("Exporter", 110, 40);
        btnExport.addActionListener(e -> exporterClientsExcel());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnExport);
        right.add(btnNouveau);

        return PageLayout.buildPageHeader(
                "Clients",
                "Gestion et suivi de la base de clients",
                right
        );
    }

    // ==================== CREATION CENTRE : filter bar + table ====================
    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);

        // Filter bar (recherche + bouton rafraîchir + supprimer)
        PageLayout.FilterBar fb = PageLayout.buildFilterBar("Rechercher un client (nom, email, société)...");
        txtRecherche = fb.search();
        txtRecherche.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { rechercherClients(); }
        });

        JButton btnRefresh = UIUtils.buildGhostButton("Actualiser", 110, 38);
        btnRefresh.addActionListener(e -> chargerClients());
        JButton btnSupprimer = UIUtils.buildGhostButton("Supprimer", 110, 38);
        btnSupprimer.addActionListener(e -> supprimerClientSelectionne());
        fb.addSlots(btnRefresh, btnSupprimer);

        center.add(fb.panel(), BorderLayout.NORTH);
        center.add(createTablePanel(), BorderLayout.CENTER);
        return center;
    }

    // ==================== CRÉATION DU TABLEAU (ModernTable) ====================
    private JPanel createTablePanel() {
        // Modèle de tableau
        String[] colonnes = {"ID", "Nom", "Email", "Téléphone", "Société", "Date création", "Statut"};
        tableModel = new DefaultTableModel(colonnes, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tableClients = new JTable(tableModel);
        ModernTable.apply(tableClients);
        tableClients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Largeurs & renderers
        ModernTable.setColumnWidth(tableClients, 0, 50, 60, 80);
        ModernTable.setColumnWidth(tableClients, 1, 150);
        ModernTable.setColumnWidth(tableClients, 2, 200);
        ModernTable.setColumnWidth(tableClients, 3, 120);
        ModernTable.setColumnWidth(tableClients, 4, 150);
        ModernTable.setColumnWidth(tableClients, 5, 150);
        ModernTable.setColumnWidth(tableClients, 6, 100, 120, 140);
        ModernTable.setColumnRenderer(tableClients, 0, ModernTable.boldRenderer());
        ModernTable.setColumnRenderer(tableClients, 6, ModernTable.statusBadgeRenderer());

        // Menu contextuel + double-clic
        tableClients.setComponentPopupMenu(createPopupMenu());
        tableClients.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) modifierClientSelectionne();
            }
        });

        // Holder avec CardLayout : table / empty-no-data / empty-no-result
        JPanel tableRounded = ModernTable.wrapRounded(tableClients);

        JPanel emptyNoData = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoData.add(PageLayout.buildEmptyState(
                "Aucun client enregistré",
                "Commencez par ajouter votre premier client.",
                "+ Nouveau Client",
                this::ouvrirFormulaireNouveauClient
        ), BorderLayout.CENTER);

        JPanel emptyNoResult = PageLayout.buildCard(new BorderLayout(), 0);
        emptyNoResult.add(PageLayout.buildEmptyState(
                "Aucun résultat",
                "Aucun client ne correspond à votre recherche.",
                "Réinitialiser",
                () -> { txtRecherche.setText(""); chargerClients(); }
        ), BorderLayout.CENTER);

        tableCards = new CardLayout();
        tableCardHolder = new JPanel(tableCards);
        tableCardHolder.setOpaque(false);
        tableCardHolder.add(tableRounded, "table");
        tableCardHolder.add(emptyNoData, "empty-no-data");
        tableCardHolder.add(emptyNoResult, "empty-no-result");
        return tableCardHolder;
    }

    private void updateTableView() {
        if (tableCards == null || tableCardHolder == null) return;
        if (tableModel.getRowCount() > 0) {
            tableCards.show(tableCardHolder, "table");
        } else if (isSearchMode) {
            tableCards.show(tableCardHolder, "empty-no-result");
        } else {
            tableCards.show(tableCardHolder, "empty-no-data");
        }
    }

    // ==================== MENU CONTEXTUEL ====================
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemModifier = new JMenuItem("✏️ Modifier");
        itemModifier.setFont(new Font("Arial", Font.PLAIN, 13));
        itemModifier.addActionListener(e -> modifierClientSelectionne());

        JMenuItem itemSupprimer = new JMenuItem("🗑️ Supprimer");
        itemSupprimer.setFont(new Font("Arial", Font.PLAIN, 13));
        itemSupprimer.addActionListener(e -> supprimerClientSelectionne());

        JMenuItem itemDetails = new JMenuItem("ℹ️ Détails");
        itemDetails.setFont(new Font("Arial", Font.PLAIN, 13));
        itemDetails.addActionListener(e -> afficherDetailsClient());

        menu.add(itemModifier);
        menu.add(itemSupprimer);
        menu.addSeparator();
        menu.add(itemDetails);

        return menu;
    }

    // ==================== FOOTER ====================
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 32, 10, 32)));

        lblTotalClients = new JLabel("Total : 0 clients");
        lblTotalClients.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotalClients.setForeground(TEXT_MUTED);

        panel.add(lblTotalClients, BorderLayout.WEST);

        return panel;
    }

    // ==================== BOUTON ROUGE (style GestionDemande) ====================
    private JButton buildRedButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN); setForeground(Color.WHITE);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(getPreferredSize().width + 28, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? RED_DARK : RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose(); super.paintComponent(g);
            }
        };
        return btn;
    }

    // ==================== BOUTON ICONE (style GestionDemande) ====================
    private JButton buildIconButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            {
                setFont(new Font("Segoe UI", Font.BOLD, 16)); setForeground(TEXT_SECOND);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setToolTipText(tooltip); setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(36, 36));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0, 0, 0, 8));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ==================== EXPORTER CLIENTS EN EXCEL ====================
    private void exporterClientsExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exporter les clients en Excel");
        fc.setSelectedFile(new java.io.File("clients_vms.xlsx"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers Excel (*.xlsx)", "xlsx"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".xlsx")) path += ".xlsx";
            final String filePath = path;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    String[] columns = {"ID", "Nom", "Email", "Téléphone", "Société", "Création", "Actif"};
                    java.util.List<java.util.Map<String, Object>> data = pkg.vms.DAO.ClientDAO.getClientsForExport();
                    ExcelExportService.exportData(filePath, "Clients", columns, data);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        ToastManager.success(GestionClientsPanel.this, "Export réussi : " + filePath);
                    } catch (Exception ex) {
                        ToastManager.error(GestionClientsPanel.this, "Erreur export : " + ex.getMessage());
                    }
                }
            };
            worker.execute();
        }
    }

    // ==================== CHARGER LES CLIENTS ====================
    private void chargerClients() {
        tableModel.setRowCount(0); // Vider le tableau

        List<Client> clients = clientManager.obtenirTousLesClients();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Client client : clients) {
            String dateCreation = client.getDateCreation() != null ?
                    sdf.format(client.getDateCreation()) : "N/A";
            String statut = client.isActif() ? "Actif" : "Inactif";

            tableModel.addRow(new Object[]{
                    client.getClientId(),
                    client.getName(),
                    client.getEmail(),
                    client.getContactNumber(),
                    client.getCompany(),
                    dateCreation,
                    statut
            });
        }

        lblTotalClients.setText("Total : " + clients.size() + " client(s)");
        isSearchMode = false;
        updateTableView();
    }

    // ==================== RECHERCHER CLIENTS ====================
    private void rechercherClients() {
        String recherche = txtRecherche.getText().trim();

        if (recherche.isEmpty()) {
            chargerClients();
            return;
        }

        isSearchMode = true;
        tableModel.setRowCount(0);
        List<Client> clients = clientManager.rechercherClients(recherche);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Client client : clients) {
            String dateCreation = client.getDateCreation() != null ?
                    sdf.format(client.getDateCreation()) : "N/A";
            String statut = client.isActif() ? "Actif" : "Inactif";

            tableModel.addRow(new Object[]{
                    client.getClientId(),
                    client.getName(),
                    client.getEmail(),
                    client.getContactNumber(),
                    client.getCompany(),
                    dateCreation,
                    statut
            });
        }

        lblTotalClients.setText("Résultats : " + clients.size() + " client(s)");
        updateTableView();
    }

    // ==================== NOUVEAU CLIENT ====================
    private void ouvrirFormulaireNouveauClient() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nouveau Client", true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = creerFormulaireClient(null, dialog);
        dialog.add(formPanel);
        dialog.setVisible(true);
    }

    // ==================== MODIFIER CLIENT ====================
    private void modifierClientSelectionne() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            ToastManager.warning(this, "Veuillez sélectionner un client à modifier");
            return;
        }

        int clientId = (int) tableModel.getValueAt(selectedRow, 0);
        Client client = clientManager.obtenirClientParId(clientId);

        if (client != null) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Modifier Client", true);
            dialog.setSize(500, 450);
            dialog.setLocationRelativeTo(this);

            JPanel formPanel = creerFormulaireClient(client, dialog);
            dialog.add(formPanel);
            dialog.setVisible(true);
        }
    }

    // ==================== FORMULAIRE CLIENT ====================
    private JPanel creerFormulaireClient(Client client, JDialog dialog) {
        boolean isModification = (client != null);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Titre
        JLabel titre = new JLabel(isModification ? "Modifier le client" : "Nouveau client");
        titre.setFont(new Font("Arial", Font.BOLD, 22));
        titre.setForeground(new Color(44, 62, 80));

        JPanel titrePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titrePanel.setBackground(Color.WHITE);
        titrePanel.add(titre);

        // Formulaire
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);

        // Champs
        JTextField txtNom = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtTelephone = new JTextField(20);
        JTextField txtSociete = new JTextField(20);
        JCheckBox chkActif = new JCheckBox("Client actif");
        chkActif.setSelected(true);
        chkActif.setBackground(Color.WHITE);

        if (isModification) {
            txtNom.setText(client.getName());
            txtEmail.setText(client.getEmail());
            txtTelephone.setText(client.getContactNumber());
            txtSociete.setText(client.getCompany());
            chkActif.setSelected(client.isActif());
        }

        // Style des champs
        Font fieldFont = new Font("Arial", Font.PLAIN, 14);
        txtNom.setFont(fieldFont);
        txtEmail.setFont(fieldFont);
        txtTelephone.setFont(fieldFont);
        txtSociete.setFont(fieldFont);

        // Ajout des composants
        int row = 0;
        ajouterChampFormulaire(formPanel, gbc, row++, "Nom complet *", txtNom);
        ajouterChampFormulaire(formPanel, gbc, row++, "Email *", txtEmail);
        ajouterChampFormulaire(formPanel, gbc, row++, "Téléphone *", txtTelephone);
        ajouterChampFormulaire(formPanel, gbc, row++, "Société", txtSociete);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        formPanel.add(chkActif, gbc);

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton btnSauvegarder = new JButton(isModification ? "💾 Mettre à jour" : "💾 Créer");
        btnSauvegarder.setFont(new Font("Arial", Font.BOLD, 14));
        btnSauvegarder.setBackground(new Color(46, 204, 113));
        btnSauvegarder.setForeground(Color.WHITE);
        btnSauvegarder.setBorderPainted(false);
        btnSauvegarder.setFocusPainted(false);
        btnSauvegarder.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSauvegarder.setPreferredSize(new Dimension(150, 40));

        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.setFont(new Font("Arial", Font.BOLD, 14));
        btnAnnuler.setBackground(new Color(149, 165, 166));
        btnAnnuler.setForeground(Color.WHITE);
        btnAnnuler.setBorderPainted(false);
        btnAnnuler.setFocusPainted(false);
        btnAnnuler.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAnnuler.setPreferredSize(new Dimension(150, 40));

        btnSauvegarder.addActionListener(e -> {
            // Validation
            if (txtNom.getText().trim().isEmpty() ||
                    txtEmail.getText().trim().isEmpty() ||
                    txtTelephone.getText().trim().isEmpty()) {
                ToastManager.warning(dialog, "Veuillez remplir tous les champs obligatoires (*)");
                return;
            }

            // Validation email
            if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                ToastManager.warning(dialog, "Format d'email invalide");
                return;
            }

            // Créer ou modifier
            if (isModification) {
                client.setName(txtNom.getText().trim());
                client.setEmail(txtEmail.getText().trim());
                client.setContactNumber(txtTelephone.getText().trim());
                client.setCompany(txtSociete.getText().trim());
                client.setActif(chkActif.isSelected());

                if (clientManager.modifierClient(client)) {
                    dialog.dispose();
                    ToastManager.success(this, "Client modifié avec succès");
                    chargerClients();
                } else {
                    ToastManager.error(dialog, "Erreur lors de la modification");
                }
            } else {
                Client nouveauClient = new Client(
                        txtNom.getText().trim(),
                        txtEmail.getText().trim(),
                        txtTelephone.getText().trim(),
                        txtSociete.getText().trim()
                );
                nouveauClient.setActif(chkActif.isSelected());

                if (clientManager.ajouterClient(nouveauClient)) {
                    dialog.dispose();
                    ToastManager.success(this, "Client créé avec succès");
                    chargerClients();
                } else {
                    ToastManager.error(dialog, "Erreur lors de la création");
                }
            }
        });

        btnAnnuler.addActionListener(e -> dialog.dispose());

        buttonPanel.add(btnSauvegarder);
        buttonPanel.add(btnAnnuler);

        panel.add(titrePanel, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void ajouterChampFormulaire(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0.3;

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(new Color(52, 73, 94));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        panel.add(field, gbc);
    }

    // ==================== ✅ SUPPRIMER CLIENT (VERSION AMÉLIORÉE) ====================
    private void supprimerClientSelectionne() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            ToastManager.warning(this, "Veuillez sélectionner un client à supprimer");
            return;
        }

        int clientId = (int) tableModel.getValueAt(selectedRow, 0);
        String nomClient = (String) tableModel.getValueAt(selectedRow, 1);

        // Le Bouton suppresion
        Object[] options = {"Supprimer définitivement", "Annuler"};
        int choix = JOptionPane.showOptionDialog(this,
                "Comment souhaitez-vous supprimer le client:\n" + nomClient + " ?\n\n" +
                        "• Désactiver: Le client sera marqué comme inactif (recommandé)\n" +
                        "• Supprimer définitivement: Suppression complète de la base de données",
                "Confirmation de suppression",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);

        boolean succes = false;
        String message = "";

        if (choix == 0) {


            // Désactivation logique
            if (clientManager.supprimerClient(clientId)) {
                succes = true;
                message = "Client désactivé avec succès!";
            } else {
                message = "Erreur lors de la désactivation!";
            }
        } else if (choix == 1) {

            // Suppression Confirmation supplémentaire
            int confirmDefinitif = JOptionPane.showConfirmDialog(this,
                    "⚠️ ATTENTION ⚠️\n\n" +
                            "Cette action est IRRÉVERSIBLE!\n" +
                            "Le client sera supprimé définitivement de la base de données.\n\n" +
                            "Êtes-vous absolument certain?",
                    "Confirmation finale",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.ERROR_MESSAGE);

            if (confirmDefinitif == JOptionPane.YES_OPTION) {
                if (clientManager.supprimerClientDefinitivement(clientId)) {
                    succes = true;
                    message = "Client supprimé définitivement!";
                } else {
                    message = "Erreur lors de la suppression définitive!";
                }
            } else {
                return; // Annulation
            }
        } else {
            return; // Annulé
        }

        // Afficher le résultat
        if (succes) {
            ToastManager.success(this, message);
            chargerClients();
        } else {
            ToastManager.error(this, message);
        }
    }

    // ==================== DÉTAILS CLIENT ====================
    private void afficherDetailsClient() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            ToastManager.warning(this, "Veuillez sélectionner un client");
            return;
        }

        int clientId = (int) tableModel.getValueAt(selectedRow, 0);
        Client client = clientManager.obtenirClientParId(clientId);

        if (client != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String dateCreation = client.getDateCreation() != null ?
                    sdf.format(client.getDateCreation()) : "N/A";

            String details = String.format(
                    "<html><body style='width: 300px; padding: 10px;'>" +
                            "<h2 style='color: #2c3e50;'>📋 Détails du Client</h2>" +
                            "<hr>" +
                            "<p><b>ID:</b> %d</p>" +
                            "<p><b>Nom:</b> %s</p>" +
                            "<p><b>Email:</b> %s</p>" +
                            "<p><b>Téléphone:</b> %s</p>" +
                            "<p><b>Société:</b> %s</p>" +
                            "<p><b>Date de création:</b> %s</p>" +
                            "<p><b>Statut:</b> %s</p>" +
                            "</body></html>",
                    client.getClientId(),
                    client.getName(),
                    client.getEmail(),
                    client.getContactNumber(),
                    client.getCompany() != null ? client.getCompany() : "N/A",
                    dateCreation,
                    client.isActif() ? "<font color='green'>✓ Actif</font>" : "<font color='red'>✗ Inactif</font>"
            );

            JOptionPane.showMessageDialog(this,
                    details,
                    "Détails du Client",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}