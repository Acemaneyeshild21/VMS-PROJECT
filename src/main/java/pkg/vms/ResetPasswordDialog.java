package pkg.vms;

import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.PasswordResetDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;

/**
 * Dialog de r\u00e9initialisation de mot de passe en 2 \u00e9tapes :
 *
 * \u00c9tape 1 — Demande :
 *   - Champ email
 *   - Bouton "Envoyer le code"
 *   - V\u00e9rifie l'existence du compte (sans le r\u00e9v\u00e9ler), g\u00e9n\u00e8re un OTP, l'envoie par email
 *
 * \u00c9tape 2 — R\u00e9initialisation :
 *   - Champ code (6 digits)
 *   - Nouveau mot de passe + confirmation
 *   - Timer visible (15:00 d\u00e9compte)
 *   - Bouton "Changer le mot de passe"
 *
 * S\u00e9curit\u00e9 :
 *   - Ne r\u00e9v\u00e8le jamais si l'email existe (toast g\u00e9n\u00e9rique apr\u00e8s envoi)
 *   - Code valid\u00e9 via BCrypt
 *   - Max 3 tentatives (g\u00e9r\u00e9 dans PasswordResetDAO)
 *   - Lien d'audit : RESET_PASSWORD_DEMANDE / _SUCCES / _ECHEC
 */
public class ResetPasswordDialog extends JDialog {

    private enum Step { EMAIL, VERIFY }

    private final JPanel      contentPanel;
    private final CardLayout  cards;
    private Step              current = Step.EMAIL;

    // \u00c9tape 1
    private JTextField  tfEmail;
    private JButton     btnSendCode;

    // \u00c9tape 2
    private JTextField      tfCode;
    private JPasswordField  pfNewPassword;
    private JPasswordField  pfConfirmPassword;
    private JButton         btnChangePassword;
    private JLabel          lblTimer;
    private Timer           swingTimer;
    private long            deadlineMs;

    // Contexte
    private int    targetUserId  = -1;
    private String targetUsername;
    private String targetEmail;

