package pkg.vms;

import pkg.vms.DAO.AuthDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Écran de connexion — design InvoiceNinja-like.
 * Split gauche (hero brand) / droite (formulaire clean).
 */
public class LoginForm extends JFrame {

    private static final Color RED     = VMSStyle.RED_PRIMARY;
    private static final Color RED_DK  = VMSStyle.RED_DARK;
    private static final Color TEXT_P  = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M  = VMSStyle.TEXT_MUTED;
    private static final Color BORDER  = VMSStyle.BORDER_LIGHT;
    private static final Color SUCCESS = VMSStyle.SUCCESS;

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblError;
    private JButton        btnLogin;

    private int xOff, yOff;

    public LoginForm() {
        setTitle("Connexion — Voucher System");
        setSize(980, 620);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        root.add(buildLeftBrand(),  BorderLayout.WEST);
        root.add(buildRightForm(),  BorderLayout.CENTER);

        enableDrag(root);
        setContentPane(root);
    }

    // =====================================================================
    //  LEFT HERO PANEL
    // =====================================================================
    private JPanel buildLeftBrand() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dégradé diagonal rouge profond
                GradientPaint gp = new GradientPaint(0, 0, RED, getWidth(), getHeight(), RED_DK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Blobs décoratifs (formes douces)
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(-80, -100, 320, 320);
                g2.fillOval(getWidth() - 180, getHeight() - 240, 340, 340);

                // Grille très subtile
                g2.setColor(new Color(255, 255, 255, 8));
                for (int y = 0; y < getHeight(); y += 40) {
                    g2.drawLine(0, y, getWidth(), y);
                }
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(420, 0));
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(52, 48, 40, 48));

        // ─── Header : logo vectoriel + nom ───
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        IntermartLogo logo = new IntermartLogo(IntermartLogo.Variant.DARK, 42, "Voucher System", "", false);
        header.add(logo);
        p.add(header, BorderLayout.NORTH);

        // ─── Centre : pitch marketing ───
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));

        JLabel heroTitle = new JLabel("<html>Gérez vos bons cadeau<br/>en toute simplicité.</html>");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(heroTitle);
        center.add(Box.createVerticalStrut(14));

        JLabel heroSub = new JLabel("<html><body style='width:320px'>"
                + "Plateforme complète de gestion des bons cadeau — "
                + "émission, validation, suivi et rédemption en temps réel.</body></html>");
        heroSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        heroSub.setForeground(new Color(255, 255, 255, 200));
        heroSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(heroSub);
        center.add(Box.createVerticalStrut(32));

        // Stats cards (petites cartes transparentes)
        JPanel stats = new JPanel(new GridLayout(1, 3, 12, 0));
        stats.setOpaque(false);
        stats.setAlignmentX(Component.LEFT_ALIGNMENT);
        stats.setMaximumSize(new Dimension(340, 78));
        stats.setPreferredSize(new Dimension(340, 78));
        stats.add(buildStatCard("6", "Rôles"));
        stats.add(buildStatCard("100%", "Traçable"));
        stats.add(buildStatCard("24/7", "Disponible"));
        center.add(stats);

        p.add(center, BorderLayout.CENTER);

        // ─── Footer ───
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footer.setOpaque(false);
        JLabel copy = new JLabel("© 2026 Voucher System");
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copy.setForeground(new Color(255, 255, 255, 130));
        footer.add(copy);
        p.add(footer, BorderLayout.SOUTH);

        return p;
    }

    private JPanel buildStatCard(String value, String label) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 26));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(new Color(255, 255, 255, 40));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 20));
        val.setForeground(Color.WHITE);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lbl.setForeground(new Color(255, 255, 255, 180));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(val);
        card.add(Box.createVerticalStrut(2));
        card.add(lbl);
        return card;
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

        // Close button (top-right)
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 0, 0, 18);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.NONE;
        p.add(buildCloseBtn(), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // Titre
        gbc.gridy = 1;
        gbc.insets = new Insets(20, 64, 4, 64);
        JLabel lblWelcome = new JLabel("Bon retour parmi nous");
        lblWelcome.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblWelcome.setForeground(TEXT_P);
        p.add(lblWelcome, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 64, 32, 64);
        JLabel lblSub = new JLabel("Connectez-vous à votre espace Voucher System");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_M);
        p.add(lblSub, gbc);

        // Username
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 64, 6, 64);
        p.add(UIUtils.buildFormLabel("Nom d'utilisateur"), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 64, 16, 64);
        txtUsername = new JTextField();
        UIUtils.styleModernField(txtUsername, "Entrez votre identifiant");
        p.add(UIUtils.wrapModernField(txtUsername), gbc);

        // Password label row (with forgot link)
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 64, 6, 64);
        JPanel pwdLabelRow = new JPanel(new BorderLayout());
        pwdLabelRow.setOpaque(false);
        pwdLabelRow.add(UIUtils.buildFormLabel("Mot de passe"), BorderLayout.WEST);
        JLabel forgot = new JLabel("Mot de passe oublié ?");
        forgot.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        forgot.setForeground(RED);
        forgot.setCursor(new Cursor(Cursor.HAND_CURSOR));
        forgot.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { forgot.setForeground(RED_DK); }
            public void mouseExited(MouseEvent e)  { forgot.setForeground(RED); }
            public void mouseClicked(MouseEvent e) {
                ResetPasswordDialog.show(LoginForm.this);
            }
        });
        pwdLabelRow.add(forgot, BorderLayout.EAST);
        p.add(pwdLabelRow, gbc);

        // Password field
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 64, 12, 64);
        txtPassword = new JPasswordField();
        p.add(UIUtils.wrapModernPasswordField(txtPassword), gbc);

        // Error message
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 64, 14, 64);
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(RED);
        p.add(lblError, gbc);

        // Login button
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 64, 18, 64);
        btnLogin = UIUtils.buildPrimaryButton("Se connecter", 0, 46);
        btnLogin.addActionListener(e -> actionLogin());
        p.add(btnLogin, gbc);

        // Register link
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 64, 20, 64);
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        JLabel lblNoAccount = new JLabel("Pas encore de compte ?");
        lblNoAccount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblNoAccount.setForeground(TEXT_S);
        JLabel lblRegister = new JLabel("Créer un compte");
        lblRegister.setFont(new Font("Segoe UI", Font.BOLD, 12));
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

        // Enter key bindings
        txtPassword.addActionListener(e -> actionLogin());
        txtUsername.addActionListener(e -> txtPassword.requestFocusInWindow());

        return p;
    }

    // ── Close button (vectoriel, style macOS-like) ────────────────────────
    private JButton buildCloseBtn() {
        JButton btn = new JButton() {
            boolean hov = false;
            {
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(32, 32));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
                addActionListener(e -> System.exit(0));
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hov) {
                    g2.setColor(VMSStyle.RED_LIGHT);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                }
                g2.setColor(hov ? RED : TEXT_M);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int pad = 11;
                g2.drawLine(pad, pad, getWidth() - pad, getHeight() - pad);
                g2.drawLine(getWidth() - pad, pad, pad, getHeight() - pad);
                g2.dispose();
            }
        };
        return btn;
    }

    // ── LOGIN ACTION ──────────────────────────────────────────────────────
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

        new SwingWorker<AuthDAO.UserSession, Void>() {
            @Override protected AuthDAO.UserSession doInBackground() throws Exception {
                return AuthDAO.authenticate(user, pass);
            }
            @Override protected void done() {
                try {
                    AuthDAO.UserSession session = get();
                    if (session != null) {
                        dispose();
                        SwingUtilities.invokeLater(() ->
                                new Dashboard(session.userId, session.username, session.role, session.email)
                                        .setVisible(true));
                    } else {
                        showError("Identifiants incorrects. Vérifiez votre nom d'utilisateur et mot de passe.");
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Se connecter");
                    }
                } catch (Exception ex) {
                    showError("Erreur de connexion à la base de données.");
                    btnLogin.setEnabled(true);
                    btnLogin.setText("Se connecter");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setForeground(RED);
    }

    private void ouvrirInscription() {
        dispose();
        SwingUtilities.invokeLater(() -> new InscriptionForm().setVisible(true));
    }

    // ── DRAG ──────────────────────────────────────────────────────────────
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
        ThemeManager.loadAndApply();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }
}
