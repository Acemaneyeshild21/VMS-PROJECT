package pkg.vms;

import pkg.vms.DAO.BonDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * V\u00e9rification de bon par code unique (simule un scan QR).
 * Affiche un verdict clair : VALIDE, UTILIS\u00c9, EXPIR\u00c9, ANNUL\u00c9 ou INTROUVABLE.
 * Utilisable par le personnel magasin pour contr\u00f4ler rapidement un bon.
 */
public class VerificationBonDialog extends JDialog {

    private static final NumberFormat NF_MUR = NumberFormat.getNumberInstance(new Locale("fr", "FR"));

    private final JTextField tfCode;
    private final JPanel     resultPanel;

    public VerificationBonDialog(Window owner) {
        super(owner, "V\u00e9rification d'un bon", ModalityType.APPLICATION_MODAL);
        setSize(640, 540);
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
        JLabel title = new JLabel("\uD83D\uDD0D  V\u00e9rifier un bon");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Saisissez ou scannez le code unique imprim\u00e9 sur le bon");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        tb.add(title);
        tb.add(Box.createVerticalStrut(3));
        tb.add(sub);
        header.add(tb, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Saisie
        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        tfCode = new JTextField();
        tfCode.setFont(new Font("Consolas", Font.BOLD, 15));
        tfCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(10, 12, 10, 12)));
        tfCode.addActionListener(e -> verifier());
        JButton btnVerify = UIUtils.buildPrimaryButton("V\u00e9rifier", 130, 42);
        btnVerify.addActionListener(e -> verifier());
        inputRow.add(tfCode, BorderLayout.CENTER);
        inputRow.add(btnVerify, BorderLayout.EAST);
        body.add(inputRow, BorderLayout.NORTH);

        // Zone r\u00e9sultat
        resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        resultPanel.add(placeholder(), BorderLayout.CENTER);
        body.add(resultPanel, BorderLayout.CENTER);

