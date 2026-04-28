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
import java.util.List;
import java.util.Map;

/**
 * Genere des bons cadeau PDF avec 4 styles visuels distincts.
 */
public class VoucherPDFGenerator {

    private static final String OUTPUT_DIR = System.getProperty("user.home") + "/VMS_Bons";

    // ── Enum des templates disponibles ──────────────────────────────────────
    public enum VoucherTemplate {
        PRESTIGE_OR("Prestige Or",        "Fond dore premium avec ruban rouge"),
        BLEU_CORPORATIF("Bleu Corporatif","Style entreprise bleu marine et or"),
        ROUGE_FESTIF("Rouge Festif",      "Design festif rouge et noir"),
        VERT_MODERNE("Vert Moderne",      "Style moderne vert et blanc");

        public final String label;
        public final String description;

        VoucherTemplate(String label, String description) {
            this.label       = label;
            this.description = description;
        }
    }

    // ── Palette commune ─────────────────────────────────────────────────────
    private static final BaseColor WHITE         = BaseColor.WHITE;
    private static final BaseColor INTERMART_RED = new BaseColor(210, 35, 45);

    // ── Entree principale (template par defaut) ──────────────────────────────
    public static String genererPDF(BonDAO.BonInfo bon) throws Exception {
        return genererPDF(bon, VoucherTemplate.PRESTIGE_OR);
    }

    // ── Entree principale (choix de template) ────────────────────────────────
    public static String genererPDF(BonDAO.BonInfo bon, VoucherTemplate template) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();

        String fileName = "BON_" + template.name() + "_"
                + bon.codeUnique.replaceAll("[^a-zA-Z0-9-]", "_") + ".pdf";
        String filePath = OUTPUT_DIR + "/" + fileName;

        Rectangle pageSize = new Rectangle(595, 285); // ~210x100mm paysage
        Document doc = new Document(pageSize, 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        switch (template) {
            case BLEU_CORPORATIF -> drawBlueCorporate(writer, doc, bon);
            case ROUGE_FESTIF    -> drawRedFestive(writer, doc, bon);
            case VERT_MODERNE    -> drawGreenModern(writer, doc, bon);
            default              -> drawGoldPrestige(writer, doc, bon);
        }

        doc.close();
        return filePath;
    }

    // ========================================================================
    // TEMPLATE 1 — PRESTIGE OR
    // Fond dore, ruban rouge coin haut-gauche, ornements, bordure double
    // ========================================================================

