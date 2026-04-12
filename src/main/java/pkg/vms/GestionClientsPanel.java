package pkg.vms;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
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
    private static final Font FONT_SUBTITLE  = new Font("Trebuchet MS", Font.PLAIN, 13);
    private static final Font FONT_NAV       = VMSStyle.FONT_NAV;
    private static final Font FONT_BTN       = new Font("Trebuchet MS", Font.BOLD, 12);
    private static final Font FONT_TABLE     = VMSStyle.FONT_NAV;
    private static final Font FONT_TABLE_HDR = VMSStyle.FONT_BADGE.deriveFont(12f);
    private static final Font FONT_TABLE_CELL= new Font("Trebuchet MS", Font.PLAIN, 12);
    private static final Font FONT_FILTER    = new Font("Trebuchet MS", Font.PLAIN, 12);

    private ClientManager clientManager;
    private JTable tableClients;
    private DefaultTableModel tableModel;
    private JTextField txtRecherche;
    private JLabel lblTotalClients;

    public GestionClientsPanel() {
        clientManager = new ClientManager();
        initializeUI();
        chargerClients();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(0, 0));
        setOpaque(false);

        // ==================== HEADER ====================
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // ==================== TABLEAU ====================
        JPanel tablePanel = createTablePanel();
        add(tablePanel, BorderLayout.CENTER);

        // ==================== FOOTER ====================
        JPanel footerPanel = createFooterPanel();
        add(footerPanel, BorderLayout.SOUTH);
    }

    // ==================== CREATION DU HEADER ====================
    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 32, 20, 32));

        // ── Titre a gauche avec accent bar rouge ──
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

        // ── Barre de recherche et boutons a droite ──
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightPanel.setOpaque(false);

        // Champ de recherche (placeholder peint comme GestionDemande)
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

        // Recherche en temps reel
        txtRecherche.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                rechercherClients();
            }
        });

        // Boutons
        JButton btnNouveau = buildRedButton("+ Nouveau Client");
        btnNouveau.addActionListener(e -> ouvrirFormulaireNouveauClient());

        JButton btnRefresh = buildIconButton("\u21BB", "Actualiser");
        btnRefresh.addActionListener(e -> chargerClients());

        JButton btnSupprimer = buildRedButton("Supprimer");
        btnSupprimer.addActionListener(e -> supprimerClientSelectionne());

        rightPanel.add(txtRecherche);
        rightPanel.add(btnNouveau);
        rightPanel.add(btnRefresh);
        rightPanel.add(btnSupprimer);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);

        return panel;
    }

    // ==================== CRÉATION DU TABLEAU ====================
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 40, 10, 40));

        // Modèle de tableau
        String[] colonnes = {"ID", "Nom", "Email", "Téléphone", "Société", "Date création", "Statut"};
        tableModel = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Tableau non éditable directement
            }
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

        // Style de l'en-tete (matching GestionDemande)
        JTableHeader header = tableClients.getTableHeader();
        header.setFont(FONT_TABLE_HDR);
        header.setBackground(new Color(248, 249, 252));
        header.setForeground(TEXT_SECOND);
        header.setPreferredSize(new Dimension(0, 42));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_LIGHT));

        // Ajuster la largeur des colonnes
        tableClients.getColumnModel().getColumn(0).setPreferredWidth(50);  // ID
        tableClients.getColumnModel().getColumn(1).setPreferredWidth(150); // Nom
        tableClients.getColumnModel().getColumn(2).setPreferredWidth(200); // Email
        tableClients.getColumnModel().getColumn(3).setPreferredWidth(120); // Téléphone
        tableClients.getColumnModel().getColumn(4).setPreferredWidth(150); // Société
        tableClients.getColumnModel().getColumn(5).setPreferredWidth(150); // Date
        tableClients.getColumnModel().getColumn(6).setPreferredWidth(80);  // Statut

        // Menu contextuel (clic droit)
        JPopupMenu popupMenu = createPopupMenu();
        tableClients.setComponentPopupMenu(popupMenu);

        // Double-clic pour modifier
        tableClients.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    modifierClientSelectionne();
                }
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
        lblTotalClients.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
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

    // ==================== CHARGER LES CLIENTS ====================
    private void chargerClients() {
        tableModel.setRowCount(0); // Vider le tableau

        List<Client> clients = clientManager.obtenirTousLesClients();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Client client : clients) {
            String dateCreation = client.getDateCreation() != null ?
                    sdf.format(client.getDateCreation()) : "N/A";
            String statut = client.isActif() ? "✓ Actif" : "✗ Inactif";

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
    }

    // ==================== RECHERCHER CLIENTS ====================
    private void rechercherClients() {
        String recherche = txtRecherche.getText().trim();

        if (recherche.isEmpty()) {
            chargerClients();
            return;
        }

        tableModel.setRowCount(0);
        List<Client> clients = clientManager.rechercherClients(recherche);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (Client client : clients) {
            String dateCreation = client.getDateCreation() != null ?
                    sdf.format(client.getDateCreation()) : "N/A";
            String statut = client.isActif() ? "✓ Actif" : "✗ Inactif";

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
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client à modifier.",
                    "Aucune sélection",
                    JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(dialog,
                        "Veuillez remplir tous les champs obligatoires (*)",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Validation email
            if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                JOptionPane.showMessageDialog(dialog,
                        "Format d'email invalide!",
                        "Validation",
                        JOptionPane.WARNING_MESSAGE);
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
                    JOptionPane.showMessageDialog(dialog,
                            "Client modifié avec succès!",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    chargerClients();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Erreur lors de la modification!",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(dialog,
                            "Client créé avec succès!",
                            "Succès",
                            JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    chargerClients();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Erreur lors de la création!",
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client à supprimer.",
                    "Aucune sélection",
                    JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(this,
                    message,
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            chargerClients();
        } else {
            JOptionPane.showMessageDialog(this,
                    message,
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==================== DÉTAILS CLIENT ====================
    private void afficherDetailsClient() {
        int selectedRow = tableClients.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un client.",
                    "Aucune sélection",
                    JOptionPane.WARNING_MESSAGE);
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