package pkg.vms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

class ParametresPanel extends JPanel {

    // ── Palette (Centralisée via VMSStyle) ──────────────────────────────────
    private static final Color BG_ROOT      = VMSStyle.BG_ROOT;
    private static final Color BG_CARD      = VMSStyle.BG_CARD;
    private static final Color BG_HOVER     = VMSStyle.BG_CARD_HOVER;
    private static final Color RED_PRIMARY  = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK     = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT    = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECOND  = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED   = VMSStyle.TEXT_MUTED;
    private static final Color ACCENT_BLUE  = VMSStyle.ACCENT_BLUE;
    private static final Color ACCENT_GREEN = VMSStyle.SUCCESS;
    private static final Color ACCENT_AMBER = VMSStyle.WARNING;
    private static final Color ACCENT_PURP  = new Color(124,  58, 237);

    // ── Fonts (Centralisées via VMSStyle) ────────────────────────────────────
    private static final Font FONT_PAGE_TITLE = VMSStyle.FONT_BRAND.deriveFont(26f);
    private static final Font FONT_SUBTITLE   = VMSStyle.FONT_NAV.deriveFont(14f);
    private static final Font FONT_SECTION    = VMSStyle.FONT_BADGE.deriveFont(9f);
    private static final Font FONT_CARD_TITLE = VMSStyle.FONT_CARD_TTL.deriveFont(15f);
    private static final Font FONT_CARD_DESC  = VMSStyle.FONT_CARD_DSC;
    private static final Font FONT_INFO_KEY   = VMSStyle.FONT_NAV.deriveFont(Font.BOLD, 12f);
    private static final Font FONT_INFO_VAL   = VMSStyle.FONT_NAV;

    private final String role;

    // =====================================================
    //  CONSTRUCTOR
    // =====================================================
    public ParametresPanel(String role) {
        this.role = role;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    // =====================================================
    //  INIT
    // =====================================================
    private void initComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        // ── Page header ───────────────────────────────────────
        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(28));

        // ── Section : Compte ──────────────────────────────────
        content.add(buildSectionLabel("MON COMPTE"));
        content.add(Box.createVerticalStrut(12));

        JPanel rowCompte = buildCardRow(
                buildCard("\uD83D\uDC64", "Mon Profil",
                        "Informations personnelles et pr\u00e9f\u00e9rences",
                        ACCENT_BLUE, "profil"),
                buildCard("\uD83D\uDD10", "S\u00e9curit\u00e9",
                        "Mot de passe et acc\u00e8s",
                        ACCENT_PURP, "securite")
        );
        content.add(rowCompte);
        content.add(Box.createVerticalStrut(28));

        // ── Section : Administration (admin only) ─────────────
        if (role.equalsIgnoreCase("Administrateur")) {
            content.add(buildSectionLabel("ADMINISTRATION"));
            content.add(Box.createVerticalStrut(12));

            JPanel rowAdmin1 = buildCardRow(
                    buildCard("\uD83D\uDC65", "Utilisateurs",
                            "Creer, modifier et gerer les comptes",
                            RED_PRIMARY, "utilisateurs"),
                    buildCard("\uD83C\uDFAD", "R\u00f4les & Permissions",
                            "Definir les niveaux d'acces",
                            ACCENT_AMBER, "roles")
            );
            JPanel rowAdmin2 = buildCardRow(
                    buildCard("\uD83D\uDDC4", "Base de Donnees",
                            "Sauvegardes et maintenance",
                            new Color(51, 65, 85), "database"),
                    buildCard("\uD83D\uDCE7", "Configuration Email",
                            "Parametres SMTP sortants",
                            ACCENT_GREEN, "email")
            );
            JPanel rowAdmin3 = buildCardRow(
                    buildCard("\uD83D\uDCCA", "Logs & Audit",
                            "Historique complet des actions",
                            new Color(100, 116, 139), "logs"),
                    buildCard("\uD83C\uDFE2", "Societes / Enseignes",
                            "Gestion multi-enseignes",
                            new Color(20, 184, 166), "societes")
            );
            content.add(rowAdmin1);
            content.add(Box.createVerticalStrut(16));
            content.add(rowAdmin2);
            content.add(Box.createVerticalStrut(16));
            content.add(rowAdmin3);
            content.add(Box.createVerticalStrut(28));
        }

