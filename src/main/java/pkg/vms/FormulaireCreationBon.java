package pkg.vms;

import pkg.vms.DAO.ClientDAO;
import pkg.vms.controller.DemandeController;
import javax.swing.*;
import java.awt.*;

public class FormulaireCreationBon extends JPanel {

    private final int userId;
    private final String username;
    private final DemandeController controller = new DemandeController();

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
    private JButton btnSubmit;

    public FormulaireCreationBon(int userId, String username) {
        this.userId = userId;
        this.username = username;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Nouvelle Demande de Bons Cadeau");
        title.setFont(VMSStyle.FONT_BRAND.deriveFont(24f));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 0, 6, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        int row = 0;

        gbc.gridy = row++;
        content.add(buildLabel("Client *"), gbc);
        gbc.gridy = row++;
        cbClient = new JComboBox<>();
        styleCombo(cbClient);
        content.add(cbClient, gbc);

        gbc.gridy = row++;
        content.add(buildLabel("Magasin *"), gbc);
        gbc.gridy = row++;
        cbMagasin = new JComboBox<>();
        styleCombo(cbMagasin);
        content.add(cbMagasin, gbc);

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

        gbc.gridy = row++;
        lblMontantTotal = new JLabel("Montant total : Rs 0.00");
        lblMontantTotal.setFont(VMSStyle.FONT_BTN_MAIN);
        lblMontantTotal.setForeground(VMSStyle.ACCENT_BLUE);
        content.add(lblMontantTotal, gbc);

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

        gbc.gridy = row++;
        content.add(buildLabel("Email destinataire (pour envoi des bons)"), gbc);
        gbc.gridy = row++;
        txtEmailDestinataire = new JTextField();
        styleField(txtEmailDestinataire);
        content.add(txtEmailDestinataire, gbc);

        gbc.gridy = row++;
        content.add(buildLabel("Motif / Commentaire"), gbc);
        gbc.gridy = row++;
        txtMotif = new JTextArea(3, 20);
        txtMotif.setFont(VMSStyle.FONT_CARD_DSC);
        txtMotif.setLineWrap(true);
        txtMotif.setWrapStyleWord(true);
        content.add(new JScrollPane(txtMotif), gbc);

        gbc.gridy = row++;
        gbc.insets = new Insets(20, 0, 10, 0);
        btnSubmit = new JButton("Soumettre la Demande");
        btnSubmit.setBackground(VMSStyle.RED_PRIMARY);
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFont(VMSStyle.FONT_BTN_MAIN);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setPreferredSize(new Dimension(0, 42));
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.addActionListener(e -> actionSubmit());
        content.add(btnSubmit, gbc);

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
        lblStatus.setText("Chargement...");
        lblStatus.setForeground(VMSStyle.TEXT_MUTED);
        controller.chargerDonneesFormulaire(
            data -> {
                for (ClientDAO.ClientInfo c : data.clients)   cbClient.addItem(c);
                for (ClientDAO.MagasinInfo m : data.magasins) cbMagasin.addItem(m);
                cbClient.addActionListener(e -> {
                    ClientDAO.ClientInfo sel = (ClientDAO.ClientInfo) cbClient.getSelectedItem();
                    if (sel != null) txtEmailDestinataire.setText(sel.email);
                });
                if (cbClient.getItemCount() > 0) {
                    ClientDAO.ClientInfo first = cbClient.getItemAt(0);
                    txtEmailDestinataire.setText(first.email);
                }
                lblStatus.setText("");
            },
            err -> {
                lblStatus.setText("Erreur chargement : " + err);
                lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            }
        );
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
        String nbStr    = txtNombreBons.getText().trim();
        String valStr   = txtValeurUnitaire.getText().trim();
        String validStr = txtValiditeJours.getText().trim();
        String type     = (String) cbType.getSelectedItem();
        String motif    = txtMotif.getText().trim();
        String emailDest = txtEmailDestinataire.getText().trim();

        if (client == null || magasin == null || nbStr.isEmpty() || valStr.isEmpty()) {
            lblStatus.setText("Veuillez remplir tous les champs obligatoires (*).");
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        int nombreBons;
        double valeurUnitaire;
        int validiteJours;
        try {
            nombreBons     = Integer.parseInt(nbStr);
            valeurUnitaire = Double.parseDouble(valStr);
            validiteJours  = validStr.isEmpty() ? 365 : Integer.parseInt(validStr);
        } catch (NumberFormatException ex) {
            lblStatus.setText("Nombre de bons ou valeur invalide.");
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        if (nombreBons <= 0 || valeurUnitaire <= 0) {
            lblStatus.setText("Le nombre de bons et la valeur doivent être positifs.");
            lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        btnSubmit.setEnabled(false);
        lblStatus.setText("Création en cours...");
        lblStatus.setForeground(VMSStyle.TEXT_MUTED);

        controller.creerDemande(userId, client.id, nombreBons, valeurUnitaire,
            type, magasin.id, validiteJours, motif, emailDest,
            demandeId -> {
                btnSubmit.setEnabled(true);
                lblStatus.setText("Demande créée avec succès ! (ID: " + demandeId + ")");
                lblStatus.setForeground(VMSStyle.SUCCESS);
                txtNombreBons.setText("1");
                txtValeurUnitaire.setText("");
                txtMotif.setText("");
                updateMontantTotal();
            },
            err -> {
                btnSubmit.setEnabled(true);
                lblStatus.setText("Erreur : " + err);
                lblStatus.setForeground(VMSStyle.RED_PRIMARY);
            }
        );
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
