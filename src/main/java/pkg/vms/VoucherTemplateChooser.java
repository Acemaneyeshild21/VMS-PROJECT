package pkg.vms;

import pkg.vms.DAO.BonDAO;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;

/**
 * Fenetre de selection de template pour les bons cadeau.
 * Affiche 4 aperçus visuels, permet de choisir, puis d'envoyer ou d'annuler.
 */
public class VoucherTemplateChooser extends JDialog {

    // ── Résultat du dialog ───────────────────────────────────────────────────
    public enum Result { SEND, DOWNLOAD, CANCEL }

    private Result result = Result.CANCEL;
    private VoucherPDFGenerator.VoucherTemplate selectedTemplate =
            VoucherPDFGenerator.VoucherTemplate.PRESTIGE_OR;

    // ── Données ──────────────────────────────────────────────────────────────
    private final int demandeId;
    private final List<BonDAO.BonInfo> bons;
    private final int userId;

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG       = new Color(245, 246, 250);
    private static final Color CARD_BG  = Color.WHITE;
    private static final Color RED      = new Color(210, 35, 45);
    private static final Color BORDER   = new Color(228, 230, 236);
    private static final Color SEL_BORD = new Color(210, 35, 45);
    private static final Color TXT_D    = new Color(22, 28, 45);
    private static final Color TXT_G    = new Color(90, 100, 120);

    private final TemplateCard[] cards = new TemplateCard[4];
    private final JLabel lblDescription = new JLabel();

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTEUR
    // ════════════════════════════════════════════════════════════════════════

    public VoucherTemplateChooser(Frame owner, int demandeId,
                                   List<BonDAO.BonInfo> bons, int userId) {
        super(owner, "Choisir un modele de bon cadeau", true);
        this.demandeId = demandeId;
        this.bons      = bons;
        this.userId    = userId;

        setSize(920, 640);
        setLocationRelativeTo(owner);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI();
        selectTemplate(VoucherPDFGenerator.VoucherTemplate.PRESTIGE_OR);
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONSTRUCTION UI
    // ════════════════════════════════════════════════════════════════════════

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);

        // ── Header ──────────────────────────────────────────────────────────
        root.add(buildHeader(), BorderLayout.NORTH);

        // ── Centre : grille de templates ────────────────────────────────────
        root.add(buildGrid(), BorderLayout.CENTER);

