package pkg.vms;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.EmailLogDAO;
import pkg.vms.DAO.SettingsDAO;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Properties;

/**
 * Service d'envoi d'emails SMTP pour VMS.
 *
 * Priorite de configuration :
 *   1. Base de donnees (app_settings, setting_key='email') — configurable via Parametres
 *   2. config.properties (smtp.host / smtp.port / smtp.user / smtp.pass) — fallback
 *
 * Pour Gmail :
 *   - Activer la validation en deux etapes sur le compte Google
 *   - Creer un "Mot de passe d'application" (Securite > Mots de passe d'applications)
 *   - Utiliser ce code de 16 caracteres comme mot de passe SMTP
 */
public class EmailService {

    // ── Chargement dynamique de la config SMTP ────────────────────────────────

    /**
     * Charge la config SMTP depuis la BD, avec fallback sur config.properties.
     * Appele a chaque envoi pour prendre en compte les modifications en cours de session.
     */
    private static SmtpConfig loadConfig() {
        // Essayer la BD d'abord
        try {
            SettingsDAO.EmailSettings dbSettings = SettingsDAO.getEmailSettings();
            if (dbSettings != null
                    && dbSettings.smtpServer != null && !dbSettings.smtpServer.isEmpty()
                    && dbSettings.smtpUsername != null && !dbSettings.smtpUsername.isEmpty()
                    && dbSettings.smtpPassword != null && !dbSettings.smtpPassword.isEmpty()) {
                return new SmtpConfig(
                        dbSettings.smtpServer,
                        dbSettings.smtpPort > 0 ? dbSettings.smtpPort : 587,
                        dbSettings.smtpUsername,
                        dbSettings.smtpPassword,
                        dbSettings.tlsEnabled,
                        dbSettings.fromEmail != null ? dbSettings.fromEmail : dbSettings.smtpUsername,
                        dbSettings.fromName  != null ? dbSettings.fromName  : "Intermart VMS",
                        dbSettings.adminEmail
                );
            }
        } catch (Exception e) {
            System.err.println("[EmailService] Impossible de charger la config depuis la BD : " + e.getMessage());
        }

        // Fallback : config.properties
        return new SmtpConfig(
                Config.get("smtp.host",       "smtp.gmail.com"),
                Integer.parseInt(Config.get("smtp.port", "587")),
                Config.get("smtp.user",       "dimitriaceman614@gmail.com"),
                Config.get("smtp.pass",       "rjlz olyf xhjq viaj"),
                true,
                Config.get("smtp.user",       "dimitriaceman614@gmail.com"),
                Config.get("smtp.from.name",  "Intermart VMS"),
                Config.get("smtp.admin.email","dimitriaceman614@gmail.com")
        );
    }

