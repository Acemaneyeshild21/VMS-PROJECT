package pkg.vms;

import pkg.vms.DAO.DBconnect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

class FormulaireCreationBon extends JDialog {

    private static final Color BG_ROOT      = new Color(245, 246, 250);
    private static final Color BG_CARD      = new Color(255, 255, 255);
    private static final Color BG_INPUT     = new Color(250, 251, 253);
    private static final Color BG_INPUT_FOC = new Color(255, 255, 255);
    private static final Color RED_PRIMARY  = new Color(210,  35,  45);
    private static final Color RED_DARK     = new Color(170,  20,  28);
    private static final Color BORDER_LIGHT = new Color(228, 230, 236);
    private static final Color TEXT_PRIMARY = new Color( 22,  28,  45);
    private static final Color TEXT_LABEL   = new Color( 55,  65,  85);
    private static final Color TEXT_MUTED   = new Color(160, 168, 185);
    private static final Color TEXT_HINT    = new Color(190, 195, 210);

    private static final Font FONT_HEADER  = new Font("Georgia",      Font.BOLD,   22);
    private static final Font FONT_SUB     = new Font("Georgia",      Font.ITALIC, 13);
    private static final Font FONT_LABEL   = new Font("Trebuchet MS", Font.BOLD,   12);
    private static final Font FONT_INPUT   = new Font("Trebuchet MS", Font.PLAIN,  13);
    private static final Font FONT_BTN     = new Font("Trebuchet MS", Font.BOLD,   14);
    private static final Font FONT_SECTION = new Font("Trebuchet MS", Font.BOLD,    9);

    private final int    userId;
    private final String role;

    private JComboBox<Object>  cboClient;
    private JSpinner           spnNombreBons;
    private JTextField         txtValeurUnitaire;
    private JTextArea          txtDescription;
    private JComboBox<String>  cboMagasin;
    private JComboBox<String>  cboJour;
    private JComboBox<String>  cboMoisVal;
    private JComboBox<String>  cboAnnee;
    private JTextField         txtEmailDestinataire;

    public FormulaireCreationBon(JFrame parent, int userId, String username, String role) {
        super(parent, "Nouvelle Demande de Bons Cadeau", true);
        this.userId = userId;
        this.role   = role;
        setSize(660, 720);
        setMinimumSize(new Dimension(560, 600));
        setLocationRelativeTo(parent);
        setUndecorated(true);
        initComponents();
    }

