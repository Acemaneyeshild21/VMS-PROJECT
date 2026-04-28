package pkg.vms;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Genere des factures PDF professionnelles pour les commandes de bons cadeau.
 * Format A4 portrait avec design VoucherManager VMS.
 */
public class FacturePDFGenerator {

    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/VMS_Bons/Factures";

    // ── Palette de couleurs ──
    private static final BaseColor RED_PRIMARY = new BaseColor(210, 35, 45);
    private static final BaseColor TEXT_DARK   = new BaseColor(22, 28, 45);
    private static final BaseColor TEXT_GRAY   = new BaseColor(90, 100, 120);
    private static final BaseColor LIGHT_BG    = new BaseColor(248, 249, 252);
    private static final BaseColor BORDER      = new BaseColor(228, 230, 236);
    private static final BaseColor WHITE       = BaseColor.WHITE;

    // ── Donnees de la facture ──
    public static class FactureInfo {
        public String numeroFacture;
        public String referenceDemande;
        public String dateFacture;
        public String dateEcheance;

        // Client info
        public String clientNom;
        public String clientEmail;
        public String clientTelephone;
        public String clientAdresse;
        public String clientSociete;

        // Voucher details
        public int nombreBons;
        public double valeurUnitaire;
        public String typeBon;

        // Payment
        public boolean estPaye;
        public String datePaiement;

        // Approver
        public String approbateurNom;
        public String comptableNom;
    }

    /**
     * Genere une facture PDF professionnelle et retourne le chemin du fichier.
     */
    public static String genererFacture(FactureInfo info) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = info.numeroFacture.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        String filePath = OUTPUT_DIR + "/" + fileName;

