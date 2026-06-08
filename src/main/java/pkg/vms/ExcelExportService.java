package pkg.vms;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service pour l'export de données au format Excel (.xlsx).
 *
 * IMPORTANT : Les clés dans chaque Map de `data` doivent correspondre
 * exactement (même casse, mêmes espaces) aux valeurs du tableau `columns`.
 * Toute discordance produira des cellules vides sans erreur visible.
 */
public class ExcelExportService {

    public static void exportData(String filePath, String sheetName, String[] columns, List<Map<String, Object>> data) throws IOException {

        // CORRECTIF 2 : Apache POI utilise AWT en interne pour calculer la
        // largeur des colonnes (autoSizeColumn). Sur un serveur Linux sans
        // interface graphique, cela provoque une HeadlessException.
        // Ce flag indique à la JVM de fonctionner sans affichage.
        System.setProperty("java.awt.headless", "true");

        // CORRECTIF 4 : Si le répertoire parent n'existe pas encore
        // (ex: "exports/2026/data.xlsx"), FileOutputStream lèverait une
        // FileNotFoundException peu explicite. mkdirs() crée toute la
        // hiérarchie de dossiers manquante de façon préventive.
        File outputFile = new File(filePath);
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(sheetName);

            // --- Style de l'en-tête ---
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_RED.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // CORRECTIF 1 : POI exige que la couleur de fond (background) soit
            // aussi définie quand on utilise SOLID_FOREGROUND. Sans cette ligne,
            // Excel peut ignorer la couleur ou afficher un fond inattendu.
            headerCellStyle.setFillBackgroundColor(IndexedColors.AUTOMATIC.getIndex());

            headerCellStyle.setBorderBottom(BorderStyle.THIN);

            // --- Style pour les cellules de date ---
            // CORRECTIF 6 (partie date) : Un CellStyle dédié est nécessaire
            // pour que Excel interprète la valeur numérique comme une date
            // lisible, plutôt que d'afficher un nombre brut.
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));

            // --- Création de la ligne d'en-tête ---
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // --- Remplissage des données ---
            int rowNum = 1;
            for (Map<String, Object> rowData : data) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < columns.length; i++) {
                    Cell cell = row.createCell(i);
                    Object value = rowData.get(columns[i]);

                    // CORRECTIF 6 : Gestion explicite de tous les types courants.
                    // Avant, Date devenait une chaîne illisible, et Boolean
                    // était simplement affiché comme "true"/"false" texte.
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        // Excel affichera TRUE/FALSE et le traitera comme booléen
                        cell.setCellValue((Boolean) value);
                    } else if (value instanceof Date) {
                        // La valeur est stockée en nombre, le style fait la mise en forme
                        cell.setCellValue((Date) value);
                        cell.setCellStyle(dateCellStyle);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    }
                    // Si value == null, on laisse la cellule vide (comportement correct)
                }
            }

            // --- Ajustement automatique de la largeur des colonnes ---
            // Fonctionne correctement grâce au flag headless positionné plus haut.
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // --- Écriture du fichier sur le disque ---
            try (FileOutputStream fileOut = new FileOutputStream(outputFile)) {
                workbook.write(fileOut);
            }
        }
    }
}