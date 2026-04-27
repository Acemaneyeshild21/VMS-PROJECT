package pkg.vms;

import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.DBconnect;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.Roles;
import pkg.vms.UIUtils;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class Dashboard extends JFrame {

    // ── Palette (Centralisée via VMSStyle) ──────────────────────────────────
    private static final Color BG_ROOT        = VMSStyle.BG_ROOT;
    private static final Color BG_SIDEBAR     = VMSStyle.BG_SIDEBAR;
    private static final Color BG_TOPBAR      = VMSStyle.BG_TOPBAR;
    private static final Color BG_CARD        = VMSStyle.BG_CARD;
    private static final Color BG_CARD_HOVER  = VMSStyle.BG_CARD_HOVER;
    private static final Color RED_PRIMARY    = VMSStyle.RED_PRIMARY;
    private static final Color RED_DARK       = VMSStyle.RED_DARK;
    private static final Color RED_LIGHT      = VMSStyle.RED_LIGHT;
    private static final Color BORDER_LIGHT   = VMSStyle.BORDER_LIGHT;
    private static final Color TEXT_PRIMARY   = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED     = VMSStyle.TEXT_MUTED;
    private static final Color ACCENT_BLUE    = VMSStyle.ACCENT_BLUE;
    private static final Color SUCCESS        = VMSStyle.SUCCESS;
    private static final Color WARNING        = VMSStyle.WARNING;
    private static final Color SHADOW_COLOR   = VMSStyle.SHADOW_COLOR;

    // ── Fonts (Centralisées via VMSStyle) ────────────────────────────────────
    private static final Font FONT_BRAND    = VMSStyle.FONT_BRAND;
    private static final Font FONT_SUBTITLE = VMSStyle.FONT_SUBTITLE;
    private static final Font FONT_NAV      = VMSStyle.FONT_NAV;
    private static final Font FONT_NAV_ACT  = VMSStyle.FONT_NAV_ACT;
    private static final Font FONT_CARD_TTL = VMSStyle.FONT_CARD_TTL;
    private static final Font FONT_CARD_DSC = VMSStyle.FONT_CARD_DSC;
    private static final Font FONT_BTN_MAIN = VMSStyle.FONT_BTN_MAIN;
    private static final Font FONT_BADGE    = VMSStyle.FONT_BADGE;
    private static final Font FONT_USER     = VMSStyle.FONT_USER;
    private static final Font FONT_KPI_VAL  = VMSStyle.FONT_KPI_VAL;
    private static final Font FONT_KPI_LBL  = VMSStyle.FONT_KPI_LBL;

    // ── State ────────────────────────────────────────────────────────────────
    private final JPanel  contentPanel;
    private final int     userId;
    private final String  username;
    private final String  role;
    private final String  email;
    private String        activePage = "Accueil";

    private final JButton[] navButtons = new JButton[7];
    private final String[]  navItems   = {"Accueil","Demandes","Clients","R\u00e9demption","Validation","Parametres"};
    private final String[]  navIcons   = {"\u2302","\uD83D\uDCCB","\u25C8","\uD83D\uDCF1","\u2713","\u2699"};

    private static final int RESIZE_MARGIN = 8;
    private Point     dragStart;
    private Rectangle dragBounds;
    private int       resizeDir = 0;

    // ── Session timeout (30 min d'inactivité = déconnexion auto) ────────────
    private static final int SESSION_TIMEOUT_MS = 30 * 60 * 1000;
    private Timer            sessionTimer;
    private AWTEventListener activityListener;

    // KPI panels — référencés pour mise à jour dynamique
    private JLabel kpiClientsVal;
    private JLabel kpiBonsVal;
    private JLabel kpiDemandesVal;
    private JLabel kpiTauxVal;
    private JLabel kpiClientsTrend;
    private JLabel kpiBonsTrend;
    private JLabel kpiDemandesTrend;

    // ── Constructor ──────────────────────────────────────────────────────────
    public Dashboard(int userId, String username, String role, String email) {
        this.userId   = userId;
        this.username = username;
        this.role     = role;
        this.email    = email;

        setTitle("Intermart \u2014 Gestion des Bons");
        setSize(1280, 820);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_ROOT);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(buildSidebar(), BorderLayout.WEST);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setOpaque(false);
        mainArea.add(buildTopBar(), BorderLayout.NORTH);

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        mainArea.add(contentPanel, BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "\u00a9 2025 Intermart Maurice \u2014 Tous droits r\u00e9serv\u00e9s  |  Connect\u00e9 : " + username,
                SwingConstants.CENTER);
        footer.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        footer.setForeground(TEXT_MUTED);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0,BORDER_LIGHT),
                BorderFactory.createEmptyBorder(8,0,10,0)));
        footer.setOpaque(true);
        footer.setBackground(BG_TOPBAR);
        mainArea.add(footer, BorderLayout.SOUTH);

        root.add(mainArea, BorderLayout.CENTER);
        add(root);
        installResizeHandler(root);
        switchPage("Accueil");
        demarrerSessionTimer();
    }

    // ── Session timeout ──────────────────────────────────────────────────────
    private void demarrerSessionTimer() {
        activityListener = event -> {
            if (sessionTimer != null && isShowing()) sessionTimer.restart();
        };
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
            AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        sessionTimer = new Timer(SESSION_TIMEOUT_MS, e -> sessionExpiree());
        sessionTimer.setRepeats(false);
        sessionTimer.start();
    }

    private void sessionExpiree() {
        sessionTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        AuditDAO.logSimple("utilisateur", userId, "DECONNEXION", userId,
            "Session expirée après 30 min d'inactivité");
        JOptionPane.showMessageDialog(this,
            "Votre session a expiré après 30 minutes d'inactivité.\nVous allez être redirigé vers la connexion.",
            "Session expirée", JOptionPane.WARNING_MESSAGE);
        deconnecter();
    }

    // ── Sidebar ──────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(BG_SIDEBAR); g2.fillRect(0,0,getWidth(),getHeight());
                g2.setColor(BORDER_LIGHT); g2.fillRect(getWidth()-1,0,1,getHeight());
                g2.setColor(RED_PRIMARY); g2.fillRect(0,0,getWidth(),3);
            }
        };
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setLayout(new BorderLayout());
        sidebar.setOpaque(false);

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(28,22,24,22));

        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false); logoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel cartIcon = new JLabel("\uD83D\uDED2");
        cartIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel logoText = new JLabel("INTERMART");
        logoText.setFont(FONT_BRAND); logoText.setForeground(RED_PRIMARY);
        logoRow.add(cartIcon); logoRow.add(logoText);

        JPanel redLine = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,RED_PRIMARY,getWidth(),0,new Color(210,35,45,0)));
                g2.fillRect(0,0,getWidth(),2);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(170,2); }
            @Override public Dimension getMaximumSize()   { return new Dimension(170,2); }
        };
        redLine.setOpaque(false); redLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Syst\u00e8me de Gestion \u2022 Maurice");
        tagline.setFont(FONT_SUBTITLE); tagline.setForeground(TEXT_MUTED);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(logoRow); brand.add(Box.createVerticalStrut(8));
        brand.add(redLine); brand.add(Box.createVerticalStrut(6)); brand.add(tagline);
        sidebar.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(8,14,8,14));

        JLabel navLabel = new JLabel("NAVIGATION");
        navLabel.setFont(new Font("Trebuchet MS", Font.BOLD, 9));
        navLabel.setForeground(TEXT_MUTED);
        navLabel.setBorder(BorderFactory.createEmptyBorder(0,8,10,0));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(navLabel);

        for (int i = 0; i < navItems.length; i++) {
            String page = navItems[i];

            // Restriction d'accès basée sur les 4 rôles officiels VMS (Roles.java)
            if ("Validation".equals(page)       && !Roles.peutValider(role))  continue;
            if ("Parametres".equals(page)       && !Roles.isAdmin(role))      continue;
            if ("Rédemption".equals(page)       && !Roles.peutRedimer(role))  continue;

            JButton btn = buildNavButton(page, navIcons[i]);
            btn.addActionListener(e -> switchPage(page));
            navButtons[i] = btn;
            nav.add(btn); nav.add(Box.createVerticalStrut(3));
        }
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(buildSidebarUserCard(), BorderLayout.SOUTH);
        return sidebar;
    }

    private JButton buildNavButton(String label, String icon) {
        JButton btn = new JButton() {
            boolean h = false;
            {
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
                setPreferredSize(new Dimension(202, 44));
                setHorizontalAlignment(SwingConstants.LEFT);
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = label.equals(activePage);
                if (active) {
                    g2.setColor(RED_LIGHT);
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                    g2.setColor(RED_PRIMARY);
                    g2.fill(new RoundRectangle2D.Double(0,8,3,getHeight()-16,3,3));
                } else if (h) {
                    g2.setColor(new Color(0,0,0,5));
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                }
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 15));
                g2.setColor(active ? RED_PRIMARY : (h ? TEXT_PRIMARY : TEXT_SECONDARY));
                g2.drawString(icon, 14, getHeight()/2+5);
                g2.setFont(active ? FONT_NAV_ACT : FONT_NAV);
                g2.setColor(active ? RED_PRIMARY : (h ? TEXT_PRIMARY : TEXT_SECONDARY));
                g2.drawString(label, 40, getHeight()/2+5);
                g2.dispose();
            }
        };
        return btn;
    }

    private JPanel buildSidebarUserCard() {
        JPanel card = new JPanel(new BorderLayout(10,0)) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_LIGHT); g.fillRect(0,0,getWidth(),1);
                g.setColor(new Color(254,248,248)); g.fillRect(0,1,getWidth(),getHeight());
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(14,16,18,16));

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(RED_PRIMARY); g2.fillOval(0,0,getWidth()-1,getHeight()-1);
                String init = username.length()>0 ? String.valueOf(username.charAt(0)).toUpperCase() : "U";
                g2.setFont(new Font("Georgia", Font.BOLD, 15));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init,(getWidth()-fm.stringWidth(init))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(38,38); }
            @Override public Dimension getMinimumSize()   { return getPreferredSize(); }
        };
        avatar.setOpaque(false);

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        JLabel nameL = new JLabel(username);
        nameL.setFont(FONT_USER); nameL.setForeground(TEXT_PRIMARY);
        JLabel roleL = new JLabel(role);
        roleL.setFont(new Font("Trebuchet MS", Font.PLAIN, 10));
        roleL.setForeground(getRoleColor(role));
        info.add(nameL); info.add(Box.createVerticalStrut(2)); info.add(roleL);

        JButton logout = new JButton("\u21A9") {
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 15));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setForeground(TEXT_MUTED); setToolTipText("D\u00e9connexion");
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { setForeground(RED_PRIMARY); }
                    public void mouseExited(MouseEvent e)  { setForeground(TEXT_MUTED); }
                });
                addActionListener(e -> confirmLogout());
            }
        };
        card.add(avatar, BorderLayout.WEST);
        card.add(info,   BorderLayout.CENTER);
        card.add(logout, BorderLayout.EAST);
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) afficherProfilUtilisateur();
            }
        });
        return card;
    }

    // ── TopBar ───────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_TOPBAR); g.fillRect(0,0,getWidth(),getHeight());
                g.setColor(BORDER_LIGHT); g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0,60));
        bar.setBorder(BorderFactory.createEmptyBorder(0,30,0,20));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel pageTitle  = new JLabel("Tableau de bord");
        pageTitle.setFont(new Font("Georgia", Font.BOLD, 17));
        pageTitle.setForeground(TEXT_PRIMARY);
        JLabel breadcrumb = new JLabel("Accueil  /  Tableau de bord");
        breadcrumb.setFont(new Font("Trebuchet MS", Font.PLAIN, 11));
        breadcrumb.setForeground(TEXT_MUTED);
        titleBlock.add(pageTitle); titleBlock.add(breadcrumb);
        bar.add(titleBlock, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));
        right.setOpaque(false);
        right.add(buildSearchBox());
        right.add(buildIconButton("\uD83D\uDD14"));
        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        sep.setPreferredSize(new Dimension(1,24)); sep.setForeground(BORDER_LIGHT);
        right.add(sep);
        right.add(buildWinCtrl("\u2014", new Color(240,190,60), e -> setState(ICONIFIED)));
        right.add(buildWinCtrl("\u25A1", new Color(50,180,100), e -> {
            if ((getExtendedState()&MAXIMIZED_BOTH)!=0) setExtendedState(NORMAL);
            else setExtendedState(MAXIMIZED_BOTH);
        }));
        right.add(buildWinCtrl("\u2715", RED_PRIMARY, e -> System.exit(0)));
        bar.add(right, BorderLayout.EAST);
        enableDragging(bar);
        return bar;
    }

    private JPanel buildSearchBox() {
        JPanel box = new JPanel(new BorderLayout(6,0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_ROOT);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),20,20));
                g2.setColor(BORDER_LIGHT); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,20,20));
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(210,34));
        box.setBorder(BorderFactory.createEmptyBorder(0,12,0,12));
        JLabel ico = new JLabel("\uD83D\uDD0D");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        JTextField field = new JTextField("Rechercher...");
        field.setOpaque(false); field.setBorder(null);
        field.setFont(FONT_CARD_DSC); field.setForeground(TEXT_MUTED);
        field.setCaretColor(RED_PRIMARY);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals("Rechercher...")) { field.setText(""); field.setForeground(TEXT_PRIMARY); }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) { field.setText("Rechercher..."); field.setForeground(TEXT_MUTED); }
            }
        });
        box.add(ico, BorderLayout.WEST); box.add(field, BorderLayout.CENTER);
        return box;
    }

    private JButton buildIconButton(String icon) {
        JButton btn = new JButton(icon) {
            boolean h = false;
            {
                setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(34,34));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true; repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0,0,0,6));
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildWinCtrl(String sym, Color col, ActionListener action) {
        JButton btn = new JButton(sym) {
            boolean h = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 11));
                setForeground(new Color(col.getRed(),col.getGreen(),col.getBlue(),120));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(26,26));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true; setForeground(col); repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; setForeground(new Color(col.getRed(),col.getGreen(),col.getBlue(),120)); repaint(); }
                });
                addActionListener(action);
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(col.getRed(),col.getGreen(),col.getBlue(),18));
                    g2.fill(new Ellipse2D.Double(1,1,getWidth()-2,getHeight()-2));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ── Switch Page ──────────────────────────────────────────────────────────
    private void switchPage(String page) {
        activePage = page;
        contentPanel.removeAll();
        for (JButton b : navButtons) { if (b != null) b.repaint(); }

        switch (page) {
            case "Accueil"         -> contentPanel.add(buildHomePage(),                    BorderLayout.CENTER);
            case "Clients"         -> contentPanel.add(new GestionClientsPanel(),           BorderLayout.CENTER);
            case "Demandes"        -> contentPanel.add(new GestionDemande(role, userId),    BorderLayout.CENTER);
            case "Bons"            -> contentPanel.add(new GestionBons(role, userId),       BorderLayout.CENTER);
            case "R\u00e9demption" -> contentPanel.add(new RedemptionPanel(userId, username, role), BorderLayout.CENTER);
            case "Validation"      -> contentPanel.add(new ValidationPanel(role, userId),   BorderLayout.CENTER);
            case "Statistiques"    -> contentPanel.add(new StatistiquesPanel(role, userId),   BorderLayout.CENTER);
            case "Parametres"      -> contentPanel.add(new ParametresPanel(role, userId),           BorderLayout.CENTER);
            case "Nouvelle Demande" -> contentPanel.add(new FormulaireCreationBon(userId, username), BorderLayout.CENTER);
            default -> {
                JPanel ph = new JPanel(new GridBagLayout());
                ph.setOpaque(false);
                JLabel lbl = new JLabel("Module \u00ab " + page + " \u00bb \u2014 Bient\u00f4t disponible");
                lbl.setFont(new Font("Georgia", Font.ITALIC, 22));
                lbl.setForeground(TEXT_MUTED);
                ph.add(lbl);
                contentPanel.add(ph, BorderLayout.CENTER);
            }
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void showPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Home Page ────────────────────────────────────────────────────────────
    private JPanel buildHomePage() {
        JPanel home = new JPanel(new BorderLayout(0, 22));
        home.setOpaque(false);
        home.add(buildCtaBanner(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 22));
        body.setOpaque(false);
        body.add(buildKpiStrip(),   BorderLayout.NORTH);
        body.add(buildModuleGrid(), BorderLayout.CENTER);
        home.add(body, BorderLayout.CENTER);

        // Charger les stats en arrière-plan
        chargerStatsAsync();
        return home;
    }

    // ── Stats depuis BD (async) ───────────────────────────────────────────────
    private void chargerStatsAsync() {
        new SwingWorker<VoucherDAO.VoucherStats, Void>() {
            @Override
            protected VoucherDAO.VoucherStats doInBackground() throws Exception {
                return VoucherDAO.getDashboardStats();
            }

            @Override
            protected void done() {
                try {
                    VoucherDAO.VoucherStats stats = get();
                    if (kpiClientsVal != null) kpiClientsVal.setText(String.valueOf(stats.activeClients));
                    if (kpiBonsVal != null) kpiBonsVal.setText(String.valueOf(stats.totalVouchers));
                    if (kpiDemandesVal != null) kpiDemandesVal.setText(String.valueOf(stats.pendingPayments));
                    if (kpiTauxVal != null) kpiTauxVal.setText(stats.validationRate + " %");
                    
                    if (kpiClientsTrend != null) kpiClientsTrend.setText(stats.activeClients + " client" + (stats.activeClients > 1 ? "s" : "") + " actif" + (stats.activeClients > 1 ? "s" : ""));
                    if (kpiBonsTrend != null) kpiBonsTrend.setText(stats.totalVouchers + " bon" + (stats.totalVouchers > 1 ? "s" : "") + " au total");
                    if (kpiDemandesTrend != null) kpiDemandesTrend.setText(stats.pendingPayments + " en attente paiement");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private JPanel buildCtaBanner() {
        JPanel banner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),12,12));
                g2.setColor(RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0,0,5,getHeight(),3,3));
                GradientPaint gp = new GradientPaint(0,0,new Color(210,35,45,14),getWidth()/3f,0,new Color(210,35,45,0));
                g2.setPaint(gp);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),12,12));
                g2.setColor(BORDER_LIGHT); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,12,12));
            }
        };
        banner.setOpaque(false);
        banner.setLayout(new BorderLayout(20,0));
        banner.setBorder(BorderFactory.createEmptyBorder(20,26,20,26));
        banner.setPreferredSize(new Dimension(0,84));

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Cr\u00e9er un Bon d'Achat");
        heading.setFont(new Font("Georgia", Font.BOLD, 17));
        heading.setForeground(TEXT_PRIMARY);
        JLabel subh = new JLabel("\u00c9mettre un nouveau bon et l'associer \u00e0 un client \u2014 Intermart Maurice");
        subh.setFont(FONT_CARD_DSC); subh.setForeground(TEXT_SECONDARY);
        textBlock.add(heading); textBlock.add(Box.createVerticalStrut(4)); textBlock.add(subh);

        JButton cta = UIUtils.buildRedButton("+ Nouveau Bon d'Achat", 218, 42);
        cta.addActionListener(e -> ouvrirFormulaireCreationBon());
        JPanel ctaWrap = new JPanel(new GridBagLayout());
        ctaWrap.setOpaque(false); ctaWrap.add(cta);

        banner.add(textBlock, BorderLayout.CENTER);
        banner.add(ctaWrap,   BorderLayout.EAST);
        return banner;
    }

    // ── KPI Strip ─────────────────────────────────────────────────────────────
    // Valeurs initiales = "..." → remplacées par chargerStatsAsync()
    private JPanel buildKpiStrip() {
        JPanel strip = new JPanel(new GridLayout(1,4,14,0));
        strip.setOpaque(false);
        strip.setPreferredSize(new Dimension(0,100));

        // Clients
        JPanel kpiClients = buildKpiPanel(
                ACCENT_BLUE, "\uD83D\uDC64", "Clients", "Clients",
                lv -> kpiClientsVal   = lv,
                lt -> kpiClientsTrend = lt
        );
        // Bons total
        JPanel kpiBons = buildKpiPanel(
                RED_PRIMARY, "\uD83C\uDF81", "Bons \u00e9mis", "Demandes",
                lv -> kpiBonsVal   = lv,
                lt -> kpiBonsTrend = lt
        );
        // En attente
        JPanel kpiDemandes = buildKpiPanel(
                WARNING, "\u23F3", "En attente", "Demandes",
                lv -> kpiDemandesVal   = lv,
                lt -> kpiDemandesTrend = lt
        );
        // Taux validation
        JPanel kpiTaux = buildKpiPanel(
                SUCCESS, "\u2713", "Taux valid.", "Demandes",
                lv -> kpiTauxVal = lv,
                lt -> {}
        );

        strip.add(kpiClients);
        strip.add(kpiBons);
        strip.add(kpiDemandes);
        strip.add(kpiTaux);
        return strip;
    }

    @FunctionalInterface interface LabelConsumer { void accept(JLabel l); }

    private JPanel buildKpiPanel(Color accent, String icon, String label, String targetPage,
                                 LabelConsumer valRef, LabelConsumer trendRef) {
        // valeur dynamique
        JLabel valL = new JLabel("...");
        valL.setFont(FONT_KPI_VAL); valL.setForeground(accent);
        valRef.accept(valL);

        JLabel lblL = new JLabel(label);
        lblL.setFont(FONT_KPI_LBL); lblL.setForeground(TEXT_SECONDARY);

        JLabel trendL = new JLabel("Chargement...");
        trendL.setFont(new Font("Trebuchet MS", Font.PLAIN, 10));
        trendL.setForeground(TEXT_MUTED);
        trendRef.accept(trendL);

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(0,0,0,8));
        left.add(valL); left.add(Box.createVerticalStrut(2));
        left.add(lblL); left.add(Box.createVerticalStrut(4)); left.add(trendL);

        JPanel iconBubble = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),18));
                g2.fillOval(0,0,getWidth()-1,getHeight()-1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(44,44); }
        };
        iconBubble.setOpaque(false);
        JLabel iconL = new JLabel(icon);
        iconL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        iconBubble.add(iconL);

        JPanel kpi = new JPanel() {
            boolean h = false;
            {
                setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                    public void mouseClicked(MouseEvent e) { switchPage(targetPage); }
                });
                setLayout(new BorderLayout());
                setBorder(BorderFactory.createEmptyBorder(16,18,16,18));
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW_COLOR);
                g2.fill(new RoundRectangle2D.Double(2,3,getWidth()-4,getHeight()-2,12,12));
                g2.setColor(h ? BG_CARD_HOVER : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-2,getHeight()-2,12,12));
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-2,3,3,3));
                g2.setColor(h ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),50) : BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0,0,getWidth()-2,getHeight()-2,12,12));
            }
        };
        kpi.add(left,       BorderLayout.CENTER);
        kpi.add(iconBubble, BorderLayout.EAST);
        return kpi;
    }

    // ── Module Grid ──────────────────────────────────────────────────────────
    private JPanel buildModuleGrid() {
        JPanel grid = new JPanel(new GridLayout(2,3,14,14));
        grid.setOpaque(false);

        // targetPage = null → bientôt disponible
        Object[][] mods = {
                {"Gestion des Bons", "\uD83C\uDF81", RED_PRIMARY,             "\u00c9mettre, modifier, archiver",          "Bons"},
                {"Demandes",         "\uD83D\uDCCB", ACCENT_BLUE,             "File de traitement en cours",               "Demandes"},
                {"Clients",          "\uD83D\uDC64", SUCCESS,                 "Fiches, historiques & fid\u00e9lit\u00e9",   "Clients"},
                {"Paiements",        "\uD83D\uDCB3", WARNING,                 "Suivi & rapprochement bancaire",             "Validation"},
                {"Validation",       "\u2713",        new Color(20,170,190),  "Approbation des demandes",                   "Validation"},
                {"Statistiques",     "\uD83D\uDCC8", new Color(150,80,210),   "Rapports & analyses",                        "Statistiques"},
        };
        for (Object[] m : mods)
            grid.add(buildModuleCard((String)m[0],(String)m[1],(Color)m[2],(String)m[3],(String)m[4]));
        return grid;
    }

    private JPanel buildModuleCard(String title, String icon, Color accent, String desc, String targetPage) {
        JPanel card = new JPanel() {
            boolean h = false;
            {
                setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(BorderFactory.createEmptyBorder(22,20,22,20));
                setLayout(new BorderLayout());
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                    public void mouseClicked(MouseEvent e) {
                        if (targetPage != null) switchPage(targetPage);
                        else JOptionPane.showMessageDialog(Dashboard.this,
                                "<html><b>" + title + "</b><br><small>" + desc + "</small><br><br>" +
                                        "<i>Module en d\u00e9veloppement</i></html>",
                                "Intermart", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(SHADOW_COLOR);
                g2.fill(new RoundRectangle2D.Double(2,3,getWidth()-4,getHeight()-2,14,14));
                g2.setColor(h ? BG_CARD_HOVER : BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-2,getHeight()-2,14,14));
                g2.setColor(h ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),60) : BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0,0,getWidth()-2,getHeight()-2,14,14));
                if (h) {
                    GradientPaint gp = new GradientPaint(0,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),0),
                            getWidth()/2f,0,new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),20));
                    g2.setPaint(gp);
                    g2.fill(new RoundRectangle2D.Double(0,0,getWidth()-2,getHeight()-2,14,14));
                }
            }
        };
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JPanel bubble = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),20));
                g2.fillOval(0,0,getWidth()-1,getHeight()-1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(50,50); }
            @Override public Dimension getMinimumSize()   { return getPreferredSize(); }
            @Override public Dimension getMaximumSize()   { return getPreferredSize(); }
        };
        bubble.setOpaque(false);
        JLabel iconL = new JLabel(icon);
        iconL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        bubble.add(iconL); bubble.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleL = new JLabel(title);
        titleL.setFont(FONT_CARD_TTL); titleL.setForeground(TEXT_PRIMARY);
        titleL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descL = new JLabel(desc);
        descL.setFont(FONT_CARD_DSC); descL.setForeground(TEXT_SECONDARY);
        descL.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel tag = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
        tag.setOpaque(false); tag.setAlignmentX(Component.LEFT_ALIGNMENT);
        String tagText  = targetPage != null ? "\u25CF  ACTIF" : "\u25CB  BIENT\u00d4T";
        Color  tagColor = targetPage != null
                ? new Color(accent.getRed(),accent.getGreen(),accent.getBlue(),200) : TEXT_MUTED;
        JLabel dot = new JLabel(tagText);
        dot.setFont(new Font("Trebuchet MS", Font.BOLD, 9)); dot.setForeground(tagColor);
        tag.add(dot);

        inner.add(bubble); inner.add(Box.createVerticalStrut(12));
        inner.add(titleL); inner.add(Box.createVerticalStrut(5));
        inner.add(descL);  inner.add(Box.createVerticalStrut(10)); inner.add(tag);
        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    // ── Profile Dialog ────────────────────────────────────────────────────────
    private void afficherProfilUtilisateur() {
        JDialog dlg = new JDialog(this, "Profil", true);
        dlg.setSize(400,390); dlg.setLocationRelativeTo(this); dlg.setUndecorated(true);
        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(BG_ROOT); g.fillRect(0,0,getWidth(),getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT,1));
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0,0,RED_PRIMARY,getWidth(),getHeight(),RED_DARK));
                g2.fillRect(0,0,getWidth(),getHeight());
            }
        };
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setPreferredSize(new Dimension(0,136));
        header.setBorder(BorderFactory.createEmptyBorder(22,0,18,0));
        JPanel av = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE); g2.fillOval(0,0,getWidth()-1,getHeight()-1);
                String init = username.length()>0?String.valueOf(username.charAt(0)).toUpperCase():"U";
                g2.setFont(new Font("Georgia",Font.BOLD,28)); g2.setColor(RED_PRIMARY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(init,(getWidth()-fm.stringWidth(init))/2,(getHeight()+fm.getAscent()-fm.getDescent())/2);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(68,68); }
            @Override public Dimension getMinimumSize()   { return getPreferredSize(); }
            @Override public Dimension getMaximumSize()   { return getPreferredSize(); }
        };
        av.setOpaque(false); av.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel nameL = new JLabel(username);
        nameL.setFont(new Font("Georgia",Font.BOLD,18)); nameL.setForeground(Color.WHITE);
        nameL.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel roleL = new JLabel(role);
        roleL.setFont(FONT_BADGE); roleL.setForeground(new Color(255,255,255,200));
        roleL.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(av); header.add(Box.createVerticalStrut(8));
        header.add(nameL); header.add(Box.createVerticalStrut(3)); header.add(roleL);

        JPanel info = new JPanel();
        info.setOpaque(true); info.setBackground(BG_CARD);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setBorder(BorderFactory.createEmptyBorder(20,28,20,28));
        info.add(createInfoRow("\uD83C\uDD94","Identifiant","#"+userId));
        info.add(Box.createVerticalStrut(12));
        info.add(createInfoRow("\uD83D\uDCE7","Adresse e-mail",email));
        info.add(Box.createVerticalStrut(12));
        info.add(createInfoRow("\uD83C\uDFAD","Niveau d'acc\u00e8s",role));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.CENTER,12,14));
        btns.setBackground(BG_ROOT);
        JButton btnLogout = UIUtils.buildRedButton("D\u00e9connexion",140,38);
        btnLogout.addActionListener(e -> { dlg.dispose(); confirmLogout(); });
        JButton btnClose = UIUtils.buildOutlineButton("Fermer",140,38);
        btnClose.addActionListener(e -> dlg.dispose());
        btns.add(btnLogout); btns.add(btnClose);

        root.add(header,BorderLayout.NORTH);
        root.add(info,  BorderLayout.CENTER);
        root.add(btns,  BorderLayout.SOUTH);
        dlg.add(root); dlg.setVisible(true);
    }

    private JPanel createInfoRow(String icon, String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10,0)); row.setOpaque(false);
        JLabel ico = new JLabel(icon);
        ico.setFont(new Font("Segoe UI Emoji",Font.PLAIN,17));
        JPanel texts = new JPanel(); texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Trebuchet MS",Font.PLAIN,10)); lbl.setForeground(TEXT_MUTED);
        JLabel val = new JLabel(value);
        val.setFont(new Font("Trebuchet MS",Font.BOLD,13)); val.setForeground(TEXT_PRIMARY);
        texts.add(lbl); texts.add(val);
        row.add(ico,BorderLayout.WEST); row.add(texts,BorderLayout.CENTER);
        JPanel sep = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_LIGHT); g.fillRect(0,getHeight()-1,getWidth(),1);
            }
        };
        sep.setOpaque(false); sep.setPreferredSize(new Dimension(0,38));
        sep.add(row,BorderLayout.CENTER);
        return sep;
    }

    // ── Buttons ──────────────────────────────────────────────────────────────

    // ── Resize ────────────────────────────────────────────────────────────────
    private void installResizeHandler(JPanel root) {
        root.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                resizeDir = getResizeDir(e.getPoint(),root);
                if (resizeDir!=0) { dragStart=e.getLocationOnScreen(); dragBounds=getBounds(); }
            }
            @Override public void mouseReleased(MouseEvent e) { resizeDir=0; }
        });
        root.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int dir = getResizeDir(e.getPoint(),root);
                root.setCursor(switch(dir){
                    case 1,9->Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR);
                    case 3,7->Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR);
                    case 2->Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
                    case 8->Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
                    case 4->Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR);
                    case 6->Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
                    default->Cursor.getDefaultCursor();
                });
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (resizeDir==0||dragStart==null) return;
                Point now = e.getLocationOnScreen();
                int dx=now.x-dragStart.x, dy=now.y-dragStart.y;
                int nx=dragBounds.x,ny=dragBounds.y,nw=dragBounds.width,nh=dragBounds.height;
                if(resizeDir==1||resizeDir==4||resizeDir==7){nx=dragBounds.x+dx;nw=dragBounds.width-dx;}
                if(resizeDir==3||resizeDir==6||resizeDir==9){nw=dragBounds.width+dx;}
                if(resizeDir==1||resizeDir==2||resizeDir==3){ny=dragBounds.y+dy;nh=dragBounds.height-dy;}
                if(resizeDir==7||resizeDir==8||resizeDir==9){nh=dragBounds.height+dy;}
                Dimension min=getMinimumSize();
                if(nw<min.width){if(nx!=dragBounds.x)nx=dragBounds.x+dragBounds.width-min.width;nw=min.width;}
                if(nh<min.height){if(ny!=dragBounds.y)ny=dragBounds.y+dragBounds.height-min.height;nh=min.height;}
                setBounds(nx,ny,nw,nh);
            }
        });
    }

    private int getResizeDir(Point p, JPanel root) {
        int m=RESIZE_MARGIN,w=root.getWidth(),h=root.getHeight();
        boolean L=p.x<m,R=p.x>w-m,T=p.y<m,B=p.y>h-m;
        if(T&&L)return 1;if(T&&R)return 3;if(B&&L)return 7;if(B&&R)return 9;
        if(T)return 2;if(B)return 8;if(L)return 4;if(R)return 6;return 0;
    }

    private void enableDragging(JPanel panel) {
        Point offset = new Point();
        panel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { offset.setLocation(e.getPoint()); }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                if (resizeDir!=0) return;
                Point p = e.getLocationOnScreen();
                setLocation(p.x-offset.x, p.y-offset.y);
            }
        });
    }

    // ── Logout ────────────────────────────────────────────────────────────────
    private void confirmLogout() {
        int c = JOptionPane.showConfirmDialog(this,
                "Confirmer la d\u00e9connexion de " + username + " ?",
                "D\u00e9connexion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (c==JOptionPane.YES_OPTION) deconnecter();
    }

    private void deconnecter() {
        if (sessionTimer != null)    sessionTimer.stop();
        if (activityListener != null)
            Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        this.dispose();
        SwingUtilities.invokeLater(() -> new LoginForm().setVisible(true));
    }

    private Color getRoleColor(String r) {
        return switch (r.toLowerCase()) {
            case "administrateur" -> RED_PRIMARY;
            case "manager"        -> WARNING;
            default               -> ACCENT_BLUE;
        };
    }

    private void ouvrirFormulaireCreationBon() {
        switchPage("Nouvelle Demande");
    }
}