        Document doc = new Document(PageSize.A4, 40, 40, 30, 40);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));

        // Footer handler
        writer.setPageEvent(new FactureFooter());

        doc.open();

        PdfContentByte canvas = writer.getDirectContent();
        float pageW = PageSize.A4.getWidth();

        // ══════════════════════════════════════════════════════════
        // 1. HEADER - Logo + Company info + FACTURE title
        // ══════════════════════════════════════════════════════════
        drawHeader(canvas, pageW, info);

        // ══════════════════════════════════════════════════════════
        // 2. CLIENT SECTION
        // ══════════════════════════════════════════════════════════
        drawClientSection(doc, info);

        // ══════════════════════════════════════════════════════════
        // 3. REFERENCE
        // ══════════════════════════════════════════════════════════
        drawReference(doc, info);

        // ══════════════════════════════════════════════════════════
        // 4. ITEMS TABLE
        // ══════════════════════════════════════════════════════════
        drawItemsTable(doc, info);

        // ══════════════════════════════════════════════════════════
        // 5. TOTALS
        // ══════════════════════════════════════════════════════════
        drawTotals(doc, info);

        // ══════════════════════════════════════════════════════════
        // 6. PAYMENT STAMP (if paid)
        // ══════════════════════════════════════════════════════════
        if (info.estPaye) {
            drawPaymentStamp(canvas, info);
        }

        // ══════════════════════════════════════════════════════════
        // 7. PAYMENT INSTRUCTIONS
        // ══════════════════════════════════════════════════════════
        drawPaymentInstructions(doc, info);

        // ══════════════════════════════════════════════════════════
        // 8. SIGNATURES
        // ══════════════════════════════════════════════════════════
        drawSignatures(doc, info);

        // ══════════════════════════════════════════════════════════
        // 9. CLOSING
        // ══════════════════════════════════════════════════════════
        drawClosing(doc);

        doc.close();
        return filePath;
    }

    // ========================================================================
    // HEADER
    // ========================================================================

    private static void drawHeader(PdfContentByte canvas, float pageW, FactureInfo info) throws Exception {
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

        float topY = PageSize.A4.getHeight() - 30;

        // ── Logo block (red rectangle with white text) ──
        float logoX = 40;
        float logoY = topY - 45;
        float logoW = 120;
        float logoH = 45;

        canvas.setColorFill(RED_PRIMARY);
        canvas.roundRectangle(logoX, logoY, logoW, logoH, 4);
        canvas.fill();

        canvas.beginText();
        canvas.setFontAndSize(bfBold, 18);
        canvas.setColorFill(WHITE);
        canvas.showTextAligned(Element.ALIGN_CENTER, "VMS", logoX + logoW / 2, logoY + logoH / 2 + 4, 0f);
        canvas.setFontAndSize(bfNormal, 8);
        canvas.showTextAligned(Element.ALIGN_CENTER, "VOUCHERS", logoX + logoW / 2, logoY + logoH / 2 - 10, 0f);
        canvas.endText();

        // ── Company details below logo ──
        float detailX = 40;
        float detailY = logoY - 14;
        float lineSpacing = 11;

        canvas.beginText();
        canvas.setFontAndSize(bfBold, 8);
        canvas.setColorFill(TEXT_DARK);
        canvas.showTextAligned(Element.ALIGN_LEFT, "VoucherManager VMS", detailX, detailY, 0f);

        canvas.setFontAndSize(bfNormal, 7.5f);
        canvas.setColorFill(TEXT_GRAY);
        canvas.showTextAligned(Element.ALIGN_LEFT, "Cybercite, Ebene, Ile Maurice", detailX, detailY - lineSpacing, 0f);
        canvas.showTextAligned(Element.ALIGN_LEFT, "T: +230 000 0000", detailX, detailY - lineSpacing * 2, 0f);
        canvas.showTextAligned(Element.ALIGN_LEFT, "E: contact@vms.mu", detailX, detailY - lineSpacing * 3, 0f);
        canvas.showTextAligned(Element.ALIGN_LEFT, "TVA: MU-123456789", detailX, detailY - lineSpacing * 4, 0f);
        canvas.endText();

        // ── "FACTURE" title (top-right, large red) ──
        canvas.beginText();
        canvas.setFontAndSize(bfBold, 32);
        canvas.setColorFill(RED_PRIMARY);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "FACTURE", pageW - 40, topY - 10, 0f);
        canvas.endText();

        // ── Invoice meta (right-aligned, below title) ──
        float metaX = pageW - 40;
        float metaY = topY - 40;

        canvas.beginText();
        canvas.setFontAndSize(bfBold, 8);
        canvas.setColorFill(TEXT_DARK);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "N. " + safe(info.numeroFacture), metaX, metaY, 0f);

        canvas.setFontAndSize(bfNormal, 8);
        canvas.setColorFill(TEXT_GRAY);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "Date : " + safe(info.dateFacture), metaX, metaY - 13, 0f);
        canvas.showTextAligned(Element.ALIGN_RIGHT, "Echeance : " + safe(info.dateEcheance), metaX, metaY - 26, 0f);
        canvas.endText();

        // ── Separator line ──
        float sepY = topY - 100;
        canvas.setColorStroke(BORDER);
        canvas.setLineWidth(1f);
        canvas.moveTo(40, sepY);
        canvas.lineTo(pageW - 40, sepY);
        canvas.stroke();
    }

    // ========================================================================
    // CLIENT SECTION
    // ========================================================================

    private static void drawClientSection(Document doc, FactureInfo info) throws DocumentException {
        // Spacer to push below the header drawn via PdfContentByte
        doc.add(spacer(110));

        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, RED_PRIMARY);
        Font fontValue = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
        Font fontSmall = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_GRAY);

        // Client box
        PdfPTable clientTable = new PdfPTable(1);
        clientTable.setWidthPercentage(55);
        clientTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell headerCell = new PdfPCell(new Phrase("FACTURER A :", fontLabel));
        headerCell.setBackgroundColor(LIGHT_BG);
        headerCell.setBorderColor(BORDER);
        headerCell.setBorderWidth(1f);
        headerCell.setPadding(8);
        headerCell.setPaddingBottom(4);
        clientTable.addCell(headerCell);

        StringBuilder clientDetails = new StringBuilder();
        if (info.clientSociete != null && !info.clientSociete.isEmpty()) {
            clientDetails.append(info.clientSociete).append("\n");
        }
        clientDetails.append(safe(info.clientNom)).append("\n");
        if (info.clientAdresse != null && !info.clientAdresse.isEmpty()) {
            clientDetails.append(info.clientAdresse).append("\n");
        }
        if (info.clientEmail != null && !info.clientEmail.isEmpty()) {
            clientDetails.append("Email : ").append(info.clientEmail).append("\n");
        }
        if (info.clientTelephone != null && !info.clientTelephone.isEmpty()) {
            clientDetails.append("Tel : ").append(info.clientTelephone);
        }

        PdfPCell detailCell = new PdfPCell();
        detailCell.setBackgroundColor(LIGHT_BG);
        detailCell.setBorderColor(BORDER);
        detailCell.setBorderWidth(1f);
        detailCell.setPadding(8);
        detailCell.setPaddingTop(4);

        // Build multi-line phrase
        String[] lines = clientDetails.toString().split("\n");
        for (int i = 0; i < lines.length; i++) {
            Font f = (i == 0 && info.clientSociete != null && !info.clientSociete.isEmpty())
                    ? new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_DARK) : fontValue;
            if (lines[i].startsWith("Email") || lines[i].startsWith("Tel")) {
                f = fontSmall;
            }
            Paragraph p = new Paragraph(lines[i], f);
            p.setLeading(14);
            detailCell.addElement(p);
        }
        clientTable.addCell(detailCell);

        doc.add(clientTable);
        doc.add(spacer(10));
    }

    // ========================================================================
    // REFERENCE
    // ========================================================================

    private static void drawReference(Document doc, FactureInfo info) throws DocumentException {
        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, TEXT_GRAY);
        Font fontValue = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);

        Paragraph refLine = new Paragraph();
        refLine.add(new Chunk("Reference demande : ", fontLabel));
        refLine.add(new Chunk(safe(info.referenceDemande), fontValue));
        refLine.setSpacingAfter(12);
        doc.add(refLine);
    }

    // ========================================================================
    // ITEMS TABLE
    // ========================================================================

    private static void drawItemsTable(Document doc, FactureInfo info) throws DocumentException {
        Font fontHeader = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, WHITE);
        Font fontCell = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{45, 10, 20, 25});

        // Header row (red background, white text)
        String[] headers = {"Designation", "Qte", "Prix Unitaire (Rs)", "Montant HT (Rs)"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
            cell.setBackgroundColor(RED_PRIMARY);
            cell.setPadding(8);
            cell.setBorderColor(RED_PRIMARY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        // Data row
        String designation = "Bons Cadeau " + safe(info.typeBon)
                + " Rs." + formatMontant(info.valeurUnitaire);
        double montantHT = info.nombreBons * info.valeurUnitaire;

        PdfPCell cellDesig = new PdfPCell(new Phrase(designation, fontCell));
        cellDesig.setPadding(8);
        cellDesig.setBorderColor(BORDER);
        cellDesig.setBackgroundColor(LIGHT_BG);
        table.addCell(cellDesig);

        PdfPCell cellQte = createDataCell(String.valueOf(info.nombreBons), fontCell);
        cellQte.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cellQte);

        PdfPCell cellPU = createDataCell(formatMontant(info.valeurUnitaire), fontCell);
        cellPU.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellPU);

        PdfPCell cellMontant = createDataCell(formatMontant(montantHT), fontCell);
        cellMontant.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(cellMontant);

        doc.add(table);
        doc.add(spacer(5));
    }

    // ========================================================================
    // TOTALS
    // ========================================================================

    private static void drawTotals(Document doc, FactureInfo info) throws DocumentException {
        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, TEXT_DARK);
        Font fontLabelBold = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK);
        Font fontTotal = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, RED_PRIMARY);

        double montantHT = info.nombreBons * info.valeurUnitaire;
        double tva = montantHT * 0.15;
        double totalTTC = montantHT * 1.15;

        // Right-aligned totals table
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(40);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{50, 50});

        // Sous-total HT
        addTotalRow(totals, "Sous-total HT :", formatMontant(montantHT), fontLabel, fontLabelBold, false);

        // TVA
        addTotalRow(totals, "TVA (15%) :", formatMontant(tva), fontLabel, fontLabelBold, false);

        // Separator
        PdfPCell sepLeft = new PdfPCell(new Phrase(""));
        sepLeft.setBorder(Rectangle.BOTTOM);
        sepLeft.setBorderColor(BORDER);
        sepLeft.setPadding(2);
        totals.addCell(sepLeft);
        PdfPCell sepRight = new PdfPCell(new Phrase(""));
        sepRight.setBorder(Rectangle.BOTTOM);
        sepRight.setBorderColor(BORDER);
        sepRight.setPadding(2);
        totals.addCell(sepRight);

        // Total TTC (bold, larger)
        PdfPCell labelCell = new PdfPCell(new Phrase("Total TTC :", fontTotal));
        labelCell.setPadding(6);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        labelCell.setBackgroundColor(LIGHT_BG);
        totals.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(formatMontant(totalTTC) + " Rs", fontTotal));
        valueCell.setPadding(6);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setBackgroundColor(LIGHT_BG);
        totals.addCell(valueCell);

        doc.add(totals);
        doc.add(spacer(15));
    }

    private static void addTotalRow(PdfPTable table, String label, String value,
                                     Font fontLabel, Font fontValue, boolean highlight) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, fontLabel));
        labelCell.setPadding(4);
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        if (highlight) labelCell.setBackgroundColor(LIGHT_BG);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value + " Rs", fontValue));
        valueCell.setPadding(4);
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (highlight) valueCell.setBackgroundColor(LIGHT_BG);
        table.addCell(valueCell);
    }

    // ========================================================================
    // PAYMENT STAMP
    // ========================================================================

    private static void drawPaymentStamp(PdfContentByte canvas, FactureInfo info) throws Exception {
        BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
        BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

        canvas.saveState();

        // Semi-transparent red stamp, rotated 45 degrees, centered on page
        float centerX = PageSize.A4.getWidth() / 2;
        float centerY = PageSize.A4.getHeight() / 2 + 40;

        // Draw stamp border (double circle)
        PdfGState gs = new PdfGState();
        gs.setFillOpacity(0.15f);
        gs.setStrokeOpacity(0.25f);
        canvas.setGState(gs);

        canvas.setColorStroke(RED_PRIMARY);
        canvas.setLineWidth(4f);
        canvas.circle(centerX, centerY, 80);
        canvas.stroke();
        canvas.setLineWidth(2f);
        canvas.circle(centerX, centerY, 72);
        canvas.stroke();

        // "PAYE" text rotated 45 degrees
        PdfGState gsText = new PdfGState();
        gsText.setFillOpacity(0.2f);
        canvas.setGState(gsText);

        canvas.beginText();
        canvas.setFontAndSize(bfBold, 52);
        canvas.setColorFill(RED_PRIMARY);
        canvas.showTextAligned(Element.ALIGN_CENTER, "PAYE", centerX, centerY - 15, 45f);
        canvas.endText();

        // Date below stamp text
        if (info.datePaiement != null && !info.datePaiement.isEmpty()) {
            PdfGState gsDate = new PdfGState();
            gsDate.setFillOpacity(0.2f);
            canvas.setGState(gsDate);

            canvas.beginText();
            canvas.setFontAndSize(bfNormal, 10);
            canvas.setColorFill(RED_PRIMARY);
            canvas.showTextAligned(Element.ALIGN_CENTER, info.datePaiement, centerX, centerY - 55, 45f);
            canvas.endText();
        }

        canvas.restoreState();
    }

    // ========================================================================
    // PAYMENT INSTRUCTIONS
    // ========================================================================

    private static void drawPaymentInstructions(Document doc, FactureInfo info) throws DocumentException {
        Font fontSectionTitle = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font fontNormal = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_GRAY);
        Font fontBold = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, TEXT_DARK);

        // Section title
        Paragraph title = new Paragraph("CONDITIONS DE PAIEMENT", fontSectionTitle);
        title.setSpacingBefore(5);
        title.setSpacingAfter(6);
        doc.add(title);

        // Gray separator
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        PdfPCell sepCell = new PdfPCell(new Phrase(""));
        sepCell.setBorder(Rectangle.BOTTOM);
        sepCell.setBorderColor(BORDER);
        sepCell.setFixedHeight(1);
        sep.addCell(sepCell);
        doc.add(sep);
        doc.add(spacer(6));

        // Payment terms
        Paragraph terms = new Paragraph("Virement bancaire sous 30 jours", fontNormal);
        terms.setSpacingAfter(8);
        doc.add(terms);

        // Bank details in a light box
        PdfPTable bankTable = new PdfPTable(1);
        bankTable.setWidthPercentage(60);
        bankTable.setHorizontalAlignment(Element.ALIGN_LEFT);

        PdfPCell bankCell = new PdfPCell();
        bankCell.setBackgroundColor(LIGHT_BG);
        bankCell.setBorderColor(BORDER);
        bankCell.setBorderWidth(0.5f);
        bankCell.setPadding(10);

        bankCell.addElement(createBankLine("Banque :", "MCB Ltd", fontBold, fontNormal));
        bankCell.addElement(createBankLine("Compte :", "000-123456-001", fontBold, fontNormal));
        bankCell.addElement(createBankLine("IBAN :", "MU00MCB0000001234560010", fontBold, fontNormal));

        bankTable.addCell(bankCell);
        doc.add(bankTable);
        doc.add(spacer(15));
    }

    private static Paragraph createBankLine(String label, String value, Font fontBold, Font fontNormal) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + " ", fontBold));
        p.add(new Chunk(value, fontNormal));
        p.setLeading(14);
        return p;
    }

    // ========================================================================
    // SIGNATURES
    // ========================================================================

    private static void drawSignatures(Document doc, FactureInfo info) throws DocumentException {
        Font fontLabel = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, TEXT_GRAY);
        Font fontName = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, TEXT_DARK);

        PdfPTable sigTable = new PdfPTable(2);
        sigTable.setWidthPercentage(100);
        sigTable.setWidths(new float[]{50, 50});

        // Approver
        PdfPCell approverCell = new PdfPCell();
        approverCell.setBorder(Rectangle.NO_BORDER);
        approverCell.setPadding(5);
        Paragraph appLabel = new Paragraph("Approbateur :", fontLabel);
        approverCell.addElement(appLabel);
        Paragraph appName = new Paragraph(safe(info.approbateurNom), fontName);
        approverCell.addElement(appName);
        Paragraph appLine = new Paragraph("______________________", fontLabel);
        appLine.setSpacingBefore(15);
        approverCell.addElement(appLine);
        sigTable.addCell(approverCell);

        // Comptable
        PdfPCell comptableCell = new PdfPCell();
        comptableCell.setBorder(Rectangle.NO_BORDER);
        comptableCell.setPadding(5);
        comptableCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph compLabel = new Paragraph("Comptable :", fontLabel);
        compLabel.setAlignment(Element.ALIGN_RIGHT);
        comptableCell.addElement(compLabel);
        Paragraph compName = new Paragraph(safe(info.comptableNom), fontName);
        compName.setAlignment(Element.ALIGN_RIGHT);
        comptableCell.addElement(compName);
        Paragraph compLine = new Paragraph("______________________", fontLabel);
        compLine.setSpacingBefore(15);
        compLine.setAlignment(Element.ALIGN_RIGHT);
        comptableCell.addElement(compLine);
        sigTable.addCell(comptableCell);

        doc.add(sigTable);
    }

    // ========================================================================
    // CLOSING
    // ========================================================================

    private static void drawClosing(Document doc) throws DocumentException {
        doc.add(spacer(15));

        Font fontMerci = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLDITALIC, TEXT_GRAY);
        Paragraph merci = new Paragraph("Merci de votre confiance", fontMerci);
        merci.setAlignment(Element.ALIGN_CENTER);
        doc.add(merci);
    }

    // ========================================================================
    // FOOTER (page event)
    // ========================================================================

    private static class FactureFooter extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            try {
                PdfContentByte canvas = writer.getDirectContent();
                float pageW = document.getPageSize().getWidth();
                float footerY = 30;

                BaseFont bfNormal = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

                // Gray separator line
                canvas.setColorStroke(BORDER);
                canvas.setLineWidth(0.5f);
                canvas.moveTo(40, footerY + 12);
                canvas.lineTo(pageW - 40, footerY + 12);
                canvas.stroke();

                // Confidentiality notice
                canvas.beginText();
                canvas.setFontAndSize(bfNormal, 6.5f);
                canvas.setColorFill(TEXT_GRAY);
                canvas.showTextAligned(Element.ALIGN_CENTER,
                        "Document genere automatiquement par VMS - Confidentiel",
                        pageW / 2, footerY, 0f);
                canvas.endText();

                // Page number
                canvas.beginText();
                canvas.setFontAndSize(bfNormal, 7);
                canvas.setColorFill(TEXT_GRAY);
                canvas.showTextAligned(Element.ALIGN_RIGHT,
                        "Page " + writer.getPageNumber(),
                        pageW - 40, footerY, 0f);
                canvas.endText();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private static PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(LIGHT_BG);
        return cell;
    }

    private static String formatMontant(double montant) {
        return String.format("%,.2f", montant);
    }

    private static String safe(String s) {
        return (s != null && !s.isEmpty()) ? s : "---";
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        return p;
    }
}
