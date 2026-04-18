package pkg.vms;

import pkg.vms.DAO.VoucherDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Fiche client 360° — dialog modal avec onglets (Infos, Historique demandes,
 * Bons, Timeline). Charge les donnees async, fermable via Escape.
 */
public class FicheClient360 extends JDialog {

    private static final SimpleDateFormat DF_DATE = new SimpleDateFormat("dd MMM yyyy", Locale.FRENCH);
    private static final SimpleDateFormat DF_DT   = new SimpleDateFormat("dd MMM yyyy \u00e0 HH:mm", Locale.FRENCH);
    private static final NumberFormat     NF_MUR  = NumberFormat.getNumberInstance(new Locale("fr", "FR"));

    private final Client client;

    // Labels KPI (mis a jour apres loadAsync)
    private JLabel lblTotalDemandes;
    private JLabel lblMontantTotal;
    private JLabel lblBonsActifs;
    private JLabel lblBonsUtilises;
    private JLabel lblDerniereActivite;

    private DefaultTableModel demandesModel;
    private DefaultTableModel bonsModel;
    private JPanel timelinePanel;

    public FicheClient360(Window owner, Client client) {
        super(owner, "Fiche client \u2014 " + client.getName(), ModalityType.APPLICATION_MODAL);
        this.client = client;
        setSize(920, 640);
        setLocationRelativeTo(owner);
        setContentPane(buildRoot());
        installEscape();
        loadAsync();
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(VMSStyle.BG_ROOT);
        root.setBorder(new EmptyBorder(0, 0, 0, 0));

        root.add(buildHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(VMSStyle.BG_CARD);
        tabs.setBorder(new EmptyBorder(0, 18, 18, 18));

        tabs.addTab("\uD83D\uDCC7  Informations", buildInfosTab());
        tabs.addTab("\uD83D\uDCC4  Historique", buildDemandesTab());
        tabs.addTab("\uD83C\uDFAB  Bons", buildBonsTab());
        tabs.addTab("\uD83D\uDD52  Activit\u00e9", buildTimelineTab());

        root.add(tabs, BorderLayout.CENTER);
        return root;
    }

    // ── Header avec avatar + nom + actions ───────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(VMSStyle.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(18, 22, 18, 22)));

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(VMSStyle.RED_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                String initial = (client.getName() != null && !client.getName().isEmpty())
                        ? client.getName().substring(0, 1).toUpperCase() : "?";
                g2.setColor(VMSStyle.RED_PRIMARY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 26));
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(initial)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initial, x, y);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(60, 60));
        header.add(avatar, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(client.getName());
        name.setFont(new Font("Segoe UI", Font.BOLD, 20));
        name.setForeground(VMSStyle.TEXT_PRIMARY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);

        String sub = (client.getCompany() != null && !client.getCompany().isEmpty())
                ? client.getCompany() + "  \u2022  " + orDash(client.getEmail())
                : orDash(client.getEmail());
        JLabel subtitle = new JLabel(sub);
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(VMSStyle.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        mid.add(name);
        mid.add(Box.createVerticalStrut(4));
        mid.add(subtitle);
        header.add(mid, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        JButton badge = new JButton(client.isActif() ? "Actif" : "Inactif");
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(client.isActif() ? new Color(4, 120, 87) : VMSStyle.TEXT_MUTED);
        badge.setBackground(client.isActif() ? new Color(220, 252, 231) : new Color(243, 244, 246));
        badge.setBorder(new EmptyBorder(4, 10, 4, 10));
        badge.setFocusPainted(false);
        badge.setOpaque(true);
        right.add(badge);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // ── Onglet Infos + KPIs ──────────────────────────────────────────────────
    private JPanel buildInfosTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 16));
        tab.setBackground(VMSStyle.BG_CARD);
        tab.setBorder(new EmptyBorder(20, 22, 20, 22));

        // KPIs rangee
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setOpaque(false);
        lblTotalDemandes    = new JLabel("\u2013");
        lblMontantTotal     = new JLabel("\u2013");
        lblBonsActifs       = new JLabel("\u2013");
        lblBonsUtilises     = new JLabel("\u2013");
        kpis.add(kpiCard("Demandes",     lblTotalDemandes, VMSStyle.ACCENT_BLUE));
        kpis.add(kpiCard("Montant total",lblMontantTotal,  VMSStyle.RED_PRIMARY));
        kpis.add(kpiCard("Bons actifs",  lblBonsActifs,    VMSStyle.SUCCESS));
        kpis.add(kpiCard("Bons utilis\u00e9s",lblBonsUtilises,  VMSStyle.WARNING));
        tab.add(kpis, BorderLayout.NORTH);

        // Detailed info card
        JPanel card = cardPanel();
        card.setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(6, 10, 6, 10);
        gc.gridy = 0;

        addInfoRow(card, gc, "ID client",      "#" + client.getClientId());
        addInfoRow(card, gc, "Nom complet",    client.getName());
        addInfoRow(card, gc, "Entreprise",     orDash(client.getCompany()));
        addInfoRow(card, gc, "Email",          orDash(client.getEmail()));
        addInfoRow(card, gc, "T\u00e9l\u00e9phone", orDash(client.getContactNumber()));
        addInfoRow(card, gc, "Date d'ajout",
                client.getDateCreation() != null ? DF_DT.format(client.getDateCreation()) : "\u2013");
        lblDerniereActivite = new JLabel("\u2013");
        lblDerniereActivite.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblDerniereActivite.setForeground(VMSStyle.TEXT_PRIMARY);
        addInfoRow(card, gc, "Derni\u00e8re activit\u00e9", lblDerniereActivite);

        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void addInfoRow(JPanel card, GridBagConstraints gc, String label, String value) {
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        v.setForeground(VMSStyle.TEXT_PRIMARY);
        addInfoRow(card, gc, label, v);
    }

    private void addInfoRow(JPanel card, GridBagConstraints gc, String label, JLabel valueLabel) {
        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(VMSStyle.TEXT_MUTED);
        gc.gridx = 0;
        gc.weightx = 0;
        card.add(l, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        card.add(valueLabel, gc);
        gc.fill = GridBagConstraints.NONE;
        gc.gridy++;
    }

    private JPanel kpiCard(String label, JLabel valueLbl, Color accent) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(accent);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(12, 16, 12, 14));

        JLabel l = new JLabel(label.toUpperCase());
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(VMSStyle.TEXT_MUTED);
        valueLbl.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLbl.setForeground(accent);

        p.add(l, BorderLayout.NORTH);
        p.add(valueLbl, BorderLayout.CENTER);
        return p;
    }

    private JPanel cardPanel() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));
        return p;
    }

    // ── Onglet Historique demandes ───────────────────────────────────────────
    private JPanel buildDemandesTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(VMSStyle.BG_CARD);
        tab.setBorder(new EmptyBorder(16, 22, 16, 22));

        String[] cols = {"Facture", "R\u00e9f\u00e9rence", "Bons", "Montant", "Statut", "Date"};
        demandesModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(demandesModel);
        styleTable(table);
        // Statut renderer
        table.getColumnModel().getColumn(4).setCellRenderer(new StatutRenderer());
        // Montant renderer
        table.getColumnModel().getColumn(3).setCellRenderer(new AmountRenderer());
        table.getColumnModel().getColumn(2).setMaxWidth(60);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        tab.add(sp, BorderLayout.CENTER);
        return tab;
    }

    // ── Onglet Bons ──────────────────────────────────────────────────────────
    private JPanel buildBonsTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(VMSStyle.BG_CARD);
        tab.setBorder(new EmptyBorder(16, 22, 16, 22));

        String[] cols = {"Code", "Facture", "Valeur", "Statut", "\u00c9mission", "Expiration"};
        bonsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(bonsModel);
        styleTable(table);
        table.getColumnModel().getColumn(3).setCellRenderer(new BonStatutRenderer());
        table.getColumnModel().getColumn(2).setCellRenderer(new AmountRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        tab.add(sp, BorderLayout.CENTER);
        return tab;
    }

    // ── Onglet Timeline ──────────────────────────────────────────────────────
    private JPanel buildTimelineTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(VMSStyle.BG_CARD);
        tab.setBorder(new EmptyBorder(16, 22, 16, 22));

        timelinePanel = new JPanel();
        timelinePanel.setLayout(new BoxLayout(timelinePanel, BoxLayout.Y_AXIS));
        timelinePanel.setOpaque(false);

        JScrollPane sp = new JScrollPane(timelinePanel);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        tab.add(sp, BorderLayout.CENTER);
        return tab;
    }

    private void styleTable(JTable table) {
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.setSelectionForeground(VMSStyle.TEXT_PRIMARY);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setForeground(VMSStyle.TEXT_SECONDARY);
        table.getTableHeader().setBackground(VMSStyle.BG_SUBTLE);
        table.getTableHeader().setReorderingAllowed(false);
    }

    // ── Chargement asynchrone ────────────────────────────────────────────────
    private void loadAsync() {
        new SwingWorker<Void, Void>() {
            VoucherDAO.ClientStats stats;
            List<VoucherDAO.DemandeRow> demandes;
            List<VoucherDAO.BonRow> bons;
            List<VoucherDAO.TimelineEvent> events;
            String error;

            @Override protected Void doInBackground() {
                try {
                    stats    = VoucherDAO.getClientStats(client.getClientId());
                    demandes = VoucherDAO.getDemandesByClient(client.getClientId());
                    bons     = VoucherDAO.getBonsByClient(client.getClientId());
                    events   = VoucherDAO.getTimelineByClient(client.getClientId(), 50);
                } catch (SQLException ex) {
                    error = ex.getMessage();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    ToastManager.error(FicheClient360.this, "Erreur chargement : " + error);
                    return;
                }
                // KPIs
                lblTotalDemandes.setText(String.valueOf(stats.totalDemandes));
                lblMontantTotal.setText(NF_MUR.format(Math.round(stats.montantTotal)) + " Rs");
                lblBonsActifs.setText(String.valueOf(stats.bonsActifs));
                lblBonsUtilises.setText(String.valueOf(stats.bonsUtilises));
                lblDerniereActivite.setText(
                        stats.derniereActivite != null ? DF_DT.format(stats.derniereActivite) : "Aucune");

                // Demandes
                for (VoucherDAO.DemandeRow r : demandes) {
                    demandesModel.addRow(new Object[]{
                            r.invoiceReference != null ? r.invoiceReference : "\u2013",
                            r.reference != null ? r.reference : "\u2013",
                            r.nombreBons,
                            r.montant,
                            r.statut,
                            r.dateCreation != null ? DF_DATE.format(r.dateCreation) : "\u2013"
                    });
                }
                // Bons
                for (VoucherDAO.BonRow r : bons) {
                    bonsModel.addRow(new Object[]{
                            truncate(r.code, 20),
                            r.demandeRef != null ? r.demandeRef : "\u2013",
                            r.valeur,
                            r.statut,
                            r.dateEmission != null ? DF_DATE.format(r.dateEmission) : "\u2013",
                            r.dateExpiration != null ? DF_DATE.format(r.dateExpiration) : "\u2013"
                    });
                }
                // Timeline
                rebuildTimeline(events);
            }
        }.execute();
    }

    private void rebuildTimeline(List<VoucherDAO.TimelineEvent> events) {
        timelinePanel.removeAll();
        if (events == null || events.isEmpty()) {
            JLabel empty = new JLabel("Aucune activit\u00e9 pour ce client", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(VMSStyle.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(40, 10, 40, 10));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            timelinePanel.add(empty);
        } else {
            for (VoucherDAO.TimelineEvent e : events) {
                timelinePanel.add(buildTimelineRow(e));
                timelinePanel.add(Box.createVerticalStrut(4));
            }
        }
        timelinePanel.revalidate();
        timelinePanel.repaint();
    }

    private JPanel buildTimelineRow(VoucherDAO.TimelineEvent e) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dot = new JLabel(iconForAction(e.action));
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        dot.setPreferredSize(new Dimension(32, 32));
        dot.setHorizontalAlignment(SwingConstants.CENTER);
        dot.setForeground(colorForAction(e.action));
        row.add(dot, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        String title = humanAction(e.action) + (e.username != null ? "  \u2022  " + e.username : "");
        JLabel tl = new JLabel(title);
        tl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tl.setForeground(VMSStyle.TEXT_PRIMARY);
        tl.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(tl);
        if (e.contexte != null && !e.contexte.isEmpty()) {
            JLabel c = new JLabel(truncate(e.contexte, 110));
            c.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            c.setForeground(VMSStyle.TEXT_MUTED);
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            mid.add(c);
        }
        row.add(mid, BorderLayout.CENTER);

        JLabel when = new JLabel(e.dateEvt != null ? DF_DT.format(e.dateEvt) : "");
        when.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        when.setForeground(VMSStyle.TEXT_MUTED);
        row.add(when, BorderLayout.EAST);
        return row;
    }

    private String iconForAction(String a) {
        if (a == null) return "\u2022";
        return switch (a) {
            case "CREATION"          -> "\u2795";
            case "MODIFICATION"      -> "\u270F\ufe0f";
            case "PAIEMENT"          -> "\uD83D\uDCB0";
            case "APPROBATION"       -> "\u2705";
            case "GENERATION"        -> "\uD83C\uDFAB";
            case "ENVOI"             -> "\uD83D\uDCE7";
            case "REDEMPTION","UTILISATION_BON" -> "\uD83D\uDCB3";
            case "REJET"             -> "\u274C";
            case "ANNULATION"        -> "\u26D4";
            case "CHANGEMENT_STATUT" -> "\uD83D\uDD04";
            default                   -> "\u2022";
        };
    }

    private Color colorForAction(String a) {
        if (a == null) return VMSStyle.TEXT_MUTED;
        return switch (a) {
            case "CREATION","APPROBATION","GENERATION" -> VMSStyle.SUCCESS;
            case "REJET","ANNULATION"                  -> VMSStyle.RED_PRIMARY;
            case "PAIEMENT","ENVOI"                    -> VMSStyle.ACCENT_BLUE;
            case "REDEMPTION","UTILISATION_BON"        -> VMSStyle.WARNING;
            default                                     -> VMSStyle.TEXT_SECONDARY;
        };
    }

    private String humanAction(String a) {
        if (a == null) return "\u00c9v\u00e9nement";
        return switch (a) {
            case "CREATION"          -> "Cr\u00e9ation";
            case "MODIFICATION"      -> "Modification";
            case "PAIEMENT"          -> "Paiement";
            case "APPROBATION"       -> "Approbation";
            case "GENERATION"        -> "G\u00e9n\u00e9ration des bons";
            case "ENVOI"             -> "Envoi par email";
            case "REDEMPTION"        -> "R\u00e9demption";
            case "UTILISATION_BON"   -> "Bon utilis\u00e9";
            case "REJET"             -> "Rejet";
            case "ANNULATION"        -> "Annulation";
            case "CHANGEMENT_STATUT" -> "Changement de statut";
            default                   -> a;
        };
    }

    // ── Renderers ────────────────────────────────────────────────────────────
    private static class StatutRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            String s = v == null ? "" : v.toString();
            Color bg;
            Color fg = Color.WHITE;
            switch (s) {
                case "PAYE","APPROUVE"       -> bg = VMSStyle.ACCENT_BLUE;
                case "GENERE"                -> bg = VMSStyle.SUCCESS;
                case "ENVOYE"                -> bg = new Color(13, 148, 136);
                case "REJETE","ANNULE"       -> bg = VMSStyle.RED_PRIMARY;
                case "ARCHIVE"               -> { bg = new Color(209, 213, 219); fg = VMSStyle.TEXT_PRIMARY; }
                case "EN_ATTENTE_PAIEMENT"   -> bg = VMSStyle.WARNING;
                default                       -> { bg = VMSStyle.BG_SUBTLE; fg = VMSStyle.TEXT_SECONDARY; }
            }
            if (sel) { bg = VMSStyle.RED_PRIMARY; fg = Color.WHITE; }
            setBackground(bg); setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            return c;
        }
    }

    private static class BonStatutRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            String s = v == null ? "" : v.toString();
            Color bg;
            Color fg = Color.WHITE;
            switch (s) {
                case "ACTIF"   -> bg = VMSStyle.SUCCESS;
                case "REDIME"  -> bg = VMSStyle.ACCENT_BLUE;
                case "EXPIRE"  -> { bg = new Color(209, 213, 219); fg = VMSStyle.TEXT_PRIMARY; }
                case "ANNULE"  -> bg = VMSStyle.RED_PRIMARY;
                default         -> { bg = VMSStyle.BG_SUBTLE; fg = VMSStyle.TEXT_SECONDARY; }
            }
            if (sel) { bg = VMSStyle.RED_PRIMARY; fg = Color.WHITE; }
            setBackground(bg); setForeground(fg);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            return c;
        }
    }

    private static class AmountRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(RIGHT);
            if (v instanceof Number n) {
                setText(NF_MUR.format(Math.round(n.doubleValue())) + " Rs");
            }
            return c;
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────
    private void installEscape() {
        JRootPane rp = getRootPane();
        InputMap im = rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rp.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private static String orDash(String s) { return (s == null || s.isEmpty()) ? "\u2013" : s; }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "\u2026";
    }

    // ── API publique ─────────────────────────────────────────────────────────
    public static void show(Component owner, Client client) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        FicheClient360 fc = new FicheClient360(w, client);
        fc.setVisible(true);
    }
}
