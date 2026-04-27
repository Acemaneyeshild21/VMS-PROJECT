package pkg.vms;

import pkg.vms.controller.ClientController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.List;

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
    private static final Font FONT_TITLE      = VMSStyle.FONT_BRAND.deriveFont(24f);
    private static final Font FONT_SUBTITLE   = new Font("Trebuchet MS", Font.PLAIN, 13);
    private static final Font FONT_BTN        = new Font("Trebuchet MS", Font.BOLD, 12);
    private static final Font FONT_TABLE_HDR  = VMSStyle.FONT_BADGE.deriveFont(12f);
    private static final Font FONT_TABLE_CELL = new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_FILTER     = new Font("Trebuchet MS", Font.PLAIN, 12);

    private static final SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private JTable tableClients;
    private DefaultTableModel tableModel;
    private JTextField txtRecherche;
    private JLabel lblTotalClients;

    private final ClientController controller = new ClientController();

    public GestionClientsPanel() {
        initializeUI();
        chargerClients();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    // ── HEADER ───────────────────────────────────────────────────────────────
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));

        JPanel leftPanel = new JPanel();
        leftPanel.setOpaque(false);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(RED_PRIMARY);
                g.fillRoundRect(0, 3, 4, getHeight() - 6, 4, 4);
            }
        };
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        JLabel titre = new JLabel("Gestion des Clients");
        titre.setFont(FONT_TITLE);
        titre.setForeground(TEXT_PRIMARY);
        titleRow.add(titre);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Gestion et suivi de la base de clients");
        subtitle.setFont(FONT_SUBTITLE);
        subtitle.setForeground(TEXT_SECOND);
        subtitle.setBorder(BorderFactory.createEmptyBorder(5, 14, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        leftPanel.add(titleRow);
        leftPanel.add(subtitle);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        txtRecherche = new JTextField(18) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(FONT_FILTER);
                    g2.setColor(TEXT_MUTED);
                    Insets i = getInsets();
                    g2.drawString("Rechercher un client...", i.left, getHeight() - i.bottom - 4);
                }
            }
        };
        txtRecherche.setFont(FONT_FILTER);
        txtRecherche.setForeground(TEXT_PRIMARY);
        txtRecherche.setBackground(BG_CARD);
        txtRecherche.setCaretColor(RED_PRIMARY);
        txtRecherche.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
        txtRecherche.setPreferredSize(new Dimension(220, 36));
        txtRecherche.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { rechercherClients(); }
        });

        JButton btnNouveau   = buildRedButton("+ Nouveau Client");
        JButton btnRefresh   = buildIconButton("↻", "Actualiser");
        JButton btnSupprimer = buildRedButton("Supprimer");
        JButton btnExport    = buildIconButton("📥", "Exporter en Excel");

        btnNouveau.addActionListener(e -> ouvrirFormulaireNouveauClient());
        btnRefresh.addActionListener(e -> chargerClients());
        btnSupprimer.addActionListener(e -> supprimerClientSelectionne());
        btnExport.addActionListener(e -> exporterClientsExcel());

        rightPanel.add(txtRecherche);
        rightPanel.add(btnNouveau);
        rightPanel.add(btnRefresh);
        rightPanel.add(btnSupprimer);
        rightPanel.add(btnExport);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        return panel;
    }

    // ── TABLE ─────────────────────────────────────────────────────────────────
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        String[] colonnes = {"ID", "Nom", "Email", "Téléphone", "Société", "Date création", "Statut"};
        tableModel = new DefaultTableModel(colonnes, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        tableClients = new JTable(tableModel) {
            @Override public Component prepareRenderer(javax.swing.table.TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : new Color(249, 250, 252));
                else c.setBackground(RED_LIGHT);
                return c;
            }
        };
        tableClients.setFont(FONT_TABLE_CELL);
        tableClients.setRowHeight(44);
        tableClients.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableClients.setShowHorizontalLines(true);
        tableClients.setShowVerticalLines(false);
        tableClients.setGridColor(new Color(240, 242, 246));
        tableClients.setSelectionBackground(RED_LIGHT);
        tableClients.setSelectionForeground(TEXT_PRIMARY);
        tableClients.setFocusable(false);
        tableClients.getTableHeader().setReorderingAllowed(false);

        JTableHeader header = tableClients.getTableHeader();
        header.setFont(FONT_TABLE_HDR);
        header.setBackground(new Color(248, 249, 252));
        header.setForeground(TEXT_SECOND);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        tableClients.getColumnModel().getColumn(0).setPreferredWidth(50);
        tableClients.getColumnModel().getColumn(1).setPreferredWidth(150);
        tableClients.getColumnModel().getColumn(2).setPreferredWidth(200);
        tableClients.getColumnModel().getColumn(3).setPreferredWidth(120);
        tableClients.getColumnModel().getColumn(4).setPreferredWidth(150);
        tableClients.getColumnModel().getColumn(5).setPreferredWidth(150);
        tableClients.getColumnModel().getColumn(6).setPreferredWidth(80);

        tableClients.setComponentPopupMenu(createPopupMenu());
        tableClients.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) modifierClientSelectionne();
            }
        });

        JScrollPane scrollPane = new JScrollPane(tableClients);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(BG_CARD);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // ── POPUP MENU ────────────────────────────────────────────────────────────
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itemModifier  = new JMenuItem("✏️ Modifier");
        JMenuItem itemSupprimer = new JMenuItem("🗑️ Supprimer");
        JMenuItem itemDetails   = new JMenuItem("ℹ️ Détails");

        itemModifier.setFont(new Font("Arial", Font.PLAIN, 13));
        itemSupprimer.setFont(new Font("Arial", Font.PLAIN, 13));
        itemDetails.setFont(new Font("Arial", Font.PLAIN, 13));

        itemModifier.addActionListener(e -> modifierClientSelectionne());
        itemSupprimer.addActionListener(e -> supprimerClientSelectionne());
        itemDetails.addActionListener(e -> afficherDetailsClient());

        menu.add(itemModifier);
        menu.add(itemSupprimer);
        menu.addSeparator();
        menu.add(itemDetails);
        return menu;
    }

    // ── FOOTER ────────────────────────────────────────────────────────────────
    private JPanel createFooterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_LIGHT),
                BorderFactory.createEmptyBorder(10, 32, 10, 32)));

        lblTotalClients = new JLabel("Total : 0 clients");
        lblTotalClients.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblTotalClients.setForeground(TEXT_MUTED);
        panel.add(lblTotalClients, BorderLayout.WEST);
        return panel;
    }

    // ── DATA LOADING ──────────────────────────────────────────────────────────
    private void chargerClients() {
        controller.chargerClients(
            clients -> populerTableau(clients, "Total : " + clients.size() + " client(s)"),
            err -> System.err.println("Erreur chargement clients: " + err)
        );
    }

    private void rechercherClients() {
        String query = txtRecherche.getText().trim();
        if (query.isEmpty()) { chargerClients(); return; }
        controller.rechercherClients(query,
            clients -> populerTableau(clients, "Résultats : " + clients.size() + " client(s)"),
            err -> System.err.println("Erreur recherche clients: " + err)
        );
    }

    private void populerTableau(List<Client> clients, String footerText) {
        tableModel.setRowCount(0);
        for (Client c : clients) {
            String dateCreation = c.getDateCreation() != null ? SDF.format(c.getDateCreation()) : "N/A";
            String statut = c.isActif() ? "✓ Actif" : "✗ Inactif";
            tableModel.addRow(new Object[]{
                c.getClientId(), c.getName(), c.getEmail(),
                c.getContactNumber(), c.getCompany(), dateCreation, statut
            });
        }
        lblTotalClients.setText(footerText);
    }

    // ── NOUVEAU CLIENT ────────────────────────────────────────────────────────
    private void ouvrirFormulaireNouveauClient() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Nouveau Client", true);
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);
        dialog.add(creerFormulaireClient(null, dialog));
        dialog.setVisible(true);
    }

    // ── MODIFIER CLIENT ───────────────────────────────────────────────────────
    private void modifierClientSelectionne() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client à modifier.",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int clientId = (int) tableModel.getValueAt(selectedRow, 0);
        controller.getClientById(clientId, client -> {
            if (client != null) {
                JDialog dialog = new JDialog(
                        (Frame) SwingUtilities.getWindowAncestor(this), "Modifier Client", true);
                dialog.setSize(500, 450);
                dialog.setLocationRelativeTo(this);
                dialog.add(creerFormulaireClient(client, dialog));
                dialog.setVisible(true);
            }
        }, err -> JOptionPane.showMessageDialog(this,
                "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
    }

    // ── FORMULAIRE CLIENT ─────────────────────────────────────────────────────
    private JPanel creerFormulaireClient(Client client, JDialog dialog) {
        boolean isModification = (client != null);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titre = new JLabel(isModification ? "Modifier le client" : "Nouveau client");
        titre.setFont(new Font("Arial", Font.BOLD, 22));
        titre.setForeground(new Color(44, 62, 80));
        JPanel titrePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titrePanel.setBackground(Color.WHITE);
        titrePanel.add(titre);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 10, 5);

        JTextField txtNom       = new JTextField(20);
        JTextField txtEmail     = new JTextField(20);
        JTextField txtTelephone = new JTextField(20);
        JTextField txtSociete   = new JTextField(20);
        JCheckBox  chkActif     = new JCheckBox("Client actif");
        chkActif.setSelected(true);
        chkActif.setBackground(Color.WHITE);

        if (isModification) {
            txtNom.setText(client.getName());
            txtEmail.setText(client.getEmail());
            txtTelephone.setText(client.getContactNumber());
            txtSociete.setText(client.getCompany());
            chkActif.setSelected(client.isActif());
        }

        Font fieldFont = new Font("Arial", Font.PLAIN, 14);
        txtNom.setFont(fieldFont);
        txtEmail.setFont(fieldFont);
        txtTelephone.setFont(fieldFont);
        txtSociete.setFont(fieldFont);

        int row = 0;
        ajouterChampFormulaire(formPanel, gbc, row++, "Nom complet *", txtNom);
        ajouterChampFormulaire(formPanel, gbc, row++, "Email *", txtEmail);
        ajouterChampFormulaire(formPanel, gbc, row++, "Téléphone *", txtTelephone);
        ajouterChampFormulaire(formPanel, gbc, row++, "Société", txtSociete);
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(chkActif, gbc);

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
            if (txtNom.getText().trim().isEmpty()
                    || txtEmail.getText().trim().isEmpty()
                    || txtTelephone.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Veuillez remplir tous les champs obligatoires (*)",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(dialog,
                        "Format d'email invalide!",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            btnSauvegarder.setEnabled(false);

            if (isModification) {
                client.setName(txtNom.getText().trim());
                client.setEmail(txtEmail.getText().trim());
                client.setContactNumber(txtTelephone.getText().trim());
                client.setCompany(txtSociete.getText().trim());
                client.setActif(chkActif.isSelected());
                controller.modifierClient(client,
                    succes -> {
                        btnSauvegarder.setEnabled(true);
                        if (succes) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Client modifié avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            chargerClients();
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Erreur lors de la modification!", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    err -> {
                        btnSauvegarder.setEnabled(true);
                        JOptionPane.showMessageDialog(dialog,
                                "Erreur base de données : " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                    });
            } else {
                Client nouveauClient = new Client(
                        txtNom.getText().trim(),
                        txtEmail.getText().trim(),
                        txtTelephone.getText().trim(),
                        txtSociete.getText().trim());
                nouveauClient.setActif(chkActif.isSelected());
                controller.creerClient(nouveauClient,
                    succes -> {
                        btnSauvegarder.setEnabled(true);
                        if (succes) {
                            JOptionPane.showMessageDialog(dialog,
                                    "Client créé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                            dialog.dispose();
                            chargerClients();
                        } else {
                            JOptionPane.showMessageDialog(dialog,
                                    "Erreur lors de la création!", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    err -> {
                        btnSauvegarder.setEnabled(true);
                        JOptionPane.showMessageDialog(dialog,
                                "Erreur base de données : " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                    });
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
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1; gbc.weightx = 0.3;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 13));
        lbl.setForeground(new Color(52, 73, 94));
        panel.add(lbl, gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(189, 195, 199), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        panel.add(field, gbc);
    }

    // ── SUPPRIMER CLIENT ──────────────────────────────────────────────────────
    private void supprimerClientSelectionne() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client à supprimer.",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int clientId  = (int)    tableModel.getValueAt(selectedRow, 0);
        String nomClient = (String) tableModel.getValueAt(selectedRow, 1);

        Object[] options = {"Supprimer définitivement", "Annuler"};
        int choix = JOptionPane.showOptionDialog(this,
                "Comment souhaitez-vous supprimer le client:\n" + nomClient + " ?\n\n"
                + "• Désactiver: Le client sera marqué comme inactif (recommandé)\n"
                + "• Supprimer définitivement: Suppression complète de la base de données",
                "Confirmation de suppression",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        if (choix == 0) {
            controller.desactiverClient(clientId,
                succes -> {
                    if (succes) {
                        JOptionPane.showMessageDialog(this, "Client désactivé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        chargerClients();
                    } else {
                        JOptionPane.showMessageDialog(this, "Erreur lors de la désactivation!", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                },
                err -> JOptionPane.showMessageDialog(this, "Erreur base de données : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
        } else if (choix == 1) {
            int confirmDefinitif = JOptionPane.showConfirmDialog(this,
                    "⚠️ ATTENTION ⚠️\n\n"
                    + "Cette action est IRRÉVERSIBLE!\n"
                    + "Le client sera supprimé définitivement de la base de données.\n\n"
                    + "Êtes-vous absolument certain?",
                    "Confirmation finale", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (confirmDefinitif == JOptionPane.YES_OPTION) {
                controller.supprimerClient(clientId,
                    succes -> {
                        if (succes) {
                            JOptionPane.showMessageDialog(this, "Client supprimé définitivement!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                            chargerClients();
                        } else {
                            JOptionPane.showMessageDialog(this, "Erreur lors de la suppression définitive!", "Erreur", JOptionPane.ERROR_MESSAGE);
                        }
                    },
                    err -> JOptionPane.showMessageDialog(this, "Erreur base de données : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        }
    }

    // ── DETAILS CLIENT ────────────────────────────────────────────────────────
    private void afficherDetailsClient() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client.",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int clientId = (int) tableModel.getValueAt(selectedRow, 0);
        controller.getClientById(clientId, client -> {
            if (client != null) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String dateCreation = client.getDateCreation() != null
                        ? sdf2.format(client.getDateCreation()) : "N/A";
                String details = String.format(
                        "<html><body style='width: 300px; padding: 10px;'>"
                        + "<h2 style='color: #2c3e50;'>📋 Détails du Client</h2><hr>"
                        + "<p><b>ID:</b> %d</p>"
                        + "<p><b>Nom:</b> %s</p>"
                        + "<p><b>Email:</b> %s</p>"
                        + "<p><b>Téléphone:</b> %s</p>"
                        + "<p><b>Société:</b> %s</p>"
                        + "<p><b>Date de création:</b> %s</p>"
                        + "<p><b>Statut:</b> %s</p>"
                        + "</body></html>",
                        client.getClientId(), client.getName(), client.getEmail(),
                        client.getContactNumber(),
                        client.getCompany() != null ? client.getCompany() : "N/A",
                        dateCreation,
                        client.isActif()
                                ? "<font color='green'>✓ Actif</font>"
                                : "<font color='red'>✗ Inactif</font>");
                JOptionPane.showMessageDialog(this, details,
                        "Détails du Client", JOptionPane.INFORMATION_MESSAGE);
            }
        }, err -> JOptionPane.showMessageDialog(this,
                "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
    }

    // ── EXPORT EXCEL ──────────────────────────────────────────────────────────
    private void exporterClientsExcel() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exporter les clients en Excel");
        fc.setSelectedFile(new java.io.File("clients_vms.xlsx"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers Excel (*.xlsx)", "xlsx"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getAbsolutePath();
            if (!path.endsWith(".xlsx")) path += ".xlsx";
            final String filePath = path;
            controller.exporterClients(filePath,
                () -> JOptionPane.showMessageDialog(this,
                        "Export réussi : " + filePath, "Export Excel", JOptionPane.INFORMATION_MESSAGE),
                err -> JOptionPane.showMessageDialog(this,
                        "Erreur lors de l'export : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
        }
    }

    // ── BUTTON BUILDERS ───────────────────────────────────────────────────────
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

    private JButton buildIconButton(String symbol, String tooltip) {
        JButton btn = new JButton(symbol) {
            boolean h = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 16)); setForeground(TEXT_SECOND);
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
}
