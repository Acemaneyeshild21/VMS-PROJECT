package pkg.vms;

import pkg.vms.DAO.ClientDAO;
import pkg.vms.DAO.VoucherDAO;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class FormulaireCreationBon extends JPanel {

    private final int userId;
    private final String username;

    private JComboBox<ClientDAO.ClientInfo> cbClient;
    private JComboBox<ClientDAO.MagasinInfo> cbMagasin;
    private JTextField txtNombreBons;
    private JTextField txtValeurUnitaire;
    private JTextField txtValiditeJours;
    private JComboBox<String> cbType;
    private JTextField txtEmailDestinataire;
    private JTextArea txtMotif;
    private JLabel lblStatus;
    private JLabel lblMontantTotal;

    public FormulaireCreationBon(int userId, String username) {
        this.userId = userId;
        this.username = username;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Nouvelle Demande de Bons Cadeau");
        title.setFont(VMSStyle.FONT_BRAND.deriveFont(24f));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        // Content
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        // Client
        gbc.gridy = row++;
        content.add(buildLabel("Client *"), gbc);
        gbc.gridy = row++;
        cbClient = new JComboBox<>();
        styleCombo(cbClient);
        content.add(cbClient, gbc);

        // Magasin
        gbc.gridy = row++;
        content.add(buildLabel("Magasin *"), gbc);
        gbc.gridy = row++;
        cbMagasin = new JComboBox<>();
        styleCombo(cbMagasin);
        content.add(cbMagasin, gbc);

        // Nombre de bons + Valeur unitaire (2 colonnes)
        gbc.gridy = row++;
        JPanel rowNbVal = new JPanel(new GridLayout(1, 2, 16, 0));
        rowNbVal.setOpaque(false);

        JPanel panelNb = new JPanel(new BorderLayout(0, 4));
        panelNb.setOpaque(false);
        panelNb.add(buildLabel("Nombre de bons *"), BorderLayout.NORTH);
        txtNombreBons = new JTextField("1");
        styleField(txtNombreBons);
        txtNombreBons.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateMontantTotal(); }
        });
        panelNb.add(txtNombreBons, BorderLayout.CENTER);

        JPanel panelVal = new JPanel(new BorderLayout(0, 4));
        panelVal.setOpaque(false);
        panelVal.add(buildLabel("Valeur unitaire (Rs) *"), BorderLayout.NORTH);
        txtValeurUnitaire = new JTextField();
        styleField(txtValeurUnitaire);
        txtValeurUnitaire.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) { updateMontantTotal(); }
        });
        panelVal.add(txtValeurUnitaire, BorderLayout.CENTER);

        rowNbVal.add(panelNb);
        rowNbVal.add(panelVal);
        content.add(rowNbVal, gbc);

        // Montant total calculé
        gbc.gridy = row++;
        lblMontantTotal = new JLabel("Montant total : Rs 0.00");
        lblMontantTotal.setFont(VMSStyle.FONT_BTN_MAIN);
        lblMontantTotal.setForeground(VMSStyle.ACCENT_BLUE);
        content.add(lblMontantTotal, gbc);

        // Validité + Type (2 colonnes)
        gbc.gridy = row++;
        JPanel rowValidType = new JPanel(new GridLayout(1, 2, 16, 0));
        rowValidType.setOpaque(false);

        JPanel panelValid = new JPanel(new BorderLayout(0, 4));
        panelValid.setOpaque(false);
        panelValid.add(buildLabel("Validité (jours)"), BorderLayout.NORTH);
        txtValiditeJours = new JTextField("365");
        styleField(txtValiditeJours);
        panelValid.add(txtValiditeJours, BorderLayout.CENTER);

        JPanel panelType = new JPanel(new BorderLayout(0, 4));
        panelType.setOpaque(false);
        panelType.add(buildLabel("Type de Bon"), BorderLayout.NORTH);
        cbType = new JComboBox<>(new String[]{"Standard", "Cadeau", "Promo"});
        styleCombo(cbType);
        panelType.add(cbType, BorderLayout.CENTER);

        rowValidType.add(panelValid);
        rowValidType.add(panelType);
        content.add(rowValidType, gbc);

        // Email destinataire
        gbc.gridy = row++;
        content.add(buildLabel("Email destinataire (pour envoi des bons)"), gbc);
        gbc.gridy = row++;
        txtEmailDestinataire = new JTextField();
        styleField(txtEmailDestinataire);
        content.add(txtEmailDestinataire, gbc);

        // Motif
        gbc.gridy = row++;
        content.add(buildLabel("Motif / Commentaire"), gbc);
        gbc.gridy = row++;
        txtMotif = new JTextArea(3, 20);
        txtMotif.setFont(VMSStyle.FONT_CARD_DSC);
        txtMotif.setLineWrap(true);
        txtMotif.setWrapStyleWord(true);
        content.add(new JScrollPane(txtMotif), gbc);

        // Bouton soumettre
        gbc.gridy = row++;
        gbc.insets = new Insets(20, 0, 10, 0);
        JButton btnSubmit = new JButton("Soumettre la Demande");
        btnSubmit.setBackground(VMSStyle.RED_PRIMARY);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(VMSStyle.FONT_BTN_MAIN);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setPreferredSize(new Dimension(0, 42));
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> actionSubmit());
        content.add(btnSubmit, gbc);

        // Status
        gbc.gridy = row;
        lblStatus = new JLabel("");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(lblStatus, gbc);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        add(scroll, BorderLayout.CENTER);

        chargerDonnees();
    }

    private void chargerDonnees() {
        try {
            List<ClientDAO.ClientInfo> clients = ClientDAO.getActiveClients();
            for (ClientDAO.ClientInfo c : clients) cbClient.addItem(c);

            List<ClientDAO.MagasinInfo> magasins = ClientDAO.getAllMagasins();
            for (ClientDAO.MagasinInfo m : magasins) cbMagasin.addItem(m);

            // Pré-remplir l'email du client sélectionné
            cbClient.addActionListener(e -> {
                ClientDAO.ClientInfo sel = (ClientDAO.ClientInfo) cbClient.getSelectedItem();
                if (sel != null) txtEmailDestinataire.setText(sel.email);
            });
            if (cbClient.getItemCount() > 0) {
                ClientDAO.ClientInfo first = cbClient.getItemAt(0);
                txtEmailDestinataire.setText(first.email);
            }
        } catch (SQLException ex) {
            lblStatus.setText("Erreur chargement: " + ex.getMessage());
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
        }
    }

    private void updateMontantTotal() {
        try {
            int nb = Integer.parseInt(txtNombreBons.getText().trim());
            double val = Double.parseDouble(txtValeurUnitaire.getText().trim());
            lblMontantTotal.setText(String.format("Montant total : Rs %,.2f", nb * val));
        } catch (NumberFormatException e) {
            lblMontantTotal.setText("Montant total : Rs 0.00");
        }
    }

    private void actionSubmit() {
        ClientDAO.ClientInfo client = (ClientDAO.ClientInfo) cbClient.getSelectedItem();
        ClientDAO.MagasinInfo magasin = (ClientDAO.MagasinInfo) cbMagasin.getSelectedItem();
        String nbStr = txtNombreBons.getText().trim();
        String valStr = txtValeurUnitaire.getText().trim();
        String validStr = txtValiditeJours.getText().trim();
        String type = (String) cbType.getSelectedItem();
        String motif = txtMotif.getText().trim();
        String emailDest = txtEmailDestinataire.getText().trim();

        if (client == null || magasin == null || nbStr.isEmpty() || valStr.isEmpty()) {
            lblStatus.setText("Veuillez remplir tous les champs obligatoires (*).");
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        try {
            int nombreBons = Integer.parseInt(nbStr);
            double valeurUnitaire = Double.parseDouble(valStr);
            int validiteJours = validStr.isEmpty() ? 365 : Integer.parseInt(validStr);

            if (nombreBons <= 0 || valeurUnitaire <= 0) {
                lblStatus.setText("Le nombre de bons et la valeur doivent être positifs.");
                lblStatus.setForeground(VMSStyle.RED_PRIMARY);
                return;
            }

            int demandeId = VoucherDAO.createVoucherRequest(
                    userId, client.id, nombreBons, valeurUnitaire,
                    type, magasin.id, validiteJours, motif, emailDest);

            if (demandeId > 0) {
                lblStatus.setText("Demande créée avec succès ! (ID: " + demandeId + ")");
                lblStatus.setForeground(VMSStyle.SUCCESS);
                // Réinitialiser le formulaire
                txtNombreBons.setText("1");
                txtValeurUnitaire.setText("");
                txtMotif.setText("");
                updateMontantTotal();
            } else {
                lblStatus.setText("Erreur lors de la création.");
                lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            }
        } catch (NumberFormatException ex) {
            lblStatus.setText("Nombre de bons ou valeur invalide.");
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
        } catch (SQLException ex) {
            lblStatus.setText("Erreur SQL: " + ex.getMessage());
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
        }
    }

    private JLabel buildLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(VMSStyle.FONT_NAV);
        lbl.setForeground(VMSStyle.TEXT_SECONDARY);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setFont(VMSStyle.FONT_CARD_DSC);
        field.setPreferredSize(new Dimension(0, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setFont(VMSStyle.FONT_CARD_DSC);
        combo.setPreferredSize(new Dimension(0, 35));
    }
}