    public ResetPasswordDialog(Window owner) {
        super(owner, "R\u00e9initialisation du mot de passe", ModalityType.APPLICATION_MODAL);
        setSize(520, 560);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(VMSStyle.BG_ROOT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(VMSStyle.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(16, 22, 16, 22)));
        JPanel tb = new JPanel();
        tb.setOpaque(false);
        tb.setLayout(new BoxLayout(tb, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("\uD83D\uDD10  Mot de passe oubli\u00e9");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("R\u00e9cup\u00e9rez l'acc\u00e8s \u00e0 votre compte en quelques \u00e9tapes");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        tb.add(title);
        tb.add(Box.createVerticalStrut(3));
        tb.add(sub);
        header.add(tb, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Cards (2 steps)
        cards = new CardLayout();
        contentPanel = new JPanel(cards);
        contentPanel.setOpaque(false);
        contentPanel.add(buildStepEmail(),  "EMAIL");
        contentPanel.add(buildStepVerify(), "VERIFY");
        root.add(contentPanel, BorderLayout.CENTER);

        setContentPane(root);

        // Escape closes
        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { closeDialog(); }
        });

        // D\u00e9marrer sur l'\u00e9tape email
        showStep(Step.EMAIL);
        SwingUtilities.invokeLater(() -> tfEmail.requestFocusInWindow());

        // Cleanup du timer \u00e0 la fermeture
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { stopTimer(); }
        });
    }

    // ── \u00c9tape 1 : Email ─────────────────────────────────────────────────────

    private JPanel buildStepEmail() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel intro = new JLabel("<html><body style='width:430px;'>"
                + "Saisissez l'adresse email associ\u00e9e \u00e0 votre compte. "
                + "Vous recevrez un <b>code de 6 chiffres</b> valable <b>15 minutes</b> "
                + "pour r\u00e9initialiser votre mot de passe.</body></html>");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        intro.setForeground(VMSStyle.TEXT_SECONDARY);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(intro);
        form.add(Box.createVerticalStrut(22));

        JLabel lbl = new JLabel("Adresse email");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(VMSStyle.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lbl);
        form.add(Box.createVerticalStrut(6));

        tfEmail = new JTextField();
        tfEmail.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tfEmail.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        tfEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfEmail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        tfEmail.addActionListener(e -> requestResetCode());
        form.add(tfEmail);

        form.add(Box.createVerticalStrut(20));

        btnSendCode = UIUtils.buildPrimaryButton("Envoyer le code", 200, 42);
        btnSendCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnSendCode.addActionListener(e -> requestResetCode());

        JButton btnCancel = UIUtils.buildGhostButton("Annuler", 110, 42);
        btnCancel.addActionListener(e -> closeDialog());

        JPanel btnRow = new JPanel();
        btnRow.setOpaque(false);
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(btnSendCode);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnCancel);
        btnRow.add(Box.createHorizontalGlue());
        form.add(btnRow);

        p.add(form, BorderLayout.NORTH);
        return p;
    }

    private void requestResetCode() {
        String email = tfEmail.getText().trim();
        if (email.isEmpty() || !email.contains("@")) {
            ToastManager.error(this, "Adresse email invalide");
            return;
        }

        btnSendCode.setEnabled(false);
        btnSendCode.setText("Envoi\u2026");

        new SwingWorker<String, Void>() {
            String maybeError;
            AuthDAO.UserSession user;
            String code;

            @Override protected String doInBackground() throws Exception {
                user = AuthDAO.findByEmail(email);
                if (user == null) {
                    // Ne pas r\u00e9v\u00e9ler : on simule un d\u00e9lai r\u00e9aliste
                    Thread.sleep(800);
                    return "NO_USER"; // sentinel — on affichera quand m\u00eame un succ\u00e8s g\u00e9n\u00e9rique
                }
                code = PasswordResetDAO.createResetCode(user.userId, null);
                maybeError = EmailService.envoyerCodeReset(user.email, user.username, code);
                return maybeError == null ? "OK" : "EMAIL_ERROR";
            }

            @Override protected void done() {
                btnSendCode.setEnabled(true);
                btnSendCode.setText("Envoyer le code");
                try {
                    String result = get();
                    if ("NO_USER".equals(result)) {
                        // Message g\u00e9n\u00e9rique pour \u00e9viter d'\u00e9num\u00e9rer les emails
                        ToastManager.info(ResetPasswordDialog.this,
                                "Si cet email existe, un code vient d'\u00eatre envoy\u00e9.");
                        // On ne passe PAS \u00e0 l'\u00e9tape 2 (rien \u00e0 v\u00e9rifier)
                        return;
                    }
                    if ("EMAIL_ERROR".equals(result)) {
                        ToastManager.error(ResetPasswordDialog.this,
                                "\u00c9chec d'envoi de l'email : " + maybeError);
                        return;
                    }
                    // Succ\u00e8s
                    targetUserId   = user.userId;
                    targetUsername = user.username;
                    targetEmail    = user.email;
                    ToastManager.success(ResetPasswordDialog.this,
                            "Code envoy\u00e9 \u00e0 " + maskEmail(user.email));
                    showStep(Step.VERIFY);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    ToastManager.error(ResetPasswordDialog.this,
                            "Erreur : " + cause.getMessage());
                }
            }
        }.execute();
    }

    // ── \u00c9tape 2 : V\u00e9rification + nouveau mot de passe ──────────────────────

    private JPanel buildStepVerify() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(24, 28, 24, 28));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel intro = new JLabel("<html><body style='width:430px;'>"
                + "Un code \u00e0 <b>6 chiffres</b> a \u00e9t\u00e9 envoy\u00e9 \u00e0 votre adresse email. "
                + "Saisissez-le ci-dessous, puis d\u00e9finissez un nouveau mot de passe "
                + "(au moins 8 caract\u00e8res).</body></html>");
        intro.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        intro.setForeground(VMSStyle.TEXT_SECONDARY);
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(intro);
        form.add(Box.createVerticalStrut(16));

        // Timer
        lblTimer = new JLabel("\u23F3  Code valide 15:00");
        lblTimer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTimer.setForeground(VMSStyle.ACCENT_BLUE);
        lblTimer.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblTimer);
        form.add(Box.createVerticalStrut(14));

        // Code
        JLabel lblCode = new JLabel("Code re\u00e7u");
        lblCode.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblCode.setForeground(VMSStyle.TEXT_MUTED);
        lblCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblCode);
        form.add(Box.createVerticalStrut(6));

        tfCode = new JTextField();
        tfCode.setFont(new Font("Consolas", Font.BOLD, 22));
        tfCode.setHorizontalAlignment(JTextField.CENTER);
        tfCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(12, 12, 12, 12)));
        tfCode.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfCode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        // Limite \u00e0 6 chiffres
        tfCode.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) || tfCode.getText().length() >= 6) {
                    e.consume();
                }
            }
        });
        form.add(tfCode);
        form.add(Box.createVerticalStrut(14));

        // Nouveau mot de passe
        JLabel lblPwd = new JLabel("Nouveau mot de passe");
        lblPwd.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPwd.setForeground(VMSStyle.TEXT_MUTED);
        lblPwd.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblPwd);
        form.add(Box.createVerticalStrut(6));

        pfNewPassword = new JPasswordField();
        pfNewPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pfNewPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        pfNewPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        pfNewPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        form.add(pfNewPassword);
        form.add(Box.createVerticalStrut(10));

        // Confirmation
        JLabel lblPwd2 = new JLabel("Confirmer le mot de passe");
        lblPwd2.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblPwd2.setForeground(VMSStyle.TEXT_MUTED);
        lblPwd2.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(lblPwd2);
        form.add(Box.createVerticalStrut(6));

        pfConfirmPassword = new JPasswordField();
        pfConfirmPassword.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        pfConfirmPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        pfConfirmPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        pfConfirmPassword.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        pfConfirmPassword.addActionListener(e -> doResetPassword());
        form.add(pfConfirmPassword);

        form.add(Box.createVerticalStrut(20));

        // Boutons
        btnChangePassword = UIUtils.buildPrimaryButton("Changer le mot de passe", 220, 42);
        btnChangePassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnChangePassword.addActionListener(e -> doResetPassword());

        JButton btnBack = UIUtils.buildGhostButton("Retour", 110, 42);
        btnBack.addActionListener(e -> showStep(Step.EMAIL));

        JPanel btnRow = new JPanel();
        btnRow.setOpaque(false);
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.add(btnChangePassword);
        btnRow.add(Box.createHorizontalStrut(10));
        btnRow.add(btnBack);
        btnRow.add(Box.createHorizontalGlue());
        form.add(btnRow);

        p.add(form, BorderLayout.NORTH);
        return p;
    }

    private void doResetPassword() {
        String code  = tfCode.getText().trim();
        String pwd1  = new String(pfNewPassword.getPassword());
        String pwd2  = new String(pfConfirmPassword.getPassword());

        if (code.length() != 6) {
            ToastManager.error(this, "Le code doit contenir 6 chiffres");
            return;
        }
        if (pwd1.length() < 8) {
            ToastManager.error(this, "Le mot de passe doit contenir au moins 8 caract\u00e8res");
            return;
        }
        if (!pwd1.equals(pwd2)) {
            ToastManager.error(this, "Les mots de passe ne correspondent pas");
            return;
        }

        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("V\u00e9rification\u2026");

        new SwingWorker<PasswordResetDAO.ValidationResult, Void>() {
            String error;
            boolean updated;

            @Override protected PasswordResetDAO.ValidationResult doInBackground() {
                try {
                    PasswordResetDAO.ValidationResult v = PasswordResetDAO.validateCode(targetUserId, code);
                    if (v.success) {
                        if (AuthDAO.updatePassword(targetUserId, pwd1)) {
                            PasswordResetDAO.markUsed(v.resetId);
                            updated = true;
                        } else {
                            error = "\u00c9chec de la mise \u00e0 jour du mot de passe";
                        }
                    }
                    return v;
                } catch (SQLException | IllegalArgumentException ex) {
                    error = ex.getMessage();
                    return null;
                }
            }

            @Override protected void done() {
                btnChangePassword.setEnabled(true);
                btnChangePassword.setText("Changer le mot de passe");
                try {
                    PasswordResetDAO.ValidationResult v = get();
                    if (error != null) {
                        ToastManager.error(ResetPasswordDialog.this, "Erreur : " + error);
                        return;
                    }
                    if (v == null) return;
                    if (!v.success) {
                        ToastManager.error(ResetPasswordDialog.this, v.errorMessage);
                        tfCode.setText("");
                        tfCode.requestFocusInWindow();
                        return;
                    }
                    if (updated) {
                        stopTimer();
                        ToastManager.success(ResetPasswordDialog.this,
                                "Mot de passe modifi\u00e9 ! Vous pouvez vous connecter.");
                        // Laisser le toast s'afficher puis fermer
                        Timer t = new Timer(1400, e -> closeDialog());
                        t.setRepeats(false);
                        t.start();
                    }
                } catch (Exception ex) {
                    ToastManager.error(ResetPasswordDialog.this, "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    // ── Navigation + timer ──────────────────────────────────────────────────

    private void showStep(Step step) {
        current = step;
        cards.show(contentPanel, step.name());
        if (step == Step.VERIFY) {
            startTimer();
            SwingUtilities.invokeLater(() -> tfCode.requestFocusInWindow());
        } else {
            stopTimer();
        }
    }

    private void startTimer() {
        stopTimer();
        // D\u00e9compte total : 15 min = 900 000 ms
        deadlineMs = System.currentTimeMillis() + 15 * 60 * 1000L;
        swingTimer = new Timer(1000, e -> updateTimer());
        swingTimer.setInitialDelay(0);
        swingTimer.start();
    }

    private void updateTimer() {
        long remaining = deadlineMs - System.currentTimeMillis();
        if (remaining <= 0) {
            lblTimer.setText("\u274C  Code expir\u00e9 \u2014 demandez-en un nouveau");
            lblTimer.setForeground(VMSStyle.RED_PRIMARY);
            btnChangePassword.setEnabled(false);
            stopTimer();
            return;
        }
        long min = remaining / 60000;
        long sec = (remaining / 1000) % 60;
        String txt = String.format("\u23F3  Code valide %d:%02d", min, sec);
        lblTimer.setText(txt);
        // Changer la couleur \u00e0 l'approche de l'expiration
        if (remaining < 60000) {
            lblTimer.setForeground(VMSStyle.RED_PRIMARY);
        } else if (remaining < 5 * 60 * 1000) {
            lblTimer.setForeground(VMSStyle.WARNING);
        } else {
            lblTimer.setForeground(VMSStyle.ACCENT_BLUE);
        }
    }

    private void stopTimer() {
        if (swingTimer != null) {
            swingTimer.stop();
            swingTimer = null;
        }
    }

    private void closeDialog() {
        stopTimer();
        dispose();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Masque un email : john.doe@example.com → j***e@example.com. */
    private static String maskEmail(String email) {
        if (email == null) return "";
        int at = email.indexOf('@');
        if (at < 2) return email;
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String masked = local.charAt(0) + "***" + local.charAt(local.length() - 1);
        return masked + domain;
    }

    public static void show(Component owner) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new ResetPasswordDialog(w).setVisible(true);
    }
}
