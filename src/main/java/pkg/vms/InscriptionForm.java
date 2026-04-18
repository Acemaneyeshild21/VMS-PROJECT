package pkg.vms;

import pkg.vms.DAO.UserDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Écran d'inscription — design InvoiceNinja-like, cohérent avec LoginForm.
 */
public class InscriptionForm extends JFrame {

    private static final Color RED     = VMSStyle.RED_PRIMARY;
    private static final Color RED_DK  = VMSStyle.RED_DARK;
    private static final Color TEXT_P  = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M  = VMSStyle.TEXT_MUTED;
    private static final Color BORDER  = VMSStyle.BORDER_LIGHT;
    private static final Color SUCCESS = VMSStyle.SUCCESS;

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static final String[] ROLES = {
            "Administrateur", "Manager", "Comptable", "Approbateur",
            "Collaborateur", "Superviseur_Magasin"
    };

    private JTextField      txtUsername;
    private JTextField      txtEmail;
    private JPasswordField  txtPassword;
    private JPasswordField  txtConfirm;
    private JComboBox<String> cboRole;
    private JLabel          lblError;
    private JButton         btnRegister;

    private int xOff, yOff;

    public InscriptionForm() {
        setTitle("Inscription — Voucher System");
        setSize(980, 720);
        setUndecorated(true);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        root.add(buildLeftBrand(), BorderLayout.WEST);
        root.add(buildRightForm(), BorderLayout.CENTER);

        enableDrag(root);
        setContentPane(root);
    }

    // =====================================================================
    //  LEFT HERO PANEL
    // =====================================================================
    private JPanel buildLeftBrand() {
        JPanel p = getJPanel();

        // Header logo
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        header.setOpaque(false);
        IntermartLogo logo = new IntermartLogo(IntermartLogo.Variant.DARK, 42, "Voucher System", "", false);
        header.add(logo);
        p.add(header, BorderLayout.NORTH);

        // Pitch
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(40, 0, 0, 0));

        JLabel heroTitle = new JLabel("<html>Rejoignez la plateforme<br/>Voucher System.</html>");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heroTitle.setForeground(Color.WHITE);
        heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(heroTitle);
        center.add(Box.createVerticalStrut(14));

        JLabel heroSub = new JLabel("<html><body style='width:300px'>"
                + "Créez votre compte en quelques secondes et accédez aux fonctionnalités "
                + "selon votre rôle : gestion, validation, suivi, et plus encore.</body></html>");
        heroSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        heroSub.setForeground(new Color(255, 255, 255, 200));
        heroSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(heroSub);
        center.add(Box.createVerticalStrut(30));

