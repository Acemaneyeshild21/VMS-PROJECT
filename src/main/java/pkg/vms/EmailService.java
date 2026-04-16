package pkg.vms;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.SettingsDAO;
import pkg.vms.DAO.VoucherDAO;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Properties;

/**
 * Service d'envoi d'emails automatiques via SMTP.
 * La config est chargée dynamiquement depuis la BD à chaque envoi (jamais en static).
 */
public class EmailService {

    /**
     * Config SMTP immuable. Chargée depuis BD ou config.properties à chaque appel.
     */
    private record SmtpConfig(
            String host,
            int port,
            String user,
            String pass,
            boolean tls,
            String fromEmail,
            String fromName,
            String adminEmail
    ) {
        boolean isConfigured() {
            return host != null && !host.isBlank()
                && user != null && !user.isBlank()
                && pass != null && !pass.isBlank();
        }
    }

    /**
     * Charge la config SMTP depuis la BD d'abord, puis config.properties en fallback.
     */
    private static SmtpConfig loadConfig() {
        try {
            SettingsDAO.EmailSettings s = SettingsDAO.getEmailSettings();
            if (s != null && s.smtpPassword != null && !s.smtpPassword.isBlank()) {
                return new SmtpConfig(
                        s.smtpServer  != null ? s.smtpServer  : "smtp.gmail.com",
                        s.smtpPort > 0        ? s.smtpPort    : 587,
                        s.smtpUsername,
                        s.smtpPassword,
                        s.tlsEnabled,
                        s.fromEmail   != null ? s.fromEmail   : s.smtpUsername,
                        s.fromName    != null ? s.fromName    : "Intermart VMS",
                        s.adminEmail  != null ? s.adminEmail  : s.smtpUsername
                );
            }
        } catch (Exception ignored) {}

        // Fallback : config.properties
        return new SmtpConfig(
                Config.get("smtp.host",        "smtp.gmail.com"),
                Integer.parseInt(Config.get("smtp.port", "587")),
                Config.get("smtp.user",        ""),
                Config.get("smtp.pass",        ""),
                true,
                Config.get("smtp.from.email",  ""),
                Config.get("smtp.from.name",   "Intermart VMS"),
                Config.get("smtp.admin.email", "")
        );
    }

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Retourne true si au moins un mot de passe SMTP est configuré.
     */
    public static boolean isConfigured() {
        return loadConfig().isConfigured();
    }

    /**
     * Envoie les bons PDF par email au client + récap à l'admin.
     */
    public static void envoyerBonsParEmail(int demandeId,
                                           List<BonDAO.BonInfo> bons,
                                           int userId) throws Exception {
        if (bons == null || bons.isEmpty()) return;

        SmtpConfig cfg = loadConfig();

        String clientEmail = bons.get(0).clientEmail;
        String clientNom   = bons.get(0).clientNom;
        String reference   = bons.get(0).reference;

        // Destinataire prioritaire : colonne email_destinataire sur la demande
        String emailDest = getEmailDestinataire(demandeId);
        if (emailDest == null || emailDest.isBlank()) emailDest = clientEmail;

        // 1. Email client avec les bons en PJ
        String sujet = "Vos bons cadeau Intermart \u2014 " + reference;
        String corps = buildCorpsEmailClient(clientNom, reference, bons);
        envoyerEmail(cfg, emailDest, null, sujet, corps, bons);

        // 2. Email admin avec récapitulatif
        if (cfg.adminEmail() != null && !cfg.adminEmail().isBlank()) {
            String recapPath = VoucherPDFGenerator.genererRecapPDF(demandeId, bons);
            BonDAO.BonInfo recapBon = new BonDAO.BonInfo();
            recapBon.pdfPath = recapPath;
            String sujetAdmin = "Recap generation \u2014 " + reference + " (" + bons.size() + " bons)";
            envoyerEmail(cfg, cfg.adminEmail(), null, sujetAdmin,
                    buildCorpsEmailAdmin(reference, bons), List.of(recapBon));
        }

        // 3. Marquer comme envoyé en BD
        VoucherDAO.marquerCommeEnvoye(demandeId, userId);
    }

    /**
     * Envoie un email de notification simple (pour workflow : approbation, refus…).
     */
    public static void envoyerNotification(String destinataire, String sujet, String corps) {
        try {
            envoyerEmail(loadConfig(), destinataire, null, sujet, corps, null);
        } catch (Exception e) {
            System.err.println("Erreur envoi notification: " + e.getMessage());
        }
    }

