package pkg.vms;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.controller.RedemptionController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class RedemptionPanel extends JPanel {

    private static final Color BG_CARD     = VMSStyle.BG_CARD;
    private static final Color RED_PRIMARY = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK    = VMSStyle.RED_DARK;
    private static final Color BORDER      = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_P      = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_S      = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_M      = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS     = VMSStyle.SUCCESS;

    private final int userId;
    private final String username;
    private final String role;
    private final RedemptionController controller = new RedemptionController();

    private JTextField txtCode;
    private JPanel resultPanel;
    private JComboBox<ClientDAO.MagasinInfo> cbMagasin;

    public RedemptionPanel(int userId, String username, String role) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;

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
        JLabel icon = new JLabel("📱");
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
        txtCode.addActionListener(e -> validerBon());

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

        JLabel hint = new JLabel("🔍 Saisissez le code ou scannez le QR code avec un lecteur USB");
        hint.setFont(new Font("Trebuchet MS", Font.ITALIC, 11));
        hint.setForeground(TEXT_M);

        scanCard.add(lblScan,   BorderLayout.NORTH);
        scanCard.add(inputRow,  BorderLayout.CENTER);
        scanCard.add(hint,      BorderLayout.SOUTH);
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

        SwingUtilities.invokeLater(() -> txtCode.requestFocusInWindow());
    }

    private void chargerMagasins() {
        controller.chargerMagasins(
            magasins -> {
                for (ClientDAO.MagasinInfo m : magasins) cbMagasin.addItem(m);
            },
            err -> System.err.println("Erreur chargement magasins: " + err)
        );
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

        controller.redimerBon(code, magasin.id, userId,
            result -> {
                afficherResultatDB(result);
                if (result.succes) txtCode.setText("");
                txtCode.requestFocusInWindow();
            },
            err -> System.err.println("Erreur inattendue redemption: " + err)
        );
    }

    private void afficherResultat(boolean succes, String message, String valeur) {
        resultPanel.removeAll();
        resultPanel.setLayout(new BoxLayout(resultPanel, BoxLayout.Y_AXIS));

        Color fgColor = succes ? SUCCESS : RED_PRIMARY;

        JLabel iconLbl = new JLabel(succes ? "✅" : "❌");
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));
        iconLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel statusLbl = new JLabel(succes ? "BON VALIDÉ" : "REJETÉ");
        statusLbl.setFont(new Font("Georgia", Font.BOLD, 22));
        statusLbl.setForeground(fgColor);
        statusLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

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

    private void afficherResultatDB(BonDAO.RedemptionResult r) {
        BonDAO.RedemptionResult.ErrorType type =
                r.errorType != null ? r.errorType
                        : (r.succes ? BonDAO.RedemptionResult.ErrorType.SUCCESS
                                    : BonDAO.RedemptionResult.ErrorType.UNKNOWN);

        Color  bannerColor;
        String iconStr;
        String statusLabel;

        switch (type) {
            case SUCCESS -> {
                bannerColor = SUCCESS;
                iconStr     = "✅";
                statusLabel = "BON VALIDÉ";
            }
            case EXPIRED -> {
                bannerColor = new Color(217, 119, 6);
                iconStr     = "⏰";
                statusLabel = "BON EXPIRÉ";
            }
            case ALREADY_USED -> {
                bannerColor = new Color(153, 27, 27);
                iconStr     = "🚫";
                statusLabel = "BON DÉJÀ UTILISÉ";
            }
            case CONNECTION_ERROR -> {
                bannerColor = new Color(194, 65, 12);
                iconStr     = "📡";
                statusLabel = "CONNEXION INDISPONIBLE";
            }
            case LOCK_TIMEOUT -> {
                bannerColor = new Color(161, 98, 7);
                iconStr     = "⏱";
                statusLabel = "TRAITEMENT EN COURS";
            }
            case INVALID_CODE -> {
                bannerColor = RED_PRIMARY;
                iconStr     = "❌";
                statusLabel = "CODE INVALIDE";
            }
            case CANCELLED -> {
                bannerColor = new Color(75, 85, 99);
                iconStr     = "🚫";
                statusLabel = "BON ANNULÉ";
            }
            default -> {
                bannerColor = RED_PRIMARY;
                iconStr     = "❌";
                statusLabel = "REJETÉ";
            }
        }

        resultPanel.removeAll();
        resultPanel.setLayout(new BorderLayout(0, 0));

        JPanel banner = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        banner.setBackground(bannerColor);

        JLabel iconLbl = new JLabel(iconStr);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel statusLbl = new JLabel(statusLabel);
        statusLbl.setFont(new Font("Georgia", Font.BOLD, 20));
        statusLbl.setForeground(Color.WHITE);

        banner.add(iconLbl);
        banner.add(statusLbl);
        resultPanel.add(banner, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel msgLbl = new JLabel("<html><div style='text-align:center;'>"
                + (r.message != null ? r.message : "") + "</div></html>");
        msgLbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        msgLbl.setForeground(TEXT_P);
        msgLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(Box.createVerticalStrut(6));
        content.add(msgLbl);

        if (type == BonDAO.RedemptionResult.ErrorType.SUCCESS) {
            content.add(Box.createVerticalStrut(10));

            JLabel valLbl = new JLabel(String.format("Valeur : Rs %,.2f", r.valeur));
            valLbl.setFont(new Font("Georgia", Font.BOLD, 22));
            valLbl.setForeground(SUCCESS);
            valLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(valLbl);

            content.add(Box.createVerticalStrut(5));
            JLabel noteLbl = new JLabel("Bon à usage unique — non fractionnable");
            noteLbl.setFont(new Font("Trebuchet MS", Font.ITALIC, 11));
            noteLbl.setForeground(TEXT_M);
            noteLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            content.add(noteLbl);
        }

        content.add(Box.createVerticalStrut(8));
        JLabel timeLbl = new JLabel(new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date()));
        timeLbl.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        timeLbl.setForeground(TEXT_M);
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(timeLbl);

        resultPanel.add(content, BorderLayout.CENTER);
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
