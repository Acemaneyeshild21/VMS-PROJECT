package pkg.vms;

import pkg.vms.DAO.UserDAO;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class FormulaireUtilisateur extends JPanel {

    private JTextField txtUsername;
    private JTextField txtEmail;
    private JPasswordField txtPassword;
    private JComboBox<String> cbRole;
    private JLabel lblMessage;

    public FormulaireUtilisateur() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Inscription Nouvel Utilisateur");
        title.setFont(VMSStyle.FONT_BRAND.deriveFont(24f));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Champs
        gbc.gridy = 0;
        content.add(new JLabel("Nom d'utilisateur"), gbc);
        gbc.gridy = 1;
        txtUsername = new JTextField();
        txtUsername.setPreferredSize(new Dimension(0, 35));
        content.add(txtUsername, gbc);

        gbc.gridy = 2;
        content.add(new JLabel("Email"), gbc);
        gbc.gridy = 3;
        txtEmail = new JTextField();
        txtEmail.setPreferredSize(new Dimension(0, 35));
        content.add(txtEmail, gbc);

        gbc.gridy = 4;
        content.add(new JLabel("Mot de passe"), gbc);
        gbc.gridy = 5;
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(0, 35));
        content.add(txtPassword, gbc);

        gbc.gridy = 6;
        content.add(new JLabel("R\u00f4le"), gbc);
        gbc.gridy = 7;
        cbRole = new JComboBox<>(new String[]{"Collaborateur", "Manager", "Administrateur"});
        cbRole.setPreferredSize(new Dimension(0, 35));
        content.add(cbRole, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(20, 0, 10, 0);
        JButton btnRegister = new JButton("Cr\u00e9er le compte");
        btnRegister.setBackground(VMSStyle.RED_PRIMARY);
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFont(VMSStyle.FONT_BTN_MAIN);
        btnRegister.addActionListener(e -> actionRegister());
        content.add(btnRegister, gbc);

        gbc.gridy = 9;
        lblMessage = new JLabel("");
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(lblMessage, gbc);

        add(content, BorderLayout.CENTER);
    }

    private void actionRegister() {
        String user = txtUsername.getText().trim();
        String email = txtEmail.getText().trim();
        String pass = new String(txtPassword.getPassword());
        String role = (String) cbRole.getSelectedItem();

        if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            lblMessage.setText("Tous les champs sont requis.");
            lblMessage.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        try {
            if (UserDAO.registerUser(user, email, pass, role)) {
                lblMessage.setText("Utilisateur cr\u00e9\u00e9 avec succ\u00e8s !");
                lblMessage.setForeground(VMSStyle.SUCCESS);
                clearFields();
            } else {
                lblMessage.setText("Erreur lors de la cr\u00e9ation.");
                lblMessage.setForeground(VMSStyle.RED_PRIMARY);
            }
        } catch (SQLException ex) {
            lblMessage.setText("Erreur SQL: " + ex.getMessage());
            lblMessage.setForeground(VMSStyle.RED_PRIMARY);
        }
    }

    private void clearFields() {
        txtUsername.setText("");
        txtEmail.setText("");
        txtPassword.setText("");
        cbRole.setSelectedIndex(0);
    }
}