    /** Conteneur immuable pour la config SMTP. */
    private record SmtpConfig(
            String host, int port, String user, String pass,
            boolean tls, String fromEmail, String fromName, String adminEmail
    ) {
        boolean isConfigured() {
            return pass != null && !pass.isEmpty()
                && user != null && !user.isEmpty()
                && host != null && !host.isEmpty();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // API PUBLIQUE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Envoie les bons PDF par email au client + recapitulatif a l'admin.
     * Marque la demande comme ENVOYE en BD apres succes.
     */
    public static void envoyerBonsParEmail(int demandeId,
                                            List<BonDAO.BonInfo> bons,
                                            int userId) throws Exception {
        if (bons == null || bons.isEmpty()) return;

        SmtpConfig cfg = loadConfig();

        String clientEmail = bons.get(0).clientEmail;
        String clientNom   = bons.get(0).clientNom;
        String reference   = bons.get(0).reference;

        // Priorite : email_destinataire de la demande, sinon email du client
        String emailDest = getEmailDestinataire(demandeId);
        if (emailDest == null || emailDest.isEmpty()) emailDest = clientEmail;

        if (emailDest == null || emailDest.isEmpty()) {
            throw new IllegalStateException("Aucune adresse email destinataire pour la demande #" + demandeId);
        }

        // 1. Envoyer les bons au client
        String sujet = "Vos bons cadeau Intermart — " + reference;
        String corps = buildCorpsEmailClient(clientNom, reference, bons);
        envoyerEmail(cfg, emailDest, null, sujet, corps, bons, demandeId, userId);

        // 2. Recapitulatif a l'admin
        if (cfg.adminEmail() != null && !cfg.adminEmail().isEmpty()) {
            String recapPath   = VoucherPDFGenerator.genererRecapPDF(demandeId, bons);
            String sujetAdmin  = "Recap generation — " + reference + " (" + bons.size() + " bons)";
            String corpsAdmin  = buildCorpsEmailAdmin(reference, bons);
            BonDAO.BonInfo recap = new BonDAO.BonInfo();
            recap.pdfPath = recapPath;
            envoyerEmail(cfg, cfg.adminEmail(), null, sujetAdmin, corpsAdmin, List.of(recap), demandeId, userId);
        }

        // 3. Marquer comme envoye
        pkg.vms.DAO.VoucherDAO.marquerCommeEnvoye(demandeId, userId);
    }

    /**
     * Envoie un email de notification simple (sans piece jointe).
     * Ne leve pas d'exception — echec silencieux avec log.
     */
    public static void envoyerNotification(String destinataire, String sujet, String corps) {
        try {
            SmtpConfig cfg = loadConfig();
            envoyerEmail(cfg, destinataire, null, sujet, corps, null, null, null);
        } catch (Exception e) {
            System.err.println("[EmailService] Notification non envoyee : " + e.getMessage());
        }
    }

    /**
     * Envoie un email de test a l'adresse indiquee.
     * Utilise la config passee en parametre (depuis le dialog de parametres).
     *
     * @return null si succes, message d'erreur sinon
     */
    public static String envoyerEmailTest(SettingsDAO.EmailSettings settings, String destinataire) {
        try {
            SmtpConfig cfg = new SmtpConfig(
                    settings.smtpServer,
                    settings.smtpPort > 0 ? settings.smtpPort : 587,
                    settings.smtpUsername,
                    settings.smtpPassword,
                    settings.tlsEnabled,
                    settings.fromEmail != null ? settings.fromEmail : settings.smtpUsername,
                    settings.fromName  != null ? settings.fromName  : "Intermart VMS",
                    settings.adminEmail
            );
            if (!cfg.isConfigured()) return "Configuration SMTP incomplete (serveur, utilisateur ou mot de passe manquant)";
            String corps = buildCorpsEmailTest(destinataire);
            envoyerEmail(cfg, destinataire, null, "[VMS] Test de configuration email", corps, null, null, null);
            return null; // succes
        } catch (AuthenticationFailedException e) {
            return "Authentification echouee. Verifiez le mot de passe SMTP (Gmail : utilisez un App Password).";
        } catch (MessagingException e) {
            return "Erreur SMTP : " + e.getMessage();
        } catch (Exception e) {
            return "Erreur : " + e.getMessage();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ENVOI INTERNE
    // ════════════════════════════════════════════════════════════════════════

    private static void envoyerEmail(SmtpConfig cfg,
                                      String to, String cc,
                                      String subject, String body,
                                      List<BonDAO.BonInfo> attachments,
                                      Integer demandeId, Integer userId) throws Exception {
        int nbPJ = attachments != null ? attachments.size() : 0;

        // Mode simulation si SMTP non configure
        if (!cfg.isConfigured()) {
            System.out.println("══════════════════════════════════════════════════");
            System.out.println("[VMS] EMAIL — mode simulation (SMTP non configure)");
            System.out.println("  A       : " + to);
            if (cc != null) System.out.println("  CC      : " + cc);
            System.out.println("  Sujet   : " + subject);
            System.out.println("  PJ      : " + nbPJ + " fichier(s)");
            System.out.println("══════════════════════════════════════════════════");
            System.out.println("[VMS] Pour activer l'envoi reel :");
            System.out.println("      Parametres > Configuration Email > renseigner SMTP");
            EmailLogDAO.log(demandeId, to, cc, subject, EmailLogDAO.STATUT_SIMULATION,
                    "SMTP non configure", nbPJ, userId);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth",                "true");
        props.put("mail.smtp.starttls.enable",     cfg.tls() ? "true" : "false");
        props.put("mail.smtp.host",                cfg.host());
        props.put("mail.smtp.port",                String.valueOf(cfg.port()));
        props.put("mail.smtp.connectiontimeout",   "10000"); // 10 s
        props.put("mail.smtp.timeout",             "15000"); // 15 s
        props.put("mail.smtp.writetimeout",        "15000");
        if (cfg.tls()) props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.user(), cfg.pass());
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(cfg.fromEmail(),
                    MimeUtility.encodeText(cfg.fromName(), "UTF-8", "B"), "UTF-8"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            if (cc != null && !cc.isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
            }
            message.setSubject(MimeUtility.encodeText(subject, "UTF-8", "B"));
            message.setSentDate(new java.util.Date());

            // Corps HTML + pieces jointes
            Multipart multipart = new MimeMultipart();

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(body, "text/html; charset=UTF-8");
            multipart.addBodyPart(htmlPart);

            if (attachments != null) {
                for (BonDAO.BonInfo bon : attachments) {
                    if (bon.pdfPath != null) {
                        File pdfFile = new File(bon.pdfPath);
                        if (pdfFile.exists()) {
                            MimeBodyPart part = new MimeBodyPart();
                            part.attachFile(pdfFile);
                            part.setFileName(MimeUtility.encodeText(pdfFile.getName(), "UTF-8", "B"));
                            multipart.addBodyPart(part);
                        }
                    }
                }
            }

            message.setContent(multipart);
            Transport.send(message);
            System.out.println("[VMS] Email envoye avec succes a : " + to);
            EmailLogDAO.log(demandeId, to, cc, subject, EmailLogDAO.STATUT_ENVOYE,
                    null, nbPJ, userId);
        } catch (Exception ex) {
            EmailLogDAO.log(demandeId, to, cc, subject, EmailLogDAO.STATUT_ECHEC,
                    ex.getMessage(), nbPJ, userId);
            throw ex;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // TEMPLATES HTML
    // ════════════════════════════════════════════════════════════════════════

    private static String buildCorpsEmailClient(String clientNom, String reference,
                                                 List<BonDAO.BonInfo> bons) {
        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
        String expiration = bons.isEmpty() ? "---"
                : (bons.get(0).dateExpiration != null && bons.get(0).dateExpiration.length() >= 10
                   ? bons.get(0).dateExpiration.substring(0, 10) : "---");

        StringBuilder bonRows = new StringBuilder();
        for (BonDAO.BonInfo b : bons) {
            bonRows.append("<tr>")
                   .append("<td style='padding:8px 12px;border-bottom:1px solid #eee;font-family:monospace;font-size:12px;'>")
                   .append(b.codeUnique).append("</td>")
                   .append("<td style='padding:8px 12px;border-bottom:1px solid #eee;text-align:right;'>")
                   .append(String.format("Rs %,.2f", b.valeur)).append("</td>")
                   .append("</tr>");
        }

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
             + "<body style='margin:0;padding:0;background:#f5f6fa;font-family:Trebuchet MS,Arial,sans-serif;'>"
             + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#f5f6fa;'><tr><td align='center' style='padding:30px 10px;'>"
             + "<table width='600' cellpadding='0' cellspacing='0' style='background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.08);'>"
             // Header
             + "<tr><td style='background:#D2232D;padding:28px 36px;text-align:center;'>"
             + "<h1 style='color:#ffffff;margin:0;font-family:Georgia,serif;font-size:26px;letter-spacing:2px;'>INTERMART</h1>"
             + "<p style='color:rgba(255,255,255,0.85);margin:6px 0 0;font-size:13px;'>Systeme de Gestion de Bons Cadeau</p>"
             + "</td></tr>"
             // Salutation
             + "<tr><td style='padding:30px 36px 0;'>"
             + "<p style='color:#161C2D;font-size:15px;margin:0 0 12px;'>Bonjour <strong>" + esc(clientNom) + "</strong>,</p>"
             + "<p style='color:#5A6478;font-size:14px;line-height:1.6;margin:0 0 24px;'>"
             + "Nous avons le plaisir de vous transmettre vos <strong>" + bons.size() + " bon(s) cadeau</strong> Intermart. "
             + "Veuillez trouver les bons en pieces jointes (un PDF par bon).</p>"
             // Recapitulatif
             + "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid #e4e6ec;border-radius:6px;margin-bottom:24px;'>"
             + "<tr style='background:#f8f9fc;'>"
             + "<td style='padding:10px 16px;font-size:12px;font-weight:bold;color:#5A6478;text-transform:uppercase;letter-spacing:1px;'>Detail</td>"
             + "<td style='padding:10px 16px;font-size:12px;font-weight:bold;color:#5A6478;text-transform:uppercase;letter-spacing:1px;'>Valeur</td></tr>"
             + "<tr><td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>Reference</td>"
             + "<td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;font-family:monospace;'>" + esc(reference) + "</td></tr>"
             + "<tr style='background:#f8f9fc;'><td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>Nombre de bons</td>"
             + "<td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>" + bons.size() + "</td></tr>"
             + "<tr><td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>Valeur totale</td>"
             + "<td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#D2232D;font-weight:bold;font-size:16px;'>Rs " + String.format("%,.2f", total) + "</td></tr>"
             + "<tr style='background:#f8f9fc;'><td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>Valable jusqu'au</td>"
             + "<td style='padding:10px 16px;border-top:1px solid #e4e6ec;color:#161C2D;'>" + expiration + "</td></tr>"
             + "</table>"
             // Liste des codes
             + "<p style='color:#161C2D;font-size:14px;font-weight:bold;margin:0 0 10px;'>Vos codes de bons :</p>"
             + "<table width='100%' cellpadding='0' cellspacing='0' style='border:1px solid #e4e6ec;border-radius:6px;margin-bottom:24px;'>"
             + "<tr style='background:#161C2D;'>"
             + "<td style='padding:8px 12px;color:#fff;font-size:12px;font-weight:bold;'>Code unique</td>"
             + "<td style='padding:8px 12px;color:#fff;font-size:12px;font-weight:bold;text-align:right;'>Valeur</td></tr>"
             + bonRows
             + "</table>"
             // Instructions
             + "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom:24px;'>"
             + "<tr><td style='background:#fff8f8;border-left:4px solid #D2232D;padding:16px;border-radius:4px;'>"
             + "<p style='color:#161C2D;font-weight:bold;font-size:14px;margin:0 0 10px;'>Comment utiliser vos bons :</p>"
             + "<ol style='color:#5A6478;font-size:13px;line-height:1.8;margin:0;padding-left:18px;'>"
             + "<li>Imprimez le PDF du bon ou conservez-le sur votre telephone</li>"
             + "<li>Presentez-le a la caisse d'un magasin Intermart participant</li>"
             + "<li>Le superviseur scannera le QR code pour validation</li>"
             + "</ol>"
             + "</td></tr></table>"
             // Mentions
             + "<p style='color:#A0A8B9;font-size:11px;line-height:1.6;'>Chaque bon est a usage unique, non fractionnable et non remboursable.</p>"
             + "<p style='color:#161C2D;font-size:14px;margin:20px 0 0;'>Cordialement,<br/><strong>L'equipe Intermart Maurice</strong></p>"
             + "</td></tr>"
             // Footer
             + "<tr><td style='background:#f8f9fc;padding:16px 36px;text-align:center;border-top:1px solid #e4e6ec;'>"
             + "<p style='color:#A0A8B9;font-size:11px;margin:0;'>© 2026 Intermart Maurice — Tous droits reserves</p>"
             + "<p style='color:#A0A8B9;font-size:11px;margin:4px 0 0;'>contact@intermart.mu | +230 000 0000 | www.intermart.mu</p>"
             + "</td></tr>"
             + "</table></td></tr></table></body></html>";
    }

    private static String buildCorpsEmailAdmin(String reference, List<BonDAO.BonInfo> bons) {
        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
        return "<!DOCTYPE html><html><body style='font-family:Trebuchet MS,Arial,sans-serif;color:#161C2D;'>"
             + "<div style='background:#D2232D;padding:16px 24px;'>"
             + "<h2 style='color:#fff;margin:0;'>VMS — Recap generation</h2></div>"
             + "<div style='padding:20px 24px;'>"
             + "<p>Reference : <strong>" + esc(reference) + "</strong></p>"
             + "<p><strong>" + bons.size() + "</strong> bon(s) generes — Total : <strong>Rs "
             + String.format("%,.2f", total) + "</strong></p>"
             + "<p style='color:#5A6478;font-size:12px;'>Le recapitulatif PDF est en piece jointe.</p>"
             + "<p style='color:#A0A8B9;font-size:11px;'>Email automatique — VMS Intermart</p>"
             + "</div></body></html>";
    }

    private static String buildCorpsEmailTest(String destinataire) {
        return "<!DOCTYPE html><html><body style='font-family:Trebuchet MS,Arial,sans-serif;'>"
             + "<div style='background:#D2232D;padding:20px 30px;text-align:center;'>"
             + "<h1 style='color:#fff;margin:0;font-family:Georgia,serif;'>INTERMART VMS</h1></div>"
             + "<div style='padding:24px 30px;'>"
             + "<h2 style='color:#161C2D;'>Test de configuration email</h2>"
             + "<p style='color:#5A6478;'>Cet email confirme que la configuration SMTP est correcte.</p>"
             + "<div style='background:#f0fff4;border-left:4px solid #22b16e;padding:14px;border-radius:4px;margin:16px 0;'>"
             + "<strong style='color:#22b16e;'>Configuration operationnelle !</strong><br>"
             + "<span style='color:#5A6478;font-size:13px;'>Les bons cadeau pourront etre envoyes a : " + esc(destinataire) + "</span>"
             + "</div>"
             + "<p style='color:#A0A8B9;font-size:12px;'>Email automatique — VMS Intermart</p>"
             + "</div></body></html>";
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private static String getEmailDestinataire(int demandeId) {
        String sql = "SELECT email_destinataire FROM demande WHERE demande_id = ?";
        try (Connection conn = pkg.vms.DAO.DBconnect.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, demandeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("[EmailService] Email destinataire : " + e.getMessage());
        }
        return null;
    }

    /** Echappe les caracteres HTML pour eviter les injections. */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Indique si le service email est configure et pret a envoyer.
     * Utile pour afficher un badge d'etat dans l'UI.
     */
    public static boolean isConfigured() {
        return loadConfig().isConfigured();
    }
}
