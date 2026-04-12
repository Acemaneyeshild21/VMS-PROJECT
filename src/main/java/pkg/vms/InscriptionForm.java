package pkg.vms;

import pkg.vms.DAO.UserDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.SQLException;
import java.util.regex.Pattern;

public class InscriptionForm extends JFrame {

    private static final Color BG         = VMSStyle.BG_ROOT;
    private static final Color RED        = VMSStyle.RED_PRIMARY;
    private static final Color RED_DK     = VMSStyle.RED_DARK;
    private static final Color TEXT_P     = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S     = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M     = VMSStyle.TEXT_MUTED;
    private static final Color BORDER     = VMSStyle.BORDER_LIGHT;
    private static final Color SUCCESS    = VMSStyle.SUCCESS;

    private static final Pattern EMAIL_RE = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static final String[] ROLES = {
            "Collaborateur", "Comptable", "Approbateur",
            "Manager", "Superviseur_Magasin", "Administrateur"
    };

    private JTextField       txtUsername;
    private JTextField       txtEmail;
    private JPasswordField   txtPassword;
    private JPasswordField   txtConfirm;
    private JComboBox<String> cboRole;
    private JLabel           lblError;
    private JButton          btnRegister;
    private boolean          showPwd  = false;
    private boolean          showConf = false;

    private int xOff, yOff;

