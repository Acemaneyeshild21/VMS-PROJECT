package pkg.vms;

import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.VoucherDAO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Consumer;

/**
 * Widgets du tableau de bord — factor\u00e9s ici pour garder Dashboard.java lisible.
 * Chaque m\u00e9thode retourne un JPanel pr\u00eat \u00e0 ins\u00e9rer dans la home.
 */
public final class DashboardWidgets {

    private static final Color TEXT_PRIMARY   = VMSStyle.TEXT_PRIMARY;
    private static final Color TEXT_SECONDARY = VMSStyle.TEXT_SECONDARY;
    private static final Color TEXT_MUTED     = VMSStyle.TEXT_MUTED;
    private static final Color SUCCESS        = VMSStyle.SUCCESS;
    private static final Color WARNING        = VMSStyle.WARNING;
    private static final Color DANGER         = VMSStyle.DANGER;
    private static final Color ACCENT_BLUE    = VMSStyle.ACCENT_BLUE;

    private DashboardWidgets() {}

    // ════════════════════════════════════════════════════════════════════════
    //  Widget "À FAIRE" — adapté au rôle
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildAFaireWidget(String role, Consumer<String> onNavigate) {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 12), 18);

        JLabel title = widgetTitle("\u2713  \u00c0 faire aujourd'hui");
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(placeholderRow("Chargement\u2026"));
        card.add(body, BorderLayout.CENTER);

        new SwingWorker<List<String[]>, Void>() {
            @Override protected List<String[]> doInBackground() throws Exception {
                java.util.ArrayList<String[]> out = new java.util.ArrayList<>();
                int n;
                switch (role != null ? role.toLowerCase() : "") {
                    case "comptable" -> {
                        n = VoucherDAO.getTachesParRole("comptable");
                        if (n > 0) out.add(new String[]{"\uD83D\uDCB3  " + n + " paiement" + (n>1?"s":"") + " \u00e0 valider",
                                "Validation"});
                    }
                    case "approbateur" -> {
                        n = VoucherDAO.getTachesParRole("approbateur");
                        if (n > 0) out.add(new String[]{"\u2705  " + n + " demande" + (n>1?"s":"") + " \u00e0 approuver",
                                "Validation"});
                    }
                    case "administrateur", "manager" -> {
                        int a = VoucherDAO.getTachesParRole("comptable");
                        int b = VoucherDAO.getTachesParRole("approbateur");
                        if (a > 0) out.add(new String[]{"\uD83D\uDCB3  " + a + " paiement" + (a>1?"s":""), "Validation"});
                        if (b > 0) out.add(new String[]{"\u2705  " + b + " approbation" + (b>1?"s":""), "Validation"});
                    }
                    default -> {
                        n = VoucherDAO.getTachesParRole("");
                        if (n > 0) out.add(new String[]{"\u23F3  " + n + " demande" + (n>1?"s":"") + " en cours", "Demandes"});
                    }
                }
                return out;
            }
            @Override protected void done() {
                body.removeAll();
                try {
                    List<String[]> items = get();
                    if (items.isEmpty()) {
                        body.add(placeholderRow("\uD83C\uDF89 Aucune t\u00e2che en attente \u2014 tout est \u00e0 jour !"));
                    } else {
                        for (String[] it : items) {
                            body.add(buildTaskRow(it[0], () -> onNavigate.accept(it[1])));
                            body.add(Box.createVerticalStrut(6));
                        }
                    }
                } catch (Exception e) {
                    body.add(placeholderRow("Erreur chargement"));
                }
                body.revalidate();
                body.repaint();
            }
        }.execute();

        return card;
    }

    private static JPanel buildTaskRow(String text, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            boolean h = false;
            {
                setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                    public void mouseClicked(MouseEvent e) { onClick.run(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(h ? VMSStyle.BG_SUBTLE : new Color(249, 250, 252));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                g2.dispose();
            }
        };
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_PRIMARY);
        JLabel arrow = new JLabel("\u203A");
        arrow.setFont(new Font("Segoe UI", Font.BOLD, 18));
        arrow.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.CENTER);
        row.add(arrow, BorderLayout.EAST);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Widget Timeline — événements audit_log
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildTimelineWidget() {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 12), 18);
        JLabel title = widgetTitle("\uD83D\uDD52  Activit\u00e9 r\u00e9cente");
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(placeholderRow("Chargement\u2026"));
        card.add(body, BorderLayout.CENTER);

        new SwingWorker<List<AuditDAO.Event>, Void>() {
            @Override protected List<AuditDAO.Event> doInBackground() { return AuditDAO.getRecent(6); }
            @Override protected void done() {
                body.removeAll();
                try {
                    List<AuditDAO.Event> evts = get();
                    if (evts.isEmpty()) {
                        body.add(placeholderRow("Aucune activit\u00e9 r\u00e9cente"));
                    } else {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm");
                        for (AuditDAO.Event e : evts) {
                            body.add(buildTimelineRow(iconForAction(e.action), colorForAction(e.action),
                                    shortLabel(e), sdf.format(e.dateEvt)));
                            body.add(Box.createVerticalStrut(4));
                        }
                    }
                } catch (Exception e) {
                    body.add(placeholderRow("Erreur"));
                }
                body.revalidate();
                body.repaint();
            }
        }.execute();
        return card;
    }

    private static String shortLabel(AuditDAO.Event e) {
        String who = e.username != null ? e.username : "Syst\u00e8me";
        String action = e.action != null ? e.action.toLowerCase().replace('_', ' ') : "";
        return who + " \u2014 " + action + " #" + e.recordId;
    }

    private static String iconForAction(String a) {
        if (a == null) return "\u2022";
        return switch (a) {
            case "PAIEMENT"    -> "\uD83D\uDCB3";
            case "APPROBATION" -> "\u2705";
            case "GENERATION"  -> "\uD83C\uDFAB";
            case "ENVOI"       -> "\uD83D\uDCE8";
            case "REJET"       -> "\u274C";
            case "REDEMPTION"  -> "\uD83D\uDCB8";
            case "CREATION"    -> "\u2795";
            case "CONNEXION"   -> "\uD83D\uDD11";
            case "ARCHIVAGE_MASSIF" -> "\uD83D\uDCE6";
            default            -> "\u2022";
        };
    }

    private static Color colorForAction(String a) {
        if (a == null) return TEXT_MUTED;
        return switch (a) {
            case "PAIEMENT"    -> ACCENT_BLUE;
            case "APPROBATION","GENERATION" -> SUCCESS;
            case "ENVOI"       -> new Color(20, 170, 190);
            case "REJET"       -> DANGER;
            case "CREATION"    -> WARNING;
            default            -> TEXT_SECONDARY;
        };
    }

    private static JPanel buildTimelineRow(String icon, Color color, String text, String time) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel iconL = new JLabel(icon);
        iconL.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 14));
        iconL.setForeground(color);
        iconL.setPreferredSize(new Dimension(24, 20));

        JLabel txtL = new JLabel(text);
        txtL.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtL.setForeground(TEXT_PRIMARY);

        JLabel timeL = new JLabel(time);
        timeL.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeL.setForeground(TEXT_MUTED);

        row.add(iconL, BorderLayout.WEST);
        row.add(txtL,  BorderLayout.CENTER);
        row.add(timeL, BorderLayout.EAST);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Widget Sparkline — émissions 30j
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildSparklineWidget() {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 10), 18);
        JLabel title = widgetTitle("\uD83D\uDCC8  \u00c9missions 30 derniers jours");
        card.add(title, BorderLayout.NORTH);

        JLabel totalLabel = new JLabel("\u2026");
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        totalLabel.setForeground(TEXT_PRIMARY);

        JLabel sub = new JLabel("demandes cr\u00e9\u00e9es");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.add(totalLabel);
        head.add(sub);

        SparklinePanel chart = new SparklinePanel();

        JPanel inner = new JPanel(new BorderLayout(0, 8));
        inner.setOpaque(false);
        inner.add(head, BorderLayout.NORTH);
        inner.add(chart, BorderLayout.CENTER);
        card.add(inner, BorderLayout.CENTER);

        new SwingWorker<int[], Void>() {
            @Override protected int[] doInBackground() throws Exception { return VoucherDAO.getEmissionsLast30Days(); }
            @Override protected void done() {
                try {
                    int[] data = get();
                    chart.setData(data);
                    int total = 0; for (int v : data) total += v;
                    totalLabel.setText(String.valueOf(total));
                } catch (Exception e) {
                    totalLabel.setText("\u2014");
                }
            }
        }.execute();
        return card;
    }

    /** Mini graphique area — sparkline. */
    private static class SparklinePanel extends JPanel {
        private int[] data = new int[0];
        SparklinePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 70));
        }
        void setData(int[] d) { this.data = d != null ? d : new int[0]; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.length == 0) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int max = 1;
            for (int v : data) if (v > max) max = v;

            Path2D line = new Path2D.Double();
            Path2D area = new Path2D.Double();
            double stepX = (double) w / Math.max(1, data.length - 1);
            area.moveTo(0, h);
            for (int i = 0; i < data.length; i++) {
                double x = i * stepX;
                double y = h - ((double) data[i] / max) * (h - 6) - 3;
                if (i == 0) line.moveTo(x, y);
                else        line.lineTo(x, y);
                area.lineTo(x, y);
            }
            area.lineTo(w, h);
            area.closePath();

            GradientPaint gp = new GradientPaint(0, 0, new Color(210, 35, 45, 70),
                    0, h, new Color(210, 35, 45, 0));
            g2.setPaint(gp);
            g2.fill(area);

            g2.setColor(VMSStyle.RED_PRIMARY);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);

            // point final
            if (data.length > 0) {
                double x = (data.length - 1) * stepX;
                double y = h - ((double) data[data.length - 1] / max) * (h - 6) - 3;
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Ellipse2D.Double(x - 4, y - 4, 8, 8));
            }
            g2.dispose();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Widget Top Clients
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildTopClientsWidget(Consumer<String> onNavigate) {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 10), 18);
        JLabel title = widgetTitle("\uD83C\uDFC6  Top clients (90j)");
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(placeholderRow("Chargement\u2026"));
        card.add(body, BorderLayout.CENTER);

        new SwingWorker<List<VoucherDAO.TopClient>, Void>() {
            @Override protected List<VoucherDAO.TopClient> doInBackground() throws Exception {
                return VoucherDAO.getTopClients(5);
            }
            @Override protected void done() {
                body.removeAll();
                try {
                    List<VoucherDAO.TopClient> top = get();
                    if (top.isEmpty()) {
                        body.add(placeholderRow("Aucune donn\u00e9e sur 90j"));
                    } else {
                        int rank = 1;
                        double max = top.get(0).montant;
                        for (VoucherDAO.TopClient c : top) {
                            body.add(buildTopClientRow(rank++, c.nom, c.montant, c.nbDemandes, max));
                            body.add(Box.createVerticalStrut(4));
                        }
                    }
                } catch (Exception e) {
                    body.add(placeholderRow("Erreur"));
                }
                body.revalidate(); body.repaint();
            }
        }.execute();
        return card;
    }

    private static JPanel buildTopClientRow(int rank, String nom, double montant, int nb, double max) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JPanel left = new JPanel(new BorderLayout(8, 0));
        left.setOpaque(false);
        JLabel rankL = new JLabel("#" + rank);
        rankL.setFont(new Font("Segoe UI", Font.BOLD, 11));
        rankL.setForeground(rank == 1 ? new Color(234, 179, 8) : TEXT_MUTED);
        rankL.setPreferredSize(new Dimension(24, 20));
        JLabel nameL = new JLabel(nom);
        nameL.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameL.setForeground(TEXT_PRIMARY);
        left.add(rankL, BorderLayout.WEST);
        left.add(nameL, BorderLayout.CENTER);

        JLabel mnt = new JLabel(String.format("Rs %,.0f", montant));
        mnt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mnt.setForeground(VMSStyle.RED_PRIMARY);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(left, BorderLayout.CENTER);
        header.add(mnt, BorderLayout.EAST);

        double ratio = max > 0 ? montant / max : 0;
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(229, 231, 235));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 4, 4));
                int w = (int) (getWidth() * ratio);
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.fill(new RoundRectangle2D.Double(0, 0, w, getHeight(), 4, 4));
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 4));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(header);
        col.add(Box.createVerticalStrut(4));
        col.add(bar);
        row.add(col, BorderLayout.CENTER);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Widget Bons expirant bientôt
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildExpiringWidget(Consumer<String> onNavigate) {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 10), 18);
        JLabel title = widgetTitle("\u23F0  Bons expirant bient\u00f4t (30j)");
        card.add(title, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(placeholderRow("Chargement\u2026"));
        card.add(body, BorderLayout.CENTER);

        new SwingWorker<List<VoucherDAO.BonExpirant>, Void>() {
            @Override protected List<VoucherDAO.BonExpirant> doInBackground() throws Exception {
                return VoucherDAO.getBonsExpirantBientot(30, 5);
            }
            @Override protected void done() {
                body.removeAll();
                try {
                    List<VoucherDAO.BonExpirant> list = get();
                    if (list.isEmpty()) {
                        body.add(placeholderRow("Aucun bon n'expire dans les 30 prochains jours"));
                    } else {
                        for (VoucherDAO.BonExpirant b : list) {
                            Color c = b.joursRestants <= 7 ? DANGER : (b.joursRestants <= 15 ? WARNING : TEXT_SECONDARY);
                            String text = b.reference + " \u2014 " + (b.client != null ? b.client : "?");
                            String time = b.joursRestants + "j";
                            body.add(buildExpiringRow(text, time, c, () -> onNavigate.accept("Demandes")));
                            body.add(Box.createVerticalStrut(4));
                        }
                    }
                } catch (Exception e) {
                    body.add(placeholderRow("Erreur"));
                }
                body.revalidate(); body.repaint();
            }
        }.execute();
        return card;
    }

    private static JPanel buildExpiringRow(String text, String time, Color color, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(8, 0)) {
            boolean h = false;
            {
                setOpaque(false); setCursor(new Cursor(Cursor.HAND_CURSOR));
                setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                    public void mouseClicked(MouseEvent e) { onClick.run(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (h) {
                    g2.setColor(VMSStyle.BG_SUBTLE);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 6, 6));
                }
                g2.dispose();
            }
        };
        JLabel txtL = new JLabel(text);
        txtL.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtL.setForeground(TEXT_PRIMARY);

        JPanel chip = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 32));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        JLabel tl = new JLabel(time);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tl.setForeground(color);
        chip.add(tl);
        chip.setPreferredSize(new Dimension(46, 22));

        row.add(txtL, BorderLayout.CENTER);
        row.add(chip, BorderLayout.EAST);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Widget Météo business 🟢🟡🔴
    // ════════════════════════════════════════════════════════════════════════
    public static JPanel buildMeteoWidget() {
        JPanel card = PageLayout.buildCard(new BorderLayout(0, 10), 18);
        JLabel title = widgetTitle("\uD83C\uDF21\uFE0F  M\u00e9t\u00e9o business");
        card.add(title, BorderLayout.NORTH);

        JLabel bigEmoji = new JLabel("\u2026", SwingConstants.CENTER);
        bigEmoji.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 48));

        JLabel stateLabel = new JLabel("Analyse en cours\u2026", SwingConstants.CENTER);
        stateLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        stateLabel.setForeground(TEXT_PRIMARY);

        JLabel detailL = new JLabel(" ", SwingConstants.CENTER);
        detailL.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detailL.setForeground(TEXT_SECONDARY);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.add(Box.createVerticalGlue());
        bigEmoji.setAlignmentX(Component.CENTER_ALIGNMENT);
        stateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        detailL.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(bigEmoji);
        col.add(Box.createVerticalStrut(4));
        col.add(stateLabel);
        col.add(Box.createVerticalStrut(2));
        col.add(detailL);
        col.add(Box.createVerticalGlue());
        card.add(col, BorderLayout.CENTER);

        new SwingWorker<VoucherDAO.VoucherStats, Void>() {
            @Override protected VoucherDAO.VoucherStats doInBackground() throws Exception {
                return VoucherDAO.getDashboardStats();
            }
            @Override protected void done() {
                try {
                    VoucherDAO.VoucherStats s = get();
                    // Règles heuristiques :
                    int score = 0;
                    if (s.validationRate >= 70) score += 2;
                    else if (s.validationRate >= 40) score += 1;
                    if (s.pendingPayments <= 5) score += 2;
                    else if (s.pendingPayments <= 15) score += 1;
                    if (s.activeClients > 0) score += 1;

                    if (score >= 4) {
                        bigEmoji.setText("\uD83D\uDFE2");
                        stateLabel.setText("Tout roule !");
                        detailL.setText("Activit\u00e9 normale, rien \u00e0 signaler.");
                    } else if (score >= 2) {
                        bigEmoji.setText("\uD83D\uDFE1");
                        stateLabel.setText("Vigilance");
                        detailL.setText(s.pendingPayments + " paiement(s) en attente, taux validation " + s.validationRate + " %");
                    } else {
                        bigEmoji.setText("\uD83D\uDD34");
                        stateLabel.setText("Attention");
                        detailL.setText("Goulot d\u2019\u00e9tranglement d\u00e9tect\u00e9 \u2014 traitez les demandes en attente.");
                    }
                } catch (Exception e) {
                    bigEmoji.setText("\u2753");
                    stateLabel.setText("Donn\u00e9es indisponibles");
                }
            }
        }.execute();
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Helpers
    // ════════════════════════════════════════════════════════════════════════
    private static JLabel widgetTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private static JPanel placeholderRow(String text) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        l.setForeground(TEXT_MUTED);
        p.add(l, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return p;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  FAB — Floating Action Button
    // ════════════════════════════════════════════════════════════════════════
    public static JButton buildFab(String tooltip, Runnable onClick) {
        JButton fab = new JButton() {
            boolean h = false;
            {
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setToolTipText(tooltip);
                setPreferredSize(new Dimension(56, 56));
                setSize(56, 56);
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
                addActionListener(e -> onClick.run());
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Ombre
                for (int i = 0; i < 5; i++) {
                    g2.setColor(new Color(17, 24, 39, 10 - i * 2));
                    g2.fillOval(2 + i, 4 + i, getWidth() - 4 - 2*i, getHeight() - 4 - 2*i);
                }
                g2.setColor(h ? VMSStyle.RED_DARK : VMSStyle.RED_PRIMARY);
                g2.fillOval(2, 2, getWidth() - 6, getHeight() - 6);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 24));
                FontMetrics fm = g2.getFontMetrics();
                String s = "+";
                int sx = (getWidth() - 4 - fm.stringWidth(s)) / 2;
                int sy = (getHeight() - 4 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, sx, sy);
                g2.dispose();
            }
        };
        return fab;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Notification Center (bell icon + popup)
    // ════════════════════════════════════════════════════════════════════════
    public static JButton buildNotificationBell(Consumer<String> onNavigate) {
        JButton bell = new JButton() {
            boolean h = false;
            int count = 0;
            {
                setOpaque(false); setContentAreaFilled(false);
                setBorderPainted(false); setFocusPainted(false);
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                setPreferredSize(new Dimension(34, 34));
                setToolTipText("Notifications");
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                    public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                });
                addActionListener(e -> showNotificationsPopup(this, onNavigate));
                refresh();
            }
            void refresh() {
                new SwingWorker<Integer, Void>() {
                    @Override protected Integer doInBackground() throws Exception {
                        VoucherDAO.VoucherStats s = VoucherDAO.getDashboardStats();
                        int exp = VoucherDAO.getBonsExpirantBientot(7, 20).size();
                        return s.pendingPayments + exp;
                    }
                    @Override protected void done() {
                        try { count = get(); putClientProperty("count", count); repaint(); } catch (Exception ignored) {}
                    }
                }.execute();
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (h) {
                    g2.setColor(new Color(0, 0, 0, 8));
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                }
                // Cloche
                g2.setColor(TEXT_SECONDARY);
                g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawLine(cx - 7, cy + 3, cx + 7, cy + 3);
                g2.drawArc(cx - 6, cy - 8, 12, 14, 0, 180);
                g2.drawLine(cx, cy - 9, cx, cy - 10);
                g2.fillOval(cx - 1, cy + 3, 3, 4);
                // Badge count
                Integer c = (Integer) getClientProperty("count");
                if (c != null && c > 0) {
                    g2.setColor(VMSStyle.RED_PRIMARY);
                    g2.fillOval(getWidth() - 14, 4, 12, 12);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                    String s = c > 99 ? "99+" : String.valueOf(c);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(s, getWidth() - 14 + (12 - fm.stringWidth(s))/2, 4 + fm.getAscent() + 1);
                }
                g2.dispose();
            }
        };
        return bell;
    }

    private static void showNotificationsPopup(Component anchor, Consumer<String> onNavigate) {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1));
        popup.setBackground(Color.WHITE);

        JLabel title = new JLabel(" Notifications");
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(10, 12, 8, 12));
        popup.add(title);
        popup.addSeparator();

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(BorderFactory.createEmptyBorder(4, 8, 10, 8));

        try {
            VoucherDAO.VoucherStats s = VoucherDAO.getDashboardStats();
            if (s.pendingPayments > 0) {
                list.add(notifRow("\uD83D\uDCB3  " + s.pendingPayments + " paiement(s) en attente",
                        WARNING, () -> { popup.setVisible(false); onNavigate.accept("Demandes"); }));
            }
            List<VoucherDAO.BonExpirant> exp = VoucherDAO.getBonsExpirantBientot(7, 5);
            for (VoucherDAO.BonExpirant b : exp) {
                list.add(notifRow("\u23F0  " + b.reference + " expire dans " + b.joursRestants + "j",
                        DANGER, () -> { popup.setVisible(false); onNavigate.accept("Demandes"); }));
            }
            if (s.pendingPayments == 0 && exp.isEmpty()) {
                list.add(notifRow("\u2705  Tout est \u00e0 jour", SUCCESS, null));
            }
        } catch (Exception e) {
            list.add(notifRow("Erreur de chargement", TEXT_MUTED, null));
        }

        JScrollPane sc = new JScrollPane(list);
        sc.setBorder(null); sc.setPreferredSize(new Dimension(320, 260));
        sc.getViewport().setOpaque(false); sc.setOpaque(false);
        popup.add(sc);
        popup.show(anchor, -260, anchor.getHeight() + 4);
    }

    private static JPanel notifRow(String text, Color color, Runnable onClick) {
        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            boolean h = false;
            {
                setOpaque(false);
                setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
                setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                if (onClick != null) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    addMouseListener(new MouseAdapter() {
                        public void mouseEntered(MouseEvent e) { h=true;  repaint(); }
                        public void mouseExited(MouseEvent e)  { h=false; repaint(); }
                        public void mouseClicked(MouseEvent e) { onClick.run(); }
                    });
                }
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (h) {
                    g2.setColor(VMSStyle.BG_SUBTLE);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 6, 6));
                }
                g2.setColor(color);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(TEXT_PRIMARY);
        row.add(lbl, BorderLayout.CENTER);
        return row;
    }
}
