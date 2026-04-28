package pkg.vms;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Genere un recu compact (format ticket thermique) lors de la redemption
 * d'un bon cadeau en magasin. Le PDF produit contient toutes les informations
 * d'audit : code bon, valeur, magasin, superviseur et QR code de verification.
 */
public class RecuRedemptionPDFGenerator {

    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/VMS_Bons/Recus";

    // ── Palette de couleurs ──
    private static final BaseColor RED           = new BaseColor(210, 35, 45);
    private static final BaseColor GREEN_SUCCESS = new BaseColor(34, 177, 110);
    private static final BaseColor DARK_TEXT     = new BaseColor(22, 28, 45);
    private static final BaseColor GRAY          = new BaseColor(90, 100, 120);
    private static final BaseColor WHITE         = BaseColor.WHITE;

    /**
     * Informations necessaires pour generer le recu de redemption.
     */
    public static class RedemptionInfo {
        public String codeBon;           // e.g., "VR0048-200-001"
        public double valeur;            // e.g., 500.00
        public String dateRedemption;    // e.g., "14/04/2026 14:35"
        public String nomMagasin;        // e.g., "VMS Point de Vente — Ebène"
        public String codeMagasin;       // e.g., "MAG-001"
        public String superviseurNom;    // e.g., "Jean Dupont"
        public String superviseurCode;   // e.g., "USR-042"
        public String clientNom;         // e.g., "Marie Laurent"
        public String referenceDemande;  // e.g., "VR0048-200"
        public int redemptionId;         // for QR audit code
    }

    /**
     * Genere un recu de redemption PDF compact et retourne le chemin du fichier.
     *
     * @param info les informations de la redemption
     * @return le chemin absolu du fichier PDF genere
     * @throws Exception en cas d'erreur de generation
     */
    public static String genererRecu(RedemptionInfo info) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "RECU_" + info.codeBon.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        String filePath = OUTPUT_DIR + "/" + fileName;

        // Format ticket thermique compact : ~80mm x 150mm
        Rectangle pageSize = new Rectangle(226, 425);
        Document doc = new Document(pageSize, 10, 10, 8, 8);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        PdfContentByte canvas = writer.getDirectContent();
        float w = pageSize.getWidth();
        float h = pageSize.getHeight();

        // Position Y courante (on dessine de haut en bas)
        float y = h;