        // Avantages list
        String[] advantages = {
                "Accès sécurisé selon votre rôle",
                "Interface claire et rapide",
                "Notifications temps réel",
                "Multi-magasins, multi-rôles"
        };
        for (String a : advantages) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(340, 28));

            JPanel check = getCheck();

            JLabel txt = new JLabel(a);
            txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            txt.setForeground(new Color(255, 255, 255, 220));
            txt.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

            row.add(check);
            row.add(txt);
            center.add(row);
            center.add(Box.createVerticalStrut(8));
        }

        p.add(center, BorderLayout.CENTER);

        JLabel copy = new JLabel("© 2026 Voucher System");
        copy.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        copy.setForeground(new Color(255, 255, 255, 130));
        p.add(copy, BorderLayout.SOUTH);

        return p;
    }

    private JPanel getCheck() {
        JPanel check = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 4, cy, cx - 1, cy + 3);
                g2.drawLine(cx - 1, cy + 3, cx + 5, cy - 3);
                g2.dispose();
            }
            @Override public Dimension getPreferredSize() { return new Dimension(18, 18); }
        };
        check.setOpaque(false);
        return check;
    }

    private JPanel getJPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, RED, getWidth(), getHeight(), RED_DK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(-80, -100, 320, 320);
                g2.fillOval(getWidth() - 180, getHeight() - 240, 340, 340);

                g2.setColor(new Color(255, 255, 255, 8));
                for (int y = 0; y < getHeight(); y += 40) {
                    g2.drawLine(0, y, getWidth(), y);
                }
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(400, 0));
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(52, 48, 40, 48));
        return p;
    }

    // =====================================================================
    //  RIGHT FORM PANEL (scrollable)
    // =====================================================================
    private JPanel buildRightForm() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Close btn
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 0, 0, 18);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.NONE;
        p.add(buildCloseBtn(), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        gbc.gridy = 1;
        gbc.insets = new Insets(12, 54, 4, 54);
        JLabel lblTitle = new JLabel("Créer un compte");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setForeground(TEXT_P);
        p.add(lblTitle, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 54, 24, 54);
        JLabel lblSub = new JLabel("Remplissez vos informations pour commencer");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_M);
        p.add(lblSub, gbc);

        // Username
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 54, 6, 54);
        p.add(UIUtils.buildFormLabel("Nom d'utilisateur"), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 54, 12, 54);
        txtUsername = new JTextField();
        UIUtils.styleModernField(txtUsername, "ex. j.dupont");
        p.add(UIUtils.wrapModernField(txtUsername), gbc);

        // Email
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 54, 6, 54);
        p.add(UIUtils.buildFormLabel("Adresse e-mail"), gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 54, 12, 54);
        txtEmail = new JTextField();
        UIUtils.styleModernField(txtEmail, "exemple@voucher.mu");
        p.add(UIUtils.wrapModernField(txtEmail), gbc);

        // Password
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 54, 6, 54);
        p.add(UIUtils.buildFormLabel("Mot de passe"), gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(0, 54, 12, 54);
        txtPassword = new JPasswordField();
        p.add(UIUtils.wrapModernPasswordField(txtPassword), gbc);

        // Confirm
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 54, 6, 54);
        p.add(UIUtils.buildFormLabel("Confirmer le mot de passe"), gbc);

        gbc.gridy = 10;
        gbc.insets = new Insets(0, 54, 12, 54);
        txtConfirm = new JPasswordField();
        p.add(UIUtils.wrapModernPasswordField(txtConfirm), gbc);

        // Role
        gbc.gridy = 11;
        gbc.insets = new Insets(0, 54, 6, 54);
        p.add(UIUtils.buildFormLabel("Rôle"), gbc);

        gbc.gridy = 12;
        gbc.insets = new Insets(0, 54, 12, 54);
        cboRole = new JComboBox<>(ROLES);
        p.add(wrapModernCombo(cboRole), gbc);

        // Error
        gbc.gridy = 13;
        gbc.insets = new Insets(0, 54, 8, 54);
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblError.setForeground(RED);
        p.add(lblError, gbc);

        // Register button
        gbc.gridy = 14;
        gbc.insets = new Insets(0, 54, 14, 54);
        btnRegister = UIUtils.buildPrimaryButton("Créer mon compte", 0, 46);
        btnRegister.addActionListener(e -> actionRegister());
        p.add(btnRegister, gbc);

        // Link back to login
        gbc.gridy = 15;
        gbc.insets = new Insets(0, 54, 22, 54);
        JPanel linkRow = getLinkRow();
        p.add(linkRow, gbc);

        txtConfirm.addActionListener(e -> actionRegister());

        JScrollPane scroll = new JScrollPane(p);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    private JPanel getLinkRow() {
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        JLabel lblHas = new JLabel("Déjà un compte ?");
        lblHas.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHas.setForeground(TEXT_S);
        JLabel lblLogin = getJLabel();
        linkRow.add(lblHas);
        linkRow.add(lblLogin);
        return linkRow;
    }

    private JLabel getJLabel() {
        JLabel lblLogin = new JLabel("Se connecter");
        lblLogin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblLogin.setForeground(RED);
        lblLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { ouvrirLogin(); }
            public void mouseEntered(MouseEvent e) { lblLogin.setForeground(RED_DK); }
            public void mouseExited(MouseEvent e)  { lblLogin.setForeground(RED); }
        });
        return lblLogin;
    }

    // ── Modern ComboBox wrapper ───────────────────────────────────────────
    private JPanel wrapModernCombo(JComboBox<String> cbo) {
        JPanel wrapper = getPanel();

        cbo.setFont(VMSStyle.FONT_INPUT);
        cbo.setForeground(TEXT_P);
        cbo.setBackground(Color.WHITE);
        cbo.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 6));
        cbo.setOpaque(false);
        cbo.setFocusable(false);

        cbo.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean sel, boolean focus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                lbl.setFont(VMSStyle.FONT_INPUT);
                lbl.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
                if (sel) {
                    lbl.setBackground(VMSStyle.RED_LIGHT);
                    lbl.setForeground(RED_DK);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(TEXT_P);
                }
                String display = value != null ? value.toString().replace('_', ' ') : "";
                lbl.setText(display);
                return lbl;
            }
        });

        wrapper.add(cbo, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel getPanel() {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_SUBTLE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, VMSStyle.INPUT_ROUND, VMSStyle.INPUT_ROUND));
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(0, 42));
        return wrapper;
    }

    // ── Close button (vectoriel) ──────────────────────────────────────────
    private JButton buildCloseBtn() {
        return new JButton() {
            boolean hov = false;
            {
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
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
    }

    // =====================================================================
    //  VALIDATION & REGISTRATION
    // =====================================================================
    private void actionRegister() {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String pwd      = new String(txtPassword.getPassword());
        String conf     = new String(txtConfirm.getPassword());
        String role     = (String) cboRole.getSelectedItem();

        if (username.isEmpty() || email.isEmpty() || pwd.isEmpty() || conf.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères.");
            return;
        }
        if (!EMAIL_RE.matcher(email).matches()) {
            showError("Veuillez entrer une adresse e-mail valide.");
            return;
        }
        if (pwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères.");
            return;
        }
        if (!pwd.equals(conf)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        btnRegister.setEnabled(false);
        btnRegister.setText("Inscription en cours...");
        lblError.setText(" ");

        new SwingWorker<Boolean, Void>() {
            private String errorMsg;
            @Override protected Boolean doInBackground() {
                try {
                    return UserDAO.registerUser(username, email, pwd, role);
                } catch (SQLException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("duplicate key")) {
                        errorMsg = "Ce nom d'utilisateur ou cet e-mail existe déjà.";
                    } else {
                        errorMsg = "Erreur de connexion à la base de données.";
                    }
                    ex.printStackTrace();
                    return false;
                }
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        showSuccess();
                        Timer t = new Timer(1500, ev -> ouvrirLogin());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        showError(errorMsg != null ? errorMsg : "L'inscription a échoué. Veuillez réessayer.");
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Créer mon compte");
                    }
                } catch (Exception ex) {
                    showError("Erreur inattendue.");
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Créer mon compte");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setForeground(RED);
    }

    private void showSuccess() {
        lblError.setText("Compte créé avec succès ! Redirection...");
        lblError.setForeground(SUCCESS);
    }

    private void ouvrirLogin() {
        dispose();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
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
}
