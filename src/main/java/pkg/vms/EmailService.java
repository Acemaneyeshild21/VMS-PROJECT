package pkg.vms;

/*
 * ═══════════════════════════════════════════════════════════════════════════════
 *  EmailService — Service d'envoi SMTP production-ready
 *  BTS SIO SLAM RP2 — VMS Intermart Maurice, Session 2026
 * ═══════════════════════════════════════════════════════════════════════════════
 *
 *  ACTIVATION GMAIL EN PRODUCTION — App Password (obligatoire depuis mai 2022)
 *  ─────────────────────────────────────────────────────────────────────────────
 *  Gmail interdit l'authentification avec votre mot de passe normal.
 *  Il faut un "Mot de passe d'application" de 16 caractères généré depuis
 *  votre compte Google. La Validation en 2 étapes doit être activée au préalable.
 *
 *  ÉTAPES :
 *   1. Ouvrez https://myaccount.google.com
 *   2. Panneau gauche → "Sécurité"
 *   3. Section "Comment vous connectez-vous à Google"
 *      → Cliquez "Validation en 2 étapes" → Activez-la si ce n'est pas fait
 *   4. Revenez sur Sécurité → cherchez "Mots de passe des applications"
 *      URL directe : https://myaccount.google.com/apppasswords
 *   5. Cliquez "Sélectionner une application" → "Autre (nom personnalisé)"
 *      → Tapez "VMS" → Cliquez "Générer"
 *   6. Copiez le code de 16 caractères affiché (ex : abcd efgh ijkl mnop)
 *   7. Dans config.properties, renseignez :
 *        mail.username=votre.compte@gmail.com
 *        mail.password=abcdefghijklmnop      ← coller sans les espaces
 *        mail.from=votre.compte@gmail.com
 *
 *  ⚠️  NE JAMMAIS committer config.properties avec mail.password renseigné !
 *      Ajoutez config.properties à .gitignore ou utilisez une variable d'env.
 *
 *  ALTERNATIVE — Mailgun (5 000 emails/mois gratuits, plus fiable en prod réelle) :
 *    mail.smtp.host=smtp.mailgun.org
 *    mail.smtp.port=587
 *    mail.username=postmaster@mg.votre-domaine.com
 *    mail.password=<API Key Mailgun SMTP>
 * ═══════════════════════════════════════════════════════════════════════════════
 */

