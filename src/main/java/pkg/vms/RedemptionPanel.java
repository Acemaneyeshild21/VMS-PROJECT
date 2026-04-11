package pkg.vms;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Panneau de rédemption en magasin.
 * Le superviseur saisit ou scanne le code du bon, le système valide en ligne
 * (anti-fraude, anti double-utilisation, vérification expiration).
 */
public class RedemptionPanel extends JPanel {

    private static final Color BG_ROOT     = VMSStyle.BG_ROOT;
    private static final Color BG_CARD     = VMSStyle.BG_CARD;
    private static final Color RED_PRIMARY = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK    = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT   = VMSStyle.RED_LIGHT;
    private static final Color BORDER      = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_P      = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S      = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M      = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS     = VMSStyle.SUCCESS;
    private static final Color WARNING     = VMSStyle.WARNING;

    private final int userId;
    private final String username;
    private final String role;
    private int magasinId = -1;

    private JTextField txtCode;
    private JPanel resultPanel;
    private JComboBox<ClientDAO.MagasinInfo> cbMagasin;

    public RedemptionPanel(int userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;

        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        initComponents();
    }

    private void initComponents() {
        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleRow.setOpaque(false);
        JLabel icon = new JLabel("\uD83D\uDCF1");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
        JLabel title = new JLabel("  Rédemption de Bon Cadeau");
        title.setFont(VMSStyle.FONT_BRAND.deriveFont(24f));
        title.setForeground(TEXT_P);
        titleRow.add(icon);
        titleRow.add(title);

        JLabel sub = new JLabel("Scannez ou saisissez le code du bon pour le valider");
        sub.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        sub.setForeground(TEXT_S);

        titleBlock.add(titleRow);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(sub);
        header.add(titleBlock, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ── Centre ──
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // Sélection du magasin
        JPanel magasinCard = buildCard();
        magasinCard.setLayout(new BorderLayout(0, 8));
        magasinCard.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        magasinCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel lblMagasin = new JLabel("POINT DE VENTE");
        lblMagasin.setFont(new Font("Trebuchet MS", Font.BOLD, 9));
        lblMagasin.setForeground(TEXT_M);
        cbMagasin = new JComboBox<>();
        cbMagasin.setFont(VMSStyle.FONT_CARD_DSC);
        cbMagasin.setPreferredSize(new Dimension(0, 35));
        chargerMagasins();

        magasinCard.add(lblMagasin, BorderLayout.NORTH);
        magasinCard.add(cbMagasin, BorderLayout.CENTER);
        center.add(magasinCard);
        center.add(Box.createVerticalStrut(16));

        // Zone de scan
        JPanel scanCard = buildCard();
        scanCard.setLayout(new BorderLayout(0, 12));
        scanCard.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        scanCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel lblScan = new JLabel("CODE DU BON");
        lblScan.setFont(new Font("Trebuchet MS", Font.BOLD, 9));
        lblScan.setForeground(TEXT_M);

        JPanel inputRow = new JPanel(new BorderLayout(10, 0));
        inputRow.setOpaque(false);

        txtCode = new JTextField();
        txtCode.setFont(new Font("Courier New", Font.BOLD, 18));
        txtCode.setForeground(TEXT_P);
        txtCode.setCaretColor(RED_PRIMARY);
        txtCode.setPreferredSize(new Dimension(0, 48));
        txtCode.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 2),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));
        txtCode.addActionListener(e -> validerBon()); // Entrée = valider

        JButton btnValider = new JButton("Valider") {
            boolean h = false;
            {
                setFont(VMSStyle.FONT_BTN_MAIN);
                setForeground(Color.WHITE);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(130, 48));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { h = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? RED_DARK : RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnValider.addActionListener(e -> validerBon());

        inputRow.add(txtCode, BorderLayout.CENTER);
        inputRow.add(btnValider, BorderLayout.EAST);

        JLabel hint = new JLabel("\uD83D\uDD0D Saisissez le code ou scannez le QR code avec un lecteur USB");
        hint.setFont(new Font("Trebuchet MS", Font.ITALIC, 11));
        hint.setForeground(TEXT_M);

        scanCard.add(lblScan, BorderLayout.NORTH);
        scanCard.add(inputRow, BorderLayout.CENTER);
        scanCard.add(hint, BorderLayout.SOUTH);
        center.add(scanCard);
        center.add(Box.createVerticalStrut(16));

        // Zone de résultat
        resultPanel = buildCard();
        resultPanel.setLayout(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        resultPanel.setPreferredSize(new Dimension(0, 200));

        JLabel placeholder = new JLabel("Le résultat de la validation apparaîtra ici");
        placeholder.setFont(new Font("Trebuchet MS", Font.ITALIC, 13));
        placeholder.setForeground(TEXT_M);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        resultPanel.add(placeholder, BorderLayout.CENTER);

        center.add(resultPanel);
        add(center, BorderLayout.CENTER);

        // Focus automatique sur le champ code
        SwingUtilities.invokeLater(() -> txtCode.requestFocusInWindow());
    }

    private void chargerMagasins() {
        try {
            java.util.List<ClientDAO.MagasinInfo> magasins = ClientDAO.getAllMagasins();
            for (ClientDAO.MagasinInfo m : magasins) cbMagasin.addItem(m);
        } catch (Exception e) {
            System.err.println("Erreur chargement magasins: " + e.getMessage());
        }
    }

    private void validerBon() {
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            afficherResultat(false, "Veuillez saisir ou scanner un code de bon.", null);
            return;
        }

        ClientDAO.MagasinInfo magasin = (ClientDAO.MagasinInfo) cbMagasin.getSelectedItem();
        if (magasin == null) {
            afficherResultat(false, "Veuillez sélectionner votre magasin.", null);
            return;
        }

        // Validation en arrière-plan
        SwingWorker<BonDAO.RedemptionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected BonDAO.RedemptionResult doInBackground() throws Exception {
                return BonDAO.redimerBon(code, magasin.id, userId);
            }

            @Override
            protected void done() {
                try {
                    BonDAO.RedemptionResult result = get();
                    afficherResultat(result.succes, result.message,
                            result.succes ? String.format("Rs %,.2f", result.valeur) : null);
                    if (result.succes) {
                        txtCode.setText("");
                    }
                } catch (Exception ex) {
                    afficherResultat(false, "Erreur : " + ex.getMessage(), null);
                }
                txtCode.requestFocusInWindow();
            }
        };
        worker.execute();
    }

    private void afficherResultat(boolean succes, String message, String valeur) {
        resultPanel.removeAll();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));

        Color bgColor = succes ? new Color(220, 252, 231) : new Color(254, 226, 226);
        Color fgColor = succes ? SUCCESS : RED_PRIMARY;
        String iconStr = succes ? "\u2705" : "\u274C";

        // Icône grande
        JLabel iconLbl = new JLabel(iconStr);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Statut
        JLabel statusLbl = new JLabel(succes ? "BON VALIDÉ" : "REJETÉ");
        statusLbl.setFont(new Font("Georgia", Font.BOLD, 22));
        statusLbl.setForeground(fgColor);
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Message
        JLabel msgLbl = new JLabel(message);
        msgLbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        msgLbl.setForeground(TEXT_P);
        msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        resultPanel.add(Box.createVerticalGlue());
        resultPanel.add(iconLbl);
        resultPanel.add(Box.createVerticalStrut(8));
        resultPanel.add(statusLbl);
        resultPanel.add(Box.createVerticalStrut(6));
        resultPanel.add(msgLbl);

        if (valeur != null) {
            JLabel valLbl = new JLabel("Valeur : " + valeur);
            valLbl.setFont(new Font("Georgia", Font.BOLD, 18));
            valLbl.setForeground(fgColor);
            valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            resultPanel.add(Box.createVerticalStrut(6));
            resultPanel.add(valLbl);
        }

        // Horodatage
        JLabel timeLbl = new JLabel(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date()));
        timeLbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        timeLbl.setForeground(TEXT_M);
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        resultPanel.add(Box.createVerticalStrut(8));
        resultPanel.add(timeLbl);

        resultPanel.add(Box.createVerticalGlue());
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel buildCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }
        };
    }
}