    private void initComponents() {
        // IMPORTANT: tous les champs initialisés ICI avant buildForm()
        txtValeurUnitaire    = buildTextField("ex: 500");
        txtEmailDestinataire = buildTextField("ex: client@entreprise.mu");
        cboClient            = new JComboBox<>();
        cboMagasin           = buildComboBox();

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_ROOT);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildScroll(),  BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);
        add(root);
    }

    private JScrollPane buildScroll() {
        JPanel centerWrap = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_ROOT);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        centerWrap.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 36, 0, 36);
        centerWrap.add(buildForm(), gbc);
        GridBagConstraints gbcBot = new GridBagConstraints();
        gbcBot.gridx = 0; gbcBot.gridy = 1;
        gbcBot.weighty = 1.0; gbcBot.fill = GridBagConstraints.VERTICAL;
        centerWrap.add(Box.createVerticalGlue(), gbcBot);
        JScrollPane scroll = new JScrollPane(centerWrap);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scroll;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, RED_PRIMARY, getWidth(), getHeight(), RED_DARK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 8));
                g2.setStroke(new BasicStroke(18f));
                for (int x = -getHeight(); x < getWidth() + getHeight(); x += 40)
                    g2.drawLine(x, 0, x + getHeight(), getHeight());
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRect(0, getHeight()-1, getWidth(), 1);
            }
        };
        header.setOpaque(false);
        header.setPreferredSize(new Dimension(0, 90));
        header.setLayout(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 20));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleRow.setOpaque(false);
        JLabel giftIcon = new JLabel("\uD83C\uDF81");
        giftIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
        JLabel title = new JLabel("Nouvelle Demande de Bons");
        title.setFont(FONT_HEADER);
        title.setForeground(Color.WHITE);
        titleRow.add(giftIcon);
        titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Remplissez les informations ci-dessous pour cr\u00e9er la demande");
        subtitle.setFont(FONT_SUB);
        subtitle.setForeground(new Color(255, 255, 255, 185));
        subtitle.setBorder(BorderFactory.createEmptyBorder(3, 2, 0, 0));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(titleRow);
        left.add(subtitle);

        JButton btnClose = new JButton("\u2715") {
            boolean h = false;
            {
                setFont(new Font("Trebuchet MS", Font.BOLD, 14));
                setForeground(new Color(255, 255, 255, 160));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(32, 32));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  setForeground(Color.WHITE); repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; setForeground(new Color(255,255,255,160)); repaint(); }
                });
                addActionListener(e -> dispose());
            }
            @Override protected void paintComponent(Graphics g) {
                if (h) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0,0,0,30));
                    g2.fill(new Ellipse2D.Double(2,2,getWidth()-4,getHeight()-4));
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        header.add(left,     BorderLayout.CENTER);
        header.add(btnClose, BorderLayout.EAST);
        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel() {
            @Override public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                return new Dimension(Math.max(d.width, 500), d.height);
            }
        };
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(24, 0, 16, 0));

        // BENEFICIAIRE
        addRow(form, buildSectionLabel("B\u00c9N\u00c9FICIAIRE"), 0, 8);
        addRow(form, buildFieldLabel("\uD83D\uDC64  Client"), 0, 4);
        chargerClients();
        addRow(form, cboClient, 0, 12);

        addRow(form, buildFieldLabel("\uD83D\uDCE7  Email Destinataire"), 0, 4);
        // NE PAS réinitialiser ici — utilise l'instance créée dans initComponents()
        addRow(form, txtEmailDestinataire, 0, 14);

        addRow(form, buildDivider(), 0, 18);

        // BON D'ACHAT
        addRow(form, buildSectionLabel("BON D'ACHAT"), 0, 8);
        addRow(form, buildFieldLabel("\uD83C\uDFAB  Nombre de Bons"), 0, 4);
        spnNombreBons = buildSpinner(1, 1, 10000, 1);
        JPanel wNombre = wrapSpinner(spnNombreBons);
        wNombre.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        addRow(form, wNombre, 0, 12);

        addRow(form, buildFieldLabel("Date d'expiration"), 0, 4);
        cboJour    = new JComboBox<>();
        cboMoisVal = new JComboBox<>();
        cboAnnee   = new JComboBox<>();
        for (int d = 1; d <= 31; d++) cboJour.addItem(String.format("%02d", d));
        String[] moisNoms = {"Janvier","Fevrier","Mars","Avril","Mai","Juin",
                "Juillet","Aout","Septembre","Octobre","Novembre","Decembre"};
        for (String m : moisNoms) cboMoisVal.addItem(m);
        int anneeBase = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        for (int a = anneeBase; a <= anneeBase + 5; a++) cboAnnee.addItem(String.valueOf(a));
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.add(java.util.Calendar.DAY_OF_MONTH, 30);
        cboJour.setSelectedItem(String.format("%02d", cal.get(java.util.Calendar.DAY_OF_MONTH)));
        cboMoisVal.setSelectedIndex(cal.get(java.util.Calendar.MONTH));
        cboAnnee.setSelectedItem(String.valueOf(cal.get(java.util.Calendar.YEAR)));
        for (JComboBox<?> cb : new JComboBox[]{cboJour, cboMoisVal, cboAnnee}) {
            cb.setFont(FONT_INPUT);
            cb.setBackground(BG_INPUT);
            cb.setForeground(TEXT_PRIMARY);
            cb.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        }
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        dateRow.setOpaque(false);
        dateRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        cboJour.setPreferredSize(new Dimension(62, 38));
        cboMoisVal.setPreferredSize(new Dimension(118, 38));
        cboAnnee.setPreferredSize(new Dimension(80, 38));
        JLabel sep1 = new JLabel("/"); sep1.setFont(FONT_INPUT); sep1.setForeground(TEXT_MUTED);
        JLabel sep2 = new JLabel("/"); sep2.setFont(FONT_INPUT); sep2.setForeground(TEXT_MUTED);
        dateRow.add(cboJour); dateRow.add(sep1);
        dateRow.add(cboMoisVal); dateRow.add(sep2);
        dateRow.add(cboAnnee);
        addRow(form, dateRow, 0, 14);

        addRow(form, buildDivider(), 0, 18);

        // POINT DE VENTE
        addRow(form, buildSectionLabel("POINT DE VENTE"), 0, 8);
        addRow(form, buildFieldLabel("\uD83C\uDFEA  Magasin D\u00e9sign\u00e9  (optionnel)"), 0, 4);
        chargerMagasins();
        addRow(form, cboMagasin, 0, 14);

        addRow(form, buildDivider(), 0, 18);

        // NOTES
        addRow(form, buildSectionLabel("NOTES"), 0, 8);
        addRow(form, buildFieldLabel("\uD83D\uDCDD  Description / Remarques (optionnel)"), 0, 4);
        txtDescription = new JTextArea(4, 40);
        txtDescription.setFont(FONT_INPUT);
        txtDescription.setForeground(TEXT_PRIMARY);
        txtDescription.setBackground(BG_INPUT);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        txtDescription.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { txtDescription.setBackground(BG_INPUT_FOC); }
            public void focusLost(FocusEvent e)   { txtDescription.setBackground(BG_INPUT); }
        });
        JScrollPane scrollDesc = new JScrollPane(txtDescription);
        scrollDesc.setBorder(BorderFactory.createLineBorder(BORDER_LIGHT, 1));
        scrollDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollDesc.setPreferredSize(new Dimension(10, 92));
        scrollDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));
        addRow(form, scrollDesc, 0, 8);

        return form;
    }

    private void addRow(JPanel form, JComponent comp, int top, int bot) {
        if (top > 0) form.add(Box.createVerticalStrut(top));
        comp.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!(comp instanceof JScrollPane) && !(comp instanceof JPanel)) {
            Dimension p = comp.getPreferredSize();
            comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.height > 0 ? p.height : 38));
        }
        form.add(comp);
        if (bot > 0) form.add(Box.createVerticalStrut(bot));
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BG_CARD);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(BORDER_LIGHT);
                g.fillRect(0, 0, getWidth(), 1);
            }
        };
        footer.setOpaque(false);
        footer.setLayout(new BorderLayout(20, 0));
        footer.setBorder(BorderFactory.createEmptyBorder(12, 28, 16, 28));

        JPanel valeurPanel = new JPanel();
        valeurPanel.setOpaque(false);
        valeurPanel.setLayout(new BoxLayout(valeurPanel, BoxLayout.Y_AXIS));
        JLabel valeurLabel = new JLabel("\uD83D\uDCB0  Prix ( Montant en Rs)");
        valeurLabel.setFont(FONT_LABEL);
        valeurLabel.setForeground(TEXT_LABEL);
        valeurLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtValeurUnitaire.setPreferredSize(new Dimension(180, 38));
        txtValeurUnitaire.setMaximumSize(new Dimension(220, 38));
        txtValeurUnitaire.setMinimumSize(new Dimension(120, 38));
        txtValeurUnitaire.setAlignmentX(Component.LEFT_ALIGNMENT);
        valeurPanel.add(valeurLabel);
        valeurPanel.add(Box.createVerticalStrut(4));
        valeurPanel.add(txtValeurUnitaire);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btnPanel.setOpaque(false);
        JButton btnAnnuler = buildGhostButton("Annuler");
        btnAnnuler.addActionListener(e -> dispose());
        JButton btnCreer = buildPrimaryButton("\u2713  Cr\u00e9er la Demande");
        btnCreer.addActionListener(e -> creerDemande());
        btnPanel.add(btnAnnuler);
        btnPanel.add(btnCreer);

        footer.add(valeurPanel, BorderLayout.WEST);
        footer.add(btnPanel,    BorderLayout.EAST);
        return footer;
    }

    // ── FIELD BUILDERS ──────────────────────────────────────────────────────
    private JLabel buildSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(RED_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JLabel buildFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField buildTextField(String hint) {
        JTextField field = new JTextField() {
            {
                setFont(FONT_INPUT);
                setForeground(TEXT_PRIMARY);
                setBackground(BG_INPUT);
                setCaretColor(RED_PRIMARY);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                        BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                setAlignmentX(Component.LEFT_ALIGNMENT);
                putClientProperty("hint", hint);
                addFocusListener(new FocusAdapter() {
                    public void focusGained(FocusEvent e) {
                        setBackground(BG_INPUT_FOC);
                        setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(new Color(210,35,45,100), 1),
                                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                    }
                    public void focusLost(FocusEvent e) {
                        setBackground(BG_INPUT);
                        setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
                    }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !isFocusOwner()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setFont(FONT_INPUT);
                    g2.setColor(TEXT_HINT);
                    Insets ins = getInsets();
                    g2.drawString((String) getClientProperty("hint"),
                            ins.left,
                            getHeight() - ins.bottom - g2.getFontMetrics().getDescent() - 2);
                }
            }
        };
        return field;
    }

    private JComboBox<String> buildComboBox() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(FONT_INPUT);
        combo.setBackground(BG_INPUT);
        combo.setForeground(TEXT_PRIMARY);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_LIGHT, 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        return combo;
    }

    private JSpinner buildSpinner(int val, int min, int max, int step) {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(val, min, max, step));
        sp.setFont(FONT_INPUT);
        JSpinner.DefaultEditor ed = (JSpinner.DefaultEditor) sp.getEditor();
        ed.getTextField().setFont(FONT_INPUT);
        ed.getTextField().setBackground(BG_INPUT);
        ed.getTextField().setForeground(TEXT_PRIMARY);
        ed.getTextField().setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        return sp;
    }

    private JPanel wrapSpinner(JSpinner sp) {
        JPanel wrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
                g2.setColor(BORDER_LIGHT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 6, 6);
            }
        };
        wrap.setOpaque(false);
        wrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        sp.setBorder(null);
        sp.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildDivider() {
        JPanel div = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(BORDER_LIGHT);
                g.fillRect(0, 0, getWidth(), 1);
            }
            @Override public Dimension getPreferredSize() { return new Dimension(Integer.MAX_VALUE, 1); }
            @Override public Dimension getMaximumSize()   { return new Dimension(Integer.MAX_VALUE, 1); }
        };
        div.setOpaque(false);
        div.setAlignmentX(Component.LEFT_ALIGNMENT);
        return div;
    }

    // ── BUTTONS ─────────────────────────────────────────────────────────────
    private JButton buildPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN); setForeground(Color.WHITE);
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(210, 42));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? RED_DARK : RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    private JButton buildGhostButton(String text) {
        JButton btn = new JButton(text) {
            boolean h = false;
            {
                setFont(FONT_BTN); setForeground(new Color(90, 100, 120));
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(120, 42));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  setForeground(TEXT_PRIMARY); repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; setForeground(new Color(90,100,120)); repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? new Color(0,0,0,8) : new Color(0,0,0,0));
                g2.fill(new RoundRectangle2D.Double(0,0,getWidth(),getHeight(),8,8));
                g2.setColor(BORDER_LIGHT); g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0,0,getWidth()-1,getHeight()-1,8,8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        return btn;
    }

    // ── DB LOADERS ──────────────────────────────────────────────────────────
    private void chargerClients() {
        cboClient.addItem(null);
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT client_id, name, email, contactnumber, company, date_creation, actif " +
                             "FROM client WHERE actif = true ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cboClient.addItem(new Client(
                        rs.getInt("client_id"), rs.getString("name"),
                        rs.getString("email"), rs.getString("contactnumber"),
                        rs.getString("company"), rs.getTimestamp("date_creation"),
                        rs.getBoolean("actif")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement clients : " + e.getMessage());
        }
        cboClient.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Client) {
                    setText(((Client) value).getName());
                    setForeground(isSelected ? Color.WHITE : TEXT_PRIMARY);
                } else {
                    setText("-- S\u00e9lectionnez un client --");
                    setForeground(TEXT_MUTED);
                }
                setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
                setFont(new Font("Trebuchet MS", Font.PLAIN, 13));
                return this;
            }
        });
    }

    private void chargerMagasins() {
        cboMagasin.addItem("-- S\u00e9lectionnez un magasin --");
        try (Connection conn = DBconnect.getConnection();
             Statement  stmt = conn.createStatement();
             ResultSet  rs   = stmt.executeQuery(
                     "SELECT magasin_id, nom_magasin FROM magazin ORDER BY nom_magasin")) {
            while (rs.next())
                cboMagasin.addItem(rs.getInt("magasin_id") + " - " + rs.getString("nom_magasin"));
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur chargement magasins : " + e.getMessage());
        }
    }

    // ── CREER DEMANDE ────────────────────────────────────────────────────────
    private void creerDemande() {
        // Validations
        if (!(cboClient.getSelectedItem() instanceof Client)) {
            showWarning("Veuillez s\u00e9lectionner un client."); return;
        }
        if (txtValeurUnitaire.getText().trim().isEmpty()) {
            showWarning("Veuillez entrer la valeur unitaire."); return;
        }
        if (txtEmailDestinataire.getText().trim().isEmpty()) {
            showWarning("Veuillez entrer l'email du destinataire."); return;
        }

        try {
            Client  selectedClient = (Client) cboClient.getSelectedItem();
            int     clientId       = selectedClient.getClientId();
            int     nombreBons     = (int) spnNombreBons.getValue();
            double  valeurUnitaire = Double.parseDouble(txtValeurUnitaire.getText().trim());
            double  montantTotal   = nombreBons * valeurUnitaire;
            // *** FIX : email du destinataire lu correctement ***
            String  email          = txtEmailDestinataire.getText().trim();

            String  magasinStr = (String) cboMagasin.getSelectedItem();
            Integer magasinId  = (magasinStr != null && !magasinStr.startsWith("--"))
                    ? Integer.parseInt(magasinStr.split(" - ")[0]) : null;

            int moisIdx    = cboMoisVal.getSelectedIndex() + 1;
            int jourExp    = Integer.parseInt((String) cboJour.getSelectedItem());
            int anneeExp   = Integer.parseInt((String) cboAnnee.getSelectedItem());
            java.util.Calendar calExp = java.util.Calendar.getInstance();
            calExp.set(anneeExp, moisIdx - 1, jourExp);
            long diffMs        = calExp.getTimeInMillis() - System.currentTimeMillis();
            int  validiteJours = Math.max(1, (int)(diffMs / (1000L * 60 * 60 * 24)));

            // *** FIX PRINCIPAL : email inclus dans l'INSERT ***
            String sql =
                    "INSERT INTO demande " +
                            "(client_id, nombre_bons, valeur_unitaire, montant_total, " +
                            " magasin_id, validite_jours, statuts, cree_par, email) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DBconnect.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                pstmt.setInt   (1, clientId);
                pstmt.setInt   (2, nombreBons);
                pstmt.setDouble(3, valeurUnitaire);
                pstmt.setDouble(4, montantTotal);
                if (magasinId != null) pstmt.setInt(5, magasinId);
                else                   pstmt.setNull(5, java.sql.Types.INTEGER);
                pstmt.setInt   (6, validiteJours);
                pstmt.setString(7, "EN_ATTENTE_PAIEMENT");
                pstmt.setInt   (8, userId);
                pstmt.setString(9, email);   // *** email bien passé ici ***

                if (pstmt.executeUpdate() > 0) {
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            int    demandeId = keys.getInt(1);
                            String reference = String.format("VR%04d-%d", demandeId, nombreBons);
                            try (PreparedStatement upd = conn.prepareStatement(
                                    "UPDATE demande SET invoice_reference = ? WHERE demande_id = ?")) {
                                upd.setString(1, reference);
                                upd.setInt   (2, demandeId);
                                upd.executeUpdate();
                            }
                            showSuccess(reference, nombreBons, montantTotal);
                            dispose();
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            showError("Valeur unitaire invalide. Entrez un nombre valide (ex: 500 ou 1250.50).");
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur lors de la cr\u00e9ation : " + e.getMessage());
        }
    }

    // ── DIALOGS ─────────────────────────────────────────────────────────────
    private void showWarning(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Champ requis", JOptionPane.WARNING_MESSAGE);
    }
    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur", JOptionPane.ERROR_MESSAGE);
    }
    private void showSuccess(String ref, int nombre, double montant) {
        JOptionPane.showMessageDialog(this,
                "<html><div style='padding:6px'>" +
                        "<b style='font-size:14px;color:#228B22'>&#10004;  Demande cr\u00e9\u00e9e avec succ\u00e8s</b><br><br>" +
                        "<table>" +
                        "<tr><td><b>R\u00e9f\u00e9rence&nbsp;&nbsp;</b></td><td>" + ref + "</td></tr>" +
                        "<tr><td><b>Nombre de bons&nbsp;&nbsp;</b></td><td>" + nombre + "</td></tr>" +
                        "<tr><td><b>Montant total&nbsp;&nbsp;</b></td><td>Rs " + String.format("%,.2f", montant) + "</td></tr>" +
                        "</table></div></html>",
                "Succ\u00e8s", JOptionPane.INFORMATION_MESSAGE);
    }
}