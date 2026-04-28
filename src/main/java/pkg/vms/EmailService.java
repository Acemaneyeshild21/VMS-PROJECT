package pkg.vms;

/*
 * ═══════════════════════════════════════════════════════════════════════════════
 *  EmailService — Service d'envoi SMTP production-ready
 *  BTS SIO SLAM RP2 — VoucherManager (VMS), Session 2026
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
    private static final String SMTP_USER    = Config.get("mail.username",            "dimitriaceman614@gmail.com");
    private static final String SMTP_PASS    = Config.get("mail.password",            "dbcxitmdmplmvuza");
    private static final String FROM_ADDR    = Config.get("mail.from",               SMTP_USER);
    private static final String FROM_NAME    = Config.get("mail.from.name",          "Voucher Manager VMS");
    private static final String ADMIN_EMAIL  = Config.get("mail.admin.email",        "dimitriaceman614@gmail.com");
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
                String subject = "Vos bons cadeaux VMS — " + demandeRef;
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
                        "Vos bons cadeaux VMS — " + ref,
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
                                "Vos bons cadeaux VMS — " + err.demandeRef,
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

    /**
     * Envoie un code OTP de réinitialisation de mot de passe.
     * Synchrone — appelé depuis un SwingWorker.doInBackground(), jamais depuis l'EDT.
     *
     * @param email email du destinataire
     * @param nom   prénom/nom ou username
     * @param code  code OTP à 6 chiffres (15 min de validité)
     * @return {@code null} si succès, message d'erreur si échec
     */
    public static String envoyerCodeReset(String email, String nom, String code) {
        if (SIMULATION) {
            System.out.printf("[EmailService SIMULATION] Code reset → %s : %s%n", email, code);
            return null; // succès simulé
        }
        String sujet = "Réinitialisation de mot de passe — " + C_SYSTEME;
        String corps = buildPasswordResetTemplate(nom, code);
        try {
            sendMessage(email, sujet, corps, null, 0, null);
            return null;
        } catch (Exception e) {
            return e.getMessage();
        }
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
    //  Templates HTML — production-ready, compatibles tous clients email
    //  Gmail · Apple Mail (iPhone/iPad) · Huawei Mail · Samsung Mail
    //  Outlook · Yahoo Mail · tous les webmails
    //
    //  RÈGLES (email-safe) :
    //  ✅ Layout 100% <table> — pas de Flexbox, pas de CSS Grid
    //  ✅ Styles 100% inline — pas de <style> externe
    //  ✅ Largeur max 600px, centré via align="center"
    //  ✅ Police Arial/Helvetica (universelle sur tous les OS)
    //  ✅ Couleurs HEX uniquement — pas de rgba()
    //  ✅ Variables {PLACEHOLDER} remplacées par fill() avant envoi
    //  ✅ Données dynamiques échappées via escHtml()
    //  ❌ Pas de media queries · Pas de JavaScript
    //
    //  BRANDING VoucherManager VMS :
    //  · Header principal  : #000099 (navy — professionnel, sobre)
    //  · Header notice     : #0066cc (bleu clair — accessible, rassurant)
    //  · Header interne    : #1e293b (gris foncé — confidentiel / admin)
    //  · Accentuation      : #25255a (bleu marine foncé — titres info-box)
    //  · Warning           : #fdf6e0 / #f2c94c / #975a00
    // ─────────────────────────────────────────────────────────────────────────

    // ── Constantes VMS (modifier ici = propagé dans tous les templates) ──
    private static final String C_SOCIETE  = "VoucherManager VMS";
    private static final String C_SYSTEME  = "VMS &ndash; Voucher Management System";
    private static final String C_SUPPORT  = "support@vms.mu";
    private static final String C_ENTPR    = "bons@vms.mu";
    private static final String C_SITE     = "www.vms.mu";
    private static final String C_ADRESSE  = "mada 1000, Mada, Maurice";
    private static final String C_REG      = "VMS Platform";

    // ─── Méthode utilitaire : remplace les {PLACEHOLDER} dans le template ───
    private static String fill(String tpl, String... kvPairs) {
        String out = tpl;
        for (int i = 0; i + 1 < kvPairs.length; i += 2)
            out = out.replace(kvPairs[i], kvPairs[i + 1]);
        return out;
    }

    // ── Formatage conditionnel des montants ───────────────────────────────────
    private static String montant(double v) {
        return v > 0 ? "Rs&nbsp;" + String.format("%,.2f", v) : "&mdash;";
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEMPLATE 1 — Bons cadeaux au client (standard, couleur #000099)
    //  Basé sur la version "Standard professionnelle"
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    //  TEMPLATE 1 RAW — Bons cadeaux au client (Standard #000099)
    //  Variables : {CLIENT_NAME} {DEMANDE_REF} {NOMBRE_BONS}
    //              {VALEUR_UNITAIRE} {VALEUR_TOTALE} {NB_PJ}
    // ─────────────────────────────────────────────────────────────────────────
    private static final String TPL_VOUCHER = """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>Vos bons cadeaux &ndash; VoucherManager VMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;padding:20px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0" border="0"
             style="max-width:600px;width:100%;background-color:#ffffff;border-radius:4px;overflow:hidden;border:1px solid #e0e0e0;">

        <!-- HEADER -->
        <tr>
          <td style="background-color:#000099;padding:24px 30px 20px;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td style="padding:0 0 6px;">
                <h1 style="color:#ffffff;font-size:22px;font-weight:bold;margin:0;">VoucherManager VMS</h1>
              </td></tr>
              <tr><td>
                <p style="color:#e9e9ff;font-size:13px;margin:0;">Voucher &amp; Gift Management System</p>
              </td></tr>
            </table>
          </td>
        </tr>

        <!-- BODY -->
        <tr>
          <td style="padding:30px;line-height:1.5;">
            <p style="color:#1a1a1a;font-size:16px;font-weight:500;margin:0 0 14px;">
              Bonjour <strong>{CLIENT_NAME}</strong>,
            </p>
            <p style="color:#344054;font-size:14px;margin:0 0 20px;">
              Les bons cadeaux associ&eacute;s &agrave; la demande <strong>{DEMANDE_REF}</strong> sont g&eacute;n&eacute;r&eacute;s et disponibles.
              Vous trouverez <strong>{NB_PJ}</strong> en pi&egrave;ce(s) jointe(s) &agrave; cet email.
            </p>

            <!-- DETAILS BOX -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="border:1px solid #e0e0e0;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:16px 18px;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td style="padding:0 0 8px;">
                    <p style="color:#25255a;font-size:13px;font-weight:bold;margin:0;letter-spacing:0.1em;text-transform:uppercase;">
                      D&Eacute;TAILS DE LA COMMANDE
                    </p>
                  </td></tr>
                  <tr><td>
                    <p style="color:#344054;font-size:13px;margin:0;line-height:1.7;">
                      R&eacute;f&eacute;rence : <strong>{DEMANDE_REF}</strong><br>
                      Nombre de bons : <strong>{NOMBRE_BONS}</strong><br>
                      Valeur unitaire : <strong>{VALEUR_UNITAIRE}</strong><br>
                      Valeur totale : <strong>{VALEUR_TOTALE}</strong>
                    </p>
                  </td></tr>
                </table>
              </td></tr>
            </table>

            <!-- WARNING -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:#fdf6e0;border:1px solid #f2c94c;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:14px 16px;">
                <p style="color:#975a00;font-size:13px;margin:0;line-height:1.5;">
                  &#9888;&#65039; <strong>Utilisation</strong> : chaque bon est &agrave; <strong>usage unique</strong>,
                  non fractionnable, et doit &ecirc;tre utilis&eacute; avant la date d&apos;expiration.
                  Toute copie ou tentative de fraude est strictement interdite.
                </p>
              </td></tr>
            </table>

            <p style="color:#344054;font-size:14px;margin:0 0 10px;">
              Pour toute question concernant cette commande, contactez notre service client :
            </p>
            <p style="margin:0 0 18px;">
              <a href="mailto:{SUPPORT_EMAIL}" style="color:#000099;text-decoration:underline;font-size:14px;">{SUPPORT_EMAIL}</a>
            </p>
            <p style="color:#667085;font-size:12px;margin:0;">
              Ce message est g&eacute;n&eacute;r&eacute; automatiquement, merci de ne pas y r&eacute;pondre directement.
            </p>
          </td>
        </tr>

        <!-- FOOTER -->
        <tr>
          <td style="background-color:#f0f0f0;padding:20px 30px;border-top:1px solid #e0e0e0;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td style="padding:0 0 4px;">
                <p style="color:#4a4a4a;font-size:12px;font-weight:bold;margin:0;">
                  VoucherManager VMS &ndash; Syst&egrave;me de gestion de bons cadeaux
                </p>
              </td></tr>
              <tr><td style="padding:4px 0 0;">
                <p style="color:#666666;font-size:11px;margin:0;line-height:1.6;">
                  {C_SYSTEME}<br>
                  {C_SOCIETE} &bull; {C_ADRESSE}<br>
                  <a href="https://{C_SITE}" style="color:#000099;text-decoration:underline;font-size:11px;">{C_SITE}</a>
                </p>
              </td></tr>
              <tr><td style="padding:10px 0 0;">
                <p style="color:#999999;font-size:11px;margin:0;">
                  Ce message est confidentiel et &agrave; usage exclusif du destinataire.
                  Toute utilisation, reproduction ou diffusion non autoris&eacute;e est strictement interdite.
                </p>
              </td></tr>
            </table>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>
""";

    /** Template 1 — Bons cadeaux envoyés au client */
    private static String buildVoucherTemplate(String clientName, String demandeRef,
                                               int nombreBons, double valeurTotale) {
        String nbPj = nombreBons + " bon" + (nombreBons > 1 ? "s" : "") +
                     " (" + nombreBons + " pièce" + (nombreBons > 1 ? "s" : "") + " jointe" + (nombreBons > 1 ? "s" : "") + ")";
        return fill(TPL_VOUCHER,
            "{CLIENT_NAME}",     escHtml(clientName),
            "{DEMANDE_REF}",     escHtml(demandeRef),
            "{NOMBRE_BONS}",     String.valueOf(nombreBons),
            "{VALEUR_UNITAIRE}", "&mdash;",
            "{VALEUR_TOTALE}",   montant(valeurTotale),
            "{NB_PJ}",           nbPj,
            "{SUPPORT_EMAIL}",   C_SUPPORT,
            "{C_SYSTEME}",       C_SYSTEME,
            "{C_SOCIETE}",       C_SOCIETE,
            "{C_ADRESSE}",       C_ADRESSE,
            "{C_SITE}",          C_SITE
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEMPLATE 2 RAW — Demande d'approbation (formelle #000099, workflow interne)
    //  Adapté de la version "Enterprise / Grands comptes"
    //  Variables : {APPROBATEUR_NAME} {DEMANDE_REF} {MONTANT_TOTAL}
    // ─────────────────────────────────────────────────────────────────────────
    private static final String TPL_APPROVAL = """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>Demande d&apos;approbation &ndash; VoucherManager VMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;padding:20px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0" border="0"
             style="max-width:600px;width:100%;background-color:#ffffff;border-radius:4px;overflow:hidden;border:1px solid #e0e0e0;">

        <!-- HEADER -->
        <tr>
          <td style="background-color:#000099;padding:24px 30px 20px;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td><h1 style="color:#ffffff;font-size:22px;font-weight:bold;margin:0;">VoucherManager VMS</h1></td></tr>
              <tr><td><p style="color:#e9e9ff;font-size:13px;margin:0;">Workflow d&apos;approbation &ndash; Enterprise Solutions</p></td></tr>
            </table>
          </td>
        </tr>

        <!-- BODY -->
        <tr>
          <td style="padding:30px;line-height:1.5;">
            <p style="color:#1a1a1a;font-size:16px;font-weight:500;margin:0 0 14px;">
              Bonjour <strong>{APPROBATEUR_NAME}</strong>,
            </p>
            <p style="color:#344054;font-size:14px;margin:0 0 18px;">
              Nous vous informons qu&apos;une demande de bons cadeaux associ&eacute;e &agrave; la r&eacute;f&eacute;rence
              <strong>{DEMANDE_REF}</strong> est en attente de votre approbation dans le syst&egrave;me VoucherManager.
            </p>

            <!-- DETAILS BOX -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="border:1px solid #e0e0e0;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:16px 18px;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td>
                    <p style="color:#25255a;font-size:13px;font-weight:bold;margin:0;letter-spacing:0.1em;text-transform:uppercase;">
                      D&Eacute;TAILS DE LA DEMANDE
                    </p>
                  </td></tr>
                  <tr><td style="padding:8px 0 0;">
                    <p style="color:#344054;font-size:13px;margin:0;line-height:1.7;">
                      R&eacute;f&eacute;rence : <strong>{DEMANDE_REF}</strong><br>
                      Montant total : <strong>{MONTANT_TOTAL}</strong><br>
                      Statut : <strong>En attente d&apos;approbation</strong>
                    </p>
                  </td></tr>
                </table>
              </td></tr>
            </table>

            <!-- WARNING -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:#fdf6e0;border:1px solid #f2c94c;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:14px 16px;">
                <p style="color:#975a00;font-size:13px;margin:0;line-height:1.5;">
                  &#9888;&#65039; <strong>Action requise</strong> : veuillez vous connecter &agrave; l&apos;application VMS
                  pour approuver ou rejeter cette demande. Toute d&eacute;cision doit &ecirc;tre trac&eacute;e dans le syst&egrave;me.
                </p>
              </td></tr>
            </table>

            <p style="color:#344054;font-size:14px;margin:0 0 10px;">
              Pour toute question technique ou administrative, contactez :
            </p>
            <p style="margin:0 0 18px;">
              <a href="mailto:{ENTPR_EMAIL}" style="color:#000099;text-decoration:underline;font-size:14px;">{ENTPR_EMAIL}</a>
            </p>
            <p style="color:#667085;font-size:12px;margin:0;">
              Ce message est g&eacute;n&eacute;r&eacute; automatiquement dans le cadre du workflow VMS. Merci de ne pas y r&eacute;pondre directement.
            </p>
          </td>
        </tr>

        <!-- FOOTER -->
        <tr>
          <td style="background-color:#f0f0f0;padding:20px 30px;border-top:1px solid #e0e0e0;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td style="padding:0 0 4px;">
                <p style="color:#4a4a4a;font-size:12px;font-weight:bold;margin:0;">
                  VoucherManager VMS &ndash; Workflow &amp; Gestion des Bons Cadeaux
                </p>
              </td></tr>
              <tr><td style="padding:4px 0 0;">
                <p style="color:#666666;font-size:11px;margin:0;line-height:1.6;">
                  {C_SYSTEME}<br>
                  {C_SOCIETE} &bull; {C_ADRESSE}<br>
                  <a href="https://{C_SITE}" style="color:#000099;text-decoration:underline;font-size:11px;">{C_SITE}</a>
                </p>
              </td></tr>
              <tr><td style="padding:10px 0 0;">
                <p style="color:#999999;font-size:11px;margin:0;">
                  Ce message est confidentiel et &agrave; usage exclusif de l&apos;organisation destinataire.
                  Toute utilisation, reproduction ou diffusion non autoris&eacute;e est strictement interdite.
                </p>
              </td></tr>
            </table>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>
""";

    /** Template 2 — Demande d'approbation (pour l'approbateur) */
    private static String buildApprovalTemplate(String approbateurName, String demandeRef,
                                                double montantTotal) {
        String nom = (approbateurName != null && !approbateurName.isBlank())
                     ? escHtml(approbateurName) : "Madame, Monsieur";
        return fill(TPL_APPROVAL,
            "{APPROBATEUR_NAME}", nom,
            "{DEMANDE_REF}",      escHtml(demandeRef),
            "{MONTANT_TOTAL}",    montant(montantTotal),
            "{ENTPR_EMAIL}",      C_ENTPR,
            "{C_SYSTEME}",        C_SYSTEME,
            "{C_SOCIETE}",        C_SOCIETE,
            "{C_ADRESSE}",        C_ADRESSE,
            "{C_SITE}",           C_SITE
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEMPLATE 3 RAW — Confirmation de paiement (#0066cc, notice client)
    //  Adapté de la version "Notice client simple et polie"
    //  Variables : {COMPTABLE_NAME} {DEMANDE_REF}
    // ─────────────────────────────────────────────────────────────────────────
    private static final String TPL_PAYMENT = """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>Confirmation de paiement &ndash; VoucherManager VMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;padding:20px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0" border="0"
             style="max-width:600px;width:100%;background-color:#ffffff;border-radius:4px;overflow:hidden;border:1px solid #e0e0e0;">

        <!-- HEADER -->
        <tr>
          <td style="background-color:#0066cc;padding:24px 30px 18px;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td><h1 style="color:#ffffff;font-size:22px;font-weight:bold;margin:0;">VoucherManager VMS</h1></td></tr>
              <tr><td><p style="color:#d9edf7;font-size:13px;margin:0;">Confirmation de validation de paiement</p></td></tr>
            </table>
          </td>
        </tr>

        <!-- BODY -->
        <tr>
          <td style="padding:30px;line-height:1.5;">
            <p style="color:#1a1a1a;font-size:16px;font-weight:500;margin:0 0 14px;">
              Bonjour <strong>{COMPTABLE_NAME}</strong>,
            </p>
            <p style="color:#344054;font-size:14px;margin:0 0 18px;">
              Nous vous confirmons que le paiement associ&eacute; &agrave; la demande
              <strong>{DEMANDE_REF}</strong> a &eacute;t&eacute; valid&eacute; avec succ&egrave;s dans le syst&egrave;me VoucherManager.
            </p>

            <!-- DETAILS BOX -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="border:1px solid #e0e0e0;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:16px 18px;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td>
                    <p style="color:#25255a;font-size:13px;font-weight:bold;margin:0;letter-spacing:0.1em;text-transform:uppercase;">
                      PAIEMENT VALID&Eacute;
                    </p>
                  </td></tr>
                  <tr><td style="padding:8px 0 0;">
                    <p style="color:#344054;font-size:13px;margin:0;line-height:1.7;">
                      R&eacute;f&eacute;rence : <strong>{DEMANDE_REF}</strong><br>
                      Statut : <strong>Paiement confirm&eacute;</strong><br>
                      Prochaine &eacute;tape : <strong>En attente d&apos;approbation</strong>
                    </p>
                  </td></tr>
                </table>
              </td></tr>
            </table>

            <!-- INFO CLIENT -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:#f0f7ff;border:1px solid #0066cc;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:14px 16px;">
                <p style="color:#003d7a;font-size:13px;margin:0;line-height:1.5;">
                  &#10003; La demande peut d&eacute;sormais &ecirc;tre soumise &agrave; l&apos;approbateur d&eacute;sign&eacute;.
                  Connectez-vous &agrave; VMS pour suivre l&apos;&eacute;volution du dossier.
                </p>
              </td></tr>
            </table>

            <p style="color:#344054;font-size:14px;margin:0 0 10px;">
              Pour toute question ou assistance, contactez le support VMS :
            </p>
            <p style="margin:0 0 18px;">
              <a href="mailto:{ENTPR_EMAIL}" style="color:#0066cc;text-decoration:underline;font-size:14px;">{ENTPR_EMAIL}</a>
            </p>
            <p style="color:#667085;font-size:12px;margin:0;">
              Ce message est g&eacute;n&eacute;r&eacute; automatiquement. Merci de ne pas y r&eacute;pondre directement.
            </p>
          </td>
        </tr>

        <!-- FOOTER -->
        <tr>
          <td style="background-color:#f0f0f0;padding:20px 30px;border-top:1px solid #e0e0e0;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td style="padding:0 0 4px;">
                <p style="color:#4a4a4a;font-size:12px;font-weight:bold;margin:0;">
                  VoucherManager VMS &ndash; Syst&egrave;me de gestion de bons cadeaux
                </p>
              </td></tr>
              <tr><td style="padding:4px 0 0;">
                <p style="color:#666666;font-size:11px;margin:0;line-height:1.6;">
                  {C_SYSTEME}<br>
                  {C_SOCIETE} &bull; {C_ADRESSE}<br>
                  <a href="https://{C_SITE}" style="color:#0066cc;text-decoration:underline;font-size:11px;">{C_SITE}</a>
                </p>
              </td></tr>
              <tr><td style="padding:10px 0 0;">
                <p style="color:#999999;font-size:11px;margin:0;">
                  Ce message est g&eacute;n&eacute;r&eacute; automatiquement. Merci de ne pas y r&eacute;pondre directement.
                </p>
              </td></tr>
            </table>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>
""";

    /** Template 3 — Confirmation de validation de paiement */
    private static String buildPaymentConfirmTemplate(String comptableName, String demandeRef) {
        String nom = (comptableName != null && !comptableName.isBlank())
                     ? escHtml(comptableName) : "Madame, Monsieur";
        return fill(TPL_PAYMENT,
            "{COMPTABLE_NAME}", nom,
            "{DEMANDE_REF}",   escHtml(demandeRef),
            "{ENTPR_EMAIL}",   C_ENTPR,
            "{C_SYSTEME}",     C_SYSTEME,
            "{C_SOCIETE}",     C_SOCIETE,
            "{C_ADRESSE}",     C_ADRESSE,
            "{C_SITE}",        C_SITE
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  TEMPLATE 4 RAW — Récapitulatif admin (#1e293b, confidentiel interne)
    //  Adapté de la version "Enterprise" — usage administrateur uniquement
    //  Variables : {DEMANDE_REF} {NOMBRE_BONS} {VALEUR_UNITAIRE} {VALEUR_TOTALE}
    // ─────────────────────────────────────────────────────────────────────────
    private static final String TPL_ADMIN_RECAP = """
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>R&eacute;capitulatif administrateur &ndash; VoucherManager VMS</title>
</head>
<body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
  <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f4f4f4;padding:20px 0;">
    <tr><td align="center">
      <table width="600" cellpadding="0" cellspacing="0" border="0"
             style="max-width:600px;width:100%;background-color:#ffffff;border-radius:4px;overflow:hidden;border:1px solid #e0e0e0;">

        <!-- HEADER -->
        <tr>
          <td style="background-color:#1e293b;padding:24px 30px 20px;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td><h1 style="color:#ffffff;font-size:22px;font-weight:bold;margin:0;">VoucherManager VMS</h1></td></tr>
              <tr><td><p style="color:#94a3b8;font-size:13px;margin:0;">R&eacute;capitulatif administrateur &ndash; Usage confidentiel</p></td></tr>
            </table>
          </td>
        </tr>

        <!-- BODY -->
        <tr>
          <td style="padding:30px;line-height:1.5;">
            <p style="color:#344054;font-size:14px;margin:0 0 18px;">
              La g&eacute;n&eacute;ration de bons cadeaux associ&eacute;e &agrave; la demande de r&eacute;f&eacute;rence
              <strong>{DEMANDE_REF}</strong> est termin&eacute;e. Le r&eacute;capitulatif PDF est joint &agrave; cet email.
            </p>

            <!-- DETAILS BOX -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="border:1px solid #e0e0e0;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:16px 18px;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0">
                  <tr><td>
                    <p style="color:#25255a;font-size:13px;font-weight:bold;margin:0;letter-spacing:0.1em;text-transform:uppercase;">
                      D&Eacute;TAILS DE LA G&Eacute;N&Eacute;RATION
                    </p>
                  </td></tr>
                  <tr><td style="padding:8px 0 0;">
                    <p style="color:#344054;font-size:13px;margin:0;line-height:1.7;">
                      R&eacute;f&eacute;rence : <strong>{DEMANDE_REF}</strong><br>
                      Nombre de bons g&eacute;n&eacute;r&eacute;s : <strong>{NOMBRE_BONS}</strong><br>
                      Valeur unitaire : <strong>{VALEUR_UNITAIRE}</strong><br>
                      Valeur totale : <strong>{VALEUR_TOTALE}</strong><br>
                      Bons envoy&eacute;s au client : <strong>Oui</strong>
                    </p>
                  </td></tr>
                </table>
              </td></tr>
            </table>

            <!-- WARNING -->
            <table width="100%" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:#fdf6e0;border:1px solid #f2c94c;border-radius:6px;margin:0 0 20px;">
              <tr><td style="padding:14px 16px;">
                <p style="color:#975a00;font-size:13px;margin:0;line-height:1.5;">
                  &#9888;&#65039; <strong>Confidentiel</strong> : ce r&eacute;capitulatif est r&eacute;serv&eacute; &agrave;
                  l&apos;administrateur. Les bons g&eacute;n&eacute;r&eacute;s sont actifs et op&eacute;rationnels.
                  Toute anomalie doit &ecirc;tre signal&eacute;e imm&eacute;diatement.
                </p>
              </td></tr>
            </table>

            <p style="color:#667085;font-size:12px;margin:0;">
              Ce message est g&eacute;n&eacute;r&eacute; automatiquement par le syst&egrave;me VMS. Merci de ne pas y r&eacute;pondre directement.
            </p>
          </td>
        </tr>

        <!-- FOOTER -->
        <tr>
          <td style="background-color:#f0f0f0;padding:20px 30px;border-top:1px solid #e0e0e0;text-align:left;">
            <table width="100%" cellpadding="0" cellspacing="0" border="0">
              <tr><td style="padding:0 0 4px;">
                <p style="color:#4a4a4a;font-size:12px;font-weight:bold;margin:0;">
                  VoucherManager VMS &ndash; Administration &amp; Gestion des Bons
                </p>
              </td></tr>
              <tr><td style="padding:4px 0 0;">
                <p style="color:#666666;font-size:11px;margin:0;line-height:1.6;">
                  {C_SYSTEME}<br>
                  {C_SOCIETE} &bull; {C_ADRESSE}
                </p>
              </td></tr>
              <tr><td style="padding:10px 0 0;">
                <p style="color:#999999;font-size:11px;margin:0;">
                  Ce message est confidentiel et &agrave; usage exclusif de l&apos;organisation destinataire.
                  Toute utilisation, reproduction ou diffusion non autoris&eacute;e est strictement interdite.
                </p>
              </td></tr>
            </table>
          </td>
        </tr>

      </table>
    </td></tr>
  </table>
</body>
</html>
""";

    /** Template 4 — Récapitulatif admin après génération des bons */
    private static String buildAdminRecapTemplate(String demandeRef, int nombreBons,
                                                  double valeurUnitaire, double valeurTotale) {
        return fill(TPL_ADMIN_RECAP,
            "{DEMANDE_REF}",     escHtml(demandeRef),
            "{NOMBRE_BONS}",     String.valueOf(nombreBons),
            "{VALEUR_UNITAIRE}", montant(valeurUnitaire),
            "{VALEUR_TOTALE}",   montant(valeurTotale),
            "{C_SYSTEME}",       C_SYSTEME,
            "{C_SOCIETE}",       C_SOCIETE,
            "{C_ADRESSE}",       C_ADRESSE,
            "{C_SITE}",          C_SITE
        );
    }

    // ── Template : réinitialisation mot de passe ──────────────────────────────

    private static final String TPL_PASSWORD_RESET = """
        <!DOCTYPE html>
        <html lang="fr"><head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1">
        <title>Réinitialisation de mot de passe</title></head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif">
        <table width="100%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 0">
          <tr><td align="center">
            <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:8px;overflow:hidden;border:1px solid #e2e8f0">
              <!-- Header -->
              <tr><td style="background:#000099;padding:32px 40px;text-align:center">
                <p style="margin:0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:1px">{C_SYSTEME}</p>
                <p style="margin:6px 0 0;font-size:12px;color:rgba(255,255,255,0.7)">{C_SOCIETE}</p>
              </td></tr>
              <!-- Body -->
              <tr><td style="padding:40px">
                <p style="margin:0 0 16px;font-size:20px;font-weight:600;color:#1e293b">Réinitialisation de mot de passe</p>
                <p style="margin:0 0 24px;color:#475569;font-size:14px;line-height:1.6">
                  Bonjour <strong>{NOM}</strong>,<br>
                  Vous avez demandé une réinitialisation de votre mot de passe.<br>
                  Voici votre code à usage unique, valable <strong>15 minutes</strong> :
                </p>
                <!-- Code OTP -->
                <table width="100%" cellpadding="0" cellspacing="0" style="margin:24px 0">
                  <tr><td align="center">
                    <div style="display:inline-block;background:#f1f5f9;border:2px solid #000099;border-radius:8px;padding:20px 40px">
                      <span style="font-size:36px;font-weight:700;color:#000099;letter-spacing:12px;font-family:monospace">{CODE}</span>
                    </div>
                  </td></tr>
                </table>
                <p style="margin:24px 0 0;color:#64748b;font-size:12px;line-height:1.6;text-align:center">
                  Si vous n&rsquo;avez pas demandé cette réinitialisation, ignorez cet email.<br>
                  Ne partagez jamais ce code — notre équipe ne vous le demandera jamais.
                </p>
              </td></tr>
              <!-- Footer -->
              <tr><td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center">
                <p style="margin:0;font-size:11px;color:#94a3b8">{C_SYSTEME} &mdash; {C_SOCIETE} &mdash; {C_ADRESSE}</p>
                <p style="margin:4px 0 0;font-size:11px;color:#94a3b8">{C_SITE}</p>
              </td></tr>
            </table>
          </td></tr>
        </table>
        </body></html>
        """;

    private static String buildPasswordResetTemplate(String nom, String code) {
        return fill(TPL_PASSWORD_RESET,
            "{NOM}",       escHtml(nom),
            "{CODE}",      escHtml(code),
            "{C_SYSTEME}", C_SYSTEME,
            "{C_SOCIETE}", C_SOCIETE,
            "{C_ADRESSE}", C_ADRESSE,
            "{C_SITE}",    C_SITE
        );
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