    private static void drawGoldPrestige(PdfWriter writer, Document doc, BonDAO.BonInfo bon)
            throws Exception {
        PdfContentByte cb  = writer.getDirectContent();
        PdfContentByte cbu = writer.getDirectContentUnder();
        float w = doc.getPageSize().getWidth(), h = doc.getPageSize().getHeight();

        // Couleurs
        BaseColor BG      = new BaseColor(252, 245, 220);
        BaseColor BG2     = new BaseColor(245, 228, 175);
        BaseColor GOLD_D  = new BaseColor(139, 101, 35);
        BaseColor GOLD_M  = new BaseColor(195, 155, 65);
        BaseColor RIB     = new BaseColor(180, 25, 35);
        BaseColor TXT_D   = new BaseColor(45, 35, 20);
        BaseColor TXT_M   = new BaseColor(100, 80, 50);

        // Fond dore
        cbu.setColorFill(BG);  cbu.rectangle(0,0,w,h); cbu.fill();
        cbu.setColorFill(BG2); cbu.rectangle(0,h*.65f,w,h*.35f); cbu.fill();
        cbu.setColorFill(new BaseColor(248,236,198)); cbu.rectangle(0,h*.55f,w,h*.12f); cbu.fill();

        // Motif cercles subtils
        cbu.saveState();
        cbu.setColorStroke(new BaseColor(220,200,150,50));
        cbu.setLineWidth(0.5f);
        cbu.circle(w*.55f, h*.5f, 75); cbu.stroke();
        cbu.circle(w*.55f, h*.5f, 90); cbu.stroke();
        cbu.restoreState();

        // Bordure double
        cb.setColorStroke(GOLD_M); cb.setLineWidth(3f);
        cb.rectangle(6,6,w-12,h-12); cb.stroke();
        cb.setColorStroke(GOLD_D); cb.setLineWidth(0.8f);
        cb.rectangle(12,12,w-24,h-24); cb.stroke();
        // Coins decoratifs
        float cs=14, m=12;
        cb.setColorStroke(GOLD_D); cb.setLineWidth(2f);
        cb.moveTo(m,h-m-cs); cb.lineTo(m,h-m); cb.lineTo(m+cs,h-m);
        cb.moveTo(w-m-cs,h-m); cb.lineTo(w-m,h-m); cb.lineTo(w-m,h-m-cs);
        cb.moveTo(m,m+cs); cb.lineTo(m,m); cb.lineTo(m+cs,m);
        cb.moveTo(w-m-cs,m); cb.lineTo(w-m,m); cb.lineTo(w-m,m+cs);
        cb.stroke();

        // Ruban rouge coin haut-gauche
        cb.setColorFill(new BaseColor(140,15,25));
        cb.moveTo(0,h); cb.lineTo(100,h); cb.lineTo(0,h-75); cb.fill();
        cb.setColorFill(RIB);
        cb.moveTo(0,h); cb.lineTo(125,h); cb.lineTo(0,h-95); cb.fill();
        cb.setColorFill(RIB);
        cb.moveTo(0,h-18); cb.lineTo(110,h); cb.lineTo(130,h); cb.lineTo(0,h-43); cb.fill();
        cb.setColorFill(new BaseColor(210,45,55));
        cb.moveTo(0,h-22); cb.lineTo(112,h); cb.lineTo(118,h); cb.lineTo(0,h-32); cb.fill();
        // Noeud
        cb.setColorFill(RIB); cb.circle(43,h-21,8); cb.fill();
        cb.setColorFill(new BaseColor(140,15,25)); cb.circle(43,h-21,5); cb.fill();
        cb.setColorFill(new BaseColor(210,45,55)); cb.circle(42,h-20,2.5f); cb.fill();
        cb.setColorFill(RIB); cb.circle(36,h-13,5); cb.fill(); cb.circle(50,h-13,5); cb.fill();
        cb.setColorFill(new BaseColor(140,15,25)); cb.circle(36,h-13,3); cb.fill(); cb.circle(50,h-13,3); cb.fill();

        BaseFont bfB  = BaseFont.createFont(BaseFont.TIMES_BOLD,   BaseFont.CP1252, false);
        BaseFont bfN  = BaseFont.createFont(BaseFont.HELVETICA,     BaseFont.CP1252, false);
        BaseFont bfBd = BaseFont.createFont(BaseFont.HELVETICA_BOLD,BaseFont.CP1252, false);
        BaseFont bfC  = BaseFont.createFont(BaseFont.COURIER,       BaseFont.CP1252, false);

        // Logo
        cb.setColorFill(INTERMART_RED); cb.roundRectangle(22,22,68,33,3); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfBd,7); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_CENTER,"INTERMART",56,43,0f);
        cb.setFontAndSize(bfN,5); cb.setColorFill(new BaseColor(255,200,200));
        cb.showTextAligned(Element.ALIGN_CENTER,"MAURICE",56,33,0f); cb.endText();

        // Titre "BON CADEAU"
        cb.beginText(); cb.setFontAndSize(bfB,30); cb.setColorFill(new BaseColor(160,120,40));
        cb.showTextAligned(Element.ALIGN_CENTER,"BON CADEAU",w*.48f+1,h-42,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfB,30); cb.setColorFill(GOLD_D);
        cb.showTextAligned(Element.ALIGN_CENTER,"BON CADEAU",w*.48f,h-41,0f); cb.endText();
        // Ligne et losanges sous le titre
        cb.setColorStroke(GOLD_M); cb.setLineWidth(1.5f);
        cb.moveTo(w*.25f,h-50); cb.lineTo(w*.70f,h-50); cb.stroke();
        drawDiamond(cb,w*.25f,h-50,3,GOLD_M); drawDiamond(cb,w*.70f,h-50,3,GOLD_M);
        drawDiamond(cb,w*.475f,h-50,4,GOLD_D);
        // Sous-titre
        cb.beginText(); cb.setFontAndSize(bfN,9); cb.setColorFill(TXT_M);
        cb.showTextAligned(Element.ALIGN_CENTER,"UN CADEAU SPECIAL POUR VOUS",w*.48f,h-62,0f); cb.endText();

        // Montant dans cadre
        float ax=w-152, ay=h-108, aw=132, ah=56;
        cb.setColorFill(new BaseColor(255,248,225)); cb.roundRectangle(ax,ay,aw,ah,6); cb.fill();
        cb.setColorStroke(GOLD_M); cb.setLineWidth(1.5f); cb.roundRectangle(ax,ay,aw,ah,6); cb.stroke();
        cb.beginText(); cb.setFontAndSize(bfB,22); cb.setColorFill(INTERMART_RED);
        cb.showTextAligned(Element.ALIGN_CENTER,String.format("Rs %,.0f",bon.valeur),ax+aw/2,ay+ah/2+2,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,7); cb.setColorFill(TXT_M);
        cb.showTextAligned(Element.ALIGN_CENTER,"VALEUR DU BON",ax+aw/2,ay+7,0f); cb.endText();

        // Details
        float dx=30, dy=h-88;
        drawInfoLine(cb,bfBd,bfN,"Beneficiaire:",safe(bon.clientNom),dx,dy,TXT_M,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Reference:",safe(bon.reference),dx,dy-16,TXT_M,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Emis le:",dateShort(bon.dateEmission),dx,dy-32,TXT_M,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Expire le:",dateShort(bon.dateExpiration),dx,dy-48,TXT_M,TXT_D);

        // QR code
        drawQRBlock(cb,w-118,57,85,bon.codeUnique,GOLD_M);

        // Code-barres
        drawBarcode(cb,writer,100,26,170,bon.codeUnique);

        // Footer
        cb.setColorStroke(GOLD_M); cb.setLineWidth(0.5f);
        cb.moveTo(22,55); cb.lineTo(w*.60f,55); cb.stroke();
        cb.beginText(); cb.setFontAndSize(bfN,5); cb.setColorFill(TXT_M);
        cb.showTextAligned(Element.ALIGN_LEFT,"Conditions : Usage unique, non remboursable, non fractionnable. Presentez en magasin.",22,67,0f);
        cb.showTextAligned(Element.ALIGN_LEFT,"T. +230 000 0000  |  www.intermart.mu  |  contact@intermart.mu",95,9,0f);
        cb.endText();
        cb.beginText(); cb.setFontAndSize(bfBd,6); cb.setColorFill(TXT_M);
        cb.showTextAligned(Element.ALIGN_LEFT,"Signature autorisee:",22,78,0f); cb.endText();
        cb.setColorStroke(TXT_M); cb.setLineWidth(0.3f);
        cb.setLineDash(2,2); cb.moveTo(118,78); cb.lineTo(250,78); cb.stroke();
        cb.setLineDash(0);
    }

    // ========================================================================
    // TEMPLATE 2 — BLEU CORPORATIF
    // Bande navy en haut, fond blanc, accents or, sobre et professionnel
    // ========================================================================

    private static void drawBlueCorporate(PdfWriter writer, Document doc, BonDAO.BonInfo bon)
            throws Exception {
        PdfContentByte cb  = writer.getDirectContent();
        PdfContentByte cbu = writer.getDirectContentUnder();
        float w = doc.getPageSize().getWidth(), h = doc.getPageSize().getHeight();

        BaseColor NAVY    = new BaseColor(30, 58, 95);
        BaseColor GOLD    = new BaseColor(201, 168, 76);
        BaseColor SILVER  = new BaseColor(215, 220, 230);
        BaseColor BG_BODY = new BaseColor(248, 249, 252);
        BaseColor TXT_D   = new BaseColor(22, 28, 45);
        BaseColor TXT_G   = new BaseColor(90, 100, 120);

        // Fond corps
        cbu.setColorFill(BG_BODY); cbu.rectangle(0,0,w,h); cbu.fill();

        // Bande navy superieure
        cbu.setColorFill(NAVY); cbu.rectangle(0,h-70,w,70); cbu.fill();

        // Bande or separatrice
        cbu.setColorFill(GOLD); cbu.rectangle(0,h-76,w,6); cbu.fill();

        // Bande silver inferieure
        cbu.setColorFill(SILVER); cbu.rectangle(0,0,w,42); cbu.fill();

        // Bordure navy
        cbu.setColorStroke(NAVY); cbu.setLineWidth(2f);
        cbu.rectangle(5,5,w-10,h-10); cbu.stroke();
        cbu.setColorStroke(GOLD); cbu.setLineWidth(0.8f);
        cbu.rectangle(9,9,w-18,h-18); cbu.stroke();

        BaseFont bfB  = BaseFont.createFont(BaseFont.TIMES_BOLD,    BaseFont.CP1252, false);
        BaseFont bfN  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
        BaseFont bfBd = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

        // Logo dans le header navy
        cb.setColorFill(WHITE); cb.roundRectangle(20,h-60,70,44,3); cb.fill();
        cb.setColorFill(NAVY); cb.roundRectangle(20,h-60,70,44,3); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfB,11); cb.setColorFill(GOLD);
        cb.showTextAligned(Element.ALIGN_CENTER,"INTERMART",55,h-35,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,7); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_CENTER,"VOUCHER SYSTEM",55,h-48,0f); cb.endText();

        // "GIFT VOUCHER" / "BON CADEAU" dans le header
        cb.beginText(); cb.setFontAndSize(bfB,28); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_LEFT,"BON CADEAU",110,h-38,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,9); cb.setColorFill(new BaseColor(180,195,220));
        cb.showTextAligned(Element.ALIGN_LEFT,"Systeme de Gestion de Bons",110,h-54,0f); cb.endText();

        // Montant dans cadre navy a droite du header
        float ax=w-155, ay=h-68, aw=140, ah=62;
        cb.setColorFill(GOLD); cb.roundRectangle(ax+3,ay-2,aw,ah,6); cb.fill();
        cb.setColorFill(NAVY); cb.roundRectangle(ax,ay,aw,ah,6); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfB,26); cb.setColorFill(GOLD);
        cb.showTextAligned(Element.ALIGN_CENTER,String.format("Rs %,.0f",bon.valeur),ax+aw/2,ay+ah/2+4,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,7); cb.setColorFill(new BaseColor(180,195,220));
        cb.showTextAligned(Element.ALIGN_CENTER,"VALEUR NOMINALE",ax+aw/2,ay+8,0f); cb.endText();

        // Corps : details du bon
        float startY = h-95;
        float col1 = 25, col2 = 310;

        // Ligne 1 : reference + client
        drawInfoLine(cb,bfBd,bfN,"Reference :",safe(bon.reference),col1,startY,TXT_G,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Beneficiaire :",safe(bon.clientNom),col2,startY,TXT_G,TXT_D);

        // Ligne 2 : dates
        drawInfoLine(cb,bfBd,bfN,"Date d'emission :",dateShort(bon.dateEmission),col1,startY-18,TXT_G,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Expire le :",dateShort(bon.dateExpiration),col2,startY-18,TXT_G,TXT_D);

        // QR code centree a droite
        drawQRBlock(cb,w-115,50,82,bon.codeUnique,GOLD);

        // Code-barres
        drawBarcode(cb,writer,20,50,200,bon.codeUnique);

        // Footer silver
        cb.beginText(); cb.setFontAndSize(bfN,6); cb.setColorFill(TXT_G);
        cb.showTextAligned(Element.ALIGN_CENTER,
                "Usage unique  |  Non remboursable  |  Non fractionnable  |  contact@intermart.mu",
                w/2,16,0f); cb.endText();

        // Code unique en police monospace
        cb.beginText(); cb.setFontAndSize(BaseFont.createFont(BaseFont.COURIER,BaseFont.CP1252,false),7);
        cb.setColorFill(NAVY);
        cb.showTextAligned(Element.ALIGN_CENTER,bon.codeUnique,w/2,33,0f); cb.endText();
    }

    // ========================================================================
    // TEMPLATE 3 — ROUGE FESTIF
    // Header rouge fonce, corps noir/sombre, texte or, style celebrate
    // ========================================================================

    private static void drawRedFestive(PdfWriter writer, Document doc, BonDAO.BonInfo bon)
            throws Exception {
        PdfContentByte cb  = writer.getDirectContent();
        PdfContentByte cbu = writer.getDirectContentUnder();
        float w = doc.getPageSize().getWidth(), h = doc.getPageSize().getHeight();

        BaseColor BG_DARK  = new BaseColor(25, 22, 28);
        BaseColor RED_D    = new BaseColor(150, 20, 30);
        BaseColor RED_M    = new BaseColor(190, 30, 42);
        BaseColor GOLD     = new BaseColor(201, 168, 76);
        BaseColor GOLD_L   = new BaseColor(230, 205, 130);

        // Fond sombre
        cbu.setColorFill(BG_DARK); cbu.rectangle(0,0,w,h); cbu.fill();

        // Header rouge fonce
        cbu.setColorFill(RED_D); cbu.rectangle(0,h-80,w,80); cbu.fill();
        cbu.setColorFill(RED_M); cbu.rectangle(0,h-78,w,3); cbu.fill();

        // Bordure or
        cbu.setColorStroke(GOLD); cbu.setLineWidth(2f);
        cbu.rectangle(7,7,w-14,h-14); cbu.stroke();
        cbu.setColorStroke(new BaseColor(100,80,35)); cbu.setLineWidth(0.5f);
        cbu.rectangle(11,11,w-22,h-22); cbu.stroke();

        // Decorations festives : petits triangles banner
        float flagY = h-82;
        float[] flagColors_r = {0.8f,0.2f,0.2f, 0.2f,0.7f,0.2f, 0.2f,0.2f,0.8f,
                                 0.8f,0.7f,0.1f, 0.7f,0.1f,0.7f};
        int numFlags = 10;
        float spacing = (w-60f)/numFlags;
        for (int i=0; i<numFlags; i++) {
            float fx = 30+i*spacing;
            int ci = (i%5)*3;
            cbu.setColorFill(new BaseColor(
                    (int)(flagColors_r[ci]*255),
                    (int)(flagColors_r[ci+1]*255),
                    (int)(flagColors_r[ci+2]*255)));
            cbu.moveTo(fx,flagY+12); cbu.lineTo(fx+spacing*.4f,flagY); cbu.lineTo(fx+spacing*.8f,flagY+12); cbu.fill();
        }

        BaseFont bfB  = BaseFont.createFont(BaseFont.TIMES_BOLD,    BaseFont.CP1252, false);
        BaseFont bfN  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
        BaseFont bfBd = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

        // Logo
        cb.setColorFill(GOLD); cb.roundRectangle(20,h-68,75,48,3); cb.fill();
        cb.setColorFill(RED_D); cb.roundRectangle(21,h-67,73,46,3); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfB,11); cb.setColorFill(GOLD);
        cb.showTextAligned(Element.ALIGN_CENTER,"INTERMART",57,h-43,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,6); cb.setColorFill(new BaseColor(230,200,200));
        cb.showTextAligned(Element.ALIGN_CENTER,"GIFT VOUCHER",57,h-56,0f); cb.endText();

        // "BON CADEAU" tres grand en or
        cb.beginText(); cb.setFontAndSize(bfB,32); cb.setColorFill(new BaseColor(80,60,20));
        cb.showTextAligned(Element.ALIGN_LEFT,"BON CADEAU",113,h-38,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfB,32); cb.setColorFill(GOLD);
        cb.showTextAligned(Element.ALIGN_LEFT,"BON CADEAU",111,h-37,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,9); cb.setColorFill(GOLD_L);
        cb.showTextAligned(Element.ALIGN_LEFT,"Offert par Intermart Maurice",113,h-54,0f); cb.endText();

        // Montant : grand, en or
        float ax=w-165, ay=h-72, aw=148, ah=66;
        cb.setColorFill(GOLD); cb.roundRectangle(ax,ay,aw,ah,6); cb.fill();
        cb.setColorFill(RED_D); cb.roundRectangle(ax+2,ay+2,aw-4,ah-4,5); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfB,28); cb.setColorFill(GOLD);
        cb.showTextAligned(Element.ALIGN_CENTER,String.format("Rs %,.0f",bon.valeur),ax+aw/2,ay+ah/2+4,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,7); cb.setColorFill(GOLD_L);
        cb.showTextAligned(Element.ALIGN_CENTER,"VALEUR DU BON",ax+aw/2,ay+8,0f); cb.endText();

        // Details
        float startY=h-102;
        drawInfoLine(cb,bfBd,bfN,"Beneficiaire :",safe(bon.clientNom),25,startY,new BaseColor(180,160,120),GOLD_L);
        drawInfoLine(cb,bfBd,bfN,"Reference :",safe(bon.reference),25,startY-16,new BaseColor(180,160,120),GOLD_L);
        drawInfoLine(cb,bfBd,bfN,"Emis le :",dateShort(bon.dateEmission),25,startY-32,new BaseColor(180,160,120),GOLD_L);
        drawInfoLine(cb,bfBd,bfN,"Expire le :",dateShort(bon.dateExpiration),25,startY-48,new BaseColor(180,160,120),GOLD_L);

        // QR code avec fond blanc
        cb.setColorFill(WHITE); cb.roundRectangle(w-116,52,96,96,4); cb.fill();
        drawQRBlock(cb,w-112,55,88,bon.codeUnique,GOLD);

        // Code-barres
        drawBarcode(cb,writer,20,28,220,bon.codeUnique);

        // Footer
        cb.beginText(); cb.setFontAndSize(bfN,5.5f); cb.setColorFill(new BaseColor(120,110,100));
        cb.showTextAligned(Element.ALIGN_CENTER,
                "Usage unique  |  Non remboursable  |  contact@intermart.mu",
                w/2,15,0f); cb.endText();
    }

    // ========================================================================
    // TEMPLATE 4 — VERT MODERNE
    // Fond blanc, bande verte a gauche, design minimaliste moderne
    // ========================================================================

    private static void drawGreenModern(PdfWriter writer, Document doc, BonDAO.BonInfo bon)
            throws Exception {
        PdfContentByte cb  = writer.getDirectContent();
        PdfContentByte cbu = writer.getDirectContentUnder();
        float w = doc.getPageSize().getWidth(), h = doc.getPageSize().getHeight();

        BaseColor GREEN   = new BaseColor(46, 125, 50);
        BaseColor GREEN_L = new BaseColor(200, 230, 201);
        BaseColor GREEN_D = new BaseColor(27, 94, 32);
        BaseColor TXT_D   = new BaseColor(30, 30, 30);
        BaseColor TXT_G   = new BaseColor(100, 110, 100);

        // Fond blanc
        cbu.setColorFill(WHITE); cbu.rectangle(0,0,w,h); cbu.fill();

        // Bande verte gauche
        cbu.setColorFill(GREEN); cbu.rectangle(0,0,90,h); cbu.fill();

        // Accent vert clair en haut
        cbu.setColorFill(GREEN_L); cbu.rectangle(90,h-8,w-90,8); cbu.fill();

        // Bordure fine verte
        cbu.setColorStroke(GREEN); cbu.setLineWidth(1.5f);
        cbu.rectangle(4,4,w-8,h-8); cbu.stroke();

        BaseFont bfB  = BaseFont.createFont(BaseFont.TIMES_BOLD,    BaseFont.CP1252, false);
        BaseFont bfN  = BaseFont.createFont(BaseFont.HELVETICA,      BaseFont.CP1252, false);
        BaseFont bfBd = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, false);

        // Logo dans la bande verte (vertical)
        cb.beginText(); cb.setFontAndSize(bfB,13); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_CENTER,"INTER",45,h/2+15,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfB,13); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_CENTER,"MART",45,h/2,0f); cb.endText();
        // Ligne decorative sous le logo
        cb.setColorStroke(new BaseColor(100,180,105)); cb.setLineWidth(1f);
        cb.moveTo(20,h/2-8); cb.lineTo(70,h/2-8); cb.stroke();
        cb.beginText(); cb.setFontAndSize(bfN,6); cb.setColorFill(new BaseColor(180,230,182));
        cb.showTextAligned(Element.ALIGN_CENTER,"BON CADEAU",45,h/2-20,0f); cb.endText();

        // Titre
        cb.beginText(); cb.setFontAndSize(bfB,28); cb.setColorFill(GREEN_D);
        cb.showTextAligned(Element.ALIGN_LEFT,"BON CADEAU",108,h-38,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,9); cb.setColorFill(TXT_G);
        cb.showTextAligned(Element.ALIGN_LEFT,"Intermart Maurice — Voucher System",108,h-52,0f); cb.endText();

        // Montant dans un cadre vert arrondi
        float ax=w-165, ay=h-80, aw=148, ah=68;
        cb.setColorFill(GREEN); cb.roundRectangle(ax,ay,aw,ah,8); cb.fill();
        cb.beginText(); cb.setFontAndSize(bfB,26); cb.setColorFill(WHITE);
        cb.showTextAligned(Element.ALIGN_CENTER,String.format("Rs %,.0f",bon.valeur),ax+aw/2,ay+ah/2+4,0f); cb.endText();
        cb.beginText(); cb.setFontAndSize(bfN,7); cb.setColorFill(GREEN_L);
        cb.showTextAligned(Element.ALIGN_CENTER,"VALEUR DU BON",ax+aw/2,ay+9,0f); cb.endText();

        // Details
        float sx=108, sy=h-72;
        drawInfoLine(cb,bfBd,bfN,"Beneficiaire :",safe(bon.clientNom),sx,sy,TXT_G,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Reference :",safe(bon.reference),sx,sy-16,TXT_G,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Emis le :",dateShort(bon.dateEmission),sx,sy-32,TXT_G,TXT_D);
        drawInfoLine(cb,bfBd,bfN,"Expire le :",dateShort(bon.dateExpiration),sx,sy-48,TXT_G,TXT_D);

        // QR code
        drawQRBlock(cb,w-115,52,82,bon.codeUnique,GREEN);

        // Code-barres
        drawBarcode(cb,writer,95,28,175,bon.codeUnique);

        // Footer
        cb.setColorStroke(GREEN_L); cb.setLineWidth(0.5f);
        cb.moveTo(93,20); cb.lineTo(w-10,20); cb.stroke();
        cb.beginText(); cb.setFontAndSize(bfN,5.5f); cb.setColorFill(TXT_G);
        cb.showTextAligned(Element.ALIGN_LEFT,
                "Usage unique  |  Non remboursable  |  contact@intermart.mu",
                95,10,0f); cb.endText();
    }

    // ========================================================================
    // METHODES UTILITAIRES COMMUNES
    // ========================================================================

    private static void drawDiamond(PdfContentByte cb, float cx, float cy, float s, BaseColor c) {
        cb.setColorFill(c);
        cb.moveTo(cx,cy+s); cb.lineTo(cx+s,cy); cb.lineTo(cx,cy-s); cb.lineTo(cx-s,cy); cb.fill();
    }

    private static void drawInfoLine(PdfContentByte cb, BaseFont bfLabel, BaseFont bfValue,
            String label, String value, float x, float y, BaseColor cLabel, BaseColor cValue) {
        try {
            cb.beginText(); cb.setFontAndSize(bfLabel,7.5f); cb.setColorFill(cLabel);
            cb.showTextAligned(Element.ALIGN_LEFT,label,x,y,0f); cb.endText();
            cb.beginText(); cb.setFontAndSize(bfValue,8.5f); cb.setColorFill(cValue);
            cb.showTextAligned(Element.ALIGN_LEFT,value,x+100,y,0f); cb.endText();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private static void drawQRBlock(PdfContentByte cb, float qx, float qy,
            float qrSize, String code, BaseColor borderColor) throws Exception {
        cb.setColorFill(WHITE); cb.roundRectangle(qx-5,qy-5,qrSize+10,qrSize+10,4); cb.fill();
        cb.setColorStroke(borderColor); cb.setLineWidth(1.5f);
        cb.roundRectangle(qx-5,qy-5,qrSize+10,qrSize+10,4); cb.stroke();
        Image qr = generateQRImage(code,200,200);
        qr.setAbsolutePosition(qx,qy);
        qr.scaleToFit(qrSize,qrSize);
        cb.addImage(qr);
        try {
            BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA,BaseFont.CP1252,false);
            cb.beginText(); cb.setFontAndSize(bf,5); cb.setColorFill(new BaseColor(130,130,130));
            cb.showTextAligned(Element.ALIGN_CENTER,"Scannez pour verifier",qx+qrSize/2,qy-12,0f); cb.endText();
        } catch (Exception ignored) {}
    }

    private static void drawBarcode(PdfContentByte cb, PdfWriter writer,
            float bx, float by, float maxW, String code) throws Exception {
        Barcode128 bc = new Barcode128();
        bc.setCode(code);
        bc.setCodeType(Barcode128.CODE128);
        bc.setBarHeight(20f);
        bc.setX(0.85f);
        bc.setFont(null);
        java.awt.Image awt = bc.createAwtImage(java.awt.Color.BLACK, java.awt.Color.WHITE);
        BufferedImage bi = new BufferedImage(awt.getWidth(null),awt.getHeight(null),BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2 = bi.createGraphics();
        g2.setColor(java.awt.Color.WHITE); g2.fillRect(0,0,bi.getWidth(),bi.getHeight());
        g2.drawImage(awt,0,0,null); g2.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi,"PNG",baos);
        Image img = Image.getInstance(baos.toByteArray());
        img.scaleToFit(maxW,22);
        img.setAbsolutePosition(bx,by);
        cb.addImage(img);
        // Code en texte
        try {
            BaseFont bfC = BaseFont.createFont(BaseFont.COURIER,BaseFont.CP1252,false);
            String disp = code.length()>38 ? code.substring(0,38)+"..." : code;
            cb.beginText(); cb.setFontAndSize(bfC,5); cb.setColorFill(new BaseColor(120,120,120));
            cb.showTextAligned(Element.ALIGN_CENTER,disp,bx+maxW/2,by-9,0f); cb.endText();
        } catch (Exception ignored) {}
    }

    private static Image generateQRImage(String data, int w, int h) throws Exception {
        Map<EncodeHintType,Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN,1);
        hints.put(EncodeHintType.CHARACTER_SET,"UTF-8");
        BitMatrix matrix = new MultiFormatWriter().encode(data,BarcodeFormat.QR_CODE,w,h,hints);
        BufferedImage bi = MatrixToImageWriter.toBufferedImage(matrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bi,"PNG",baos);
        return Image.getInstance(baos.toByteArray());
    }

    private static String safe(String s) { return (s!=null&&!s.isEmpty())?s:"---"; }

    private static String dateShort(String ts) {
        if (ts==null||ts.length()<10) return "---";
        return ts.substring(0,10);
    }

    // ========================================================================
    // PDF RECAP (inchange)
    // ========================================================================

    public static String genererRecapPDF(int demandeId, List<BonDAO.BonInfo> bons) throws Exception {
        File dir = new File(OUTPUT_DIR);
        if (!dir.exists()) dir.mkdirs();
        String filePath = OUTPUT_DIR + "/RECAP_DEMANDE_" + demandeId + ".pdf";

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();

        BaseColor RED  = new BaseColor(210,35,45);
        BaseColor DARK = new BaseColor(22,28,45);
        BaseColor GRAY = new BaseColor(90,100,120);

        Font fTitle = new Font(Font.FontFamily.TIMES_ROMAN, 20, Font.BOLD, RED);
        Font fSub   = new Font(Font.FontFamily.HELVETICA,   10, Font.NORMAL, GRAY);
        Font fHead  = new Font(Font.FontFamily.HELVETICA,    9, Font.BOLD,   WHITE);
        Font fCell  = new Font(Font.FontFamily.HELVETICA,    9, Font.NORMAL, DARK);

        Paragraph title = new Paragraph("RECAPITULATIF DE GENERATION", fTitle);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);
        doc.add(new Paragraph("Demande #" + demandeId + " -- " + bons.size() + " bon(s)", fSub));
        doc.add(new Paragraph("Date : " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date()), fSub));
        doc.add(Chunk.NEWLINE);
        if (!bons.isEmpty()) {
            doc.add(new Paragraph("Client : " + bons.get(0).clientNom, fSub));
            doc.add(new Paragraph("Reference : " + bons.get(0).reference, fSub));
            doc.add(Chunk.NEWLINE);
        }

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{8, 35, 15, 20, 22});
        for (String h : new String[]{"#","Code Unique","Valeur (Rs)","Emission","Expiration"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, fHead));
            c.setBackgroundColor(RED); c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }
        int i=1;
        for (BonDAO.BonInfo b : bons) {
            table.addCell(recapCell(String.valueOf(i++),fCell));
            table.addCell(recapCell(b.codeUnique,fCell));
            table.addCell(recapCell(String.format("%,.2f",b.valeur),fCell));
            table.addCell(recapCell(dateShort(b.dateEmission),fCell));
            table.addCell(recapCell(dateShort(b.dateExpiration),fCell));
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);
        double total = bons.stream().mapToDouble(b->b.valeur).sum();
        Paragraph tot = new Paragraph(String.format("Montant total : Rs %,.2f", total),
                new Font(Font.FontFamily.TIMES_ROMAN,14,Font.BOLD,RED));
        tot.setAlignment(Element.ALIGN_RIGHT);
        doc.add(tot);
        doc.close();
        return filePath;
    }

    private static PdfPCell recapCell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text,font));
        c.setPadding(5); c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }
}