    /**
     * Envoie un email de test avec les paramètres fournis (depuis ParametresPanel).
     *
     * @param settings   config telle que saisie dans l'interface
     * @param destinataire adresse de test (en général : l'utilisateur connecté)
     * @return null si succès, message d'erreur lisible sinon
     */
    public static String envoyerEmailTest(SettingsDAO.EmailSettings settings,
                                          String destinataire) {
        if (destinataire == null || destinataire.isBlank()) {
            return "Veuillez saisir une adresse email de test.";
        }

        SmtpConfig cfg = new SmtpConfig(
                settings.smtpServer,
                settings.smtpPort,
                settings.smtpUsername,
                settings.smtpPassword,
                settings.tlsEnabled,
                settings.fromEmail  != null ? settings.fromEmail  : settings.smtpUsername,
                settings.fromName   != null ? settings.fromName   : "Intermart VMS",
                settings.adminEmail != null ? settings.adminEmail : settings.smtpUsername
        );

        if (!cfg.isConfigured()) {
            return "Configuration incomplète : serveur, utilisateur et mot de passe sont obligatoires.";
        }

        String sujet = "[VMS TEST] Configuration email Intermart";
        String corps = "<html><body style='font-family:Arial,sans-serif;color:#161C2D;'>"
            + "<div style='background:#D2232D;padding:16px;text-align:center;'>"
            + "<h2 style='color:white;margin:0;'>INTERMART — Test Email</h2></div>"
            + "<div style='padding:20px;'>"
            + "<p>Cet email confirme que votre configuration SMTP est <strong>correctement parametree</strong>.</p>"
            + "<table style='border-collapse:collapse;width:100%;margin:15px 0;'>"
            + "<tr><td style='padding:6px;border:1px solid #ddd;'><b>Serveur</b></td>"
            + "<td style='padding:6px;border:1px solid #ddd;'>" + cfg.host() + ":" + cfg.port() + "</td></tr>"
            + "<tr><td style='padding:6px;border:1px solid #ddd;'><b>Utilisateur</b></td>"
            + "<td style='padding:6px;border:1px solid #ddd;'>" + cfg.user() + "</td></tr>"
            + "<tr><td style='padding:6px;border:1px solid #ddd;'><b>TLS</b></td>"
            + "<td style='padding:6px;border:1px solid #ddd;'>" + (cfg.tls() ? "Oui" : "Non") + "</td></tr>"
            + "</table>"
            + "<p style='color:#5A6478;font-size:12px;'>Email automatique — VMS Intermart</p>"
            + "</div></body></html>";

        try {
            envoyerEmail(cfg, destinataire, null, sujet, corps, null);
            return null; // succès
        } catch (AuthenticationFailedException e) {
            return "Echec d'authentification SMTP.\n"
                 + "Verifiez le mot de passe (pour Gmail : utilisez un App Password, pas votre mot de passe principal).\n"
                 + "Detail: " + e.getMessage();
        } catch (MessagingException e) {
            return "Erreur SMTP : " + e.getMessage();
        } catch (Exception e) {
            return "Erreur inattendue : " + e.getMessage();
        }
    }

    // ── Méthodes internes ─────────────────────────────────────────────────────