        // ══════════════════════════════════════════════════════════
        // 1. LOGO AREA : rectangle rouge avec "VMS" + "VOUCHER SYSTEM"
        // ══════════════════════════════════════════════════════════
        y = drawLogoArea(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 2. TITRE : "RECU DE REDEMPTION"
        // ══════════════════════════════════════════════════════════
        y = drawTitle(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 3. SEPARATEUR
        // ══════════════════════════════════════════════════════════
        y = drawDashedLine(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 4. DATE / HEURE
        // ══════════════════════════════════════════════════════════
        y = drawDateTime(canvas, w, y, info);

        // ══════════════════════════════════════════════════════════
        // 5. INFOS MAGASIN
        // ══════════════════════════════════════════════════════════
        y = drawStoreInfo(canvas, w, y, info);

        // ══════════════════════════════════════════════════════════
        // 6. SEPARATEUR
        // ══════════════════════════════════════════════════════════
        y = drawDashedLine(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 7. DETAILS DU BON
        // ══════════════════════════════════════════════════════════
        y = drawVoucherDetails(canvas, w, y, info);

        // ══════════════════════════════════════════════════════════
        // 8. SEPARATEUR
        // ══════════════════════════════════════════════════════════
        y = drawDashedLine(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 9. STATUS BOX : "VALIDE ET REDIME"
        // ══════════════════════════════════════════════════════════
        y = drawStatusBox(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 10. SUPERVISEUR
        // ══════════════════════════════════════════════════════════
        y = drawSupervisor(canvas, w, y, info);

        // ══════════════════════════════════════════════════════════
        // 11. QR CODE
        // ══════════════════════════════════════════════════════════
        y = drawQRCode(canvas, writer, w, y, info);

        // ══════════════════════════════════════════════════════════
        // 12. AVERTISSEMENT
        // ══════════════════════════════════════════════════════════
        y = drawWarning(canvas, w, y);

        // ══════════════════════════════════════════════════════════
        // 13. FOOTER
        // ══════════════════════════════════════════════════════════
        drawFooter(canvas, w, y);

        doc.close();
        return filePath;
    }

    // ========================================================================
    // METHODES DE DESSIN
    // ========================================================================

    /**
     * Dessine le logo VMS : rectangle rouge avec texte blanc.
     */
    private static float drawLogoArea(PdfContentByte canvas, float w, float y) {
        float logoH = 40;
        float logoY = y - logoH;
        float margin = 15;

        // Rectangle accent
        canvas.setColorFill(RED);
        canvas.rectangle(margin, logoY, w - 2 * margin, logoH);
        canvas.fill();

        try {
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

            // "VMS"
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 16);
            canvas.setColorFill(WHITE);
            canvas.showTextAligned(Element.ALIGN_CENTER, "VMS", w / 2, logoY + 18, 0f);
            canvas.endText();

            // "VOUCHER SYSTEM"
            canvas.beginText();
            canvas.setFontAndSize(bf, 8);
            canvas.setColorFill(new BaseColor(255, 200, 200));
            canvas.showTextAligned(Element.ALIGN_CENTER, "VOUCHER SYSTEM", w / 2, logoY + 6, 0f);
            canvas.endText();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return logoY - 6;
    }

    /**
     * Dessine le titre "RECU DE REDEMPTION" centre en gras.
     */
    private static float drawTitle(PdfContentByte canvas, float w, float y) {
        try {
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            canvas.beginText();
            canvas.setFontAndSize(bfBold, 11);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_CENTER, "RECU DE REDEMPTION", w / 2, y - 12, 0f);
            canvas.endText();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return y - 20;
    }

    /**
     * Dessine une ligne pointillee horizontale (separateur).
     */
    private static float drawDashedLine(PdfContentByte canvas, float w, float y) {
        float lineY = y - 6;
        float margin = 12;

        canvas.saveState();
        canvas.setColorStroke(GRAY);
        canvas.setLineWidth(0.5f);
        canvas.setLineDash(3, 2);
        canvas.moveTo(margin, lineY);
        canvas.lineTo(w - margin, lineY);
        canvas.stroke();
        canvas.restoreState();

        return lineY - 6;
    }

    /**
     * Dessine la date et l'heure de la redemption.
     */
    private static float drawDateTime(PdfContentByte canvas, float w, float y, RedemptionInfo info) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            // Extraire date et heure du format "dd/MM/yyyy HH:mm"
            String datePart = info.dateRedemption;
            String heurePart = "";
            if (info.dateRedemption != null && info.dateRedemption.contains(" ")) {
                String[] parts = info.dateRedemption.split(" ");
                datePart = parts[0];
                heurePart = parts[1];
            }

            float lineY = y - 2;

            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Date:", 15, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, datePart, 42, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Heure:", 120, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, heurePart, 152, lineY, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return y - 14;
    }

    /**
     * Dessine les informations du magasin (nom + code).
     */
    private static float drawStoreInfo(PdfContentByte canvas, float w, float y, RedemptionInfo info) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            float lineY = y - 2;

            // Magasin
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Magasin:", 15, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, info.nomMagasin, 60, lineY, 0f);
            canvas.endText();

            // Code magasin
            lineY -= 13;
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Code:", 15, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, info.codeMagasin, 60, lineY, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return y - 28;
    }

    /**
     * Dessine les details du bon : code, valeur, client, reference.
     */
    private static float drawVoucherDetails(PdfContentByte canvas, float w, float y, RedemptionInfo info) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);
            BaseFont bfMono = BaseFont.createFont(BaseFont.COURIER_BOLD, BaseFont.CP1252, false);

            float lineY = y - 2;
            float labelX = 15;
            float valueX = 85;

            // Code Bon (monospace)
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Code Bon:", labelX, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bfMono, 8);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, info.codeBon, valueX, lineY, 0f);
            canvas.endText();

            // Valeur (LARGE, bold, red)
            lineY -= 22;
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Valeur:", labelX, lineY + 2, 0f);
            canvas.endText();

            String montant = String.format("Rs %,.2f", info.valeur);
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 16);
            canvas.setColorFill(RED);
            canvas.showTextAligned(Element.ALIGN_LEFT, montant, valueX, lineY - 2, 0f);
            canvas.endText();

            // Client
            lineY -= 22;
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Client:", labelX, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, info.clientNom, valueX, lineY, 0f);
            canvas.endText();

            // Ref. Demande
            lineY -= 13;
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Ref. Demande:", labelX, lineY, 0f);
            canvas.endText();

            canvas.beginText();
            canvas.setFontAndSize(bf, 7.5f);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, info.referenceDemande, valueX, lineY, 0f);
            canvas.endText();

