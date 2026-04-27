package pkg.vms;

import pkg.vms.controller.ParametresController;
import pkg.vms.DAO.MagasinDAO;
import pkg.vms.DAO.SettingsDAO;
import pkg.vms.DAO.SocieteDAO;
import pkg.vms.DAO.StatistiquesDAO;
import pkg.vms.DAO.UserDAO;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

class ParametresPanel extends JPanel {

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
    private static final Color ACCENT_PURP  = new Color(124, 58, 237);

    private static final Font FONT_PAGE_TITLE = VMSStyle.FONT_BRAND.deriveFont(26f);
    private static final Font FONT_SUBTITLE   = VMSStyle.FONT_NAV.deriveFont(14f);
    private static final Font FONT_SECTION    = VMSStyle.FONT_BADGE.deriveFont(9f);
    private static final Font FONT_CARD_TITLE = VMSStyle.FONT_CARD_TTL.deriveFont(15f);
    private static final Font FONT_CARD_DESC  = VMSStyle.FONT_CARD_DSC;
    private static final Font FONT_INFO_KEY   = VMSStyle.FONT_NAV.deriveFont(Font.BOLD, 12f);
    private static final Font FONT_INFO_VAL   = VMSStyle.FONT_NAV;

    private final String role;
    private final int userId;
    private final ParametresController controller = new ParametresController();

    public ParametresPanel(String role, int userId) {
        this.role = role;
        this.userId = userId;
        setLayout(new BorderLayout());
        setOpaque(false);
        initComponents();
    }

    private void initComponents() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        content.add(buildPageHeader());
        content.add(Box.createVerticalStrut(28));
        content.add(buildSectionLabel("MON COMPTE"));
        content.add(Box.createVerticalStrut(12));

        JPanel rowCompte = buildCardRow(
                buildCard("👤", "Mon Profil", "Informations personnelles", ACCENT_BLUE, "profil"),
                buildCard("🔐", "Sécurité", "Changer le mot de passe", ACCENT_PURP, "securite")
        );
        content.add(rowCompte);
        content.add(Box.createVerticalStrut(28));

        if (role.equalsIgnoreCase("Administrateur")) {
            content.add(buildSectionLabel("ADMINISTRATION"));
            content.add(Box.createVerticalStrut(12));

            JPanel rowAdmin1 = buildCardRow(
                    buildCard("👥", "Utilisateurs", "Créer, modifier et gérer les comptes", RED_PRIMARY, "utilisateurs"),
                    buildCard("🎭", "Rôles & Permissions", "Définir les niveaux d'accès", ACCENT_AMBER, "roles")
            );
            JPanel rowAdmin2 = buildCardRow(
                    buildCard("🗄️", "Base de Données", "Sauvegardes et maintenance", new Color(51, 65, 85), "database"),
                    buildCard("📧", "Configuration Email", "Paramètres SMTP sortants", ACCENT_GREEN, "email")
            );
            JPanel rowAdmin3 = buildCardRow(
                    buildCard("📊", "Logs & Audit", "Historique complet des actions", new Color(100, 116, 139), "logs"),
                    buildCard("🏢", "Societes / Enseignes", "Gestion multi-enseignes", new Color(20, 184, 166), "societes")
            );
            content.add(rowAdmin1);
            content.add(Box.createVerticalStrut(16));
            content.add(rowAdmin2);
            content.add(Box.createVerticalStrut(16));
            content.add(rowAdmin3);
            content.add(Box.createVerticalStrut(28));
        }

        if (role.equalsIgnoreCase("Administrateur") || role.equalsIgnoreCase("Manager")) {
            content.add(buildSectionLabel("OPÉRATIONS"));
            content.add(Box.createVerticalStrut(12));

            JPanel rowOps = buildCardRow(
                    buildCard("🏪", "Magasins", "Points de vente et superviseurs", ACCENT_BLUE, "magasins"),
                    buildCard("📋", "Bons Cadeau", "Modeles, QR Code et regles", RED_PRIMARY, "config_bons")
            );
            JPanel rowOps2 = buildCardRow(
                    buildCard("📈", "Rapports Excel", "Connexion ODBC et exports", ACCENT_GREEN, "rapports"),
                    buildCardDisabled("🔒", "Parametres Avances", "Bientot disponible")
            );
            content.add(rowOps);
            content.add(Box.createVerticalStrut(16));
            content.add(rowOps2);
            content.add(Box.createVerticalStrut(28));
        }

