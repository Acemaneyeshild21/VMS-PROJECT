package pkg.vms;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.DAO.DBconnect;
import pkg.vms.DAO.StatistiquesDAO;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service de génération de rapports Excel multi-feuilles professionnels.
 *
 * <h3>Rapports disponibles</h3>
 * <ul>
 *   <li>{@link #genererRapportComplet(String)} — Workbook complet 5 feuilles</li>
 *   <li>{@link #genererRapportDemandes(String)} — Feuille unique "Demandes"</li>
 *   <li>{@link #genererRapportBons(String)} — Feuille unique "Bons"</li>
 *   <li>{@link #genererRapportClients(String)} — Feuille unique "Clients"</li>
 *   <li>{@link #genererRapportExpiration(String, int)} — Bons proches d'expiration</li>
 * </ul>
 *
 * <h3>Feuilles du rapport complet</h3>
 * <ol>
 *   <li>📊 Tableau de bord — métriques clés, top clients, résumé financier</li>
 *   <li>📋 Demandes — toutes les demandes avec statut, montant, dates</li>
 *   <li>🎟 Bons — détail de chaque bon (code, valeur, statut, expiration)</li>
 *   <li>👥 Clients — liste clients avec cumul demandes/montants</li>
 *   <li>🔌 Config ODBC — guide complet pour connexion Excel → PostgreSQL</li>
 * </ol>
 *
 * <h3>ODBC (rapports connectés)</h3>
 * La feuille "Config ODBC" fournit la chaîne de connexion, les étapes
 * d'installation du driver psqlODBC, et les requêtes SQL pour connecter
 * Excel directement à la base PostgreSQL via Données → À partir d'autres sources.
 *
 * <p>Branding VMS : header navy {@code #000099}, alternance de lignes gris clair.</p>
 */
public class RapportExcelService {

    // ── Couleurs branding VMS ──────────────────────────────────────────────
    private static final byte[] COLOR_NAVY    = hexToRgb("#000099");
    private static final byte[] COLOR_RED     = hexToRgb("#CC0000");
    private static final byte[] COLOR_LIGHT   = hexToRgb("#EEF0F8");
    private static final byte[] COLOR_WHITE   = hexToRgb("#FFFFFF");
    private static final byte[] COLOR_GREEN   = hexToRgb("#166534");
    private static final byte[] COLOR_ORANGE  = hexToRgb("#B45309");
    private static final byte[] COLOR_GRAY    = hexToRgb("#6B7280");
    private static final byte[] COLOR_ALTROW  = hexToRgb("#F8F9FF");

    private static final String SOCIETE  = "VoucherManager VMS";
    private static final String SYSTEME  = "VMS — Voucher Management System";
    private static final String DATE_FMT = "dd/MM/yyyy HH:mm";
    private static final String DATE_ONLY = "dd/MM/yyyy";

    // ── Config DB (lue depuis config.properties) ───────────────────────────
    private static String dbHost     = "localhost";
    private static String dbPort     = "5432";
    private static String dbName     = "VMS";
    private static String dbUser     = "postgres";

    static {
        System.setProperty("java.awt.headless", "true");
        try (InputStream is = RapportExcelService.class
                .getClassLoader().getResourceAsStream("config.properties")) {
            if (is != null) {
                Properties p = new Properties();
                p.load(is);
                String url = p.getProperty("db.url", "jdbc:postgresql://localhost:5432/VMS");
                // parse jdbc:postgresql://HOST:PORT/DB
                String[] parts = url.replace("jdbc:postgresql://", "").split("/");
                if (parts.length >= 2) {
                    String[] hostPort = parts[0].split(":");
                    dbHost = hostPort[0];
                    dbPort = hostPort.length > 1 ? hostPort[1] : "5432";
                    dbName = parts[1];
                }
                dbUser = p.getProperty("db.user", "postgres");
            }
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RAPPORT COMPLET (5 feuilles)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Génère le rapport complet VMS (5 feuilles) au chemin indiqué.
     * Toutes les données sont récupérées depuis la base PostgreSQL.
     *
     * @param filePath chemin absolu du fichier .xlsx à créer
     * @throws Exception en cas d'erreur I/O ou SQL
     */
    public static void genererRapportComplet(String filePath) throws Exception {
        System.setProperty("java.awt.headless", "true");
        try (XSSFWorkbook wb = new XSSFWorkbook()) {

            Styles s = new Styles(wb);

            // Feuille 1 — Tableau de bord
            buildSheetDashboard(wb, s);

            // Feuille 2 — Demandes
            buildSheetDemandes(wb, s);

            // Feuille 3 — Bons
            buildSheetBons(wb, s);

            // Feuille 4 — Clients
            buildSheetClients(wb, s);

            // Feuille 5 — Config ODBC
            buildSheetOdbc(wb, s);

            // Activer la première feuille
            wb.setActiveSheet(0);

            writeWorkbook(wb, filePath);
        }
    }

    // ── Rapport partiel : Demandes ─────────────────────────────────────────
    public static void genererRapportDemandes(String filePath) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            buildSheetDemandes(wb, s);
            writeWorkbook(wb, filePath);
        }
    }

    // ── Rapport partiel : Bons ─────────────────────────────────────────────
    public static void genererRapportBons(String filePath) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            buildSheetBons(wb, s);
            writeWorkbook(wb, filePath);
        }
    }

    // ── Rapport partiel : Clients ──────────────────────────────────────────
    public static void genererRapportClients(String filePath) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            buildSheetClients(wb, s);
            writeWorkbook(wb, filePath);
        }
    }

    // ── Rapport partiel : Bons proches d'expiration ────────────────────────
    public static void genererRapportExpiration(String filePath, int joursMax) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Styles s = new Styles(wb);
            buildSheetExpiration(wb, s, joursMax);
            writeWorkbook(wb, filePath);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE 1 — TABLEAU DE BORD
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetDashboard(XSSFWorkbook wb, Styles s) throws SQLException {
        XSSFSheet sheet = wb.createSheet("📊 Tableau de bord");
        sheet.setColumnWidth(0, 35 * 256);
        sheet.setColumnWidth(1, 25 * 256);
        sheet.setColumnWidth(2, 25 * 256);
        sheet.setColumnWidth(3, 25 * 256);

        int row = 0;

        // ── Titre ──────────────────────────────────────────────────────────
        row = writeSectionTitle(sheet, s, row, SYSTEME, 4);
        row = writeSectionTitle(sheet, s, row, SOCIETE + " — Rapport de gestion des bons cadeau", 4);
        writeMetaRow(sheet, s, row++, "Généré le",
                new SimpleDateFormat(DATE_FMT).format(new Date()), 4);
        row++; // espace

        // ── Métriques clés ─────────────────────────────────────────────────
        row = writeHeaderRow(sheet, s, row, new String[]{
            "Indicateur", "Valeur", "Unité", "Période"
        });

        try (Connection conn = DBconnect.getConnection()) {
            // Demandes totales
            appendKpi(sheet, s, row++, conn,
                "Demandes totales",
                "SELECT COUNT(*) FROM demande", "demandes", "Toutes périodes");
            appendKpi(sheet, s, row++, conn,
                "Demandes en attente",
                "SELECT COUNT(*) FROM demande WHERE statuts IN ('DEMANDE','PAYE','APPROUVE')", "demandes", "En cours");
            appendKpi(sheet, s, row++, conn,
                "Bons générés",
                "SELECT COUNT(*) FROM bon", "bons", "Toutes périodes");
            appendKpi(sheet, s, row++, conn,
                "Bons actifs",
                "SELECT COUNT(*) FROM bon WHERE statut = 'ACTIF'", "bons", "Aujourd'hui");
            appendKpi(sheet, s, row++, conn,
                "Bons rédimés",
                "SELECT COUNT(*) FROM bon WHERE statut = 'REDIME'", "bons", "Toutes périodes");
            appendKpi(sheet, s, row++, conn,
                "Montant total émis (Rs)",
                "SELECT COALESCE(SUM(valeur), 0) FROM bon", "Rs", "Toutes périodes");
            appendKpi(sheet, s, row++, conn,
                "Montant rédimé (Rs)",
                "SELECT COALESCE(SUM(b.valeur), 0) FROM bon b WHERE b.statut = 'REDIME'", "Rs", "Toutes périodes");
            appendKpi(sheet, s, row++, conn,
                "Clients actifs",
                "SELECT COUNT(*) FROM client WHERE actif = TRUE", "clients", "Aujourd'hui");
            appendKpi(sheet, s, row++, conn,
                "Bons expirant dans 30j",
                "SELECT COUNT(*) FROM bon WHERE statut='ACTIF' AND date_expiration BETWEEN NOW() AND NOW() + INTERVAL '30 days'",
                "bons", "Prochains 30 jours");
        }
        row++;

        // ── Top 5 clients ──────────────────────────────────────────────────
        row = writeSectionHeader(sheet, s, row, "Top 5 Clients par volume de demandes", 4);
        row = writeHeaderRow(sheet, s, row, new String[]{
            "Client", "Email", "Nb Demandes", "Montant Total (Rs)"
        });

        String topClientSql = """
            SELECT c.name, c.email, COUNT(d.demande_id) AS nb,
                   COALESCE(SUM(d.montant_total), 0) AS total
            FROM client c
            LEFT JOIN demande d ON d.clientid = c.clientid
            GROUP BY c.clientid, c.name, c.email
            ORDER BY nb DESC
            LIMIT 5
            """;
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(topClientSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Row r = sheet.createRow(row++);
                cell(r, 0, s.data).setCellValue(rs.getString("name"));
                cell(r, 1, s.data).setCellValue(rs.getString("email"));
                cell(r, 2, s.dataNum).setCellValue(rs.getInt("nb"));
                cell(r, 3, s.dataCurrency).setCellValue(rs.getDouble("total"));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE 2 — DEMANDES
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetDemandes(XSSFWorkbook wb, Styles s) throws SQLException {
        XSSFSheet sheet = wb.createSheet("📋 Demandes");
        int[] widths = {14, 14, 25, 20, 14, 14, 14, 16, 16, 18, 18};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int row = 0;
        row = writeSectionTitle(sheet, s, row, "Rapport Demandes — " + SOCIETE, widths.length);
        writeMetaRow(sheet, s, row++, "Généré le",
                new SimpleDateFormat(DATE_FMT).format(new Date()), widths.length);
        row++;

        row = writeHeaderRow(sheet, s, row, new String[]{
            "ID", "Référence", "Client", "Email Client",
            "Nb Bons", "Valeur Unit. (Rs)", "Montant Total (Rs)",
            "Statut", "Type Bon", "Date Création", "Date Modification"
        });

        String sql = """
            SELECT d.demande_id, d.reference, c.name, c.email,
                   d.nombre_bons, d.valeur_unitaire, d.montant_total,
                   d.statuts, d.type_bon,
                   d.date_creation, d.date_modification
            FROM demande d
            JOIN client c ON d.clientid = c.clientid
            ORDER BY d.date_creation DESC
            """;

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CellStyle rowStyle = (row % 2 == 0) ? s.data : s.dataAlt;
                CellStyle numStyle = (row % 2 == 0) ? s.dataNum : s.dataNumAlt;
                CellStyle curStyle = (row % 2 == 0) ? s.dataCurrency : s.dataCurrencyAlt;
                Row r = sheet.createRow(row++);

                cell(r, 0, numStyle).setCellValue(rs.getInt("demande_id"));
                cell(r, 1, rowStyle).setCellValue(str(rs.getString("reference")));
                cell(r, 2, rowStyle).setCellValue(str(rs.getString("name")));
                cell(r, 3, rowStyle).setCellValue(str(rs.getString("email")));
                cell(r, 4, numStyle).setCellValue(rs.getInt("nombre_bons"));
                cell(r, 5, curStyle).setCellValue(rs.getDouble("valeur_unitaire"));
                cell(r, 6, curStyle).setCellValue(rs.getDouble("montant_total"));
                cell(r, 7, statutStyle(wb, s, rs.getString("statuts"))).setCellValue(str(rs.getString("statuts")));
                cell(r, 8, rowStyle).setCellValue(str(rs.getString("type_bon")));
                var dc = rs.getTimestamp("date_creation");
                cell(r, 9, rowStyle).setCellValue(dc != null ? sdf.format(dc) : "");
                var dm = rs.getTimestamp("date_modification");
                cell(r, 10, rowStyle).setCellValue(dm != null ? sdf.format(dm) : "");
            }
        }

        // Filtre automatique sur les en-têtes
        sheet.setAutoFilter(new CellRangeAddress(3, row - 1, 0, widths.length - 1));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE 3 — BONS
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetBons(XSSFWorkbook wb, Styles s) throws SQLException {
        XSSFSheet sheet = wb.createSheet("🎟 Bons");
        int[] widths = {12, 28, 14, 14, 14, 18, 18, 14, 25};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int row = 0;
        row = writeSectionTitle(sheet, s, row, "Rapport Bons Cadeau — " + SOCIETE, widths.length);
        writeMetaRow(sheet, s, row++, "Généré le",
                new SimpleDateFormat(DATE_FMT).format(new Date()), widths.length);
        row++;

        row = writeHeaderRow(sheet, s, row, new String[]{
            "ID Bon", "Code Unique", "Valeur (Rs)", "Statut",
            "Demande ID", "Date Génération", "Date Expiration",
            "Rédimé le", "Magasin Rédemption"
        });

        String sql = """
            SELECT b.bon_id, b.code_unique, b.valeur, b.statut,
                   b.demande_id, b.date_emission, b.date_expiration,
                   r.date_redemption, m.nom_magasin
            FROM bon b
            LEFT JOIN redemption r ON r.bon_id = b.bon_id
            LEFT JOIN magasin m ON r.magasin_id = m.magasin_id
            ORDER BY b.bon_id DESC
            """;

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CellStyle rowStyle = (row % 2 == 0) ? s.data : s.dataAlt;
                CellStyle numStyle = (row % 2 == 0) ? s.dataNum : s.dataNumAlt;
                CellStyle curStyle = (row % 2 == 0) ? s.dataCurrency : s.dataCurrencyAlt;
                Row r = sheet.createRow(row++);

                cell(r, 0, numStyle).setCellValue(rs.getInt("bon_id"));
                cell(r, 1, s.code).setCellValue(str(rs.getString("code_unique")));
                cell(r, 2, curStyle).setCellValue(rs.getDouble("valeur"));
                cell(r, 3, statutStyle(wb, s, rs.getString("statut"))).setCellValue(str(rs.getString("statut")));
                cell(r, 4, numStyle).setCellValue(rs.getInt("demande_id"));
                var dg = rs.getTimestamp("date_emission");
                cell(r, 5, rowStyle).setCellValue(dg != null ? sdf.format(dg) : "");
                var de = rs.getTimestamp("date_expiration");
                cell(r, 6, rowStyle).setCellValue(de != null ? sdf.format(de) : "");
                var dr = rs.getTimestamp("date_redemption");
                cell(r, 7, rowStyle).setCellValue(dr != null ? sdf.format(dr) : "—");
                cell(r, 8, rowStyle).setCellValue(str(rs.getString("nom_magasin")));
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(3, row - 1, 0, widths.length - 1));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE 4 — CLIENTS
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetClients(XSSFWorkbook wb, Styles s) throws SQLException {
        XSSFSheet sheet = wb.createSheet("👥 Clients");
        int[] widths = {10, 28, 28, 18, 14, 14, 20, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int row = 0;
        row = writeSectionTitle(sheet, s, row, "Rapport Clients — " + SOCIETE, widths.length);
        writeMetaRow(sheet, s, row++, "Généré le",
                new SimpleDateFormat(DATE_FMT).format(new Date()), widths.length);
        row++;

        row = writeHeaderRow(sheet, s, row, new String[]{
            "ID", "Nom", "Email", "Téléphone",
            "Nb Demandes", "Montant Total (Rs)", "Date Inscription", "Actif"
        });

        String sql = """
            SELECT c.clientid, c.name, c.email, c.contact_number,
                   COUNT(d.demande_id) AS nb_demandes,
                   COALESCE(SUM(d.montant_total), 0) AS montant_total,
                   c.date_creation, c.actif
            FROM client c
            LEFT JOIN demande d ON d.clientid = c.clientid
            GROUP BY c.clientid, c.name, c.email, c.contact_number, c.date_creation, c.actif
            ORDER BY nb_demandes DESC, c.name
            """;

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_ONLY);
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                CellStyle rowStyle = (row % 2 == 0) ? s.data : s.dataAlt;
                CellStyle numStyle = (row % 2 == 0) ? s.dataNum : s.dataNumAlt;
                CellStyle curStyle = (row % 2 == 0) ? s.dataCurrency : s.dataCurrencyAlt;
                Row r = sheet.createRow(row++);

                cell(r, 0, numStyle).setCellValue(rs.getInt("clientid"));
                cell(r, 1, rowStyle).setCellValue(str(rs.getString("name")));
                cell(r, 2, rowStyle).setCellValue(str(rs.getString("email")));
                cell(r, 3, rowStyle).setCellValue(str(rs.getString("contact_number")));
                cell(r, 4, numStyle).setCellValue(rs.getInt("nb_demandes"));
                cell(r, 5, curStyle).setCellValue(rs.getDouble("montant_total"));
                var dc = rs.getTimestamp("date_creation");
                cell(r, 6, rowStyle).setCellValue(dc != null ? sdf.format(dc) : "");
                cell(r, 7, rs.getBoolean("actif") ? s.statutOk : s.statutKo)
                        .setCellValue(rs.getBoolean("actif") ? "Oui" : "Non");
            }
        }

        sheet.setAutoFilter(new CellRangeAddress(3, row - 1, 0, widths.length - 1));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE : BONS PROCHES EXPIRATION
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetExpiration(XSSFWorkbook wb, Styles s, int joursMax) throws SQLException {
        XSSFSheet sheet = wb.createSheet("⚠ Expiration J+" + joursMax);
        int[] widths = {28, 14, 14, 20, 18, 25};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);

        int row = 0;
        row = writeSectionTitle(sheet, s, row,
                "Bons expirant dans les " + joursMax + " prochains jours — " + SOCIETE, widths.length);
        writeMetaRow(sheet, s, row++, "Généré le",
                new SimpleDateFormat(DATE_FMT).format(new Date()), widths.length);
        row++;

        row = writeHeaderRow(sheet, s, row, new String[]{
            "Code Unique", "Valeur (Rs)", "Demande ID",
            "Date Expiration", "Jours Restants", "Client"
        });

        String sql = """
            SELECT b.code_unique, b.valeur, b.demande_id, b.date_expiration,
                   EXTRACT(DAY FROM b.date_expiration - NOW())::INT AS jours_restants,
                   c.name AS client_name
            FROM bon b
            JOIN demande d ON d.demande_id = b.demande_id
            JOIN client c  ON c.clientid   = d.clientid
            WHERE b.statut = 'ACTIF'
              AND b.date_expiration > NOW()
              AND b.date_expiration < NOW() + (? || ' days')::INTERVAL
            ORDER BY b.date_expiration ASC
            """;

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        try (Connection conn = DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, joursMax);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CellStyle rowStyle = (row % 2 == 0) ? s.data : s.dataAlt;
                    CellStyle numStyle = (row % 2 == 0) ? s.dataNum : s.dataNumAlt;
                    CellStyle curStyle = (row % 2 == 0) ? s.dataCurrency : s.dataCurrencyAlt;
                    Row r = sheet.createRow(row++);

                    cell(r, 0, s.code).setCellValue(str(rs.getString("code_unique")));
                    cell(r, 1, curStyle).setCellValue(rs.getDouble("valeur"));
                    cell(r, 2, numStyle).setCellValue(rs.getInt("demande_id"));
                    var de = rs.getTimestamp("date_expiration");
                    cell(r, 3, rowStyle).setCellValue(de != null ? sdf.format(de) : "");
                    int jours = rs.getInt("jours_restants");
                    cell(r, 4, jours <= 7 ? s.statutKo : jours <= 14 ? s.statutWarn : numStyle)
                            .setCellValue(jours);
                    cell(r, 5, rowStyle).setCellValue(str(rs.getString("client_name")));
                }
            }
        }
        sheet.setAutoFilter(new CellRangeAddress(3, row - 1, 0, widths.length - 1));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FEUILLE 5 — CONFIGURATION ODBC
    // ═══════════════════════════════════════════════════════════════════════

    private static void buildSheetOdbc(XSSFWorkbook wb, Styles s) {
        XSSFSheet sheet = wb.createSheet("🔌 Config ODBC");
        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 65 * 256);

        int row = 0;

        row = writeSectionTitle(sheet, s, row, "Configuration ODBC — Connexion Excel → PostgreSQL", 2);
        row++;

        // ── Section 1 : Chaîne de connexion ───────────────────────────────
        row = writeSectionHeader(sheet, s, row, "1. Chaîne de connexion ODBC", 2);
        writeOdbcRow(sheet, s, row++, "Driver",       "PostgreSQL Unicode");
        writeOdbcRow(sheet, s, row++, "Serveur (Server)", dbHost);
        writeOdbcRow(sheet, s, row++, "Port",         dbPort);
        writeOdbcRow(sheet, s, row++, "Base de données (Database)", dbName);
        writeOdbcRow(sheet, s, row++, "Utilisateur (Username)", dbUser);
        writeOdbcRow(sheet, s, row++, "Mot de passe", "(voir config.properties)");
        row++;

        // ── Section 2 : Installation ───────────────────────────────────────
        row = writeSectionHeader(sheet, s, row, "2. Installation du driver psqlODBC (Windows)", 2);
        String[][] steps = {
            {"Étape 1", "Télécharger psqlODBC depuis : https://www.postgresql.org/ftp/odbc/versions/msi/"},
            {"Étape 2", "Installer psqlodbc_16_xx_0000-x64.msi (ou x86 si Excel 32 bits)"},
            {"Étape 3", "Ouvrir Outils d'administration Windows → Sources de données ODBC (64 bits)"},
            {"Étape 4", "Onglet DSN Système → Ajouter → Sélectionner 'PostgreSQL Unicode(x64)'"},
            {"Étape 5", "Nom DSN : VMS_DB | Serveur : " + dbHost + " | Port : " + dbPort},
            {"Étape 6", "Base de données : " + dbName + " | Username : " + dbUser},
            {"Étape 7", "Cliquer Test → Success ! → OK"},
        };
        for (String[] step : steps) {
            writeOdbcRow(sheet, s, row++, step[0], step[1]);
        }
        row++;

        // ── Section 3 : Requêtes SQL dans Excel ───────────────────────────
        row = writeSectionHeader(sheet, s, row, "3. Connexion depuis Excel (Données → Autres sources → ODBC)", 2);
        String[][] queries = {
            {"Toutes les demandes",
             "SELECT d.reference, c.name, d.montant_total, d.statuts, d.date_creation " +
             "FROM demande d JOIN client c ON d.clientid = c.clientid ORDER BY d.date_creation DESC"},
            {"Bons actifs",
             "SELECT code_unique, valeur, date_expiration FROM bon WHERE statut = 'ACTIF' ORDER BY date_expiration"},
            {"Statistiques par statut",
             "SELECT statuts, COUNT(*) AS nb, SUM(montant_total) AS total FROM demande GROUP BY statuts"},
            {"Top clients",
             "SELECT c.name, COUNT(d.demande_id) AS nb_demandes, SUM(d.montant_total) AS total " +
             "FROM client c JOIN demande d ON d.clientid = c.clientid GROUP BY c.clientid, c.name ORDER BY total DESC LIMIT 10"},
        };
        for (String[] q : queries) {
            writeOdbcRow(sheet, s, row++, q[0], q[1]);
        }
        row++;

        // ── Section 4 : Actualisation automatique ─────────────────────────
        row = writeSectionHeader(sheet, s, row, "4. Actualisation automatique des données dans Excel", 2);
        String[][] refresh = {
            {"Actualiser manuellement", "Onglet Données → Actualiser tout (Ctrl+Alt+F5)"},
            {"Actualisation auto", "Clic droit sur le tableau → Propriétés de la connexion → Actualiser toutes les N minutes"},
            {"Actualisation à l'ouverture", "Propriétés de la connexion → Cocher 'Actualiser les données lors de l'ouverture du fichier'"},
        };
        for (String[] r : refresh) {
            writeOdbcRow(sheet, s, row++, r[0], r[1]);
        }
        row++;

        // ── Section 5 : Tableau croisé dynamique ─────────────────────────
        row = writeSectionHeader(sheet, s, row, "5. Tableau croisé dynamique (TCD) depuis ODBC", 2);
        String[][] tcd = {
            {"Étape 1", "Insertion → Tableau croisé dynamique → Source externe"},
            {"Étape 2", "Choisir connexion → DSN VMS_DB configuré à l'étape 2"},
            {"Étape 3", "SQL : SELECT * FROM v_demandes_completes"},
            {"Étape 4", "Glisser 'statuts' en Lignes, 'montant_total' en Valeurs (Somme)"},
            {"Vue disponible", "v_demandes_completes, v_bons_details, v_bons_proche_expiration, v_stats_societe"},
        };
        for (String[] t : tcd) {
            writeOdbcRow(sheet, s, row++, t[0], t[1]);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS — Cellules et styles
    // ═══════════════════════════════════════════════════════════════════════

    private static int writeSectionTitle(XSSFSheet sheet, Styles s, int row, String title, int colspan) {
        Row r = sheet.createRow(row);
        r.setHeight((short) 700);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(s.title);
        if (colspan > 1) sheet.addMergedRegion(new CellRangeAddress(row, row, 0, colspan - 1));
        return row + 1;
    }

    private static int writeSectionHeader(XSSFSheet sheet, Styles s, int row, String title, int colspan) {
        Row r = sheet.createRow(row);
        r.setHeight((short) 500);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        c.setCellStyle(s.sectionHeader);
        if (colspan > 1) sheet.addMergedRegion(new CellRangeAddress(row, row, 0, colspan - 1));
        return row + 1;
    }

    private static void writeMetaRow(XSSFSheet sheet, Styles s, int row, String key, String val, int colspan) {
        Row r = sheet.createRow(row);
        Cell ck = r.createCell(0);
        ck.setCellValue(key + " : " + val);
        ck.setCellStyle(s.meta);
        if (colspan > 1) sheet.addMergedRegion(new CellRangeAddress(row, row, 0, colspan - 1));
    }

    private static int writeHeaderRow(XSSFSheet sheet, Styles s, int row, String[] cols) {
        Row r = sheet.createRow(row);
        r.setHeight((short) 450);
        for (int i = 0; i < cols.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(cols[i]);
            c.setCellStyle(s.header);
        }
        return row + 1;
    }

    private static void writeOdbcRow(XSSFSheet sheet, Styles s, int row, String key, String value) {
        Row r = sheet.createRow(row);
        r.setHeight((short) 350);
        Cell ck = r.createCell(0);
        ck.setCellValue(key);
        ck.setCellStyle(s.odbcKey);
        Cell cv = r.createCell(1);
        cv.setCellValue(value);
        cv.setCellStyle(s.odbcVal);
    }

    private static void appendKpi(XSSFSheet sheet, Styles s, int row,
                                   Connection conn, String label, String sql,
                                   String unit, String periode) throws SQLException {
        Row r = sheet.createRow(row);
        cell(r, 0, s.data).setCellValue(label);
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                cell(r, 1, s.dataCurrency).setCellValue(rs.getDouble(1));
            }
        }
        cell(r, 2, s.data).setCellValue(unit);
        cell(r, 3, s.data).setCellValue(periode);
    }

    private static Cell cell(Row row, int col, CellStyle style) {
        Cell c = row.createCell(col);
        if (style != null) c.setCellStyle(style);
        return c;
    }

    private static CellStyle statutStyle(XSSFWorkbook wb, Styles s, String statut) {
        if (statut == null) return s.data;
        return switch (statut.toUpperCase()) {
            case "ACTIF", "APPROUVE", "ENVOYE", "GENERE", "REDIME" -> s.statutOk;
            case "REJETE", "ANNULE", "EXPIRE"                       -> s.statutKo;
            case "PAYE", "DEMANDE"                                  -> s.statutWarn;
            default -> s.data;
        };
    }

    private static String str(String s) { return s != null ? s : ""; }

    private static void writeWorkbook(XSSFWorkbook wb, String filePath) throws IOException {
        File f = new File(filePath);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        try (FileOutputStream fos = new FileOutputStream(f)) {
            wb.write(fos);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STYLES — centralisés dans une classe interne
    // ═══════════════════════════════════════════════════════════════════════

    private static class Styles {
        final CellStyle title, sectionHeader, meta, header;
        final CellStyle data, dataAlt, dataNum, dataNumAlt, dataCurrency, dataCurrencyAlt;
        final CellStyle statutOk, statutKo, statutWarn;
        final CellStyle code;
        final CellStyle odbcKey, odbcVal;

        Styles(XSSFWorkbook wb) {
            title         = mkTitle(wb);
            sectionHeader = mkSectionHeader(wb);
            meta          = mkMeta(wb);
            header        = mkHeader(wb);
            data          = mkData(wb, null, false, false);
            dataAlt       = mkData(wb, COLOR_ALTROW, false, false);
            dataNum       = mkData(wb, null, true, false);
            dataNumAlt    = mkData(wb, COLOR_ALTROW, true, false);
            dataCurrency  = mkData(wb, null, false, true);
            dataCurrencyAlt = mkData(wb, COLOR_ALTROW, false, true);
            statutOk      = mkStatut(wb, COLOR_GREEN);
            statutKo      = mkStatut(wb, COLOR_RED);
            statutWarn    = mkStatut(wb, COLOR_ORANGE);
            code          = mkCode(wb);
            odbcKey       = mkOdbcKey(wb);
            odbcVal       = mkOdbcVal(wb);
        }

        private static CellStyle mkTitle(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(COLOR_NAVY, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setAlignment(HorizontalAlignment.LEFT);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            Font f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 14);
            ((XSSFFont) f).setColor(new XSSFColor(COLOR_WHITE, null));
            cs.setFont(f);
            cs.setWrapText(false);
            return cs;
        }

        private static CellStyle mkSectionHeader(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(COLOR_LIGHT, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setAlignment(HorizontalAlignment.LEFT);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setBorderBottom(BorderStyle.MEDIUM);
            cs.setBottomBorderColor(new XSSFColor(COLOR_NAVY, null).getIndex());
            Font f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 11);
            ((XSSFFont) f).setColor(new XSSFColor(COLOR_NAVY, null));
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkMeta(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setAlignment(HorizontalAlignment.LEFT);
            Font f = wb.createFont();
            f.setItalic(true); f.setFontHeightInPoints((short) 9);
            ((XSSFFont) f).setColor(new XSSFColor(COLOR_GRAY, null));
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkHeader(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(COLOR_NAVY, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setAlignment(HorizontalAlignment.CENTER);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            cs.setBorderBottom(BorderStyle.MEDIUM);
            Font f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 10);
            ((XSSFFont) f).setColor(new XSSFColor(COLOR_WHITE, null));
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkData(XSSFWorkbook wb, byte[] bg, boolean number, boolean currency) {
            XSSFCellStyle cs = wb.createCellStyle();
            if (bg != null) {
                cs.setFillForegroundColor(new XSSFColor(bg, null));
                cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            }
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setBorderBottom(BorderStyle.HAIR);
            cs.setBottomBorderColor(new XSSFColor(COLOR_LIGHT, null).getIndex());
            if (number || currency) {
                cs.setAlignment(HorizontalAlignment.RIGHT);
            }
            if (currency) {
                DataFormat df = wb.createDataFormat();
                cs.setDataFormat(df.getFormat("#,##0.00 \"Rs\""));
            } else if (number) {
                DataFormat df = wb.createDataFormat();
                cs.setDataFormat(df.getFormat("#,##0"));
            }
            Font f = wb.createFont();
            f.setFontHeightInPoints((short) 10);
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkStatut(XSSFWorkbook wb, byte[] color) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setAlignment(HorizontalAlignment.CENTER);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            Font f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 10);
            ((XSSFFont) f).setColor(new XSSFColor(color, null));
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkCode(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            Font f = wb.createFont();
            f.setFontName("Courier New"); f.setFontHeightInPoints((short) 9);
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkOdbcKey(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setFillForegroundColor(new XSSFColor(COLOR_LIGHT, null));
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            Font f = wb.createFont();
            f.setBold(true); f.setFontHeightInPoints((short) 10);
            ((XSSFFont) f).setColor(new XSSFColor(COLOR_NAVY, null));
            cs.setFont(f);
            return cs;
        }

        private static CellStyle mkOdbcVal(XSSFWorkbook wb) {
            XSSFCellStyle cs = wb.createCellStyle();
            cs.setVerticalAlignment(VerticalAlignment.CENTER);
            cs.setWrapText(true);
            Font f = wb.createFont();
            f.setFontName("Courier New"); f.setFontHeightInPoints((short) 9);
            cs.setFont(f);
            return cs;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  Utilitaire couleur
    // ─────────────────────────────────────────────────────────────────────

    private static byte[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