    public InscriptionForm() {
        setTitle("Inscription \u2014 Intermart VMS");
        setSize(960, 680);
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
    //  LEFT BRANDING PANEL  (identical to LoginForm for visual coherence)
    // =====================================================================
    private JPanel buildLeftBrand() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, RED, 0, getHeight(), RED_DK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(255, 255, 255, 6));
                for (int i = 0; i < 40; i++) {
                    g2.fillRect(0, i * 28, getWidth(), 1);
                }

                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillOval(-60, getHeight() - 200, 260, 260);
                g2.fillOval(getWidth() - 100, -80, 200, 200);
                g2.dispose();
            }
        };
        p.setPreferredSize(new Dimension(380, 0));
        p.setLayout(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(60, 44, 48, 44));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

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

        String[] features = {
                "Cr\u00e9ez votre compte en quelques secondes",
                "Acc\u00e8s s\u00e9curis\u00e9 selon votre r\u00f4le",
                "Gestion compl\u00e8te des bons cadeau",
                "Suivi des demandes en temps r\u00e9el",
                "Multi-magasins & multi-r\u00f4les"
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
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.WHITE);

        // Scrollable form so it works on smaller screens
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Close button
        gbc.gridy = 0;
        gbc.insets = new Insets(16, 0, 0, 16);
        gbc.anchor = GridBagConstraints.NORTHEAST;
        gbc.fill = GridBagConstraints.NONE;
        p.add(buildCloseBtn(), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        // Title
        gbc.gridy = 1;
        gbc.insets = new Insets(6, 50, 2, 50);
        JLabel lblTitle = new JLabel("Cr\u00e9er un compte");
        lblTitle.setFont(new Font("Georgia", Font.BOLD, 26));
        lblTitle.setForeground(TEXT_P);
        p.add(lblTitle, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 50, 20, 50);
        JLabel lblSub = new JLabel("Rejoignez la plateforme Intermart VMS");
        lblSub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        lblSub.setForeground(TEXT_M);
        p.add(lblSub, gbc);

        // ── Username ──
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 50, 3, 50);
        p.add(buildLabel("Nom d'utilisateur"), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 50, 12, 50);
        txtUsername = new JTextField();
        styleField(txtUsername, "Entrez votre identifiant");
        p.add(txtUsername, gbc);

        // ── Email ──
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 50, 3, 50);
        p.add(buildLabel("Adresse e-mail"), gbc);

        gbc.gridy = 6;
        gbc.insets = new Insets(0, 50, 12, 50);
        txtEmail = new JTextField();
        styleField(txtEmail, "exemple@intermart.mu");
        p.add(txtEmail, gbc);

        // ── Password ──
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 50, 3, 50);
        p.add(buildLabel("Mot de passe"), gbc);

        gbc.gridy = 8;
        gbc.insets = new Insets(0, 50, 12, 50);
        txtPassword = new JPasswordField();
        JPanel pwdRow = buildPasswordRow(txtPassword, true);
        p.add(pwdRow, gbc);

        // ── Confirm password ──
        gbc.gridy = 9;
        gbc.insets = new Insets(0, 50, 3, 50);
        p.add(buildLabel("Confirmer le mot de passe"), gbc);

        gbc.gridy = 10;
        gbc.insets = new Insets(0, 50, 12, 50);
        txtConfirm = new JPasswordField();
        JPanel confRow = buildPasswordRow(txtConfirm, false);
        p.add(confRow, gbc);

        // ── Role ──
        gbc.gridy = 11;
        gbc.insets = new Insets(0, 50, 3, 50);
        p.add(buildLabel("R\u00f4le"), gbc);

        gbc.gridy = 12;
        gbc.insets = new Insets(0, 50, 12, 50);
        cboRole = new JComboBox<>(ROLES);
        styleComboBox(cboRole);
        p.add(cboRole, gbc);

        // ── Error ──
        gbc.gridy = 13;
        gbc.insets = new Insets(0, 50, 8, 50);
        lblError = new JLabel(" ");
        lblError.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblError.setForeground(RED);
        p.add(lblError, gbc);

        // ── Register button ──
        gbc.gridy = 14;
        gbc.insets = new Insets(0, 50, 12, 50);
        btnRegister = buildGradientButton("Cr\u00e9er mon compte");
        p.add(btnRegister, gbc);

        // ── Link to login ──
        gbc.gridy = 15;
        gbc.insets = new Insets(0, 50, 20, 50);
        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        linkRow.setOpaque(false);
        JLabel lblHas = new JLabel("D\u00e9j\u00e0 un compte ?");
        lblHas.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblHas.setForeground(TEXT_M);
        JLabel lblLogin = new JLabel("Se connecter");
        lblLogin.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        lblLogin.setForeground(RED);
        lblLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLogin.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) { ouvrirLogin(); }
            public void mouseEntered(MouseEvent e) { lblLogin.setForeground(RED_DK); }
            public void mouseExited(MouseEvent e)  { lblLogin.setForeground(RED); }
        });
        linkRow.add(lblHas);
        linkRow.add(lblLogin);
        p.add(linkRow, gbc);

        // Enter key on last field triggers registration
        txtConfirm.addActionListener(e -> actionRegister());

        JScrollPane scroll = new JScrollPane(p);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        outer.add(scroll, BorderLayout.CENTER);
        return outer;
    }

    // ── Label helper ──────────────────────────────────────────────────────
    private JLabel buildLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        lbl.setForeground(TEXT_S);
        return lbl;
    }

    // ── Styled text field (same pattern as LoginForm) ─────────────────────
    private void styleField(JTextField field, String placeholder) {
        field.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        field.setForeground(TEXT_P);
        field.setCaretColor(RED);
        field.setPreferredSize(new Dimension(0, 42));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

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

    // ── Password row with toggle ──────────────────────────────────────────
    private JPanel buildPasswordRow(JPasswordField field, boolean isPrimary) {
        JPanel row = new JPanel(new BorderLayout(0, 0));
        row.setBackground(Color.WHITE);
        row.setPreferredSize(new Dimension(0, 42));
        row.setBorder(BorderFactory.createLineBorder(BORDER, 1));

        field.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        field.setForeground(TEXT_P);
        field.setCaretColor(RED);
        field.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 8));
        field.setOpaque(false);

        JButton btnToggle = new JButton("\u25CF") {
            {
                setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
                setForeground(TEXT_M);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(42, 42));
                setToolTipText("Afficher / masquer");
                addActionListener(e -> {
                    if (isPrimary) {
                        showPwd = !showPwd;
                        field.setEchoChar(showPwd ? (char) 0 : '\u2022');
                        setText(showPwd ? "\u25CB" : "\u25CF");
                    } else {
                        showConf = !showConf;
                        field.setEchoChar(showConf ? (char) 0 : '\u2022');
                        setText(showConf ? "\u25CB" : "\u25CF");
                    }
                });
            }
        };

        row.add(field, BorderLayout.CENTER);
        row.add(btnToggle, BorderLayout.EAST);
        return row;
    }

    // ── Styled combo box ──────────────────────────────────────────────────
    private void styleComboBox(JComboBox<String> cbo) {
        cbo.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        cbo.setForeground(TEXT_P);
        cbo.setBackground(Color.WHITE);
        cbo.setPreferredSize(new Dimension(0, 42));
        cbo.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        cbo.setFocusable(false);

        cbo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean sel, boolean focus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, sel, focus);
                lbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
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
    }

    // ── Gradient button (matches LoginForm style) ─────────────────────────
    private JButton buildGradientButton(String text) {
        JButton btn = new JButton(text) {
            boolean hov = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 15));
                setForeground(Color.WHITE);
                setOpaque(false);
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(0, 46));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
                });
                addActionListener(e -> actionRegister());
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

    // ── Close button ──────────────────────────────────────────────────────
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

    // =====================================================================
    //  VALIDATION & REGISTRATION
    // =====================================================================
    private void actionRegister() {
        String username = txtUsername.getText().trim();
        String email    = txtEmail.getText().trim();
        String pwd      = new String(txtPassword.getPassword());
        String conf     = new String(txtConfirm.getPassword());
        String role     = (String) cboRole.getSelectedItem();

        // Validations
        if (username.isEmpty() || email.isEmpty() || pwd.isEmpty() || conf.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caract\u00e8res.");
            return;
        }
        if (!EMAIL_RE.matcher(email).matches()) {
            showError("Veuillez entrer une adresse e-mail valide.");
            return;
        }
        if (pwd.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caract\u00e8res.");
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
                        errorMsg = "Ce nom d'utilisateur ou cet e-mail existe d\u00e9j\u00e0.";
                    } else {
                        errorMsg = "Erreur de connexion \u00e0 la base de donn\u00e9es.";
                    }
                    ex.printStackTrace();
                    return false;
                }
            }

            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        showSuccess("Compte cr\u00e9\u00e9 avec succ\u00e8s ! Redirection...");
                        Timer t = new Timer(1500, ev -> ouvrirLogin());
                        t.setRepeats(false);
                        t.start();
                    } else {
                        showError(errorMsg != null ? errorMsg : "L'inscription a \u00e9chou\u00e9. Veuillez r\u00e9essayer.");
                        btnRegister.setEnabled(true);
                        btnRegister.setText("Cr\u00e9er mon compte");
                    }
                } catch (Exception ex) {
                    showError("Erreur inattendue.");
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Cr\u00e9er mon compte");
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setForeground(RED);
    }

    private void showSuccess(String msg) {
        lblError.setText(msg);
        lblError.setForeground(SUCCESS);
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────
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
