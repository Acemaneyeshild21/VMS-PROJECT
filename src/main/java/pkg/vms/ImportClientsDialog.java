package pkg.vms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Import CSV de clients — parse le fichier, affiche un apercu avec validation
 * (doublons, emails invalides), puis ins\u00e8re les lignes valid\u00e9es.
 *
 * Format CSV attendu : name;email;contact_number;company
 * S\u00e9parateur : \u003b ou \u002c ou tab. UTF-8 BOM accept\u00e9. En-t\u00eate optionnelle.
 */
public class ImportClientsDialog extends JDialog {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static class Row {
        String name, email, contact, company;
        String erreur;     // null = OK
        boolean import_ = true;
    }

    private final ClientManager manager;
    private final DefaultTableModel model;
    private final JTable table;
    private final JLabel lblStats;
    private final JButton btnImport;
    private final List<Row> rows = new ArrayList<>();
    private final Runnable onImported;

    public ImportClientsDialog(Window owner, ClientManager manager, Runnable onImported) {
        super(owner, "Importer des clients depuis CSV", ModalityType.APPLICATION_MODAL);
        this.manager = manager;
        this.onImported = onImported;

        setSize(880, 560);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(VMSStyle.BG_ROOT);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(VMSStyle.BG_CARD);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, VMSStyle.BORDER_LIGHT),
                new EmptyBorder(16, 22, 16, 22)));
        JLabel title = new JLabel("\uD83D\uDCE5  Import de clients");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(VMSStyle.TEXT_PRIMARY);
        JLabel sub = new JLabel("Format attendu : name;email;contact_number;company (en-t\u00eate optionnelle)");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(VMSStyle.TEXT_MUTED);
        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(sub);
        header.add(titleBox, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // Toolbar actions
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        JButton btnChoose = UIUtils.buildPrimaryButton("Choisir un fichier CSV\u2026", 200, 36);
        btnChoose.addActionListener(e -> chooseFile());
        JButton btnExample = UIUtils.buildGhostButton("Exemple", 100, 36);
        btnExample.addActionListener(e -> showExample());
        toolbar.add(btnChoose);
        toolbar.add(btnExample);
        content.add(toolbar, BorderLayout.NORTH);

        // Table
        String[] cols = {"Importer", "Nom", "Email", "T\u00e9l", "Entreprise", "Statut"};
        model = new DefaultTableModel(cols, 0) {
            @Override public Class<?> getColumnClass(int c) {
                return c == 0 ? Boolean.class : String.class;
            }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        model.addTableModelListener(ev -> {
            if (ev.getColumn() == 0) {
                int idx = ev.getFirstRow();
                if (idx >= 0 && idx < rows.size()) {
                    Boolean val = (Boolean) model.getValueAt(idx, 0);
                    rows.get(idx).import_ = val != null && val;
                    updateStats();
                }
            }
        });
        table = new JTable(model);
        table.setRowHeight(30);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setGridColor(VMSStyle.BORDER_SOFT);
        table.setSelectionBackground(VMSStyle.RED_LIGHT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        table.getTableHeader().setBackground(VMSStyle.BG_SUBTLE);
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(0).setMaxWidth(80);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusRenderer());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(VMSStyle.BORDER_LIGHT));
        sp.getViewport().setBackground(VMSStyle.BG_CARD);
        content.add(sp, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        lblStats = new JLabel("Aucun fichier charg\u00e9");
        lblStats.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblStats.setForeground(VMSStyle.TEXT_SECONDARY);
        footer.add(lblStats, BorderLayout.WEST);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton btnCancel = UIUtils.buildGhostButton("Fermer", 100, 36);
        btnCancel.addActionListener(e -> dispose());
        btnImport = UIUtils.buildPrimaryButton("Importer", 140, 36);
        btnImport.setEnabled(false);
        btnImport.addActionListener(e -> doImport());
        actions.add(btnCancel);
        actions.add(btnImport);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);

        root.add(content, BorderLayout.CENTER);
        setContentPane(root);

        // Escape closes
        JRootPane rp = getRootPane();
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        rp.getActionMap().put("close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers CSV (*.csv, *.txt)", "csv", "txt"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        parseFile(fc.getSelectedFile());
    }

    private void showExample() {
        String example =
                "name;email;contact_number;company\n" +
                "Jean Dupont;jean.dupont@example.com;+230 5123 4567;Intermart Ltd\n" +
                "Marie Curie;marie@example.mu;5789 1234;Curie Boutique\n";
        JTextArea ta = new JTextArea(example);
        ta.setFont(new Font("Consolas", Font.PLAIN, 12));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(500, 160));
        JOptionPane.showMessageDialog(this, sp, "Exemple de fichier CSV", JOptionPane.INFORMATION_MESSAGE);
    }

    private void parseFile(File file) {
        rows.clear();
        model.setRowCount(0);
        Set<String> emailsFichier = new HashSet<>();
        Set<String> emailsExistants = loadExistingEmails();

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader br = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) {
                    line = line.replace("\uFEFF", ""); // strip BOM
                    first = false;
                    if (looksLikeHeader(line)) continue;
                }
                if (line.trim().isEmpty()) continue;

                String[] parts = splitCsv(line);
                Row r = new Row();
                r.name    = safeGet(parts, 0);
                r.email   = safeGet(parts, 1);
                r.contact = safeGet(parts, 2);
                r.company = safeGet(parts, 3);

                // Validation
                if (r.name.isEmpty()) { r.erreur = "Nom manquant"; r.import_ = false; }
                else if (r.email.isEmpty() || !EMAIL.matcher(r.email).matches()) { r.erreur = "Email invalide"; r.import_ = false; }
                else if (emailsExistants.contains(r.email.toLowerCase())) { r.erreur = "Doublon (existe d\u00e9j\u00e0)"; r.import_ = false; }
                else if (!emailsFichier.add(r.email.toLowerCase())) { r.erreur = "Doublon dans le fichier"; r.import_ = false; }

                rows.add(r);
                model.addRow(new Object[]{
                        r.import_, r.name, r.email, r.contact, r.company,
                        r.erreur == null ? "OK" : r.erreur
                });
            }
        } catch (IOException ex) {
            ToastManager.error(this, "Erreur lecture CSV : " + ex.getMessage());
            return;
        }

        updateStats();
        btnImport.setEnabled(countImportables() > 0);
    }

    private Set<String> loadExistingEmails() {
        Set<String> out = new HashSet<>();
        for (Client c : manager.obtenirTousLesClients()) {
            if (c.getEmail() != null) out.add(c.getEmail().toLowerCase());
        }
        return out;
    }

    private boolean looksLikeHeader(String line) {
        String l = line.toLowerCase();
        return l.contains("name") || l.contains("nom") || l.contains("email");
    }

    private String[] splitCsv(String line) {
        // Detect separator: ; , or tab
        char sep;
        if (line.contains(";")) sep = ';';
        else if (line.contains("\t")) sep = '\t';
        else sep = ',';
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') { inQuotes = !inQuotes; }
            else if (ch == sep && !inQuotes) { out.add(cur.toString().trim()); cur.setLength(0); }
            else cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out.toArray(new String[0]);
    }

    private String safeGet(String[] arr, int idx) {
        if (idx >= arr.length) return "";
        String s = arr[idx] == null ? "" : arr[idx].trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    private int countImportables() {
        int n = 0;
        for (Row r : rows) if (r.import_ && r.erreur == null) n++;
        return n;
    }

    private void updateStats() {
        int total = rows.size();
        int ok = countImportables();
        int ko = total - ok;
        lblStats.setText(String.format("%d ligne(s) lue(s) \u2014 \u2705 %d importables / \u26A0 %d en erreur", total, ok, ko));
        btnImport.setEnabled(ok > 0);
    }

    private void doImport() {
        int toImport = countImportables();
        if (toImport == 0) return;
        int ok = 0, fail = 0;
        for (Row r : rows) {
            if (!r.import_ || r.erreur != null) continue;
            Client c = new Client(r.name, r.email, r.contact, r.company);
            if (manager.ajouterClient(c)) ok++;
            else fail++;
        }
        String msg = ok + " client(s) import\u00e9(s)";
        if (fail > 0) msg += ", " + fail + " \u00e9chec(s)";
        if (ok > 0) ToastManager.info(this, msg);
        else ToastManager.warning(this, msg);
        if (onImported != null) onImported.run();
        dispose();
    }

    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            String s = v == null ? "" : v.toString();
            if ("OK".equals(s)) { setForeground(VMSStyle.SUCCESS); setFont(new Font("Segoe UI", Font.BOLD, 12)); }
            else { setForeground(VMSStyle.RED_PRIMARY); setFont(new Font("Segoe UI", Font.PLAIN, 12)); }
            if (sel) setForeground(Color.WHITE);
            return c;
        }
    }

    public static void show(Component owner, ClientManager manager, Runnable onImported) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        new ImportClientsDialog(w, manager, onImported).setVisible(true);
    }
}
