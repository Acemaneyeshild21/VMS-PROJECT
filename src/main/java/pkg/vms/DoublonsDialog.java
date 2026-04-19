package pkg.vms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * D\u00e9tection de doublons clients. Regroupe les clients par email identique
 * ou nom normalis\u00e9 identique. Permet de s\u00e9lectionner les doublons \u00e0 supprimer
 * (on garde toujours au moins un client par groupe).
 */
public class DoublonsDialog extends JDialog {

    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);

    private final ClientManager manager;
    private final Runnable      onChanged;
    private final JPanel        groupsPanel;
    private final JLabel        lblSummary;
    private final JButton       btnDelete;
    private final Map<Integer, JCheckBox> deleteChecks = new HashMap<>();

    public DoublonsDialog(Window owner, ClientManager manager, Runnable onChanged) {
        super(owner, "D\u00e9tection de doublons clients", ModalityType.APPLICATION_MODAL);
        this.manager   = manager;
        this.onChanged = onChanged;
        setSize(860, 600);
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
        JLabel title = new JLabel("\uD83D\uDD0E  D\u00e9tection de doublons");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Groupement par email identique ou nom normalis\u00e9. Cochez les entr\u00e9es \u00e0 supprimer.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        tb.add(title);
        tb.add(Box.createVerticalStrut(3));
        tb.add(sub);
        header.add(tb, BorderLayout.WEST);

        JButton btnRefresh = UIUtils.buildGhostButton("Relancer le scan", 150, 34);
        btnRefresh.addActionListener(e -> scan());
        JPanel hRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hRight.setOpaque(false);
        hRight.add(btnRefresh);
        header.add(hRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Scrollable groups
        groupsPanel = new JPanel();
        groupsPanel.setLayout(new BoxLayout(groupsPanel, BoxLayout.Y_AXIS));
        groupsPanel.setOpaque(false);
        groupsPanel.setBorder(new EmptyBorder(16, 22, 16, 22));
        JScrollPane sp = new JScrollPane(groupsPanel);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        root.add(sp, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(VMSStyle.BG_CARD);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(12, 22, 12, 22)));
        lblSummary = new JLabel("\u2014");
        lblSummary.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSummary.setForeground(VMSStyle.TEXT_SECONDARY);
        footer.add(lblSummary, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnClose = UIUtils.buildGhostButton("Fermer", 100, 34);
        btnClose.addActionListener(e -> dispose());
        btnDelete = UIUtils.buildPrimaryButton("Supprimer la s\u00e9lection", 200, 34);
        btnDelete.setEnabled(false);
        btnDelete.addActionListener(e -> doDelete());
        actions.add(btnClose);
        actions.add(btnDelete);
        footer.add(actions, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        scan();
    }

    private void scan() {
        groupsPanel.removeAll();
        deleteChecks.clear();
        lblSummary.setText("Analyse en cours\u2026");
        btnDelete.setEnabled(false);

        new SwingWorker<Map<String, List<Client>>, Void>() {
            @Override protected Map<String, List<Client>> doInBackground() {
                return findDuplicates(manager.obtenirTousLesClients());
            }
            @Override protected void done() {
                try {
                    Map<String, List<Client>> groups = get();
                    renderGroups(groups);
                } catch (Exception ex) {
                    ToastManager.error(DoublonsDialog.this, "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private Map<String, List<Client>> findDuplicates(List<Client> all) {
        // Key = "email:<val>" ou "nom:<val>"
        Map<String, List<Client>> byEmail = new LinkedHashMap<>();
        Map<String, List<Client>> byName  = new LinkedHashMap<>();

        for (Client c : all) {
            if (c.getEmail() != null && !c.getEmail().isEmpty()) {
                String k = "email:" + c.getEmail().trim().toLowerCase();
                byEmail.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
            }
            if (c.getName() != null && !c.getName().isEmpty()) {
                String norm = normalize(c.getName());
                if (!norm.isEmpty()) {
                    String k = "nom:" + norm;
                    byName.computeIfAbsent(k, x -> new ArrayList<>()).add(c);
                }
            }
        }

        Map<String, List<Client>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Client>> e : byEmail.entrySet()) {
            if (e.getValue().size() > 1) result.put(e.getKey(), e.getValue());
        }
        for (Map.Entry<String, List<Client>> e : byName.entrySet()) {
            if (e.getValue().size() > 1) {
                // Ne pas dupliquer un groupe d\u00e9j\u00e0 identifi\u00e9 par email
                boolean alreadyCovered = false;
                for (List<Client> g : result.values()) {
                    if (sameClients(g, e.getValue())) { alreadyCovered = true; break; }
                }
                if (!alreadyCovered) result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    private boolean sameClients(List<Client> a, List<Client> b) {
        if (a.size() != b.size()) return false;
        outer:
        for (Client ca : a) {
            for (Client cb : b) if (cb.getClientId() == ca.getClientId()) continue outer;
            return false;
        }
        return true;
    }

    private String normalize(String s) {
        if (s == null) return "";
        String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}", "");          // accents
        n = n.toLowerCase().replaceAll("[^a-z0-9]", "");
        return n;
    }

    private void renderGroups(Map<String, List<Client>> groups) {
        groupsPanel.removeAll();
        if (groups.isEmpty()) {
            JLabel empty = new JLabel("\u2705 Aucun doublon d\u00e9tect\u00e9");
            empty.setFont(new Font("Segoe UI", Font.BOLD, 14));
            empty.setForeground(VMSStyle.SUCCESS);
            empty.setBorder(new EmptyBorder(40, 10, 40, 10));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            groupsPanel.add(empty);
            lblSummary.setText("Base client saine");
        } else {
            int totalClients = 0;
            for (List<Client> g : groups.values()) totalClients += g.size();
            lblSummary.setText(groups.size() + " groupe(s) de doublons, " + totalClients + " clients concern\u00e9s");

            int idx = 1;
            for (Map.Entry<String, List<Client>> e : groups.entrySet()) {
                groupsPanel.add(buildGroupCard(idx++, e.getKey(), e.getValue()));
                groupsPanel.add(Box.createVerticalStrut(12));
            }
        }
        groupsPanel.revalidate();
        groupsPanel.repaint();
        updateSelectionCount();
    }

    private JPanel buildGroupCard(int idx, String key, List<Client> clients) {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.setColor(VMSStyle.WARNING);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        String type = key.startsWith("email:") ? "Email identique" : "Nom normalis\u00e9 identique";
        String val  = key.substring(key.indexOf(':') + 1);

        JLabel head = new JLabel("Groupe #" + idx + "  \u2022  " + type + "  \u2022  \"" + val + "\"");
        head.setFont(new Font("Segoe UI", Font.BOLD, 13));
        head.setForeground(VMSStyle.TEXT_PRIMARY);
        card.add(head, BorderLayout.NORTH);

        JPanel rows = new JPanel();
        rows.setOpaque(false);
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        for (Client c : clients) {
            rows.add(buildClientRow(c, clients.size()));
            rows.add(Box.createVerticalStrut(4));
        }
        card.add(rows, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildClientRow(Client c, int groupSize) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 6, 6, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox cb = new JCheckBox();
        cb.setOpaque(false);
        cb.setFont(new Font("Segoe UI", Font.BOLD, 11));
        cb.setText("Supprimer");
        cb.setForeground(VMSStyle.RED_PRIMARY);
        cb.addActionListener(e -> updateSelectionCount());
        deleteChecks.put(c.getClientId(), cb);
        row.add(cb, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        JLabel name = new JLabel("#" + c.getClientId() + "  \u2022  " + safe(c.getName()));
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        name.setForeground(VMSStyle.TEXT_PRIMARY);
        name.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel detail = new JLabel(safe(c.getEmail()) + "  \u2022  " + safe(c.getContactNumber()) +
                "  \u2022  " + safe(c.getCompany()));
        detail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        detail.setForeground(VMSStyle.TEXT_MUTED);
        detail.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(name);
        mid.add(detail);
        row.add(mid, BorderLayout.CENTER);

        JLabel date = new JLabel(c.getDateCreation() != null ? "Cr\u00e9\u00e9 le " + DF.format(c.getDateCreation()) : "");
        date.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        date.setForeground(VMSStyle.TEXT_MUTED);
        row.add(date, BorderLayout.EAST);
        return row;
    }

    private void updateSelectionCount() {
        int selected = 0;
        for (JCheckBox cb : deleteChecks.values()) if (cb.isSelected()) selected++;
        btnDelete.setText(selected > 0 ? "Supprimer (" + selected + ")" : "Supprimer la s\u00e9lection");
        btnDelete.setEnabled(selected > 0);
    }

    private void doDelete() {
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> e : deleteChecks.entrySet()) {
            if (e.getValue().isSelected()) ids.add(e.getKey());
        }
        if (ids.isEmpty()) return;

        if (!UIUtils.confirmDialog(SwingUtilities.getWindowAncestor(this),
                "Supprimer " + ids.size() + " client(s) ?",
                "Cette action va d\u00e9sactiver les clients s\u00e9lectionn\u00e9s. Les demandes associ\u00e9es sont conserv\u00e9es.",
                "Supprimer", "Annuler")) return;

        int ok = 0, fail = 0;
        for (int id : ids) {
            if (manager.supprimerClient(id)) ok++;
            else fail++;
        }
        String msg = ok + " client(s) supprim\u00e9(s)";
        if (fail > 0) msg += ", " + fail + " \u00e9chec(s)";
        if (ok > 0) ToastManager.info(this, msg);
        else ToastManager.warning(this, msg);
        if (onChanged != null) onChanged.run();
        scan();
    }

    private static String safe(String s) { return (s == null || s.isEmpty()) ? "\u2013" : s; }

    public static void show(Component owner, ClientManager manager, Runnable onChanged) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new DoublonsDialog(w, manager, onChanged).setVisible(true);
    }
}