import jakarta.mail.*;
import jakarta.mail.internet.*;
import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.EmailDAO;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Service d'envoi d'emails SMTP production-ready.
 *
 * <p>Tous les envois sont <b>asynchrones</b> (pool de 3 threads dédiés).
 * Les callbacks {@code onSuccess} / {@code onError} sont toujours rappelés
 * sur l'EDT Swing, prêts à mettre à jour l'interface sans risque.</p>
 *
 * <p>Si {@code mail.password} est vide dans {@code config.properties},
 * l'email est loggué en console (mode simulation) sans jamais lancer d'erreur.</p>
 *
 * <p>En cas d'échec SMTP réel, l'erreur est persistée dans la table
 * {@code email_errors} pour relance ultérieure via {@link #resendEmail}.</p>
 */
public class EmailService {

    // ── Config SMTP ──────────────────────────────────────────────────────────
    private static final String SMTP_HOST    = Config.get("mail.smtp.host",           "smtp.gmail.com");
    private static final String SMTP_PORT    = Config.get("mail.smtp.port",           "587");
    private static final String SMTP_USER    = Config.get("mail.username",            "");
    private static final String SMTP_PASS    = Config.get("mail.password",            "");
    private static final String FROM_ADDR    = Config.get("mail.from",               SMTP_USER);
    private static final String FROM_NAME    = Config.get("mail.from.name",          "Intermart VMS");
    private static final String ADMIN_EMAIL  = Config.get("mail.admin.email",        "admin@intermart.mu");
    private static final int    TIMEOUT_MS   = Config.getInt("mail.timeout",          10_000);
    private static final int    CONN_TIMEOUT = Config.getInt("mail.connection.timeout", 10_000);

    /** true si SMTP non configuré → mode simulation (log console uniquement) */
    private static final boolean SIMULATION  = SMTP_PASS == null || SMTP_PASS.isBlank();

    /** Pool de 3 threads dédiés email — daemon, ne bloque pas l'EDT */
    private static final ExecutorService EMAIL_EXECUTOR =
            Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "vms-email");
                t.setDaemon(true);
                return t;
            });

    // ── Types d'email ────────────────────────────────────────────────────────
    public enum EmailType {
        VOUCHER, APPROVAL_REQUEST, PAYMENT_CONFIRMATION, ADMIN_RECAP
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. sendVoucherEmail — Bons PDF au client
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie les bons PDF au client de façon asynchrone.
     *
     * @param toEmail     Adresse destinataire (client)
     * @param clientName  Prénom / Nom affiché dans l'email
     * @param bonsPDF     Liste des fichiers PDF à joindre (peut être vide)
     * @param demandeRef  Référence de la demande (ex: "DEM-2026-0042")
     * @param valeurTotale Montant total affiché dans l'email
     * @param demandeId   ID de la demande pour l'audit
     * @param userId      ID de l'utilisateur déclencheur
     * @param onSuccess   Callback EDT — appelé si envoi OK
     * @param onError     Callback EDT — appelé avec le message d'erreur si échec
     */
    public static void sendVoucherEmail(String toEmail, String clientName,
                                        List<File> bonsPDF, String demandeRef,
                                        double valeurTotale,
                                        int demandeId, int userId,
                                        Runnable onSuccess, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                String subject = "Vos bons cadeaux Intermart — " + demandeRef;
                int nb = bonsPDF == null ? 0 : bonsPDF.size();
                String body = buildVoucherTemplate(clientName, demandeRef, nb, valeurTotale);
                sendMessage(toEmail, subject, body, bonsPDF, demandeId, EmailType.VOUCHER);
                AuditDAO.logEnvoi(demandeId, true, userId,
                        "Vouchers envoyés à " + toEmail + " (" + nb + " PDF)");
                SwingUtilities.invokeLater(onSuccess);
            } catch (Exception ex) {
                persistError(demandeId, toEmail, demandeRef, EmailType.VOUCHER, ex);
                AuditDAO.logEnvoi(demandeId, false, userId, ex.getMessage());
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    /**
     * Variante avec {@link BonDAO.BonInfo} — interopérabilité avec GestionDemande.
     * Envoie les bons au client ET le récap PDF à l'admin dans la même opération.
     * Le bon N'est PAS marqué "ENVOYE" ici : c'est l'appelant qui le fait
     * uniquement si le callback onSuccess est déclenché (garantie de cohérence).
     */
    public static void envoyerBonsParEmail(int demandeId, List<BonDAO.BonInfo> bons,
                                           int userId,
                                           Runnable onSuccess, Consumer<String> onError) {
        if (bons == null || bons.isEmpty()) {
            SwingUtilities.invokeLater(onSuccess);
            return;
        }
        EMAIL_EXECUTOR.submit(() -> {
            try {
                BonDAO.BonInfo first = bons.get(0);
                String dest    = (first.clientEmail != null && !first.clientEmail.isBlank())
                                 ? first.clientEmail : "";
                String ref     = first.reference != null ? first.reference : "DEM-" + demandeId;
                double total   = bons.stream().mapToDouble(b -> b.valeur).sum();
                List<File> pdfs = bons.stream()
                        .filter(b -> b.pdfPath != null)
                        .map(b -> new File(b.pdfPath))
                        .filter(File::exists)
                        .toList();

                // Email client
                sendMessage(dest,
                        "Vos bons cadeaux Intermart — " + ref,
                        buildVoucherTemplate(first.clientNom, ref, bons.size(), total),
                        pdfs, demandeId, EmailType.VOUCHER);

                // Email récap admin
                String recapPath = VoucherPDFGenerator.genererRecapPDF(demandeId, bons);
                List<File> recapPdfs = (recapPath != null && new File(recapPath).exists())
                        ? List.of(new File(recapPath)) : List.of();
                sendMessage(ADMIN_EMAIL,
                        "Récapitulatif génération — " + ref + " (" + bons.size() + " bons)",
                        buildAdminRecapTemplate(ref, bons.size(),
                                bons.isEmpty() ? 0 : bons.get(0).valeur, total),
                        recapPdfs, demandeId, EmailType.ADMIN_RECAP);

                AuditDAO.logEnvoi(demandeId, true, userId,
                        "Emails envoyés : client=" + dest + ", admin=" + ADMIN_EMAIL);
                SwingUtilities.invokeLater(onSuccess);

            } catch (Exception ex) {
                BonDAO.BonInfo first = bons.get(0);
                String ref = first.reference != null ? first.reference : "DEM-" + demandeId;
                persistError(demandeId,
                        first.clientEmail != null ? first.clientEmail : "",
                        ref, EmailType.VOUCHER, ex);
                AuditDAO.logEnvoi(demandeId, false, userId, ex.getMessage());
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. sendApprovalRequestEmail — Notification à l'approbateur
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Notifie l'approbateur qu'une demande attend son action dans VMS.
     */
    public static void sendApprovalRequestEmail(String toEmail, String approbateurName,
                                                String demandeRef, double montantTotal,
                                                int demandeId, int userId,
                                                Runnable onSuccess, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                sendMessage(toEmail,
                        "Action requise — Demande " + demandeRef + " en attente d'approbation",
                        buildApprovalTemplate(approbateurName, demandeRef, montantTotal),
                        null, demandeId, EmailType.APPROVAL_REQUEST);
                AuditDAO.logSimple("demande", demandeId, "EMAIL_APPROBATION", userId,
                        "Notification approbation → " + toEmail);
                SwingUtilities.invokeLater(onSuccess);
            } catch (Exception ex) {
                persistError(demandeId, toEmail, demandeRef, EmailType.APPROVAL_REQUEST, ex);
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. sendPaymentConfirmationEmail — Confirmation de paiement validé
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Confirme qu'un paiement a été validé dans VMS (envoyé au comptable / demandeur).
     */
    public static void sendPaymentConfirmationEmail(String toEmail, String comptableName,
                                                    String demandeRef,
                                                    int demandeId, int userId,
                                                    Runnable onSuccess, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                sendMessage(toEmail,
                        "✅ Paiement confirmé — " + demandeRef,
                        buildPaymentConfirmTemplate(comptableName, demandeRef),
                        null, demandeId, EmailType.PAYMENT_CONFIRMATION);
                AuditDAO.logSimple("demande", demandeId, "EMAIL_PAIEMENT", userId,
                        "Confirmation paiement → " + toEmail);
                SwingUtilities.invokeLater(onSuccess);
            } catch (Exception ex) {
                persistError(demandeId, toEmail, demandeRef, EmailType.PAYMENT_CONFIRMATION, ex);
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. sendRecapEmail — Récapitulatif PDF à l'admin
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envoie le récapitulatif de génération à l'administrateur avec PDF joint.
     */
    public static void sendRecapEmail(String adminEmail, String demandeRef,
                                      int nombreBons, double valeurUnitaire,
                                      File recapPdf,
                                      int demandeId, int userId,
                                      Runnable onSuccess, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                double total = nombreBons * valeurUnitaire;
                List<File> attach = (recapPdf != null && recapPdf.exists())
                        ? List.of(recapPdf) : List.of();
                sendMessage(adminEmail,
                        "Récapitulatif génération bons — " + demandeRef,
                        buildAdminRecapTemplate(demandeRef, nombreBons, valeurUnitaire, total),
                        attach, demandeId, EmailType.ADMIN_RECAP);
                AuditDAO.logSimple("demande", demandeId, "EMAIL_RECAP_ADMIN", userId,
                        "Récap admin → " + adminEmail);
                SwingUtilities.invokeLater(onSuccess);
            } catch (Exception ex) {
                persistError(demandeId, adminEmail, demandeRef, EmailType.ADMIN_RECAP, ex);
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  5. resendEmail — Relance un email en erreur
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Relit l'enregistrement {@code email_errors} identifié par {@code emailErrorId},
     * reconstruit l'email depuis la base de données et le renvoie.
     * Incrémente {@code nb_tentatives} en cas d'échec, marque {@code resolved=true}
     * si la relance réussit.
     */
    public static void resendEmail(long emailErrorId, int userId,
                                   Runnable onSuccess, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                EmailDAO.EmailError err = EmailDAO.getError(emailErrorId);
                if (err == null)
                    throw new Exception("Erreur email #" + emailErrorId + " introuvable en base.");

                switch (err.emailType) {
                    case VOUCHER -> {
                        List<BonDAO.BonInfo> bons = BonDAO.getBonsByDemande(err.demandeId);
                        if (bons.isEmpty())
                            throw new Exception("Aucun bon trouvé pour demande #" + err.demandeId);
                        BonDAO.BonInfo first = bons.get(0);
                        List<File> pdfs = bons.stream()
                                .filter(b -> b.pdfPath != null)
                                .map(b -> new File(b.pdfPath))
                                .filter(File::exists)
                                .toList();
                        double total = bons.stream().mapToDouble(b -> b.valeur).sum();
                        sendMessage(err.toEmail,
                                "Vos bons cadeaux Intermart — " + err.demandeRef,
                                buildVoucherTemplate(first.clientNom, err.demandeRef,
                                        bons.size(), total),
                                pdfs, err.demandeId, EmailType.VOUCHER);
                    }
                    case APPROVAL_REQUEST -> sendMessage(err.toEmail,
                            "Action requise — Demande " + err.demandeRef,
                            buildApprovalTemplate("", err.demandeRef, 0),
                            null, err.demandeId, EmailType.APPROVAL_REQUEST);

                    case PAYMENT_CONFIRMATION -> sendMessage(err.toEmail,
                            "✅ Paiement confirmé — " + err.demandeRef,
                            buildPaymentConfirmTemplate("", err.demandeRef),
                            null, err.demandeId, EmailType.PAYMENT_CONFIRMATION);

                    case ADMIN_RECAP -> sendMessage(err.toEmail,
                            "Récapitulatif génération bons — " + err.demandeRef,
                            buildAdminRecapTemplate(err.demandeRef, 0, 0, 0),
                            null, err.demandeId, EmailType.ADMIN_RECAP);
                }

                EmailDAO.markResolved(emailErrorId);
                AuditDAO.logSimple("demande", err.demandeId, "EMAIL_RELANCE", userId,
                        "Relance #" + emailErrorId + " réussie → " + err.toEmail);
                SwingUtilities.invokeLater(onSuccess);

            } catch (Exception ex) {
                EmailDAO.incrementTentatives(emailErrorId, ex.getMessage());
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  6. testConnection — Test SMTP (pour ParametresPanel)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ouvre une connexion SMTP et la ferme immédiatement, SANS envoyer d'email.
     *
     * @return {@code true} si la connexion SMTP réussit
     * @throws Exception message d'erreur détaillé si échec
     */
    public static boolean testConnection() throws Exception {
        if (SIMULATION)
            throw new Exception(
                "SMTP non configuré — renseignez mail.password dans config.properties.\n" +
                "Voir le commentaire en tête de EmailService.java pour la procédure Gmail App Password.");
        Session session = buildSession();
        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(SMTP_HOST, SMTP_USER, SMTP_PASS);
        }
        return true;
    }

    /**
     * Version asynchrone de {@link #testConnection} pour ParametresController.
     */
    public static void testConnectionAsync(Consumer<Boolean> onResult, Consumer<String> onError) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                boolean ok = testConnection();
                SwingUtilities.invokeLater(() -> onResult.accept(ok));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> onError.accept(ex.getMessage()));
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Notification simple (best-effort, sans callback)
    // ─────────────────────────────────────────────────────────────────────────

    /** Envoie une notification simple sans pièce jointe ni callback. */
    public static void envoyerNotification(String destinataire, String sujet, String corps) {
        EMAIL_EXECUTOR.submit(() -> {
            try {
                sendMessage(destinataire, sujet, corps, null, 0, null);
            } catch (Exception e) {
                System.err.println("[EmailService] Notification échouée → " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Méthodes privées — Session SMTP & envoi
    // ─────────────────────────────────────────────────────────────────────────

    private static Session buildSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",               "true");
        props.put("mail.smtp.starttls.enable",    "true");
        props.put("mail.smtp.starttls.required",  "true");
        props.put("mail.smtp.host",               SMTP_HOST);
        props.put("mail.smtp.port",               SMTP_PORT);
        props.put("mail.smtp.ssl.trust",          SMTP_HOST);
        props.put("mail.smtp.timeout",            String.valueOf(TIMEOUT_MS));
        props.put("mail.smtp.connectiontimeout",  String.valueOf(CONN_TIMEOUT));
        props.put("mail.smtp.writetimeout",       String.valueOf(TIMEOUT_MS));
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USER, SMTP_PASS);
            }
        });
    }

    /**
     * Méthode centrale : construit le message MIME et l'envoie via SMTP.
     * En mode simulation (mail.password vide), log en console et retourne sans erreur.
     */
    private static void sendMessage(String to, String subject, String htmlBody,
                                    List<File> attachments,
                                    int demandeId, EmailType type) throws Exception {
        // ── Mode simulation ──────────────────────────────────────────────────
        if (SIMULATION) {
            System.out.println("┌─ EMAIL SIMULATION ─────────────────────────────────────");
            System.out.println("│  À       : " + to);
            System.out.println("│  Sujet   : " + subject);
            System.out.println("│  Type    : " + type);
            System.out.println("│  PJ      : " + (attachments != null ? attachments.size() : 0) + " fichier(s)");
            System.out.println("│  → Renseignez mail.password dans config.properties pour activer l'envoi réel.");
            System.out.println("└────────────────────────────────────────────────────────");
            return;
        }

        if (to == null || to.isBlank())
            throw new MessagingException("Adresse email destinataire vide ou nulle.");

        // ── Construction du message MIME ─────────────────────────────────────
        Session session = buildSession();
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(FROM_ADDR, FROM_NAME, "UTF-8"));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject(subject, "UTF-8");

        // Corps HTML
        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(htmlPart);

        // Pièces jointes PDF
        if (attachments != null) {
            for (File f : attachments) {
                if (f != null && f.exists() && f.canRead()) {
                    MimeBodyPart attach = new MimeBodyPart();
                    attach.attachFile(f);
                    // Encode le nom de fichier en Base64 pour les clients non-Latin (RFC 2047)
                    attach.setFileName(MimeUtility.encodeText(f.getName(), "UTF-8", "B"));
                    multipart.addBodyPart(attach);
                }
            }
        }

        msg.setContent(multipart);
        msg.saveChanges();
        Transport.send(msg);
        System.out.println("[EmailService] ✅ Envoyé → " + to + " | " + subject);
    }

    private static void persistError(int demandeId, String toEmail, String demandeRef,
                                     EmailType type, Exception ex) {
        try {
            EmailDAO.saveError(demandeId, toEmail, demandeRef, type,
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
        } catch (Exception e) {
            System.err.println("[EmailService] Impossible de persister l'erreur en base : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Templates HTML — compatibles TOUS clients email
    //  (Gmail, Apple Mail iPhone/iPad, Huawei Mail, Samsung Mail,
    //   Outlook, Yahoo Mail, tous les webmails)
    //
    //  RÈGLES :
    //  ✅ Layout 100% <table> — aucun Flexbox, aucun CSS Grid
    //  ✅ Styles 100% inline — aucune balise <style> externe
    //  ✅ Largeur max 600px, centré sur la page
    //  ✅ Police Arial / Helvetica (universelle, présente sur tous les OS)
    //  ✅ Couleurs en HEX (#RRGGBB), jamais rgba()
    //  ✅ Données dynamiques échappées via escHtml()
    //  ❌ Pas de media queries dans le corps de l'email
    //  ❌ Pas de JavaScript
    // ─────────────────────────────────────────────────────────────────────────

    /** Template 1 — Bons cadeaux envoyés au client */
    private static String buildVoucherTemplate(String clientName, String demandeRef,
                                               int nombreBons, double valeurTotale) {
        String nbStr   = nombreBons + " bon" + (nombreBons > 1 ? "s" : "");
        String pjStr   = nombreBons + " pièce" + (nombreBons > 1 ? "s" : "") +
                         " jointe" + (nombreBons > 1 ? "s" : "");
        return "<!DOCTYPE html>" +
        "<html lang=\"fr\"><head>" +
        "<meta charset=\"UTF-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
        "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
        "<title>Vos bons cadeaux Intermart</title></head>" +
        "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +

        // ── Wrapper ──
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f4f4f4;padding:20px 0;\">" +
        "<tr><td align=\"center\">" +

        // ── Container 600px ──
        "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #dddddd;\">" +

        // HEADER
        "<tr><td style=\"background-color:#D2232D;padding:30px 30px 28px;text-align:center;\">" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\"><tr><td align=\"center\">" +
        "<h1 style=\"color:#ffffff;font-size:28px;margin:0;font-family:Georgia,serif;letter-spacing:3px;font-weight:bold;\">INTERMART</h1>" +
        "<p style=\"color:#f8c8c8;font-size:13px;margin:8px 0 0;letter-spacing:1px;\">Système de gestion de bons cadeaux</p>" +
        "</td></tr></table></td></tr>" +

        // BODY
        "<tr><td style=\"padding:32px 30px;\">" +
        "<p style=\"color:#222222;font-size:16px;margin:0 0 14px;\">Bonjour <strong>" + escHtml(clientName) + "</strong>,</p>" +
        "<p style=\"color:#555555;font-size:14px;line-height:1.75;margin:0 0 24px;\">" +
        "Nous avons le plaisir de vous transmettre vos bons cadeaux Intermart pour la demande " +
        "<strong>" + escHtml(demandeRef) + "</strong>. " +
        "Vous trouverez <strong>" + nbStr + "</strong> en " + pjStr + " à cet email.</p>" +

        // Info box
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"background-color:#fff5f5;border-left:4px solid #D2232D;border-radius:4px;margin:0 0 20px;\">" +
        "<tr><td style=\"padding:16px;\">" +
        "<p style=\"color:#D2232D;font-size:13px;margin:0 0 10px;\"><strong>&#128203; Détails de votre commande</strong></p>" +
        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
        "<tr><td style=\"color:#777777;font-size:13px;padding:3px 0;width:160px;\">Référence&nbsp;:</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;\">" + escHtml(demandeRef) + "</td></tr>" +
        "<tr><td style=\"color:#777777;font-size:13px;padding:3px 0;\">Nombre de bons&nbsp;:</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;\">" + nombreBons + "</td></tr>" +
        (valeurTotale > 0 ?
        "<tr><td style=\"color:#777777;font-size:13px;padding:3px 0;\">Valeur totale&nbsp;:</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;\">Rs " + String.format("%,.2f", valeurTotale) + "</td></tr>" : "") +
        "</table></td></tr></table>" +

        // Warning box
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"background-color:#fff8e1;border-left:4px solid #f59e0b;border-radius:4px;margin:0 0 24px;\">" +
        "<tr><td style=\"padding:14px;\">" +
        "<p style=\"color:#78350f;font-size:13px;margin:0;line-height:1.6;\">" +
        "&#9888;&#65039; Chaque bon est à <strong>usage unique</strong>, non fractionnable et lié à sa date de validité. " +
        "Présentez le PDF imprimé ou sur écran en magasin pour validation.</p>" +
        "</td></tr></table>" +

        "<p style=\"color:#555555;font-size:14px;line-height:1.6;margin:0 0 6px;\">Pour toute question, contactez-nous à " +
        "<a href=\"mailto:support@intermart.mu\" style=\"color:#D2232D;text-decoration:none;\">support@intermart.mu</a></p>" +
        "<p style=\"color:#555555;font-size:14px;margin:18px 0 0;\">Cordialement,<br><strong>L'équipe Intermart Maurice</strong></p>" +
        "</td></tr>" +

        // FOOTER
        "<tr><td style=\"background-color:#f8f9fa;padding:18px 30px;text-align:center;border-top:1px solid #eeeeee;\">" +
        "<p style=\"color:#aaaaaa;font-size:11px;margin:0;line-height:1.6;\">" +
        "&#169; 2026 Intermart Maurice &#8212; Email généré automatiquement, merci de ne pas répondre.<br>" +
        "Intermart Maurice Ltd &#8226; Port-Louis, Île Maurice</p>" +
        "</td></tr></table>" + // Container

        "</td></tr></table>" + // Wrapper
        "</body></html>";
    }

    /** Template 2 — Demande d'approbation (pour l'approbateur) */
    private static String buildApprovalTemplate(String approbateurName, String demandeRef,
                                                double montantTotal) {
        return "<!DOCTYPE html>" +
        "<html lang=\"fr\"><head>" +
        "<meta charset=\"UTF-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
        "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
        "<title>Demande d'approbation — VMS</title></head>" +
        "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f4f4f4;padding:20px 0;\">" +
        "<tr><td align=\"center\">" +
        "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #dddddd;\">" +

        // HEADER orange
        "<tr><td style=\"background-color:#f59e0b;padding:28px 30px;text-align:center;\">" +
        "<h1 style=\"color:#ffffff;font-size:22px;margin:0;font-weight:bold;\">&#128203; Action requise</h1>" +
        "<p style=\"color:#fef3c7;font-size:13px;margin:8px 0 0;\">Demande en attente de votre approbation — VMS Intermart</p>" +
        "</td></tr>" +

        "<tr><td style=\"padding:30px;\">" +
        "<p style=\"color:#222222;font-size:16px;margin:0 0 14px;\">Bonjour" +
        (!approbateurName.isBlank() ? " <strong>" + escHtml(approbateurName) + "</strong>" : "") + ",</p>" +
        "<p style=\"color:#555555;font-size:14px;line-height:1.75;margin:0 0 22px;\">" +
        "Une nouvelle demande de bons cadeaux requiert votre approbation dans le système VMS Intermart.</p>" +

        // Details
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"background-color:#fffbeb;border-left:4px solid #f59e0b;border-radius:4px;margin:0 0 22px;\">" +
        "<tr><td style=\"padding:16px;\">" +
        "<p style=\"color:#92400e;font-size:13px;margin:0 0 10px;\"><strong>&#128203; Informations de la demande</strong></p>" +
        "<table cellpadding=\"0\" cellspacing=\"0\" border=\"0\">" +
        "<tr><td style=\"color:#777777;font-size:13px;padding:3px 0;width:160px;\">Référence&nbsp;:</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;\">" + escHtml(demandeRef) + "</td></tr>" +
        (montantTotal > 0 ?
        "<tr><td style=\"color:#777777;font-size:13px;padding:3px 0;\">Montant total&nbsp;:</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;\">Rs " + String.format("%,.2f", montantTotal) + "</td></tr>" : "") +
        "</table></td></tr></table>" +

        "<p style=\"color:#555555;font-size:14px;line-height:1.6;margin:0 0 14px;\">" +
        "Connectez-vous à l'application VMS pour approuver ou rejeter cette demande.</p>" +
        "<p style=\"color:#555555;font-size:14px;margin:16px 0 0;\">Cordialement,<br><strong>Système VMS Intermart</strong></p>" +
        "</td></tr>" +

        "<tr><td style=\"background-color:#f8f9fa;padding:18px 30px;text-align:center;border-top:1px solid #eeeeee;\">" +
        "<p style=\"color:#aaaaaa;font-size:11px;margin:0;\">&#169; 2026 Intermart Maurice &#8212; Email automatique VMS</p>" +
        "</td></tr></table></td></tr></table></body></html>";
    }

    /** Template 3 — Confirmation de validation de paiement */
    private static String buildPaymentConfirmTemplate(String comptableName, String demandeRef) {
        return "<!DOCTYPE html>" +
        "<html lang=\"fr\"><head>" +
        "<meta charset=\"UTF-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
        "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
        "<title>Paiement validé — VMS</title></head>" +
        "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f4f4f4;padding:20px 0;\">" +
        "<tr><td align=\"center\">" +
        "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #dddddd;\">" +

        // HEADER vert
        "<tr><td style=\"background-color:#16a34a;padding:28px 30px;text-align:center;\">" +
        "<h1 style=\"color:#ffffff;font-size:22px;margin:0;font-weight:bold;\">&#9989; Paiement validé</h1>" +
        "<p style=\"color:#bbf7d0;font-size:13px;margin:8px 0 0;\">Confirmation de validation — VMS Intermart</p>" +
        "</td></tr>" +

        "<tr><td style=\"padding:30px;\">" +
        "<p style=\"color:#222222;font-size:16px;margin:0 0 14px;\">Bonjour" +
        (!comptableName.isBlank() ? " <strong>" + escHtml(comptableName) + "</strong>" : "") + ",</p>" +
        "<p style=\"color:#555555;font-size:14px;line-height:1.75;margin:0 0 22px;\">" +
        "Le paiement pour la demande suivante a été validé avec succès dans VMS Intermart.</p>" +

        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"background-color:#f0fdf4;border-left:4px solid #16a34a;border-radius:4px;margin:0 0 22px;\">" +
        "<tr><td style=\"padding:16px;\">" +
        "<p style=\"color:#14532d;font-size:13px;margin:0 0 6px;\"><strong>&#128203; Référence validée</strong></p>" +
        "<p style=\"color:#222222;font-size:16px;font-weight:bold;margin:0;\">" + escHtml(demandeRef) + "</p>" +
        "</td></tr></table>" +

        "<p style=\"color:#555555;font-size:14px;line-height:1.6;margin:0 0 14px;\">" +
        "La demande peut désormais être traitée pour la génération des bons cadeaux.</p>" +
        "<p style=\"color:#555555;font-size:14px;margin:16px 0 0;\">Cordialement,<br><strong>Système VMS Intermart</strong></p>" +
        "</td></tr>" +

        "<tr><td style=\"background-color:#f8f9fa;padding:18px 30px;text-align:center;border-top:1px solid #eeeeee;\">" +
        "<p style=\"color:#aaaaaa;font-size:11px;margin:0;\">&#169; 2026 Intermart Maurice &#8212; Email automatique VMS</p>" +
        "</td></tr></table></td></tr></table></body></html>";
    }

    /** Template 4 — Récapitulatif admin après génération des bons */
    private static String buildAdminRecapTemplate(String demandeRef, int nombreBons,
                                                  double valeurUnitaire, double valeurTotale) {
        return "<!DOCTYPE html>" +
        "<html lang=\"fr\"><head>" +
        "<meta charset=\"UTF-8\">" +
        "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\">" +
        "<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">" +
        "<title>Récapitulatif génération — VMS Admin</title></head>" +
        "<body style=\"margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;\">" +
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color:#f4f4f4;padding:20px 0;\">" +
        "<tr><td align=\"center\">" +
        "<table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" " +
        "style=\"max-width:600px;width:100%;background-color:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #dddddd;\">" +

        // HEADER gris foncé
        "<tr><td style=\"background-color:#1e293b;padding:28px 30px;text-align:center;\">" +
        "<h1 style=\"color:#ffffff;font-size:22px;margin:0;font-weight:bold;\">&#128196; Récapitulatif Admin</h1>" +
        "<p style=\"color:#94a3b8;font-size:13px;margin:8px 0 0;\">Génération de bons cadeaux terminée — VMS Intermart</p>" +
        "</td></tr>" +

        "<tr><td style=\"padding:30px;\">" +
        "<p style=\"color:#222222;font-size:15px;margin:0 0 20px;\">" +
        "La génération de bons pour la demande <strong>" + escHtml(demandeRef) + "</strong> est terminée.</p>" +

        // Tableau des stats
        "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;margin:0 0 22px;\">" +
        "<tr style=\"background-color:#1e293b;\">" +
        "<th style=\"color:#ffffff;font-size:12px;text-align:left;padding:10px 14px;font-weight:bold;\">Paramètre</th>" +
        "<th style=\"color:#ffffff;font-size:12px;text-align:right;padding:10px 14px;font-weight:bold;\">Valeur</th></tr>" +
        "<tr style=\"background-color:#f8fafc;\">" +
        "<td style=\"color:#555555;font-size:13px;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">Référence</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;text-align:right;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">" + escHtml(demandeRef) + "</td></tr>" +
        "<tr>" +
        "<td style=\"color:#555555;font-size:13px;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">Bons générés</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;text-align:right;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">" + nombreBons + "</td></tr>" +
        (valeurUnitaire > 0 ?
        "<tr style=\"background-color:#f8fafc;\"><td style=\"color:#555555;font-size:13px;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">Valeur unitaire</td>" +
        "<td style=\"color:#222222;font-size:13px;font-weight:bold;text-align:right;padding:10px 14px;border-bottom:1px solid #e2e8f0;\">Rs " + String.format("%,.2f", valeurUnitaire) + "</td></tr>" : "") +
        (valeurTotale > 0 ?
        "<tr><td style=\"color:#555555;font-size:14px;padding:12px 14px;font-weight:bold;\">Montant total</td>" +
        "<td style=\"color:#D2232D;font-size:15px;font-weight:bold;text-align:right;padding:12px 14px;\">Rs " + String.format("%,.2f", valeurTotale) + "</td></tr>" : "") +
        "</table>" +

        "<p style=\"color:#555555;font-size:13px;line-height:1.6;margin:0;\">Le PDF récapitulatif est joint à cet email. Les bons ont été envoyés au client.</p>" +
        "</td></tr>" +

        "<tr><td style=\"background-color:#f8f9fa;padding:18px 30px;text-align:center;border-top:1px solid #eeeeee;\">" +
        "<p style=\"color:#aaaaaa;font-size:11px;margin:0;\">" +
        "&#169; 2026 Intermart Maurice &#8212; Email automatique VMS Admin<br>" +
        "Cet email est confidentiel &#8212; destiné à l'administrateur uniquement.</p>" +
        "</td></tr></table></td></tr></table></body></html>";
    }

    /**
     * Échappe les caractères HTML dangereux dans toutes les données dynamiques.
     * Indispensable pour éviter l'injection HTML dans les emails.
     */
    private static String escHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