        // ── Section : Op\u00e9rations (admin + manager) ──────────────
        if (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Manager")) {
            content.add(buildSectionLabel("OPeRATIONS"));
            content.add(Box.createVerticalStrut(12));

            JPanel rowOps = buildCardRow(
                    buildCard("\uD83C\uDFEA", "Magasins",
                            "Points de vente et superviseurs",
                            ACCENT_BLUE, "magasins"),
                    buildCard("\uD83D\uDCCB", "Bons Cadeau",
                            "Modeles, QR Code et regles",
                            RED_PRIMARY, "config_bons")
            );
            JPanel rowOps2 = buildCardRow(
                    buildCard("\uD83D\uDCC8", "Rapports Excel",
                            "Connexion ODBC et exports",
                            ACCENT_GREEN, "rapports"),
                    buildCardDisabled("\uD83D\uDD12", "Parametres Avances",
                            "Bientot disponible")
            );
            content.add(rowOps);
            content.add(Box.createVerticalStrut(16));
            content.add(rowOps2);
            content.add(Box.createVerticalStrut(28));
        }

        // ── Section : Acc\u00e8s limit\u00e9 (utilisateur standard) ──────
        if (role.equalsIgnoreCase("Utilisateur")) {
            content.add(buildSectionLabel("PARAMETRES SYSTEME"));
            content.add(Box.createVerticalStrut(12));
            JPanel rowLocked = buildCardRow(
                    buildCardDisabled("\uD83D\uDC65", "Gestion Utilisateurs",
                            "Reservee aux administrateurs"),
                    buildCardDisabled("\uD83D\uDDC4", "Base de Donnees",
                            "Reservee aux administrateurs")
            );
            content.add(rowLocked);
            content.add(Box.createVerticalStrut(28));
        }