        // ── Footer : description + boutons ──────────────────────────────────
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 14, 24));

        JLabel title = new JLabel("Choisir un modele de bon cadeau");
        title.setFont(new Font("Georgia", Font.BOLD, 20));
        title.setForeground(TXT_D);
        header.add(title, BorderLayout.WEST);

        // Info bons
        String info = bons.size() + " bon(s)  |  "
                + (bons.isEmpty() ? "" : String.format("Rs %,.0f x %d", bons.get(0).valeur, bons.size()));
        JLabel lblInfo = new JLabel(info);
        lblInfo.setFont(new Font("Trebuchet MS", Font.PLAIN, 12));
        lblInfo.setForeground(TXT_G);
        header.add(lblInfo, BorderLayout.EAST);

        return header;
    }

    private JPanel buildGrid() {
        JPanel center = new JPanel(new BorderLayout());
        center.setBackground(BG);
        center.setBorder(BorderFactory.createEmptyBorder(16, 20, 8, 20));

        // Grille 2x2 de templates
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setBackground(BG);

        VoucherPDFGenerator.VoucherTemplate[] templates = VoucherPDFGenerator.VoucherTemplate.values();
        for (int i = 0; i < templates.length; i++) {
            TemplateCard card = new TemplateCard(templates[i]);
            cards[i] = card;
            grid.add(card);
        }

        center.add(grid, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER);
                g2.drawLine(0, 0, getWidth(), 0);
                g2.dispose();
            }
        };
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 24, 14, 24));

        // Description du template selectionne
        lblDescription.setFont(new Font("Trebuchet MS", Font.ITALIC, 12));
        lblDescription.setForeground(TXT_G);
        footer.add(lblDescription, BorderLayout.WEST);

        // Boutons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton btnCancel   = UIUtils.buildOutlineButton("Annuler",         120, 38);
        JButton btnDownload = UIUtils.buildOutlineButton("Telecharger PDF", 165, 38);
        JButton btnSend     = UIUtils.buildRedButton("Envoyer par email",   180, 38);

        btnCancel.addActionListener(e -> {
            result = Result.CANCEL;
            dispose();
        });

        btnDownload.addActionListener(e -> {
            result = Result.DOWNLOAD;
            dispose();
        });

        btnSend.addActionListener(e -> {
            result = Result.SEND;
            dispose();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnDownload);
        btnPanel.add(btnSend);
        footer.add(btnPanel, BorderLayout.EAST);

        return footer;
    }

    // ════════════════════════════════════════════════════════════════════════
    // CARTE TEMPLATE
    // ════════════════════════════════════════════════════════════════════════

    private class TemplateCard extends JPanel {
        private final VoucherPDFGenerator.VoucherTemplate template;
        private boolean selected = false;
        private boolean hovered  = false;

        TemplateCard(VoucherPDFGenerator.VoucherTemplate template) {
            this.template = template;
            setLayout(new BorderLayout());
            setBackground(CARD_BG);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 130));

            // Preview du template (dessin personnalise)
            VoucherPreviewPanel preview = new VoucherPreviewPanel(template);

            // Label nom en bas
            JLabel lblName = new JLabel(template.label, SwingConstants.CENTER);
            lblName.setFont(new Font("Trebuchet MS", Font.BOLD, 12));
            lblName.setForeground(TXT_D);
            lblName.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));

            add(preview, BorderLayout.CENTER);
            add(lblName, BorderLayout.SOUTH);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectTemplate(template);
                }
                @Override public void mouseEntered(MouseEvent e) {
                    hovered = true; repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    hovered = false; repaint();
                }
            });
        }

        void setSelected(boolean sel) { this.selected = sel; repaint(); }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
            // Ombre
            if (hovered || selected) {
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fill(new RoundRectangle2D.Double(2, 3, getWidth()-2, getHeight()-2, 12, 12));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth()-1, getHeight()-1, 12, 12));
            }
            // Bordure (rouge si sélectionné, gris sinon)
            g2.setColor(selected ? SEL_BORD : (hovered ? new Color(180,180,200) : BORDER));
            g2.setStroke(new BasicStroke(selected ? 2.5f : 1f));
            g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 12, 12));
            // Badge "Selectionne"
            if (selected) {
                g2.setColor(SEL_BORD);
                g2.fillOval(getWidth()-22, 8, 14, 14);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(getWidth()-18, 15, getWidth()-15, 18);
                g2.drawLine(getWidth()-15, 18, getWidth()-10, 12);
            }
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // APERCU MINIATURE D'UN TEMPLATE
    // ════════════════════════════════════════════════════════════════════════

    private class VoucherPreviewPanel extends JPanel {
        private final VoucherPDFGenerator.VoucherTemplate template;

        VoucherPreviewPanel(VoucherPDFGenerator.VoucherTemplate t) {
            this.template = t;
            setPreferredSize(new Dimension(200, 105));
            setOpaque(false);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth() - 4, h = getHeight() - 4;
            g2.translate(2, 2);
            g2.clipRect(0, 0, w, h);

            switch (template) {
                case PRESTIGE_OR     -> drawGoldPreview(g2, w, h);
                case BLEU_CORPORATIF -> drawBluePreview(g2, w, h);
                case ROUGE_FESTIF    -> drawRedPreview(g2, w, h);
                case VERT_MODERNE    -> drawGreenPreview(g2, w, h);
            }
            g2.dispose();
        }

        // ── Apercu Or ───────────────────────────────────────────────────────
        private void drawGoldPreview(Graphics2D g, int w, int h) {
            // Fond dore
            g.setPaint(new GradientPaint(0,0, new Color(252,245,220), 0,h, new Color(235,210,155)));
            g.fillRoundRect(0,0,w,h,8,8);
            // Ruban rouge
            g.setColor(new Color(170,20,30));
            int[] rx = {0,50,0}; int[] ry = {0,0,42};
            g.fillPolygon(rx,ry,3);
            g.setColor(new Color(190,30,40));
            int[] rx2={0,42,0}; int[] ry2={0,0,35};
            g.fillPolygon(rx2,ry2,3);
            // Bordure or
            g.setColor(new Color(195,155,65)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1,1,w-2,h-2,8,8);
            // "BON CADEAU"
            g.setFont(new Font("Georgia",Font.BOLD,11));
            g.setColor(new Color(100,70,20));
            g.drawString("BON CADEAU", w/2-30, h/2-8);
            // Montant
            g.setFont(new Font("Georgia",Font.BOLD,14));
            g.setColor(new Color(210,35,45));
            String amt = bons.isEmpty() ? "Rs ---" : String.format("Rs %,.0f",bons.get(0).valeur);
            g.drawString(amt, w/2-25, h/2+10);
            // QR placeholder
            g.setColor(Color.WHITE);    g.fillRect(w-30,h/2-15,25,25);
            g.setColor(new Color(195,155,65)); g.setStroke(new BasicStroke(1f));
            g.drawRect(w-30,h/2-15,25,25);
            drawQRPlaceholder(g, w-28, h/2-13, 21);
        }

        // ── Apercu Bleu ──────────────────────────────────────────────────────
        private void drawBluePreview(Graphics2D g, int w, int h) {
            g.setColor(new Color(248,249,252)); g.fillRoundRect(0,0,w,h,8,8);
            // Header navy
            g.setColor(new Color(30,58,95)); g.fillRect(0,0,w,28);
            // Bande or
            g.setColor(new Color(201,168,76)); g.fillRect(0,28,w,3);
            // Footer silver
            g.setColor(new Color(215,220,230)); g.fillRect(0,h-16,w,16);
            // Bordure navy
            g.setColor(new Color(30,58,95)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1,1,w-2,h-2,8,8);
            // Logo dans header
            g.setColor(new Color(201,168,76));
            g.setFont(new Font("Georgia",Font.BOLD,8));
            g.drawString("INTERMART",5,18);
            // "BON CADEAU" en blanc
            g.setColor(Color.WHITE);
            g.setFont(new Font("Georgia",Font.BOLD,10));
            g.drawString("BON CADEAU",5,42);
            // Montant
            g.setColor(new Color(30,58,95));
            g.setFont(new Font("Georgia",Font.BOLD,13));
            String amt = bons.isEmpty() ? "Rs ---" : String.format("Rs %,.0f",bons.get(0).valeur);
            g.drawString(amt, 5, h/2+15);
            // QR
            g.setColor(Color.WHITE); g.fillRect(w-30,30,25,25);
            g.setColor(new Color(30,58,95)); g.setStroke(new BasicStroke(1f));
            g.drawRect(w-30,30,25,25);
            drawQRPlaceholder(g,w-28,32,21);
        }

        // ── Apercu Rouge ─────────────────────────────────────────────────────
        private void drawRedPreview(Graphics2D g, int w, int h) {
            g.setColor(new Color(25,22,28)); g.fillRoundRect(0,0,w,h,8,8);
            // Header rouge
            g.setColor(new Color(150,20,30)); g.fillRect(0,0,w,30);
            // Deco bannieres
            Color[] flags = {new Color(200,50,50), new Color(50,180,50), new Color(50,50,200),
                             new Color(200,180,50)};
            for (int i=0;i<8;i++) {
                int fx=5+i*(w-10)/8;
                g.setColor(flags[i%4]);
                g.fillPolygon(new int[]{fx,fx+9,fx+18},new int[]{2,12,2},3);
            }
            // Logo
            g.setColor(new Color(201,168,76));
            g.setFont(new Font("Georgia",Font.BOLD,8));
            g.drawString("INTERMART",5,22);
            // "BON CADEAU" en or
            g.setColor(new Color(201,168,76));
            g.setFont(new Font("Georgia",Font.BOLD,11));
            g.drawString("BON CADEAU",5,50);
            // Montant en or
            g.setColor(new Color(230,205,130));
            g.setFont(new Font("Georgia",Font.BOLD,14));
            String amt = bons.isEmpty() ? "Rs ---" : String.format("Rs %,.0f",bons.get(0).valeur);
            g.drawString(amt, 5, h/2+12);
            // QR fond blanc
            g.setColor(Color.WHITE); g.fillRect(w-32,32,26,26);
            g.setColor(new Color(201,168,76)); g.setStroke(new BasicStroke(1f));
            g.drawRect(w-32,32,26,26);
            drawQRPlaceholder(g,w-30,34,22);
            // Bordure or
            g.setColor(new Color(201,168,76)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1,1,w-2,h-2,8,8);
        }

        // ── Apercu Vert ──────────────────────────────────────────────────────
        private void drawGreenPreview(Graphics2D g, int w, int h) {
            g.setColor(Color.WHITE); g.fillRoundRect(0,0,w,h,8,8);
            // Bande verte gauche
            g.setColor(new Color(46,125,50)); g.fillRect(0,0,32,h);
            // Accent vert clair haut
            g.setColor(new Color(200,230,201)); g.fillRect(32,0,w-32,6);
            // Bordure verte
            g.setColor(new Color(46,125,50)); g.setStroke(new BasicStroke(1.5f));
            g.drawRoundRect(1,1,w-2,h-2,8,8);
            // Logo blanc dans bande
            g.setColor(Color.WHITE);
            g.setFont(new Font("Georgia",Font.BOLD,7));
            g.drawString("INTER",5,h/2-3);
            g.drawString("MART",7,h/2+7);
            // "BON CADEAU" vert
            g.setColor(new Color(27,94,32));
            g.setFont(new Font("Georgia",Font.BOLD,11));
            g.drawString("BON CADEAU",38,28);
            // Montant dans cadre vert
            g.setColor(new Color(46,125,50));
            g.fillRoundRect(38, h/2-8, w-45, 22, 5, 5);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Georgia",Font.BOLD,12));
            String amt = bons.isEmpty() ? "Rs ---" : String.format("Rs %,.0f",bons.get(0).valeur);
            g.drawString(amt, 44, h/2+8);
            // QR
            g.setColor(Color.WHITE); g.fillRect(w-32,h-38,26,26);
            g.setColor(new Color(46,125,50)); g.setStroke(new BasicStroke(1f));
            g.drawRect(w-32,h-38,26,26);
            drawQRPlaceholder(g,w-30,h-36,22);
        }

        // ── Dessin QR miniature ──────────────────────────────────────────────
        private void drawQRPlaceholder(Graphics2D g, int x, int y, int s) {
            int cell = s/5;
            g.setColor(new Color(40,40,40));
            boolean[][] pattern = {
                {true,true,true,false,true},
                {true,false,true,false,false},
                {true,true,true,false,true},
                {false,false,true,true,false},
                {true,false,false,true,true}
            };
            for (int row=0;row<5;row++) for (int col=0;col<5;col++) {
                if (pattern[row][col]) g.fillRect(x+col*cell, y+row*cell, cell-1, cell-1);
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GESTION DE LA SELECTION
    // ════════════════════════════════════════════════════════════════════════

    private void selectTemplate(VoucherPDFGenerator.VoucherTemplate t) {
        this.selectedTemplate = t;
        VoucherPDFGenerator.VoucherTemplate[] templates = VoucherPDFGenerator.VoucherTemplate.values();
        for (int i = 0; i < cards.length && i < templates.length; i++) {
            if (cards[i] != null)
                cards[i].setSelected(templates[i] == t);
        }
        lblDescription.setText("  " + t.label + " — " + t.description);
    }

    // ════════════════════════════════════════════════════════════════════════
    // METHODE STATIQUE D'UTILISATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Ouvre la fenetre de choix, genere les PDFs avec le template choisi,
     * puis envoie ou telecharge selon l'action selectionnee.
     *
     * @return true si action reussie, false si annule
     */
    public static boolean afficherEtGenerer(Frame owner, int demandeId,
                                             List<BonDAO.BonInfo> bons, int userId) {
        VoucherTemplateChooser chooser = new VoucherTemplateChooser(owner, demandeId, bons, userId);
        chooser.setVisible(true); // bloquant (modal)

        if (chooser.result == Result.CANCEL) return false;

        VoucherPDFGenerator.VoucherTemplate template = chooser.selectedTemplate;

        // Générer les PDFs en arrière-plan
        SwingWorker<Boolean, String> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                publish("Generation des PDFs...");
                for (BonDAO.BonInfo bon : bons) {
                    String path = VoucherPDFGenerator.genererPDF(bon, template);
                    pkg.vms.DAO.BonDAO.updatePdfPath(bon.bonId, path);
                    bon.pdfPath = path;
                }
                if (chooser.result == Result.SEND) {
                    publish("Envoi des emails...");
                    EmailService.envoyerBonsParEmail(demandeId, bons, userId);
                }
                return true;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                // On pourrait afficher un progress dialog ici
            }

            @Override
            protected void done() {
                try {
                    get();
                    String msg = chooser.result == Result.SEND
                            ? bons.size() + " bon(s) genere(s) et envoyes par email !"
                            : bons.size() + " bon(s) genere(s) dans " +
                              System.getProperty("user.home") + "/VMS_Bons";
                    JOptionPane.showMessageDialog(owner, msg,
                            "Generation terminee", JOptionPane.INFORMATION_MESSAGE);

                    // Ouvrir le dossier si telechargement
                    if (chooser.result == Result.DOWNLOAD) {
                        try { Desktop.getDesktop().open(
                                new java.io.File(System.getProperty("user.home") + "/VMS_Bons"));
                        } catch (Exception ignored) {}
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner,
                            "Erreur : " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
        return true;
    }

    // Getters pour utilisation externe
    public Result       getResult()           { return result; }
    public VoucherPDFGenerator.VoucherTemplate getSelectedTemplate() { return selectedTemplate; }
}