        if (role.equalsIgnoreCase("Collaborateur")) {
            content.add(buildSectionLabel("PARAMETRES SYSTEME"));
            content.add(Box.createVerticalStrut(12));
            JPanel rowLocked = buildCardRow(
                    buildCardDisabled("👥", "Gestion Utilisateurs", "Reservee aux administrateurs"),
                    buildCardDisabled("🗄️", "Base de Donnees", "Reservee aux administrateurs")
            );
            content.add(rowLocked);
            content.add(Box.createVerticalStrut(28));
        }

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

    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        JLabel icon = new JLabel("⚙️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JLabel title = new JLabel("  Paramètres");
        title.setFont(FONT_PAGE_TITLE);
        title.setForeground(TEXT_PRIMARY);

        titleRow.add(icon);
        titleRow.add(title);

        JLabel sub = new JLabel("Configuration et gestion du système  —  Rôle : " + role);
        sub.setFont(FONT_SUBTITLE);
        sub.setForeground(TEXT_SECOND);
        sub.setBorder(BorderFactory.createEmptyBorder(6, 14, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(titleRow);
        header.add(sub);
        return header;
    }

    private JLabel buildSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(RED_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel buildCardRow(JPanel c1, JPanel c2) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        row.add(c1);
        row.add(c2);
        return row;
    }

    private JPanel buildCard(String emoji, String titre, String desc, Color accent, String action) {
        JPanel card = new JPanel() {
            boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true; setCursor(new Cursor(Cursor.HAND_CURSOR)); repaint(); }
                    public void mouseExited(MouseEvent e)  { hovered = false; repaint(); }
                    public void mouseClicked(MouseEvent e) { ouvrirParametre(action); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? BG_HOVER : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(hovered ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 120) : BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 12, 12));
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Double(0, 0, 4, getHeight(), 4, 4));
                g2.dispose();
            }
        };
        card.setLayout(new BorderLayout(0, 0));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 16));

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

        JLabel arrow = new JLabel("›");
        arrow.setFont(new Font("Trebuchet MS", Font.BOLD, 20));
        arrow.setForeground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140));
        arrow.setPreferredSize(new Dimension(20, 20));

        card.add(iconLbl,   BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        card.add(arrow,     BorderLayout.EAST);

        return card;
    }

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

        JLabel lockLbl = new JLabel("🔒");
        lockLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        lockLbl.setForeground(TEXT_MUTED);

        card.add(iconLbl,   BorderLayout.WEST);
        card.add(textBlock, BorderLayout.CENTER);
        card.add(lockLbl,   BorderLayout.EAST);

        return card;
    }

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

        card.add(buildInfoItem("💻", "Version", "VMS 1.0.0"));
        card.add(buildInfoItem("🗄️", "Base de données", "PostgreSQL"));
        card.add(buildInfoItem("👤", "Rôle actif", role));

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

    private void ouvrirParametre(String action) {
        switch (action) {
            case "profil":        afficherGestionProfil();         break;
            case "securite":      afficherChangementMotDePasse();  break;
            case "utilisateurs":  afficherGestionUtilisateurs();   break;
            case "roles":         afficherGestionRoles();          break;
            case "database":      afficherGestionDatabase();       break;
            case "email":         afficherConfigEmail();           break;
            case "logs":          afficherLogs();                  break;
            case "societes":      afficherGestionSocietes();       break;
            case "magasins":      afficherGestionMagasins();       break;
            case "config_bons":   afficherConfigBons();            break;
            case "rapports":      afficherConfigRapports();        break;
            default:
                JOptionPane.showMessageDialog(this,
                        "Fonctionnalité en cours de développement",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ── MON PROFIL ────────────────────────────────────────────────────────────

    private void afficherGestionProfil() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Mon Profil", true);
        dlg.setSize(500, 350);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("👤 Gérer mon Profil");
        title.setFont(FONT_PAGE_TITLE.deriveFont(20f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(2, 2, 12, 12));
        form.setOpaque(false);

        JLabel usernameL = new JLabel("Nom d'utilisateur:");
        usernameL.setForeground(TEXT_PRIMARY);
        usernameL.setFont(FONT_INFO_KEY);
        JTextField usernameF = new JTextField();
        usernameF.setFont(FONT_INFO_VAL);

        JLabel emailL = new JLabel("Email:");
        emailL.setForeground(TEXT_PRIMARY);
        emailL.setFont(FONT_INFO_KEY);
        JTextField emailF = new JTextField();
        emailF.setFont(FONT_INFO_VAL);

        form.add(usernameL); form.add(usernameF);
        form.add(emailL);    form.add(emailF);

        controller.chargerProfil(userId, profile -> {
            if (profile != null) {
                usernameF.setText(profile.username);
                emailF.setText(profile.email);
            }
        }, err -> System.err.println("Erreur chargement profil: " + err));

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Enregistrer", 120, 38);
        btnSave.addActionListener(e -> {
            btnSave.setEnabled(false);
            controller.mettreAJourProfil(userId, usernameF.getText(), emailF.getText(),
                succes -> {
                    btnSave.setEnabled(true);
                    if (succes) {
                        JOptionPane.showMessageDialog(dlg, "Profil mis à jour avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Erreur lors de la mise à jour", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                },
                err -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur: " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 120, 38);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── CHANGER MOT DE PASSE ─────────────────────────────────────────────────

    private void afficherChangementMotDePasse() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Changer le mot de passe", true);
        dlg.setSize(480, 300);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("🔐 Changer votre mot de passe");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 12, 12));
        form.setOpaque(false);

        JLabel ancienL = new JLabel("Mot de passe actuel:");
        ancienL.setFont(FONT_INFO_KEY); ancienL.setForeground(TEXT_PRIMARY);
        JPasswordField ancienF = new JPasswordField();

        JLabel nouveauL = new JLabel("Nouveau mot de passe:");
        nouveauL.setFont(FONT_INFO_KEY); nouveauL.setForeground(TEXT_PRIMARY);
        JPasswordField nouveauF = new JPasswordField();

        JLabel confirmL = new JLabel("Confirmer:");
        confirmL.setFont(FONT_INFO_KEY); confirmL.setForeground(TEXT_PRIMARY);
        JPasswordField confirmF = new JPasswordField();

        form.add(ancienL);  form.add(ancienF);
        form.add(nouveauL); form.add(nouveauF);
        form.add(confirmL); form.add(confirmF);

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Valider", 120, 38);
        btnSave.addActionListener(e -> {
            String ancien  = new String(ancienF.getPassword());
            String nouveau = new String(nouveauF.getPassword());
            String confirm = new String(confirmF.getPassword());

            if (ancien.isEmpty() || nouveau.isEmpty() || confirm.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Tous les champs sont obligatoires", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!nouveau.equals(confirm)) {
                JOptionPane.showMessageDialog(dlg, "Les mots de passe ne correspondent pas!", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (nouveau.length() < 6) {
                JOptionPane.showMessageDialog(dlg, "Le mot de passe doit contenir au moins 6 caractères", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnSave.setEnabled(false);
            controller.changerMotDePasse(userId, ancien, nouveau,
                succes -> {
                    btnSave.setEnabled(true);
                    if (succes) {
                        JOptionPane.showMessageDialog(dlg, "Mot de passe changé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Ancien mot de passe incorrect", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                },
                err -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur: " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 120, 38);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── GESTION UTILISATEURS ─────────────────────────────────────────────────

    private void afficherGestionUtilisateurs() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gestion des Utilisateurs", true);
        dlg.setSize(900, 550);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("👥 Gestion des Utilisateurs");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Nom", "Email", "Rôle"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));
        table.getTableHeader().setForeground(TEXT_SECOND);

        controller.chargerUtilisateurs(users -> {
            for (UserDAO.UserProfile u : users)
                model.addRow(new Object[]{u.userId, u.username, u.email, u.role});
        }, err -> System.err.println("Erreur chargement utilisateurs: " + err));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);

        JButton btnAdd = UIUtils.buildRedButton("+ Ajouter", 120, 36);
        btnAdd.addActionListener(e -> afficherFormulaireAjouterUtilisateur(dlg, model));

        JButton btnClose = UIUtils.buildOutlineButton("Fermer", 120, 36);
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnAdd);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    private void afficherFormulaireAjouterUtilisateur(JDialog parent, DefaultTableModel model) {
        JDialog dlg = new JDialog(parent, "Ajouter un Utilisateur", true);
        dlg.setSize(450, 380);
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Nouvel Utilisateur");
        title.setFont(FONT_PAGE_TITLE.deriveFont(16f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 2, 12, 12));
        form.setOpaque(false);

        JLabel usernameL = new JLabel("Nom d'utilisateur:"); usernameL.setFont(FONT_INFO_KEY); usernameL.setForeground(TEXT_PRIMARY);
        JTextField usernameF = new JTextField();
        JLabel emailL = new JLabel("Email:");             emailL.setFont(FONT_INFO_KEY);    emailL.setForeground(TEXT_PRIMARY);
        JTextField emailF = new JTextField();
        JLabel passwordL = new JLabel("Mot de passe:");  passwordL.setFont(FONT_INFO_KEY); passwordL.setForeground(TEXT_PRIMARY);
        JPasswordField passwordF = new JPasswordField();
        JLabel roleL = new JLabel("Rôle:");              roleL.setFont(FONT_INFO_KEY);     roleL.setForeground(TEXT_PRIMARY);
        JComboBox<String> roleC = new JComboBox<>();

        controller.chargerRolesDisponibles(roles -> {
            for (String r : roles) roleC.addItem(r);
        }, err -> System.err.println("Erreur chargement roles: " + err));

        form.add(usernameL); form.add(usernameF);
        form.add(emailL);    form.add(emailF);
        form.add(passwordL); form.add(passwordF);
        form.add(roleL);     form.add(roleC);

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Créer", 120, 38);
        btnSave.addActionListener(e -> {
            String username = usernameF.getText();
            String email    = emailF.getText();
            String password = new String(passwordF.getPassword());
            String role     = (String) roleC.getSelectedItem();

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Tous les champs sont obligatoires", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnSave.setEnabled(false);
            controller.creerUtilisateur(username, email, password, role,
                succes -> {
                    btnSave.setEnabled(true);
                    if (succes) {
                        JOptionPane.showMessageDialog(dlg, "Utilisateur créé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    } else {
                        JOptionPane.showMessageDialog(dlg, "Erreur lors de la création", "Erreur", JOptionPane.ERROR_MESSAGE);
                    }
                },
                err -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur: " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 120, 38);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── GESTION ROLES ─────────────────────────────────────────────────────────

    private void afficherGestionRoles() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gestion des Rôles", true);
        dlg.setSize(750, 520);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("🎭 Utilisateurs et leurs Rôles");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Nom d'utilisateur", "Email", "Rôle actuel"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));

        controller.chargerUtilisateurs(users -> {
            for (UserDAO.UserProfile u : users)
                model.addRow(new Object[]{u.userId, u.username, u.email, u.role});
        }, err -> System.err.println("Erreur chargement utilisateurs pour roles: " + err));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setOpaque(false);

        String[] rolesDisponibles = {"Administrateur", "Manager", "Comptable", "Approbateur", "Collaborateur", "Superviseur_Magasin"};

        JButton btnChanger = UIUtils.buildRedButton("Changer le rôle", 160, 36);
        btnChanger.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(dlg, "Sélectionnez un utilisateur", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int uid = (int) model.getValueAt(row, 0);
            String currentRole = (String) model.getValueAt(row, 3);
            String newRole = (String) JOptionPane.showInputDialog(dlg,
                    "Nouveau rôle pour " + model.getValueAt(row, 1) + " :",
                    "Changer le rôle", JOptionPane.PLAIN_MESSAGE, null, rolesDisponibles, currentRole);
            if (newRole != null && !newRole.equals(currentRole)) {
                controller.changerRoleUtilisateur(uid, newRole,
                    () -> {
                        model.setValueAt(newRole, row, 3);
                        JOptionPane.showMessageDialog(dlg, "Rôle mis à jour avec succès !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    },
                    err -> JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        JButton btnClose = UIUtils.buildOutlineButton("Fermer", 120, 36);
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnChanger);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── BASE DE DONNEES ───────────────────────────────────────────────────────

    private void afficherGestionDatabase() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Monitoring Base de Données", true);
        dlg.setSize(650, 500);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("🗄️ Monitoring Base de Données");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"Table", "Nombre de lignes"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
        table.setRowHeight(30);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));

        JLabel lblInfo = new JLabel("Chargement...");
        lblInfo.setFont(FONT_SUBTITLE);
        lblInfo.setForeground(TEXT_SECOND);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        controller.chargerStatsDB(
            stats -> {
                lblInfo.setText(stats.info);
                for (Object[] row : stats.rows) model.addRow(row);
            },
            err -> lblInfo.setText("Erreur : " + err)
        );

        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(lblInfo, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 8));
        footer.setOpaque(false);

        JButton btnVacuum = UIUtils.buildRedButton("Optimiser (VACUUM)", 180, 36);
        btnVacuum.addActionListener(e -> {
            btnVacuum.setEnabled(false);
            controller.vacuumDB(
                () -> {
                    btnVacuum.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Optimisation VACUUM ANALYZE terminée !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                },
                err -> {
                    btnVacuum.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnTest = UIUtils.buildRedButton("Tester connexion", 160, 36);
        btnTest.addActionListener(e ->
            controller.testerConnexionDB(
                ok -> JOptionPane.showMessageDialog(dlg,
                        ok ? "Connexion OK !" : "Connexion échouée",
                        ok ? "Succès" : "Erreur",
                        ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE),
                err -> JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE))
        );

        JButton btnClose = UIUtils.buildOutlineButton("Fermer", 120, 36);
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnVacuum);
        footer.add(btnTest);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── CONFIG EMAIL ─────────────────────────────────────────────────────────

    private void afficherConfigEmail() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Configuration Email", true);
        dlg.setSize(550, 450);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("📧 Configuration Email SMTP");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(6, 2, 12, 12));
        form.setOpaque(false);

        JLabel serverL = new JLabel("Serveur SMTP:"); serverL.setFont(FONT_INFO_KEY); serverL.setForeground(TEXT_PRIMARY);
        JTextField serverF = new JTextField();
        JLabel portL = new JLabel("Port:");          portL.setFont(FONT_INFO_KEY);   portL.setForeground(TEXT_PRIMARY);
        JTextField portF = new JTextField();
        JLabel userL = new JLabel("Nom d'utilisateur:"); userL.setFont(FONT_INFO_KEY); userL.setForeground(TEXT_PRIMARY);
        JTextField userF = new JTextField();
        JLabel passL = new JLabel("Mot de passe:"); passL.setFont(FONT_INFO_KEY);   passL.setForeground(TEXT_PRIMARY);
        JPasswordField passF = new JPasswordField();
        JLabel tlsL = new JLabel("TLS Enabled:");  tlsL.setFont(FONT_INFO_KEY);    tlsL.setForeground(TEXT_PRIMARY);
        JCheckBox tlsC = new JCheckBox();
        JLabel fromL = new JLabel("From Email:"); fromL.setFont(FONT_INFO_KEY);   fromL.setForeground(TEXT_PRIMARY);
        JTextField fromF = new JTextField();

        form.add(serverL); form.add(serverF);
        form.add(portL);   form.add(portF);
        form.add(userL);   form.add(userF);
        form.add(passL);   form.add(passF);
        form.add(tlsL);    form.add(tlsC);
        form.add(fromL);   form.add(fromF);

        controller.chargerConfigEmail(settings -> {
            if (settings != null) {
                serverF.setText(settings.smtpServer);
                portF.setText(String.valueOf(settings.smtpPort));
                userF.setText(settings.smtpUsername);
                passF.setText(settings.smtpPassword);
                tlsC.setSelected(settings.tlsEnabled);
                fromF.setText(settings.fromEmail);
            }
        }, err -> System.err.println("Erreur chargement config email: " + err));

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Enregistrer", 140, 38);
        btnSave.addActionListener(e -> {
            try {
                SettingsDAO.EmailSettings settings = new SettingsDAO.EmailSettings(
                        serverF.getText(),
                        Integer.parseInt(portF.getText()),
                        userF.getText(),
                        new String(passF.getPassword()),
                        tlsC.isSelected(),
                        fromF.getText());
                btnSave.setEnabled(false);
                controller.sauvegarderConfigEmail(settings,
                    succes -> {
                        btnSave.setEnabled(true);
                        if (succes) {
                            JOptionPane.showMessageDialog(dlg, "Configuration email enregistrée!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                            dlg.dispose();
                        }
                    },
                    err -> {
                        btnSave.setEnabled(true);
                        JOptionPane.showMessageDialog(dlg, "Erreur: " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                    });
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dlg, "Le port doit être un nombre", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 140, 38);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── LOGS & AUDIT ──────────────────────────────────────────────────────────

    private void afficherLogs() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Logs & Audit Trail", true);
        dlg.setSize(1020, 620);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Historique des actions");
        title.setFont(FONT_PAGE_TITLE.deriveFont(20f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"Date", "Action", "Description", "Utilisateur", "Table", "Record ID"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));
        table.getTableHeader().setForeground(TEXT_SECOND);

        controller.chargerLogs(role, userId, rows -> {
            for (String[] row : rows)
                model.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], row[5]});
        }, err -> model.addRow(new Object[]{"", "Erreur", err, "", "", ""}));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setOpaque(false);
        JButton btnFermer = UIUtils.buildOutlineButton("Fermer");
        btnFermer.addActionListener(e -> dlg.dispose());
        footer.add(btnFermer);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── GESTION SOCIETES ──────────────────────────────────────────────────────

    private void afficherGestionSocietes() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gestion des Sociétés", true);
        dlg.setSize(800, 550);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("🏢 Gestion des Sociétés / Enseignes");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Nom", "Adresse", "Téléphone", "Email"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        table.setRowHeight(32);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));

        Runnable chargerSocietes = () -> {
            model.setRowCount(0);
            controller.chargerSocietes(
                societes -> {
                    for (SocieteDAO.Societe s : societes)
                        model.addRow(new Object[]{s.societeId, s.nom, s.adresse, s.telephone, s.email});
                },
                err -> System.err.println("Erreur chargement societes: " + err));
        };
        chargerSocietes.run();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setOpaque(false);

        JButton btnAdd = UIUtils.buildRedButton("+ Ajouter", 120, 36);
        btnAdd.addActionListener(e -> {
            JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
            JTextField fNom = new JTextField(), fAdresse = new JTextField(), fTel = new JTextField(), fEmail = new JTextField();
            form.add(new JLabel("Nom :"));      form.add(fNom);
            form.add(new JLabel("Adresse :"));  form.add(fAdresse);
            form.add(new JLabel("Téléphone :")); form.add(fTel);
            form.add(new JLabel("Email :"));    form.add(fEmail);
            int r = JOptionPane.showConfirmDialog(dlg, form, "Nouvelle Société", JOptionPane.OK_CANCEL_OPTION);
            if (r == JOptionPane.OK_OPTION && !fNom.getText().trim().isEmpty()) {
                controller.ajouterSociete(fNom.getText().trim(), fAdresse.getText().trim(),
                    fTel.getText().trim(), fEmail.getText().trim(),
                    () -> {
                        chargerSocietes.run();
                        JOptionPane.showMessageDialog(dlg, "Société ajoutée !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    },
                    err -> JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        JButton btnDelete = UIUtils.buildRedButton("Supprimer", 120, 36);
        btnDelete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row < 0) { JOptionPane.showMessageDialog(dlg, "Sélectionnez une société"); return; }
            int id = (int) model.getValueAt(row, 0);
            int conf = JOptionPane.showConfirmDialog(dlg, "Supprimer cette société ?", "Confirmation", JOptionPane.YES_NO_OPTION);
            if (conf == JOptionPane.YES_OPTION) {
                controller.supprimerSociete(id, chargerSocietes::run,
                    err -> JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        JButton btnClose = UIUtils.buildOutlineButton("Fermer", 120, 36);
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnAdd);
        footer.add(btnDelete);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── GESTION MAGASINS ──────────────────────────────────────────────────────

    private void afficherGestionMagasins() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Gestion des Magasins", true);
        dlg.setSize(850, 550);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("🏪 Points de vente Intermart");
        title.setFont(FONT_PAGE_TITLE.deriveFont(20f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "Nom", "Adresse", "Superviseur"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        table.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        table.setRowHeight(36);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(240, 242, 246));
        table.getTableHeader().setFont(new Font("Trebuchet MS", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(248, 249, 252));
        table.getTableHeader().setForeground(TEXT_SECOND);

        controller.chargerMagasins(magasins -> {
            for (MagasinDAO.Magasin m : magasins)
                model.addRow(new Object[]{
                    m.magasinId, m.nomMagasin, m.adresse,
                    m.superviseurNom != null ? m.superviseurNom : "—"
                });
        }, err -> System.err.println("Erreur chargement magasins: " + err));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);

        JButton btnAdd = UIUtils.buildRedButton("+ Ajouter", 120, 36);
        btnAdd.addActionListener(e -> afficherFormulaireAjouterMagasin(dlg, model));

        JButton btnClose = UIUtils.buildOutlineButton("Fermer", 120, 36);
        btnClose.addActionListener(e -> dlg.dispose());

        footer.add(btnAdd);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    private void afficherFormulaireAjouterMagasin(JDialog parent, DefaultTableModel model) {
        JDialog dlg = new JDialog(parent, "Ajouter un Magasin", true);
        dlg.setSize(480, 380);
        dlg.setLocationRelativeTo(parent);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Nouveau Magasin");
        title.setFont(FONT_PAGE_TITLE.deriveFont(16f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(3, 2, 12, 12));
        form.setOpaque(false);

        JLabel nomL = new JLabel("Nom du magasin:"); nomL.setFont(FONT_INFO_KEY); nomL.setForeground(TEXT_PRIMARY);
        JTextField nomF = new JTextField();
        JLabel adresseL = new JLabel("Adresse:"); adresseL.setFont(FONT_INFO_KEY); adresseL.setForeground(TEXT_PRIMARY);
        JTextField adresseF = new JTextField();
        JLabel superviseurL = new JLabel("Superviseur:"); superviseurL.setFont(FONT_INFO_KEY); superviseurL.setForeground(TEXT_PRIMARY);
        JComboBox<String> superviseurC = new JComboBox<>();
        superviseurC.addItem("Aucun");

        form.add(nomL);         form.add(nomF);
        form.add(adresseL);     form.add(adresseF);
        form.add(superviseurL); form.add(superviseurC);

        root.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Créer", 120, 38);
        btnSave.addActionListener(e -> {
            String nom     = nomF.getText();
            String adresse = adresseF.getText();

            if (nom.isEmpty() || adresse.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Tous les champs sont obligatoires", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnSave.setEnabled(false);
            controller.ajouterMagasin(nom, adresse,
                succes -> {
                    btnSave.setEnabled(true);
                    if (succes) {
                        JOptionPane.showMessageDialog(dlg, "Magasin créé avec succès!", "Succès", JOptionPane.INFORMATION_MESSAGE);
                        dlg.dispose();
                    }
                },
                err -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur: " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 120, 38);
        btnCancel.addActionListener(e -> dlg.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        root.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── CONFIG BONS ───────────────────────────────────────────────────────────

    private void afficherConfigBons() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Configuration des Bons", true);
        dlg.setSize(500, 400);
        dlg.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG_ROOT);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("📋 Configuration des Bons d'Achat");
        title.setFont(FONT_PAGE_TITLE.deriveFont(18f));
        title.setForeground(TEXT_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(5, 2, 12, 12));
        form.setOpaque(false);

        JLabel lVal  = new JLabel("Validité par défaut (jours) :"); lVal.setFont(FONT_INFO_KEY);  lVal.setForeground(TEXT_PRIMARY);
        JTextField fVal = new JTextField("365");
        JLabel lType = new JLabel("Type par défaut :"); lType.setFont(FONT_INFO_KEY); lType.setForeground(TEXT_PRIMARY);
        JComboBox<String> fType = new JComboBox<>(new String[]{"Standard", "Cadeau", "Promo"});
        JLabel lEnt  = new JLabel("Nom entreprise (PDF) :"); lEnt.setFont(FONT_INFO_KEY); lEnt.setForeground(TEXT_PRIMARY);
        JTextField fEnt = new JTextField("Intermart Maurice");
        JLabel lQr   = new JLabel("Format code :"); lQr.setFont(FONT_INFO_KEY); lQr.setForeground(TEXT_PRIMARY);
        JComboBox<String> fQr = new JComboBox<>(new String[]{"QR_CODE", "CODE_128", "QR_CODE + CODE_128"});
        JLabel lSig  = new JLabel("Signature numérique :"); lSig.setFont(FONT_INFO_KEY); lSig.setForeground(TEXT_PRIMARY);
        JCheckBox fSig = new JCheckBox();

        form.add(lVal);  form.add(fVal);
        form.add(lType); form.add(fType);
        form.add(lEnt);  form.add(fEnt);
        form.add(lQr);   form.add(fQr);
        form.add(lSig);  form.add(fSig);

        controller.chargerConfigBons(settings -> {
            if (settings.containsKey("bon_validite_defaut") && !settings.get("bon_validite_defaut").isEmpty())
                fVal.setText(settings.get("bon_validite_defaut"));
            if (settings.containsKey("bon_type_defaut") && !settings.get("bon_type_defaut").isEmpty())
                fType.setSelectedItem(settings.get("bon_type_defaut"));
            if (settings.containsKey("bon_entreprise") && !settings.get("bon_entreprise").isEmpty())
                fEnt.setText(settings.get("bon_entreprise"));
            if (settings.containsKey("bon_format_qr") && !settings.get("bon_format_qr").isEmpty())
                fQr.setSelectedItem(settings.get("bon_format_qr"));
            if (settings.containsKey("bon_signature"))
                fSig.setSelected("true".equals(settings.get("bon_signature")));
        }, err -> System.err.println("Erreur chargement config bons: " + err));

        root.add(form, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        footer.setOpaque(false);

        JButton btnSave = UIUtils.buildRedButton("Enregistrer", 140, 36);
        btnSave.addActionListener(e -> {
            Map<String, String> settings = new LinkedHashMap<>();
            settings.put("bon_validite_defaut", fVal.getText().trim());
            settings.put("bon_type_defaut", (String) fType.getSelectedItem());
            settings.put("bon_entreprise", fEnt.getText().trim());
            settings.put("bon_format_qr", (String) fQr.getSelectedItem());
            settings.put("bon_signature", fSig.isSelected() ? "true" : "false");
            btnSave.setEnabled(false);
            controller.sauvegarderConfigBons(settings,
                () -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Configuration sauvegardée !", "Succès", JOptionPane.INFORMATION_MESSAGE);
                    dlg.dispose();
                },
                err -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Erreur : " + err, "Erreur", JOptionPane.ERROR_MESSAGE);
                });
        });

        JButton btnCancel = UIUtils.buildOutlineButton("Annuler", 120, 36);
        btnCancel.addActionListener(e -> dlg.dispose());

        footer.add(btnSave);
        footer.add(btnCancel);
        root.add(footer, BorderLayout.SOUTH);

        dlg.add(root);
        dlg.setVisible(true);
    }

    // ── RAPPORTS EXCEL ────────────────────────────────────────────────────────

    private void afficherConfigRapports() {
        JPanel p = new JPanel(new GridLayout(4, 1, 10, 10));
        JButton btnDemandes = new JButton("Exporter les Demandes (.xlsx)");
        JButton btnBons     = new JButton("Exporter les Bons Cadeaux (.xlsx)");
        JButton btnClients  = new JButton("Exporter la Liste Clients (.xlsx)");
        JButton btnBonsExp  = new JButton("⏰  Exporter Bons Proches d'Expiration (.xlsx)");

        btnDemandes.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("Export_Demandes.xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                controller.exporterDemandes(chooser.getSelectedFile().getAbsolutePath(),
                    () -> JOptionPane.showMessageDialog(this, "Export des demandes réussi !"),
                    err -> JOptionPane.showMessageDialog(this, "Erreur export : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        btnBons.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("Export_Bons.xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                controller.exporterBons(chooser.getSelectedFile().getAbsolutePath(),
                    () -> JOptionPane.showMessageDialog(this, "Export des bons réussi !"),
                    err -> JOptionPane.showMessageDialog(this, "Erreur export : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        btnClients.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("Export_Clients.xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                controller.exporterClients(chooser.getSelectedFile().getAbsolutePath(),
                    () -> JOptionPane.showMessageDialog(this, "Export des clients réussi !"),
                    err -> JOptionPane.showMessageDialog(this, "Erreur export : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        btnBonsExp.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setSelectedFile(new java.io.File("Export_Bons_Expiration.xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                controller.exporterBonsExpiration(chooser.getSelectedFile().getAbsolutePath(),
                    () -> JOptionPane.showMessageDialog(this, "Export réussi !"),
                    err -> JOptionPane.showMessageDialog(this, "Erreur export : " + err, "Erreur", JOptionPane.ERROR_MESSAGE));
            }
        });

        p.add(btnDemandes);
        p.add(btnBons);
        p.add(btnClients);
        p.add(btnBonsExp);

        JOptionPane.showMessageDialog(this, p, "Export de données Excel", JOptionPane.PLAIN_MESSAGE);
    }
}
