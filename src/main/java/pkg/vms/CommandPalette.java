package pkg.vms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Palette de commandes globale — Ctrl+K.
 * Dialog centré avec recherche rapide et exécution au clavier.
 */
public class CommandPalette extends JDialog {

    // ── Modèle de commande ──────────────────────────────────────────────────
    public static class Command {
        final String  id;
        final String  label;
        final String  subtitle;
        final String  icon;      // glyph unicode
        final String  keywords;  // mots-clés pour la recherche
        final String  shortcut;  // e.g. "Ctrl+N"
        final Runnable action;

        public Command(String id, String label, String subtitle, String icon,
                       String keywords, String shortcut, Runnable action) {
            this.id = id;
            this.label = label;
            this.subtitle = subtitle;
            this.icon = icon;
            this.keywords = keywords;
            this.shortcut = shortcut;
            this.action = action;
        }
    }

    private final List<Command> allCommands;
    private final List<Command> filtered = new ArrayList<>();
    private final JTextField search;
    private final JPanel      listPanel;
    private final JScrollPane scroll;
    private int selectedIdx = 0;

    private CommandPalette(Window owner, List<Command> commands) {
        super(owner, "", ModalityType.APPLICATION_MODAL);
        this.allCommands = commands;
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        JPanel root = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Ombre
                for (int i = 0; i < 6; i++) {
                    g2.setColor(new Color(0, 0, 0, 14 - i * 2));
                    g2.fill(new RoundRectangle2D.Double(i, i + 2, getWidth() - 2 * i, getHeight() - 2 * i, 16, 16));
                }
                g2.setColor(VMSStyle.BG_CARD);
                g2.fill(new RoundRectangle2D.Double(6, 6, getWidth() - 12, getHeight() - 12, 14, 14));
                g2.setColor(VMSStyle.BORDER_LIGHT);
                g2.draw(new RoundRectangle2D.Double(6, 6, getWidth() - 13, getHeight() - 13, 14, 14));
                g2.dispose();
            }
        };
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(18, 18, 18, 18));

        // ── Champ de recherche ─────────────────────────────────────────────
        JPanel top = new JPanel(new BorderLayout(10, 0));
        top.setOpaque(false);
        JLabel ic = new JLabel("\uD83D\uDD0D");
        ic.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        ic.setForeground(VMSStyle.TEXT_MUTED);
        top.add(ic, BorderLayout.WEST);

        search = new JTextField();
        search.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        search.setBorder(new EmptyBorder(6, 0, 6, 0));
        search.setForeground(VMSStyle.TEXT_PRIMARY);
        search.setBackground(VMSStyle.BG_CARD);
        search.putClientProperty("JTextField.placeholderText",
                "Tapez une commande ou recherchez\u2026");
        top.add(search, BorderLayout.CENTER);

        JLabel hint = new JLabel("esc");
        hint.setFont(new Font("Segoe UI", Font.BOLD, 10));
        hint.setForeground(VMSStyle.TEXT_MUTED);
        hint.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                new EmptyBorder(3, 7, 3, 7)));
        top.add(hint, BorderLayout.EAST);

        JPanel topWrap = new JPanel(new BorderLayout());
        topWrap.setOpaque(false);
        topWrap.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(0, 0, 10, 0)));
        topWrap.add(top, BorderLayout.CENTER);

        root.add(topWrap, BorderLayout.NORTH);

        // ── Liste des résultats ────────────────────────────────────────────
        listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(640, 360));
        root.add(scroll, BorderLayout.CENTER);

        // ── Footer hints ────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(8, 0, 0, 0)));
        footer.add(kbdHint("\u2191\u2193", "Naviguer"));
        footer.add(kbdHint("Enter", "Ex\u00e9cuter"));
        footer.add(kbdHint("Esc", "Fermer"));
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        setSize(680, 480);
        setLocationRelativeTo(owner);

        // ── Listeners clavier ───────────────────────────────────────────────
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e)  { refilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e)  { refilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refilter(); }
        });

        InputMap  im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        am.put("close", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { dispose(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        am.put("down", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { move(+1); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        am.put("up", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { move(-1); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "execute");
        am.put("execute", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { executeSelected(); }
        });

        refilter();
        SwingUtilities.invokeLater(search::requestFocusInWindow);
    }

    private JLabel kbdHint(String keys, String desc) {
        JLabel l = new JLabel(
                "<html><span style='color:#111827;font-weight:bold;'>" + keys +
                "</span>&nbsp;<span style='color:#9ca3af;'>" + desc + "</span></html>");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    private void move(int delta) {
        if (filtered.isEmpty()) return;
        selectedIdx = Math.max(0, Math.min(filtered.size() - 1, selectedIdx + delta));
        rebuildRows();
        // Scroll into view
        if (selectedIdx < listPanel.getComponentCount()) {
            Component c = listPanel.getComponent(selectedIdx);
            scroll.getViewport().scrollRectToVisible(c.getBounds());
        }
    }

    private void executeSelected() {
        if (selectedIdx >= 0 && selectedIdx < filtered.size()) {
            Command cmd = filtered.get(selectedIdx);
            dispose();
            SwingUtilities.invokeLater(cmd.action);
        }
    }

    private void refilter() {
        String q = search.getText().trim().toLowerCase();
        filtered.clear();
        for (Command c : allCommands) {
            if (q.isEmpty() || match(c, q)) filtered.add(c);
        }
        selectedIdx = 0;
        rebuildRows();
    }

    private boolean match(Command c, String q) {
        String hay = (c.label + " " + c.subtitle + " " + c.keywords).toLowerCase();
        // Simple substring + fuzzy (tous les caractères dans l'ordre)
        if (hay.contains(q)) return true;
        int i = 0;
        for (int k = 0; k < q.length(); k++) {
            char ch = q.charAt(k);
            int found = hay.indexOf(ch, i);
            if (found < 0) return false;
            i = found + 1;
        }
        return true;
    }

    private void rebuildRows() {
        listPanel.removeAll();
        if (filtered.isEmpty()) {
            JLabel empty = new JLabel("Aucune commande trouv\u00e9e", SwingConstants.CENTER);
            empty.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            empty.setForeground(VMSStyle.TEXT_MUTED);
            empty.setBorder(new EmptyBorder(40, 10, 40, 10));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
        } else {
            for (int i = 0; i < filtered.size(); i++) {
                listPanel.add(buildRow(filtered.get(i), i == selectedIdx, i));
                listPanel.add(Box.createVerticalStrut(2));
            }
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel buildRow(Command cmd, boolean selected, int idx) {
        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (selected) {
                    g2.setColor(VMSStyle.RED_LIGHT);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    g2.setColor(VMSStyle.RED_PRIMARY);
                    g2.fillRect(0, 0, 3, getHeight());
                }
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(9, 12, 9, 12));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(cmd.icon);
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        icon.setPreferredSize(new Dimension(28, 28));
        icon.setForeground(selected ? VMSStyle.RED_PRIMARY : VMSStyle.TEXT_SECONDARY);
        row.add(icon, BorderLayout.WEST);

        JPanel mid = new JPanel();
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setOpaque(false);
        JLabel title = new JLabel(cmd.label);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mid.add(title);
        if (cmd.subtitle != null && !cmd.subtitle.isEmpty()) {
            JLabel sub = new JLabel(cmd.subtitle);
            sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            sub.setForeground(VMSStyle.TEXT_MUTED);
            sub.setAlignmentX(Component.LEFT_ALIGNMENT);
            mid.add(sub);
        }
        row.add(mid, BorderLayout.CENTER);

        if (cmd.shortcut != null && !cmd.shortcut.isEmpty()) {
            JLabel sc = new JLabel(cmd.shortcut);
            sc.setFont(new Font("Segoe UI", Font.BOLD, 10));
            sc.setForeground(VMSStyle.TEXT_MUTED);
            sc.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT, 1, true),
                    new EmptyBorder(2, 6, 2, 6)));
            row.add(sc, BorderLayout.EAST);
        }

        row.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                selectedIdx = idx;
                rebuildRows();
            }
            @Override public void mouseClicked(MouseEvent e) {
                selectedIdx = idx;
                executeSelected();
            }
        });
        return row;
    }

    // ── API publique ─────────────────────────────────────────────────────────

    public static void show(Window owner, List<Command> commands) {
        CommandPalette cp = new CommandPalette(owner, commands);
        cp.setVisible(true);
    }

    /** Construit la liste standard de commandes pour le Dashboard. */
    public static List<Command> buildDefaultCommands(String role,
                                                     Consumer<String> onNavigate,
                                                     Runnable onRefresh,
                                                     Runnable onLogout,
                                                     Runnable onAuditLog,
                                                     Runnable onSessions,
                                                     Runnable onVerifBon,
                                                     Runnable onEmailHistory) {
        boolean isAdmin        = "Administrateur".equalsIgnoreCase(role);
        boolean isManager      = "Manager".equalsIgnoreCase(role);
        boolean isComptable    = "Comptable".equalsIgnoreCase(role);
        boolean isApprobateur  = "Approbateur".equalsIgnoreCase(role);
        boolean isSuperviseur  = "Superviseur_Magasin".equalsIgnoreCase(role);

        List<Command> cmds = new ArrayList<>();

        cmds.add(new Command("nav.home", "Aller \u00e0 l'Accueil",
                "Tableau de bord principal", "\uD83C\uDFE0",
                "accueil dashboard home tableau",
                "Ctrl+Home", () -> onNavigate.accept("Accueil")));

        cmds.add(new Command("nav.demandes", "Ouvrir Demandes",
                "Liste des demandes en cours", "\uD83D\uDCC4",
                "demandes requests en cours",
                null, () -> onNavigate.accept("Demandes")));

        cmds.add(new Command("nav.archives", "Ouvrir Archives",
                "Demandes cl\u00f4tur\u00e9es / rejet\u00e9es", "\uD83D\uDCE6",
                "archives historique closed",
                null, () -> onNavigate.accept("Archives")));

        cmds.add(new Command("nav.clients", "Gestion Clients",
                "Carnet d'adresses clients", "\uD83D\uDC65",
                "clients contacts crm",
                null, () -> onNavigate.accept("Clients")));

        if (isAdmin || isSuperviseur || isManager) {
            cmds.add(new Command("nav.redemption", "R\u00e9demption",
                    "Utilisation des bons en magasin", "\uD83D\uDCB3",
                    "redemption utilisation magasin",
                    null, () -> onNavigate.accept("R\u00e9demption")));
        }

        if (isAdmin || isManager || isComptable || isApprobateur) {
            cmds.add(new Command("nav.validation", "Validation",
                    "File d'attente de validation", "\u2705",
                    "validation approbation paiement",
                    null, () -> onNavigate.accept("Validation")));
        }

        if (isAdmin) {
            cmds.add(new Command("nav.parametres", "Param\u00e8tres",
                    "Configuration syst\u00e8me", "\u2699\ufe0f",
                    "parametres settings config admin",
                    null, () -> onNavigate.accept("Parametres")));
        }

        // Actions rapides
        if (isAdmin || isComptable) {
            cmds.add(new Command("action.new", "Nouvelle demande",
                    "Cr\u00e9er une demande de voucher", "\u2795",
                    "new nouvelle demande creer create",
                    "Ctrl+N", () -> onNavigate.accept("Nouvelle Demande")));
        }

        if (isAdmin && onAuditLog != null) {
            cmds.add(new Command("admin.audit", "Journal d'audit",
                    "Consulter tous les \u00e9v\u00e9nements syst\u00e8me", "\uD83D\uDD10",
                    "audit journal log tracabilite admin",
                    null, onAuditLog));
        }

        if (isAdmin && onSessions != null) {
            cmds.add(new Command("admin.sessions", "Sessions actives",
                    "Voir les utilisateurs connect\u00e9s r\u00e9cemment", "\uD83D\uDC65",
                    "sessions actives connexions utilisateurs admin",
                    null, onSessions));
        }

        if (isAdmin && onEmailHistory != null) {
            cmds.add(new Command("admin.emails", "Historique emails",
                    "Journal des envois SMTP (succ\u00e8s et \u00e9checs)", "\u2709\uFE0F",
                    "historique email smtp envoi echec log admin",
                    null, onEmailHistory));
        }

        if (onVerifBon != null) {
            cmds.add(new Command("action.verify", "V\u00e9rifier un bon",
                    "Contr\u00f4ler le statut d'un bon par son code", "\uD83D\uDD0D",
                    "verifier bon code qr scan valide utilise",
                    null, onVerifBon));
        }

        cmds.add(new Command("action.refresh", "Rafra\u00eechir",
                "Recharger les donn\u00e9es du tableau de bord", "\uD83D\uDD04",
                "refresh reload actualiser",
                "F5", onRefresh));

        cmds.add(new Command("action.logout", "D\u00e9connexion",
                "Se d\u00e9connecter de l'application", "\uD83D\uDEAA",
                "deconnexion logout sortir quitter",
                null, onLogout));

        return cmds;
    }
}