    private static void envoyerEmail(SmtpConfig cfg, String to, String cc,
                                     String subject, String body,
                                     List<BonDAO.BonInfo> attachments) throws Exception {
        if (!cfg.isConfigured()) {
            System.out.println("══════════════════════════════════════════");
            System.out.println("EMAIL (mode simulation — SMTP non configure)");
            System.out.println("  A: " + to + "  Sujet: " + subject);
            System.out.println("══════════════════════════════════════════");
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",                 "true");
        props.put("mail.smtp.starttls.enable",      cfg.tls() ? "true" : "false");
        props.put("mail.smtp.ssl.protocols",        "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.host",                 cfg.host());
        props.put("mail.smtp.port",                 String.valueOf(cfg.port()));
        props.put("mail.smtp.connectiontimeout",    "10000");
        props.put("mail.smtp.timeout",              "15000");
        props.put("mail.smtp.writetimeout",         "15000");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.user(), cfg.pass());
            }
        });

        Message message = new MimeMessage(session);
        String encodedFromName = MimeUtility.encodeText(cfg.fromName(), "UTF-8", "B");
        message.setFrom(new InternetAddress(cfg.fromEmail(), encodedFromName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        if (cc != null && !cc.isBlank()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
        }
        message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
        message.setSentDate(new java.util.Date());

        Multipart multipart = new MimeMultipart();

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(body, "text/html; charset=UTF-8");
        multipart.addBodyPart(htmlPart);

        if (attachments != null) {
            for (BonDAO.BonInfo bon : attachments) {
                if (bon.pdfPath != null) {
                    File pdfFile = new File(bon.pdfPath);
                    if (pdfFile.exists()) {
                        MimeBodyPart attachPart = new MimeBodyPart();
                        attachPart.attachFile(pdfFile);
                        String safeName = MimeUtility.encodeText(pdfFile.getName(), "UTF-8", "B");
                        attachPart.setFileName(safeName);
                        multipart.addBodyPart(attachPart);
                    }
                }
            }
        }

        message.setContent(multipart);
        Transport.send(message);
        System.out.println("Email envoye a : " + to);
    }

    private static String buildCorpsEmailClient(String clientNom, String reference,
                                                 List<BonDAO.BonInfo> bons) {
        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
        StringBuilder bonsList = new StringBuilder();
        for (BonDAO.BonInfo b : bons) {
            bonsList.append("<tr><td style='padding:6px;border:1px solid #e4e6ec;'>")
                    .append(b.codeUnique).append("</td>")
                    .append("<td style='padding:6px;border:1px solid #e4e6ec;text-align:right;'>Rs ")
                    .append(String.format("%,.2f", b.valeur)).append("</td></tr>");
        }
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='margin:0;padding:0;font-family:Trebuchet MS,sans-serif;color:#161C2D;background:#f5f5f5;'>"
            + "<div style='max-width:600px;margin:20px auto;background:white;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
            + "<div style='background:#D2232D;padding:24px;text-align:center;'>"
            + "<h1 style='color:white;margin:0;font-family:Georgia,serif;letter-spacing:2px;'>INTERMART</h1>"
            + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Systeme de Gestion de Bons Cadeau</p>"
            + "</div>"
            + "<div style='padding:28px;'>"
            + "<p style='font-size:16px;'>Bonjour <strong>" + clientNom + "</strong>,</p>"
            + "<p>Nous avons le plaisir de vous transmettre vos bons cadeau Intermart.</p>"
            + "<table style='width:100%;border-collapse:collapse;margin:16px 0;font-size:14px;'>"
            + "<tr style='background:#f8f9fc;'>"
            + "<td style='padding:8px;border:1px solid #e4e6ec;'><strong>Reference</strong></td>"
            + "<td style='padding:8px;border:1px solid #e4e6ec;'>" + reference + "</td></tr>"
            + "<tr><td style='padding:8px;border:1px solid #e4e6ec;'><strong>Nombre de bons</strong></td>"
            + "<td style='padding:8px;border:1px solid #e4e6ec;'>" + bons.size() + "</td></tr>"
            + "<tr style='background:#f8f9fc;'>"
            + "<td style='padding:8px;border:1px solid #e4e6ec;'><strong>Montant total</strong></td>"
            + "<td style='padding:8px;border:1px solid #e4e6ec;'><strong>Rs " + String.format("%,.2f", total) + "</strong></td></tr>"
            + "</table>"
            + "<p style='font-weight:600;margin-top:20px;'>Detail des bons :</p>"
            + "<table style='width:100%;border-collapse:collapse;font-size:13px;'>"
            + "<tr style='background:#D2232D;color:white;'>"
            + "<th style='padding:8px;border:1px solid #c00;text-align:left;'>Code bon</th>"
            + "<th style='padding:8px;border:1px solid #c00;text-align:right;'>Valeur</th></tr>"
            + bonsList
            + "</table>"
            + "<div style='background:#fff8e1;border-left:4px solid #FFC107;padding:12px 16px;margin:20px 0;border-radius:4px;font-size:13px;'>"
            + "<strong>Instructions de validation :</strong><br>"
            + "1. Chaque bon est individuel, a usage unique et non fractionnable.<br>"
            + "2. Presentez le PDF imprime ou votre ecran en caisse.<br>"
            + "3. Validite selon les conditions generales en vigueur."
            + "</div>"
            + "<p>Cordialement,<br><strong>L'equipe Intermart Maurice</strong></p>"
            + "</div>"
            + "<div style='background:#f8f9fc;padding:12px;text-align:center;font-size:11px;color:#A0A8B9;'>"
            + "&#169; 2026 Intermart Maurice &#8212; Tous droits reserves"
            + "</div></div></body></html>";
    }

    private static String buildCorpsEmailAdmin(String reference, List<BonDAO.BonInfo> bons) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
            + "<body style='font-family:Trebuchet MS,sans-serif;color:#161C2D;'>"
            + "<h2 style='color:#D2232D;'>Recapitulatif de generation &#8212; " + reference + "</h2>"
            + "<p><strong>" + bons.size() + "</strong> bon(s) ont ete generes et envoyes au client.</p>"
            + "<p>Le recapitulatif PDF est en piece jointe.</p>"
            + "<p style='color:#5A6478;font-size:12px;'>Email automatique &#8212; VMS Intermart</p>"
            + "</body></html>";
    }

    /**
     * Alias kept for backward compatibility with NotificationService.
     */
    public static void sendEmail(String to, String subject, String body) {
        envoyerNotification(to, subject, body);
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
            System.err.println("Erreur recuperation email destinataire: " + e.getMessage());
        }
        return null;
    }
}
