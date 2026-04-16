package pkg.vms;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.VoucherDAO;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Properties;

/**
 * Service d'envoi d'emails automatiques via SMTP.
 * Gère l'envoi des bons PDF au client et le récapitulatif à l'admin.
 */
public class EmailService {

    // ── Configuration SMTP (à adapter selon le serveur) ──
    // Pour Gmail, utilisez un "App Password" (Mot de passe d'application) :
    // 1. Allez sur votre compte Google > Sécurité
    // 2. Activez la Validation en deux étapes
    // 3. Recherchez "Mots de passe d'application"
    // 4. Créez-en un pour "Messagerie" et "Autre (VMS)"
    // 5. Copiez le code de 16 caractères ci-dessous
    private static final String SMTP_HOST = Config.get("smtp.host", "smtp.gmail.com");
    private static final String SMTP_PORT = Config.get("smtp.port", "587");
    private static final String SMTP_USER = Config.get("smtp.user", "vms.intermart@gmail.com");
    private static final String SMTP_PASS = Config.get("smtp.pass", "");
    private static final String FROM_NAME = Config.get("smtp.from.name", "Intermart VMS");

    private static final String ADMIN_EMAIL = Config.get("smtp.admin.email", "admin@intermart.mu");

    /**
     * Envoie les bons PDF par email au client + récap à l'admin.
     */
    public static void envoyerBonsParEmail(int demandeId, java.util.List<pkg.vms.DAO.BonDAO.BonInfo> bons, int userId) throws Exception {
        if (bons == null || bons.isEmpty()) return;

        // Récupérer les infos de la demande
        String clientEmail = bons.get(0).clientEmail;
        String clientNom   = bons.get(0).clientNom;
        String reference   = bons.get(0).reference;

        // Récupérer l'email destinataire depuis la demande
        String emailDest = getEmailDestinataire(demandeId);
        if (emailDest == null || emailDest.isEmpty()) {
            emailDest = clientEmail;
        }

        // 1. Envoyer les bons au client
        String sujet = "Vos bons cadeau Intermart — " + reference;
        String corps = buildCorpsEmailClient(clientNom, reference, bons);
        envoyerEmail(emailDest, null, sujet, corps, bons);

        // 2. Générer et envoyer le récapitulatif à l'admin
        String recapPath = VoucherPDFGenerator.genererRecapPDF(demandeId, bons);
        String sujetAdmin = "Récapitulatif génération — " + reference + " (" + bons.size() + " bons)";
        String corpsAdmin = buildCorpsEmailAdmin(reference, bons);

        pkg.vms.DAO.BonDAO.BonInfo recapBon = new pkg.vms.DAO.BonDAO.BonInfo();
        recapBon.pdfPath = recapPath;
        envoyerEmail(ADMIN_EMAIL, null, sujetAdmin, corpsAdmin, java.util.List.of(recapBon));

        // 3. Marquer comme envoyé
        pkg.vms.DAO.VoucherDAO.marquerCommeEnvoye(demandeId, userId);
    }

    /**
     * Envoie un email de notification simple (ex: notification au comptable, à l'approbateur).
     */
    public static void envoyerNotification(String destinataire, String sujet, String corps) {
        try {
            envoyerEmail(destinataire, null, sujet, corps, null);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification: " + e.getMessage());
        }
    }

