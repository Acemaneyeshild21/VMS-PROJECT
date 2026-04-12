package pkg.vms;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import pkg.vms.DAO.BonDAO;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import com.itextpdf.text.pdf.draw.LineSeparator;
/**
 * Génère un PDF par bon avec QR code unique, code-barres, et signature autorisée.
 */
public class VoucherPDFGenerator {

    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/VMS_Bons";

    // Couleurs du thème
    private static final BaseColor RED_PRIMARY = new BaseColor(210, 35, 45);
    private static final BaseColor TEXT_DARK   = new BaseColor(22, 28, 45);
    private static final BaseColor TEXT_GRAY   = new BaseColor(90, 100, 120);
    private static final BaseColor BG_LIGHT    = new BaseColor(248, 249, 252);

    /**
     * Génère un PDF pour un bon et retourne le chemin du fichier.
     */
    public static String genererPDF(BonDAO.BonInfo bon) throws Exception {
        // Créer le dossier de sortie
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "BON_" + bon.codeUnique.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        String filePath = OUTPUT_DIR + "/" + fileName;

        Document doc = new Document(PageSize.A5.rotate(), 30, 30, 25, 25);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        // ── Bordure du bon ──
        PdfContentByte cb = writer.getDirectContentUnder();
        cb.setColorStroke(RED_PRIMARY);
        cb.setLineWidth(2f);
        cb.roundRectangle(15, 15, doc.getPageSize().getWidth() - 30,
                          doc.getPageSize().getHeight() - 30, 10);
        cb.stroke();

        // Ligne décorative rouge en haut
        cb.setColorFill(RED_PRIMARY);
        cb.rectangle(15, doc.getPageSize().getHeight() - 20,
                     doc.getPageSize().getWidth() - 30, 5);
        cb.fill();

        // ── Polices ──
        Font fontTitle     = new Font(Font.FontFamily.TIMES_ROMAN, 22, Font.BOLD, RED_PRIMARY);
        Font fontSubtitle  = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_GRAY);
        Font fontLabel     = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_GRAY);
        Font fontValue     = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, TEXT_DARK);
        Font fontAmount    = new Font(Font.FontFamily.TIMES_ROMAN, 28, Font.BOLD, RED_PRIMARY);
        Font fontCode      = new Font(Font.FontFamily.COURIER, 8, Font.NORMAL, TEXT_GRAY);
        Font fontSignature = new Font(Font.FontFamily.TIMES_ROMAN, 9, Font.ITALIC, TEXT_GRAY);

        // ── En-tête ──
        Paragraph header = new Paragraph();
        header.add(new Chunk("INTERMART", fontTitle));
        header.add(new Chunk("  ", fontSubtitle));
        header.add(new Chunk("BON CADEAU", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, TEXT_DARK)));
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        Paragraph sub = new Paragraph("Système de Gestion de Bons — Île Maurice", fontSubtitle);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(15);
        doc.add(sub);

        // ── Ligne de séparation ──
        LineSeparator line = new LineSeparator(1, 90, RED_PRIMARY, Element.ALIGN_CENTER, -2);
        doc.add(line);
        doc.add(Chunk.NEWLINE);

        // ── Table principale (2 colonnes : infos + QR) ──
        PdfPTable mainTable = new PdfPTable(2);
        mainTable.setWidthPercentage(95);
        mainTable.setWidths(new float[]{60, 40});

        // Colonne gauche : détails du bon
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(8);

        // Valeur du bon
        Paragraph valeur = new Paragraph();
        valeur.add(new Chunk("VALEUR\n", fontLabel));
        valeur.add(new Chunk(String.format("Rs %,.2f", bon.valeur), fontAmount));
        valeur.setSpacingAfter(12);
        leftCell.addElement(valeur);

        // Détails
        leftCell.addElement(buildDetailLine("Référence", bon.reference, fontLabel, fontValue));
        leftCell.addElement(buildDetailLine("Bénéficiaire", bon.clientNom, fontLabel, fontValue));
        leftCell.addElement(buildDetailLine("Date d'émission",
                bon.dateEmission != null && bon.dateEmission.length() >= 10
                        ? bon.dateEmission.substring(0, 10) : "—", fontLabel, fontValue));
        leftCell.addElement(buildDetailLine("Date d'expiration",
                bon.dateExpiration != null && bon.dateExpiration.length() >= 10
                        ? bon.dateExpiration.substring(0, 10) : "—", fontLabel, fontValue));

        mainTable.addCell(leftCell);

        // Colonne droite : QR code + code-barres
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(5);
        rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        // Générer QR Code
        Image qrImage = generateQRImage(bon.codeUnique, 150, 150);
        qrImage.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(qrImage);

        // Code-barres 1D (Code 128)
        Image barcodeImage = generateBarcodeImage(bon.codeUnique, writer);
        barcodeImage.setAlignment(Element.ALIGN_CENTER);
        rightCell.addElement(barcodeImage);

        // Code texte
        Paragraph codeText = new Paragraph(bon.codeUnique, fontCode);
        codeText.setAlignment(Element.ALIGN_CENTER);
        codeText.setSpacingBefore(3);
        rightCell.addElement(codeText);

        mainTable.addCell(rightCell);
        doc.add(mainTable);

        // ── Zone de conditions ──
        doc.add(Chunk.NEWLINE);
        PdfPTable condTable = new PdfPTable(1);
        condTable.setWidthPercentage(95);
        PdfPCell condCell = new PdfPCell();
        condCell.setBackgroundColor(BG_LIGHT);
        condCell.setBorderColor(new BaseColor(228, 230, 236));
        condCell.setPadding(8);

        Font fontCond = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, TEXT_GRAY);
        condCell.addElement(new Paragraph("CONDITIONS D'UTILISATION", fontLabel));
        condCell.addElement(new Paragraph(
                "• Ce bon est à usage unique et non fractionnable. " +
                "• Valable uniquement dans les magasins Intermart désignés. " +
                "• Non remboursable, non échangeable contre de l'argent. " +
                "• Présentez ce bon (imprimé ou numérique) au superviseur magasin pour validation.", fontCond));
        condTable.addCell(condCell);
        doc.add(condTable);

        // ── Signature autorisée ──
        Paragraph sig = new Paragraph();
        sig.setSpacingBefore(10);
        sig.add(new Chunk("Signature autorisée : ", fontSignature));
        sig.add(new Chunk("Direction Intermart Maurice", fontSignature));
        sig.setAlignment(Element.ALIGN_RIGHT);
        doc.add(sig);

        doc.close();
        return filePath;
    }

    /**
     * Génère un QR Code sous forme d'image iText.
     */
    private static Image generateQRImage(String data, int width, int height) throws Exception {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
        BufferedImage buffImage = MatrixToImageWriter.toBufferedImage(matrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffImage, "PNG", baos);
        return Image.getInstance(baos.toByteArray());
    }

    /**
     * Génère un code-barres Code128 via iText.
     */
    private static Image generateBarcodeImage(String code, PdfWriter writer) throws Exception {
        Barcode128 barcode128 = new Barcode128();
        barcode128.setCode(code);
        barcode128.setCodeType(Barcode128.CODE128);
        barcode128.setBarHeight(30f);
        barcode128.setFont(null); // Pas de texte sous le code-barres (on l'ajoute séparément)

        java.awt.Image awtImage = barcode128.createAwtImage(java.awt.Color.BLACK, java.awt.Color.WHITE);
        BufferedImage buffered = new BufferedImage(awtImage.getWidth(null), awtImage.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = buffered.createGraphics();
        g2.drawImage(awtImage, 0, 0, null);
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(buffered, "PNG", baos);
        Image img = Image.getInstance(baos.toByteArray());
        img.scaleToFit(180, 40);
        return img;
    }

    private static Paragraph buildDetailLine(String label, String value, Font fontLabel, Font fontValue) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " : ", fontLabel));
        p.add(new Chunk(value != null ? value : "—", fontValue));
        p.setSpacingAfter(4);
        return p;
    }

    /**
     * Génère un PDF récapitulatif avec tous les bons d'une demande.
     */
    public static String genererRecapPDF(int demandeId, java.util.List<BonDAO.BonInfo> bons) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "RECAP_DEMANDE_" + demandeId + ".pdf";
        String filePath = OUTPUT_DIR + "/" + fileName;

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        Font fontTitle = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD, RED_PRIMARY);
        Font fontSub   = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_GRAY);
        Font fontHead  = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
        Font fontCell  = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);

        // Header
        Paragraph title = new Paragraph("RÉCAPITULATIF DE GÉNÉRATION", fontTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        doc.add(new Paragraph("Demande #" + demandeId + " — " + bons.size() + " bon(s) générés", fontSub));
        doc.add(new Paragraph("Date : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()), fontSub));
        doc.add(Chunk.NEWLINE);

        if (!bons.isEmpty()) {
            doc.add(new Paragraph("Client : " + bons.get(0).clientNom, fontSub));
            doc.add(new Paragraph("Référence : " + bons.get(0).reference, fontSub));
            doc.add(Chunk.NEWLINE);
        }

        // Table récap
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 35, 15, 20, 22});

        String[] headers = {"#", "Code Unique", "Valeur (Rs)", "Émission", "Expiration"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHead));
            cell.setBackgroundColor(RED_PRIMARY);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }

        int i = 1;
        for (BonDAO.BonInfo bon : bons) {
            table.addCell(createCell(String.valueOf(i++), fontCell));
            table.addCell(createCell(bon.codeUnique, fontCell));
            table.addCell(createCell(String.format("%,.2f", bon.valeur), fontCell));
            table.addCell(createCell(bon.dateEmission != null && bon.dateEmission.length() >= 10
                    ? bon.dateEmission.substring(0, 10) : "—", fontCell));
            table.addCell(createCell(bon.dateExpiration != null && bon.dateExpiration.length() >= 10
                    ? bon.dateExpiration.substring(0, 10) : "—", fontCell));
        }
        doc.add(table);

        // Total
        doc.add(Chunk.NEWLINE);
        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
        Paragraph totalP = new Paragraph(String.format("Montant total : Rs %,.2f", total),
                new Font(Font.FontFamily.TIMES_ROMAN, 14, Font.BOLD, RED_PRIMARY));
        totalP.setAlignment(Element.ALIGN_RIGHT);
        doc.add(totalP);

        doc.close();
        return filePath;
    }

    private static PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
}