            return lineY - 8;

        } catch (Exception e) {
            e.printStackTrace();
            return y - 70;
        }
    }

    /**
     * Dessine la boite de statut verte avec "VALIDE ET REDIME" et un checkmark.
     */
    private static float drawStatusBox(PdfContentByte canvas, float w, float y) {
        float boxH = 24;
        float margin = 15;
        float boxY = y - boxH;

        // Fond vert
        canvas.setColorFill(GREEN_SUCCESS);
        canvas.roundRectangle(margin, boxY, w - 2 * margin, boxH, 4);
        canvas.fill();

        try {
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            // Checkmark unicode + texte
            canvas.beginText();
            canvas.setFontAndSize(bfBold, 11);
            canvas.setColorFill(WHITE);
            canvas.showTextAligned(Element.ALIGN_CENTER, "VALIDE ET REDIME", w / 2 + 5, boxY + 8, 0f);
            canvas.endText();

            // Checkmark (dessine manuellement)
            float cx = margin + 18;
            float cy = boxY + boxH / 2;
            canvas.setColorStroke(WHITE);
            canvas.setLineWidth(2f);
            canvas.moveTo(cx - 6, cy);
            canvas.lineTo(cx - 2, cy - 5);
            canvas.lineTo(cx + 7, cy + 5);
            canvas.stroke();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return boxY - 8;
    }

    /**
     * Dessine les informations du superviseur.
     */
    private static float drawSupervisor(PdfContentByte canvas, float w, float y, RedemptionInfo info) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            float lineY = y - 2;

            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7);
            canvas.setColorFill(GRAY);
            canvas.showTextAligned(Element.ALIGN_LEFT, "Superviseur:", 15, lineY, 0f);
            canvas.endText();

            String superviseurText = info.superviseurNom + " (" + info.superviseurCode + ")";
            canvas.beginText();
            canvas.setFontAndSize(bf, 7);
            canvas.setColorFill(DARK_TEXT);
            canvas.showTextAligned(Element.ALIGN_LEFT, superviseurText, 72, lineY, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return y - 14;
    }

    /**
     * Dessine le QR code d'audit contenant "RECU-[redemptionId]-[codeBon]".
     */
    private static float drawQRCode(PdfContentByte canvas, PdfWriter writer,
                                     float w, float y, RedemptionInfo info) {
        float qrDisplaySize = 65;
        float qrY = y - qrDisplaySize - 4;

        try {
            // Generer le contenu du QR code
            String qrData = "RECU-" + info.redemptionId + "-" + info.codeBon;

            Image qrImage = generateQRImage(qrData, 150, 150);
            qrImage.scaleToFit(qrDisplaySize, qrDisplaySize);
            float qrX = (w - qrDisplaySize) / 2;
            qrImage.setAbsolutePosition(qrX, qrY);
            canvas.addImage(qrImage);

            // Label sous le QR
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);
            canvas.beginText();
            canvas.setFontAndSize(bf, 5.5f);
            canvas.setColorFill(GRAY);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Code audit: " + qrData, w / 2, qrY - 10, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return qrY - 16;
    }

    /**
     * Dessine le message d'avertissement en rouge gras.
     */
    private static float drawWarning(PdfContentByte canvas, float w, float y) {
        try {
            BaseFont bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

            canvas.beginText();
            canvas.setFontAndSize(bfBold, 7.5f);
            canvas.setColorFill(RED);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Ce bon ne peut plus etre utilise", w / 2, y - 2, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return y - 14;
    }

    /**
     * Dessine le pied de page : "Document genere par VMS" + timestamp.
     */
    private static void drawFooter(PdfContentByte canvas, float w, float y) {
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, false);

            // Ligne de separation fine
            canvas.setColorStroke(GRAY);
            canvas.setLineWidth(0.3f);
            canvas.moveTo(15, y - 2);
            canvas.lineTo(w - 15, y - 2);
            canvas.stroke();

            // "Document genere par VMS"
            canvas.beginText();
            canvas.setFontAndSize(bf, 6);
            canvas.setColorFill(GRAY);
            canvas.showTextAligned(Element.ALIGN_CENTER, "Document genere par VMS", w / 2, y - 12, 0f);
            canvas.endText();

            // Timestamp de generation
            String timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
            canvas.beginText();
            canvas.setFontAndSize(bf, 5.5f);
            canvas.setColorFill(GRAY);
            canvas.showTextAligned(Element.ALIGN_CENTER, timestamp, w / 2, y - 21, 0f);
            canvas.endText();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ========================================================================
    // GENERATION QR CODE
    // ========================================================================

    /**
     * Genere un QR Code sous forme d'image iText a partir d'une chaine.
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
}