    /**
     * Envoie un email avec pièces jointes optionnelles.
     */
    private static void envoyerEmail(String to, String cc, String subject, String body,
                                      List<BonDAO.BonInfo> attachments) throws Exception {
        // Si SMTP non configuré, log et skip
        if (SMTP_PASS == null || SMTP_PASS.isEmpty()) {
            System.out.println("═══════════════════════════════════════════════");
            System.out.println("EMAIL (SMTP non configuré — mode simulation)");
            System.out.println("  À: " + to);
            if (cc != null) System.out.println("  CC: " + cc);
            System.out.println("  Sujet: " + subject);
            System.out.println("  Pièces jointes: " + (attachments != null ? attachments.size() : 0));
            System.out.println("═══════════════════════════════════════════════");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SMTP_USER, FROM_NAME));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        if (cc != null && !cc.isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        message.setSubject(subject);

        // Corps HTML + pièces jointes
        Multipart multipart = new MimeMultipart();

        // Partie texte HTML
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(body, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        // Pièces jointes (PDFs)
        if (attachments != null) {
            for (BonDAO.BonInfo bon : attachments) {
                if (bon.pdfPath != null) {
                    File pdfFile = new File(bon.pdfPath);
                    if (pdfFile.exists()) {
                        MimeBodyPart attachPart = new MimeBodyPart();
                        attachPart.attachFile(pdfFile);
                        attachPart.setFileName(pdfFile.getName());
                        multipart.addBodyPart(attachPart);
                    }
                }
            }
        }

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("Email envoyé avec succès à : " + to);
    }

    private static String buildCorpsEmailClient(String clientNom, String reference, List<BonDAO.BonInfo> bons) {
        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
        return "<html><body style='font-family:Trebuchet MS,sans-serif;color:#161C2D;'>"
             + "<div style='background:#D2232D;padding:20px;text-align:center;'>"
             + "<h1 style='color:white;margin:0;font-family:Georgia,serif;'>INTERMART</h1>"
             + "<p style='color:rgba(255,255,255,0.8);margin:5px 0 0;'>Système de Gestion de Bons Cadeau</p>"
             + "</div>"
             + "<div style='padding:20px;'>"
             + "<p>Bonjour <strong>" + clientNom + "</strong>,</p>"
             + "<p>Nous avons le plaisir de vous transmettre vos bons cadeau Intermart.</p>"
             + "<table style='width:100%;border-collapse:collapse;margin:15px 0;'>"
             + "<tr style='background:#f8f9fc;'>"
             + "<td style='padding:8px;border:1px solid #e4e6ec;'><strong>Référence</strong></td>"
             + "<td style='padding:8px;border:1px solid #e4e6ec;'>" + reference + "</td></tr>"
             + "<tr><td style='padding:8px;border:1px solid #e4e6ec;'><strong>Nombre de bons</strong></td>"
             + "<td style='padding:8px;border:1px solid #e4e6ec;'>" + bons.size() + "</td></tr>"
             + "<tr style='background:#f8f9fc;'>"
             + "<td style='padding:8px;border:1px solid #e4e6ec;'><strong>Montant total</strong></td>"
             + "<td style='padding:8px;border:1px solid #e4e6ec;'>Rs " + String.format("%,.2f", total) + "</td></tr>"
             + "</table>"
             + "<p>Veuillez trouver ci-joint les bons individuels au format PDF.</p>"
             + "<p style='color:#5A6478;font-size:12px;'>"
             + "Chaque bon est à usage unique, non fractionnable, et doit être présenté en magasin pour validation."
             + "</p>"
             + "<p>Cordialement,<br/><strong>L'équipe Intermart Maurice</strong></p>"
             + "</div>"
             + "<div style='background:#f8f9fc;padding:10px;text-align:center;font-size:11px;color:#A0A8B9;'>"
             + "© 2026 Intermart Maurice — Tous droits réservés"
             + "</div></body></html>";
    }

    private static String buildCorpsEmailAdmin(String reference, List<BonDAO.BonInfo> bons) {
        return "<html><body style='font-family:Trebuchet MS,sans-serif;'>"
             + "<h2 style='color:#D2232D;'>Récapitulatif de génération — " + reference + "</h2>"
             + "<p><strong>" + bons.size() + "</strong> bon(s) ont été générés et envoyés au client.</p>"
             + "<p>Le récapitulatif PDF est en pièce jointe.</p>"
             + "<p style='color:#5A6478;font-size:12px;'>Email automatique — VMS Intermart</p>"
             + "</body></html>";
    }

    private static String getEmailDestinataire(int demandeId) {
        String sql = "SELECT email_destinataire FROM demande WHERE demande_id = ?";
        try (Connection conn = pkg.vms.DAO.DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("Erreur récupération email destinataire: " + e.getMessage());
        }
        return null;
    }

    public static void sendEmail(String clientEmail, String s, String corps) {
    }
}
