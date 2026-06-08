package pkg.vms;

import pkg.vms.DAO.AuditDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Dialog listant les utilisateurs connect\u00e9s r\u00e9cemment, d\u00e9duits de l'audit_log
 * (action CONNEXION). Admin uniquement.
 */
public class SessionsActivesDialog extends JDialog {

    private static final SimpleDateFormat DF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH);

    private final DefaultTableModel model;
    private final JComboBox<String> cbFenetre;
    private final JLabel            lblCount;

    public SessionsActivesDialog(Window owner) {
        super(owner, "Sessions actives", ModalityType.APPLICATION_MODAL);
        setSize(760, 480);
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
        JLabel title = new JLabel("\uD83D\uDC65  Sessions actives");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Utilisateurs connect\u00e9s r\u00e9cemment (source : journal d'audit)");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        tb.add(title);
        tb.add(Box.createVerticalStrut(3));
        tb.add(sub);
        header.add(tb, BorderLayout.WEST);

        cbFenetre = new JComboBox<>(new String[]{"1 heure","4 heures","12 heures","24 heures","7 jours"});
        cbFenetre.setSelectedIndex(1);
        cbFenetre.setPreferredSize(new Dimension(130, 32));
        cbFenetre.addActionListener(e -> refresh());

        JButton btnRefresh = UIUtils.buildGhostButton("Actualiser", 120, 34);
        btnRefresh.addActionListener(e -> refresh());

        JPanel hRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        hRight.setOpaque(false);
        hRight.add(new JLabel("Fen\u00eatre :"));
        hRight.add(cbFenetre);
        hRight.add(btnRefresh);
        header.add(hRight, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // Table
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 22, 16, 22));

        String[] cols = {"Utilisateur", "Derni\u00e8re connexion", "Connexions", "\u00c9checs"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return (c == 2 || c == 3) ? Integer.class : String.class;
            }
        };
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(VMSStyle.BG_SUBTLE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(3).setCellRenderer(new EchecsRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        content.add(sp, BorderLayout.CENTER);

        lblCount = new JLabel("\u2014");
        lblCount.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblCount.setForeground(VMSStyle.TEXT_MUTED);
        JPanel fRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fRow.setOpaque(false);
        fRow.add(lblCount);
        content.add(fRow, BorderLayout.SOUTH);
        root.add(content, BorderLayout.CENTER);

        setContentPane(root);

        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        refresh();
    }

    private void refresh() {
        int heures = switch (cbFenetre.getSelectedIndex()) {
            case 0 -> 1;
            case 1 -> 4;
            case 2 -> 12;
            case 3 -> 24;
            case 4 -> 168;
            default -> 4;
        };
        new SwingWorker<List<AuditDAO.Session>, Void>() {
            @Override protected List<AuditDAO.Session> doInBackground() {
                return AuditDAO.getSessionsActives(heures);
            }
            @Override protected void done() {
                try {
                    List<AuditDAO.Session> sessions = get();
                    model.setRowCount(0);
                    for (AuditDAO.Session s : sessions) {
                        model.addRow(new Object[]{
                                s.username,
                                s.derniereConnexion != null ? DF.format(s.derniereConnexion) : "\u2013",
                                s.nbConnexions,
                                s.echecs
                        });
                    }
                    lblCount.setText(sessions.size() + " utilisateur(s) actif(s) sur les " +
                            (heures >= 24 ? (heures / 24) + " dernier(s) jour(s)" : heures + " derni\u00e8re(s) heure(s)"));
                } catch (Exception ex) {
                    ToastManager.error(SessionsActivesDialog.this, "Erreur : " + ex.getMessage());
                }
            }
        }.execute();
    }

    private static class EchecsRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            int n = (v instanceof Integer) ? (Integer) v : 0;
            if (n == 0) { setForeground(VMSStyle.TEXT_MUTED); setFont(new Font("Segoe UI", Font.PLAIN, 12)); }
            else { setForeground(VMSStyle.RED_PRIMARY); setFont(new Font("Segoe UI", Font.BOLD, 12)); }
            if (sel) setForeground(Color.WHITE);
            return c;
        }
    }

    public static void show(Component owner) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new SessionsActivesDialog(w).setVisible(true);
    }
}