        // ── Section : Infos syst\u00e8me ──────────────────────────────
        content.add(buildSectionLabel("INFORMATIONS SYSTEME"));
        content.add(Box.createVerticalStrut(12));
        content.add(buildSystemInfoCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scroll, BorderLayout.CENTER);
    }

    // =====================================================
    //  PAGE HEADER
    // =====================================================
    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Red left accent bar + title row
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(RED_PRIMARY);
                g.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
            }
        };
        titleRow.setOpaque(false);
        titleRow.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel("\u2699\uFE0F");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("  Param\u00e8tres");
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_PRIMARY);

        titleRow.add(icon);
        titleRow.add(title);

        JLabel sub = new JLabel("Configuration et gestion du syst\u00e8me  \u2014  R\u00f4le : " + role);
        sub.setFont(FONT_SUBTITLE);
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(6, 14, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleRow);
        header.add(sub);
        return header;
    }

    // =====================================================
    //  SECTION LABEL
    // =====================================================
    private JLabel buildSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(RED_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // =====================================================
    //  CARD ROW  (2 cards side by side)
    // =====================================================
    private JPanel buildCardRow(JPanel c1, JPanel c2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row.add(c1);
        row.add(c2);
        return row;
    }

    // =====================================================
    //  ACTIVE CARD
    // =====================================================
    private JPanel buildCard(String emoji, String titre, String desc,
                             Color accent, String action) {
        JPanel card = new JPanel() {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        setCursor(new Cursor(Cursor.HAND_CURSOR));
                        repaint();
                    }
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                    public void mouseClicked(MouseEvent e) { ouvrirParametre(action); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Card background
                g2.setColor(hovered ? BG_HOVER : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));

                // Border
                g2.setColor(hovered ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120) : BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 12, 12));

                // Left accent stripe
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Double(0, 0, 4, getHeight(), 4, 4));

                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 16));

        // Icon bubble
        JLabel iconLbl = new JLabel(emoji) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);
        iconLbl.setPreferredSize(new Dimension(48, 48));
        iconLbl.setOpaque(false);

        // Text block
        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(titre);
        titleLbl.setFont(FONT_CARD_TITLE);
        titleLbl.setForeground(TEXT_PRIMARY);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(FONT_CARD_DESC);
        descLbl.setForeground(TEXT_MUTED);
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBlock.add(titleLbl);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(descLbl);

        // Arrow indicator
        JLabel arrow = new JLabel("\u203A");
        arrow.setFont(new Font("Trebuchet MS", Font.BOLD, 20));
        arrow.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
        arrow.setPreferredSize(new Dimension(20, 20));

        card.add(iconLbl,   BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        card.add(arrow,     BorderLayout.EAST);

        return card;
    }

    // =====================================================
    //  DISABLED CARD
    // =====================================================
    private JPanel buildCardDisabled(String emoji, String titre, String desc) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 249, 252));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 12, 12));
                // Dashed left stripe
                g2.setColor(new Color(200, 205, 215));
                float[] dash = {4f, 4f};
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
                g2.drawLine(2, 8, 2, getHeight()-8);
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 16));

        JLabel iconLbl = new JLabel(emoji);
        iconLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        iconLbl.setHorizontalAlignment(SwingConstants.CENTER);
        iconLbl.setVerticalAlignment(SwingConstants.CENTER);
        iconLbl.setPreferredSize(new Dimension(48, 48));
        iconLbl.setForeground(TEXT_MUTED);

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(titre);
        titleLbl.setFont(FONT_CARD_TITLE);
        titleLbl.setForeground(TEXT_MUTED);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLbl = new JLabel(desc);
        descLbl.setFont(FONT_CARD_DESC);
        descLbl.setForeground(new Color(200, 205, 215));
        descLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBlock.add(titleLbl);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(descLbl);

        JLabel lockLbl = new JLabel("\uD83D\uDD12");
        lockLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        lockLbl.setForeground(TEXT_MUTED);

        card.add(iconLbl,   BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        card.add(lockLbl,   BorderLayout.EAST);

        return card;
    }

    // =====================================================
    //  SYSTEM INFO CARD
    // =====================================================
    private JPanel buildSystemInfoCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 12, 12));
                // Red top bar
                g2.setColor(RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), 4, 4, 4));
                g2.dispose();
            }
        };
        card.setLayout(new GridLayout(1, 3, 0, 0));
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
        card.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));

        card.add(buildInfoItem("\uD83D\uDCBB", "Version", "VMS 1.0.0"));
        card.add(buildInfoItem("\uD83D\uDDC4", "Base de donn\u00e9es", "PostgreSQL"));
        card.add(buildInfoItem("\uD83D\uDC64", "R\u00f4le actif", role));

        return card;
    }

    private JPanel buildInfoItem(String emoji, String key, String value) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel keyLbl = new JLabel(emoji + "  " + key);
        keyLbl.setFont(FONT_INFO_KEY);
        keyLbl.setForeground(TEXT_MUTED);
        keyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("Trebuchet MS", Font.BOLD, 14));
        valLbl.setForeground(TEXT_PRIMARY);
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        p.add(keyLbl);
        p.add(Box.createVerticalStrut(4));
        p.add(valLbl);
        return p;
    }

    // =====================================================
    //  ROUTING
    // =====================================================
    private void ouvrirParametre(String action) {
        switch (action) {
            case "profil":        afficherGestionProfil();          break;
            case "securite":      afficherChangementMotDePasse();   break;
            case "utilisateurs":  afficherGestionUtilisateurs();    break;
            case "roles":         afficherGestionRoles();           break;
            case "database":      afficherGestionDatabase();        break;
            case "email":         afficherConfigEmail();            break;
            case "logs":          afficherLogs();                   break;
            case "societes":      afficherGestionSocietes();        break;
            case "magasins":      afficherGestionMagasins();        break;
            case "config_bons":   afficherConfigBons();             break;
            case "rapports":      afficherConfigRapports();         break;
            default:
                JOptionPane.showMessageDialog(this,
                        "Fonctionnalit\u00e9 en cours de d\u00e9veloppement",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // =====================================================
    //  ACTION DIALOGS  (inchang\u00e9s, juste regroup\u00e9s)
    // =====================================================
    private void afficherGestionProfil() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDC64 Gestion du Profil</h3>" +
                        "<p>Permettra de modifier :</p>" +
                        "<ul><li>Nom d'utilisateur</li><li>Email</li><li>Photo de profil</li></ul></html>",
                "Mon Profil", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherChangementMotDePasse() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        JPasswordField txtAncien  = new JPasswordField();
        JPasswordField txtNouveau = new JPasswordField();
        JPasswordField txtConfirm = new JPasswordField();
        panel.add(new JLabel("Mot de passe actuel :")); panel.add(txtAncien);
        panel.add(new JLabel("Nouveau mot de passe :")); panel.add(txtNouveau);
        panel.add(new JLabel("Confirmer :")); panel.add(txtConfirm);

        int r = JOptionPane.showConfirmDialog(this, panel,
                "\uD83D\uDD10 Changer le mot de passe",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (r == JOptionPane.OK_OPTION) {
            String nouveau  = new String(txtNouveau.getPassword());
            String confirm  = new String(txtConfirm.getPassword());
            if (!nouveau.equals(confirm)) {
                JOptionPane.showMessageDialog(this,
                        "Les mots de passe ne correspondent pas !",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Mot de passe chang\u00e9 avec succ\u00e8s !",
                        "Succ\u00e8s", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void afficherGestionUtilisateurs() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDC65 Gestion des Utilisateurs</h3>" +
                        "<ul><li>Cr\u00e9er de nouveaux utilisateurs</li>" +
                        "<li>Modifier les informations</li>" +
                        "<li>Attribuer des r\u00f4les</li>" +
                        "<li>D\u00e9sactiver / Supprimer des comptes</li></ul></html>",
                "Gestion Utilisateurs", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherGestionRoles() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83C\uDFAD R\u00f4les disponibles</h3>" +
                        "<ul><li><b>Administrateur</b> \u2014 Acc\u00e8s complet</li>" +
                        "<li><b>Manager</b> \u2014 Gestion op\u00e9rationnelle</li>" +
                        "<li><b>Comptable</b> \u2014 Validation paiements</li>" +
                        "<li><b>Approbateur</b> \u2014 Approbation des demandes</li>" +
                        "<li><b>Utilisateur</b> \u2014 Cr\u00e9ation de demandes</li>" +
                        "<li><b>Superviseur Magasin</b> \u2014 R\u00e9demption des bons</li></ul></html>",
                "Gestion des R\u00f4les", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherGestionDatabase() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDDC4 Base de Donn\u00e9es</h3>" +
                        "<ul><li>Sauvegarde automatique</li><li>Restauration</li>" +
                        "<li>Optimisation des tables</li><li>Gestion des index</li></ul></html>",
                "Base de Donn\u00e9es", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherConfigEmail() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDCE7 Configuration Email</h3>" +
                        "<ul><li>Serveur SMTP</li><li>Port et s\u00e9curit\u00e9 (TLS/SSL)</li>" +
                        "<li>Authentification</li><li>Mod\u00e8les d'emails</li></ul></html>",
                "Configuration Email", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherLogs() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDCCA Logs & Audit Trail</h3>" +
                        "<ul><li>Connexions / D\u00e9connexions</li><li>Cr\u00e9ation de demandes</li>" +
                        "<li>Validation paiements</li><li>G\u00e9n\u00e9ration de bons</li>" +
                        "<li>R\u00e9demptions</li></ul></html>",
                "Logs Syst\u00e8me", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherGestionSocietes() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83C\uDFE2 Gestion des Soci\u00e9t\u00e9s</h3>" +
                        "<ul><li>Ajouter de nouvelles enseignes</li>" +
                        "<li>Configuration par soci\u00e9t\u00e9</li>" +
                        "<li>Logo et branding</li></ul></html>",
                "Gestion Soci\u00e9t\u00e9s", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherGestionMagasins() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83C\uDFEA Gestion des Magasins</h3>" +
                        "<ul><li>Ajouter / Modifier des magasins</li>" +
                        "<li>Assigner des superviseurs</li>" +
                        "<li>Configuration de r\u00e9demption</li></ul></html>",
                "Gestion Magasins", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherConfigBons() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDCCB Configuration des Bons</h3>" +
                        "<ul><li>Mod\u00e8le PDF personnalis\u00e9</li><li>Format QR Code / Code-barres</li>" +
                        "<li>Signature num\u00e9rique</li><li>Validit\u00e9 par d\u00e9faut</li></ul></html>",
                "Configuration Bons", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherConfigRapports() {
        JOptionPane.showMessageDialog(this,
                "<html><h3>\uD83D\uDCC8 Configuration Rapports Excel</h3>" +
                        "<ul><li>Configuration ODBC PostgreSQL</li>" +
                        "<li>Mod\u00e8les de rapports</li><li>Export des donn\u00e9es</li></ul></html>",
                "Configuration Rapports", JOptionPane.INFORMATION_MESSAGE);
    }
}