        root.add(body, BorderLayout.CENTER);
        setContentPane(root);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        SwingUtilities.invokeLater(tfCode::requestFocusInWindow);
    }

    private JPanel placeholder() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JLabel l = new JLabel("\u2022 Collez le code pour obtenir le verdict \u2022");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(VMSStyle.TEXT_MUTED);
        p.add(l);
        return p;
    }

    private void verifier() {
        String code = tfCode.getText().trim();
        if (code.isEmpty()) {
            showVerdict(Verdict.NOT_FOUND, null, "Veuillez saisir un code");
            return;
        }
        showVerdict(Verdict.LOADING, null, "Recherche en cours\u2026");

        new SwingWorker<BonDAO.VerifInfo, Void>() {
            String err;
            @Override protected BonDAO.VerifInfo doInBackground() {
                try { return BonDAO.verifierBon(code); }
                catch (SQLException ex) { err = ex.getMessage(); return null; }
            }
            @Override protected void done() {
                try {
                    BonDAO.VerifInfo v = get();
                    if (err != null) {
                        showVerdict(Verdict.NOT_FOUND, null, "Erreur : " + err);
                        return;
                    }
                    if (v == null) {
                        showVerdict(Verdict.NOT_FOUND, null, "Aucun bon trouv\u00e9 pour le code : " + code);
                        return;
                    }
                    Verdict verd;
                    if ("REDIME".equals(v.bon.statut)) verd = Verdict.USED;
                    else if ("ANNULE".equals(v.bon.statut)) verd = Verdict.CANCELLED;
                    else if ("EXPIRE".equals(v.bon.statut) || v.expired) verd = Verdict.EXPIRED;
                    else verd = Verdict.VALID;
                    showVerdict(verd, v, null);
                } catch (Exception ex) {
                    showVerdict(Verdict.NOT_FOUND, null, "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private enum Verdict { VALID, USED, EXPIRED, CANCELLED, NOT_FOUND, LOADING }

    private void showVerdict(Verdict v, BonDAO.VerifInfo info, String errorMsg) {
        resultPanel.removeAll();
        resultPanel.add(buildVerdictCard(v, info, errorMsg), BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel buildVerdictCard(Verdict v, BonDAO.VerifInfo info, String errorMsg) {
        final Color accent;
        final String icon;
        final String label;
        switch (v) {
            case VALID     -> { accent = VMSStyle.SUCCESS;     icon = "\u2705"; label = "BON VALIDE"; }
            case USED      -> { accent = VMSStyle.ACCENT_BLUE; icon = "\uD83D\uDCB3"; label = "D\u00c9J\u00c0 UTILIS\u00c9"; }
            case EXPIRED   -> { accent = new Color(120,120,120);icon="\u23F0"; label = "EXPIR\u00c9"; }
            case CANCELLED -> { accent = VMSStyle.RED_PRIMARY; icon = "\u26D4"; label = "ANNUL\u00c9"; }
            case LOADING   -> { accent = VMSStyle.TEXT_MUTED;  icon = "\u23F3"; label = "Recherche\u2026"; }
            default        -> { accent = VMSStyle.RED_PRIMARY; icon = "\u274C"; label = "INTROUVABLE"; }
        }

        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(accent);
                g2.fillRect(0, 0, getWidth(), 6);
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        // Big verdict
        JPanel big = new JPanel();
        big.setOpaque(false);
        big.setLayout(new BoxLayout(big, BoxLayout.Y_AXIS));
        JLabel icL = new JLabel(icon, SwingConstants.CENTER);
        icL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        icL.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel lbL = new JLabel(label, SwingConstants.CENTER);
        lbL.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lbL.setForeground(accent);
        lbL.setAlignmentX(Component.CENTER_ALIGNMENT);
        big.add(icL);
        big.add(Box.createVerticalStrut(4));
        big.add(lbL);
        card.add(big, BorderLayout.NORTH);

        // Detail
        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(4, 10, 4, 10);
        gc.gridy = 0;
        gc.fill = GridBagConstraints.HORIZONTAL;

        if (errorMsg != null && info == null) {
            JLabel l = new JLabel(errorMsg, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            l.setForeground(VMSStyle.TEXT_MUTED);
            details.add(l, gc);
        } else if (info != null) {
            addDetail(details, gc, "Code", info.bon.codeUnique);
            addDetail(details, gc, "Valeur", NF_MUR.format(Math.round(info.bon.valeur)) + " Rs");
            addDetail(details, gc, "Client", info.bon.clientNom != null ? info.bon.clientNom : "\u2013");
            addDetail(details, gc, "Facture", info.bon.reference != null ? info.bon.reference : "\u2013");
            addDetail(details, gc, "\u00c9mis le", info.bon.dateEmission != null ? info.bon.dateEmission.substring(0, Math.min(16, info.bon.dateEmission.length())) : "\u2013");
            addDetail(details, gc, "Expire le", info.bon.dateExpiration != null ? info.bon.dateExpiration.substring(0, Math.min(16, info.bon.dateExpiration.length())) : "\u2013");
            if (info.redemptionDate != null) {
                addDetail(details, gc, "Rachet\u00e9 le", info.redemptionDate.substring(0, Math.min(16, info.redemptionDate.length())));
                if (info.redemptionMagasin != null) addDetail(details, gc, "Magasin", info.redemptionMagasin);
                if (info.redemptionUtilisateur != null) addDetail(details, gc, "Agent", info.redemptionUtilisateur);
            }
        }
        card.add(details, BorderLayout.CENTER);

        return card;
    }

    private void addDetail(JPanel p, GridBagConstraints gc, String label, String value) {
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(VMSStyle.TEXT_MUTED);
        gc.gridx = 0; gc.weightx = 0;
        p.add(l, gc);
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        v.setForeground(VMSStyle.TEXT_PRIMARY);
        gc.gridx = 1; gc.weightx = 1;
        p.add(v, gc);
        gc.gridy++;
    }

    public static void show(Component owner) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new VerificationBonDialog(w).setVisible(true);
    }
}
