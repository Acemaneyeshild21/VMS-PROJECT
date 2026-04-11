package pkg.vms;

import pkg.vms.DAO.AuthDAO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.SQLException;

public class LoginForm extends JFrame {

    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JLabel lblMessage;

    private int xOffset = 0;
    private int yOffset = 0;

    public LoginForm() {
        setTitle("Connexion — Intermart VMS");
        setSize(900, 600);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_ROOT);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
            }
        };
        root.setOpaque(false);

        JPanel mainLayout = new JPanel(new GridLayout(1, 2));
        mainLayout.setOpaque(false);

        mainLayout.add(buildLeftPanel());
        mainLayout.add(buildRightPanel());

        root.add(mainLayout, BorderLayout.CENTER);
        
        root.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                xOffset = e.getX();
                yOffset = e.getY();
            }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xOffset, e.getYOnScreen() - yOffset);
            }
        });

        setContentPane(root);
        setBackground(new Color(0, 0, 0, 0));
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, VMSStyle.RED_PRIMARY, getWidth(), getHeight(), VMSStyle.RED_DARK);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
                
                g2.setColor(new Color(255, 255, 255, 10));
                for (int i = 0; i < 20; i++) {
                    g2.rotate(Math.toRadians(20), getWidth()/2.0, getHeight()/2.0);
                    g2.fillRect(-100, i * 40, getWidth() + 200, 2);
                    g2.rotate(Math.toRadians(-20), getWidth()/2.0, getHeight()/2.0);
                }
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(60, 48, 60, 40));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel logoIcon = new JLabel("🛒");
        logoIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 52));
        logoIcon.setForeground(Color.WHITE);

        JLabel lblName = new JLabel("INTERMART");
        lblName.setFont(VMSStyle.FONT_BRAND);
        lblName.setForeground(Color.WHITE);

        JLabel lblTagline = new JLabel("Voucher Management System");
        lblTagline.setFont(VMSStyle.FONT_SUBTITLE);
        lblTagline.setForeground(new Color(255, 255, 255, 180));

        content.add(logoIcon);
        content.add(Box.createVerticalStrut(20));
        content.add(lblName);
        content.add(lblTagline);
        content.add(Box.createVerticalStrut(40));

        String[] features = {
            "✓  Gestion des bons cadeau",
            "✓  Suivi des demandes en temps réel",
            "✓  Multi-magasins & multi-rôles",
            "✓  Rapports Excel automatisés"
        };
        for (String f : features) {
            JLabel lbl = new JLabel(f);
            lbl.setFont(VMSStyle.FONT_NAV);
            lbl.setForeground(new Color(255, 255, 255, 200));
            content.add(lbl);
            content.add(Box.createVerticalStrut(10));
        }

        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 56, 0, 56));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        JButton btnClose = new JButton("✕");
        btnClose.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnClose.setBorderPainted(false);
        btnClose.setContentAreaFilled(false);
        btnClose.setFocusPainted(false);
        btnClose.setForeground(VMSStyle.TEXT_MUTED);
        btnClose.addActionListener(e -> System.exit(0));
        
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setOpaque(false);
        topBar.add(btnClose);
        
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(topBar, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(20, 0, 10, 0);
        JLabel lblWelcome = new JLabel("Bienvenue");
        lblWelcome.setFont(VMSStyle.FONT_BRAND.deriveFont(30f));
        lblWelcome.setForeground(VMSStyle.TEXT_PRIMARY);
        panel.add(lblWelcome, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 30, 0);
        JLabel lblSub = new JLabel("Connectez-vous à votre espace VMS");
        lblSub.setFont(VMSStyle.FONT_NAV);
        lblSub.setForeground(VMSStyle.TEXT_MUTED);
        panel.add(lblSub, gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(new JLabel("Nom d'utilisateur"), gbc);
        
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 20, 0);
        txtUsername = new JTextField();
        txtUsername.setPreferredSize(new Dimension(0, 40));
        panel.add(txtUsername, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 5, 0);
        panel.add(new JLabel("Mot de passe"), gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 30, 0);
        txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(0, 40));
        panel.add(txtPassword, gbc);

        gbc.gridy = 7;
        JButton btnLogin = new JButton("Se connecter");
        btnLogin.setBackground(VMSStyle.RED_PRIMARY);
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setPreferredSize(new Dimension(0, 45));
        btnLogin.setFont(VMSStyle.FONT_BTN_MAIN);
        btnLogin.addActionListener(e -> actionLogin());
        panel.add(btnLogin, gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(10, 0, 0, 0);
        lblMessage = new JLabel("");
        lblMessage.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(lblMessage, gbc);

        return panel;
    }

    private void actionLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            lblMessage.setText("Veuillez remplir tous les champs.");
            lblMessage.setForeground(VMSStyle.RED_PRIMARY);
            return;
        }

        try {
            AuthDAO.UserSession session = AuthDAO.authenticate(user, pass);
            if (session != null) {
                SwingUtilities.invokeLater(() -> {
                    Dashboard dashboard = new Dashboard(session.userId, session.username, session.role, session.email);
                    dashboard.setVisible(true);
                    this.dispose();
                });
            } else {
                lblMessage.setText("Identifiants incorrects.");
                lblMessage.setForeground(VMSStyle.RED_PRIMARY);
            }
        } catch (SQLException ex) {
            lblMessage.setText("Erreur base de données.");
            lblMessage.setForeground(VMSStyle.RED_PRIMARY);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginForm().setVisible(true);
        });
    }
}
