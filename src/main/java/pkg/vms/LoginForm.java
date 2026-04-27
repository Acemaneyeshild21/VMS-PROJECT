package pkg.vms;

import pkg.vms.DAO.AuthDAO;
import pkg.vms.controller.LoginController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class LoginForm extends JFrame {

    private static final Color BG         = VMSStyle.BG_ROOT;
    private static final Color RED        = VMSStyle.RED_PRIMARY;
    private static final Color RED_DK     = VMSStyle.RED_DARK;
    private static final Color TEXT_P     = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S     = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M     = VMSStyle.TEXT_MUTED;
    private static final Color BORDER     = VMSStyle.BORDER_LIGHT;
    private static final Color SUCCESS    = VMSStyle.SUCCESS;

    private final LoginController loginController = new LoginController();

    private JTextField       txtUsername;
    private JPasswordField   txtPassword;
    private JLabel           lblError;
    private JButton          btnLogin;
    private boolean          showPassword = false;

    private int xOff, yOff;

    public LoginForm() {
        setTitle("Connexion \u2014 Intermart VMS");
        setSize(960, 580);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        root.add(buildLeftBrand(),  BorderLayout.WEST);
        root.add(buildRightForm(), BorderLayout.CENTER);

        enableDrag(root);
        setContentPane(root);
    }

    // =====================================================================
    //  LEFT BRANDING PANEL
    // =====================================================================
    private JPanel buildLeftBrand() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, RED, 0, getHeight(), RED_DK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Subtle pattern
                g2.setColor(new Color(255, 255, 255, 6));
                for (int i = 0; i < 30; i++) {
                    int y = i * 28;
                    g2.fillRect(0, y, getWidth(), 1);
                }

                // Decorative circles
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(-60, getHeight() - 200, 260, 260);
                g2.fillOval(getWidth() - 100, -80, 200, 200);
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(400, 0));
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(60, 44, 48, 44));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Logo
        JLabel cartIcon = new JLabel("\uD83D\uDED2");
        cartIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 44));
        cartIcon.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(cartIcon);
        content.add(Box.createVerticalStrut(18));

        JLabel brand = new JLabel("INTERMART");
        brand.setFont(new Font("Georgia", Font.BOLD, 32));
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(brand);

        JLabel tagline = new JLabel("Voucher Management System");
        tagline.setFont(new Font("Georgia", Font.ITALIC, 13));
        tagline.setForeground(new Color(255, 255, 255, 170));
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(tagline);

        // Separator
        content.add(Box.createVerticalStrut(36));
        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, new Color(255,255,255,100), getWidth(), 0, new Color(255,255,255,0)));
                g2.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize()  { return new Dimension(200, 1); }
            @Override public Dimension getMaximumSize()    { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        sep.setOpaque(false);
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(sep);
        content.add(Box.createVerticalStrut(32));

        // Features
        String[] features = {
                "Gestion compl\u00e8te des bons cadeau",
                "Suivi des demandes en temps r\u00e9el",
                "Multi-magasins & multi-r\u00f4les",
                "G\u00e9n\u00e9ration PDF avec QR Code",
                "R\u00e9demption s\u00e9curis\u00e9e anti-fraude"
        };
        for (String f : features) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

            JLabel check = new JLabel("\u2713  ");
            check.setFont(new Font("Trebuchet MS", Font.BOLD, 13));
            check.setForeground(new Color(255, 255, 255, 220));
            JLabel txt = new JLabel(f);
            txt.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
            txt.setForeground(new Color(255, 255, 255, 180));
            row.add(check);
            row.add(txt);
            content.add(row);
            content.add(Box.createVerticalStrut(6));
        }

        p.add(content, BorderLayout.CENTER);

        // Footer
        JLabel copy = new JLabel("\u00a9 2025 Intermart Maurice. Tous droits r\u00e9serv\u00e9s.");
        copy.setFont(new Font("Trebuchet MS", Font.PLAIN, 10));
        copy.setForeground(new Color(255, 255, 255, 100));
        p.add(copy, BorderLayout.SOUTH);

        return p;
    }

    // =====================================================================
    //  RIGHT FORM PANEL
    // =====================================================================
    private JPanel buildRightForm() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 60, 0, 60);

        // Close button
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 0, 0, 16);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.NONE;
        JButton btnClose = buildCloseBtn();
        p.add(btnClose, gbc);

        // Reset constraints
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 60, 0, 60);

        // Welcome
        gbc.gridy = 1;
        gbc.insets = new Insets(10, 60, 4, 60);
        JLabel lblWelcome = new JLabel("Connexion");
        lblWelcome.setFont(new Font("Georgia", Font.BOLD, 28));
        lblWelcome.setForeground(TEXT_P);
        p.add(lblWelcome, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 60, 28, 60);
        JLabel lblSub = new JLabel("Acc\u00e9dez \u00e0 votre espace de gestion Intermart");
        lblSub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_M);
        p.add(lblSub, gbc);

        // Username field
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 60, 4, 60);
        JLabel lblUser = new JLabel("Nom d'utilisateur");
        lblUser.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        lblUser.setForeground(TEXT_S);
        p.add(lblUser, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 60, 16, 60);
        txtUsername = new JTextField();
        styleField(txtUsername, "\u2302  Entrez votre identifiant");
        p.add(txtUsername, gbc);

        // Password field
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 60, 4, 60);
        JLabel lblPass = new JLabel("Mot de passe");
        lblPass.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        lblPass.setForeground(TEXT_S);
        p.add(lblPass, gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 60, 8, 60);
        JPanel passRow = buildPasswordField();
        p.add(passRow, gbc);

        // Error message
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 60, 12, 60);
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblError.setForeground(RED);
        p.add(lblError, gbc);

        // Login button
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 60, 16, 60);
        btnLogin = buildLoginButton();
        p.add(btnLogin, gbc);

        // Register link
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 60, 0, 60);
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        JLabel lblNoAccount = new JLabel("Pas encore de compte ?");
        lblNoAccount.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblNoAccount.setForeground(TEXT_M);
        JLabel lblRegister = new JLabel("S'inscrire");
        lblRegister.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        lblRegister.setForeground(RED);
        lblRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblRegister.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { ouvrirInscription(); }
            public void mouseEntered(MouseEvent e) { lblRegister.setForeground(RED_DK); }
            public void mouseExited(MouseEvent e)  { lblRegister.setForeground(RED); }
        });
        linkRow.add(lblNoAccount);
        linkRow.add(lblRegister);
        p.add(linkRow, gbc);

        // Enter key
        txtPassword.addActionListener(e -> actionLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        return p;
    }

    // ── Styled text field ──────────────────────────────────────────────────
    private void styleField(JTextField field, String placeholder) {
        field.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        field.setForeground(TEXT_P);
        field.setCaretColor(RED);
        field.setPreferredSize(new Dimension(0, 44));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        // Placeholder
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { field.repaint(); }
            public void focusLost(FocusEvent e)   { field.repaint(); }
        });

        field.setUI(new javax.swing.plaf.basic.BasicTextFieldUI() {
            @Override protected void paintSafely(Graphics g) {
                super.paintSafely(g);
                if (field.getText().isEmpty() && !field.hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
                    g2.setColor(TEXT_M);
                    Insets i = field.getInsets();
                    g2.drawString(placeholder, i.left, field.getHeight() / 2 + 5);
                }
            }
        });
    }

    // ── Password field with toggle ─────────────────────────────────────────
    private JPanel buildPasswordField() {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setBackground(Color.WHITE);
        row.setPreferredSize(new Dimension(0, 44));
        row.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        txtPassword.setForeground(TEXT_P);
        txtPassword.setCaretColor(RED);
        txtPassword.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 8));
        txtPassword.setOpaque(false);

        JButton btnToggle = new JButton("\u25CF") {
            {
                setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
                setForeground(TEXT_M);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(44, 44));
                setToolTipText("Afficher / masquer le mot de passe");
                addActionListener(e -> {
                    showPassword = !showPassword;
                    if (showPassword) {
                        txtPassword.setEchoChar((char) 0);
                        setText("\u25CB");
                    } else {
                        txtPassword.setEchoChar('\u2022');
                        setText("\u25CF");
                    }
                });
            }
        };

        row.add(txtPassword, BorderLayout.CENTER);
        row.add(btnToggle, BorderLayout.EAST);
        return row;
    }

    // ── Login button ───────────────────────────────────────────────────────
    private JButton buildLoginButton() {
        JButton btn = new JButton("Se connecter") {
            boolean hov = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 15));
                setForeground(Color.WHITE);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(0, 48));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
                addActionListener(e -> actionLogin());
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = hov
                        ? new GradientPaint(0, 0, RED_DK, getWidth(), 0, RED)
                        : new GradientPaint(0, 0, RED, getWidth(), 0, RED_DK);
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ── Close button ───────────────────────────────────────────────────────
    private JButton buildCloseBtn() {
        JButton btn = new JButton("\u2715") {
            boolean hov = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 13));
                setForeground(TEXT_M);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(32, 32));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true; setForeground(RED); repaint(); }
                    public void mouseExited(MouseEvent e)  { hov = false; setForeground(TEXT_M); repaint(); }
                });
                addActionListener(e -> System.exit(0));
            }
            @Override protected void paintComponent(Graphics g) {
                if (hov) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(RED.getRed(), RED.getGreen(), RED.getBlue(), 12));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 6, 6));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ── LOGIN ACTION ───────────────────────────────────────────────────────
    private void actionLogin() {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Connexion en cours...");
        lblError.setText(" ");

        loginController.authenticate(user, pass,
            session -> {
                dispose();
                SwingUtilities.invokeLater(() ->
                    new Dashboard(session.userId, session.username, session.role, session.email)
                            .setVisible(true));
            },
            errMsg -> {
                showError(errMsg);
                btnLogin.setEnabled(true);
                btnLogin.setText("Se connecter");
            }
        );
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setForeground(RED);
    }

    // ── INSCRIPTION ────────────────────────────────────────────────────────
    private void ouvrirInscription() {
        dispose();
        SwingUtilities.invokeLater(() -> {
            new InscriptionForm().setVisible(true);
        });
    }

    // ── DRAG ───────────────────────────────────────────────────────────────
    private void enableDrag(JPanel panel) {
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { xOff = e.getX(); yOff = e.getY(); }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                setLocation(e.getXOnScreen() - xOff, e.getYOnScreen() - yOff);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
