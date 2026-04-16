package pkg.vms.Service;

import pkg.vms.EmailService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service pour envoyer des notifications email automatiques
 * lors d'événements importants dans l'application.
 */
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Envoyer une notification de nouvelle demande au comptable
     * Appelé quand une nouvelle demande est créée
     */
    public static void notifyNewDemande(String reference, String clientNom, double montant) {
        try {
            String comptableEmail = "comptable@intermart.mu";

            String corps = "<h2>📋 Nouvelle Demande à Valider</h2>" +
                    "<p>Une nouvelle demande de bons cadeau attend votre validation.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Référence:</strong></td>" +
                    "<td>" + reference + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Client:</strong></td>" +
                    "<td>" + clientNom + "</td>" +
                    "</tr>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Montant:</strong></td>" +
                    "<td>Rs " + String.format("%,.2f", montant) + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Date:</strong></td>" +
                    "<td>" + LocalDateTime.now().format(DATE_FORMATTER) + "</td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='color: #DC143C; margin-top: 15px;'><strong>⚠️ Action requise:</strong> Veuillez consulter l'application pour valider cette demande.</p>";

            EmailService.sendEmail(comptableEmail, "🔔 Nouvelle Demande à Valider - " + reference, corps);
            System.out.println("✅ Notification comptable envoyée pour " + reference);
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notification comptable: " + e.getMessage());
        }
    }

    /**
     * Envoyer une notification d'approbation requise à l'approbateur
     * Appelé quand le paiement est validé et la demande est prête pour approbation
     */
    public static void notifyApprovalRequired(String reference, String clientNom, int nombreBons, double montant) {
        try {
            String approbateurEmail = "approbateur@intermart.mu";

            String corps = "<h2>✅ Demande Prête pour Approbation</h2>" +
                    "<p>Une demande avec paiement validé attend votre approbation pour la génération des bons.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Référence:</strong></td>" +
                    "<td>" + reference + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Client:</strong></td>" +
                    "<td>" + clientNom + "</td>" +
                    "</tr>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Nombre de Bons:</strong></td>" +
                    "<td>" + nombreBons + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Montant Total:</strong></td>" +
                    "<td>Rs " + String.format("%,.2f", montant) + "</td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='color: #10B981; margin-top: 15px;'><strong>✓ Action recommandée:</strong> Approuvez cette demande pour procéder à la génération des bons.</p>";

            EmailService.sendEmail(approbateurEmail, "🔔 Approbation Requise - " + reference, corps);
            System.out.println("✅ Notification approbation envoyée");
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notification approbation: " + e.getMessage());
        }
    }

    /**
     * Envoyer une notification de génération de bons réussie
     * Appelé après la génération des bons
     */
    public static void notifyBonsGenerated(String reference, int nombreBons, String superviseurEmail) {
        try {
            String corps = "<h2>🎉 Bons Générés avec Succès!</h2>" +
                    "<p>Les bons cadeau ont été générés automatiquement et sont maintenant disponibles pour envoi.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Référence:</strong></td>" +
                    "<td>" + reference + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Nombre de Bons Générés:</strong></td>" +
                    "<td>" + nombreBons + "</td>" +
                    "</tr>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Date/Heure Génération:</strong></td>" +
                    "<td>" + LocalDateTime.now().format(DATE_FORMATTER) + "</td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='color: #10B981; margin-top: 15px;'><strong>✓ Status:</strong> Les bons sont maintenant disponibles pour rédemption en magasin.</p>";

            EmailService.sendEmail(superviseurEmail, "🎁 Bons Générés avec Succès - " + reference, corps);
            System.out.println("✅ Notification génération envoyée");
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notification génération: " + e.getMessage());
        }
    }

    /**
     * Envoyer une alerte au client pour un bon proche expiration
     * À appeler automatiquement pour les bons expirant dans les 30 jours
     */
    public static void notifyBonsNearExpiration(String codeUnique, String clientNom, String clientEmail, String dateExp) {
        try {
            String corps = "<h2>⚠️ Bon Cadeau Expirant Bientôt</h2>" +
                    "<p>Bonjour <strong>" + clientNom + "</strong>,</p>" +
                    "<p>Nous vous rappelons qu'un de vos bons cadeau expirera très bientôt.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Code Bon:</strong></td>" +
                    "<td><code>" + codeUnique + "</code></td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Date d'Expiration:</strong></td>" +
                    "<td style='color: red;'><strong>" + dateExp + "</strong></td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='margin-top: 15px;'><strong>✓ Action suggérée:</strong> Veuillez utiliser ce bon avant la date limite dans nos points de vente partenaires.</p>" +
                    "<p style='color: #666; font-size: 12px;'>Si vous avez des questions, contactez-nous directement.</p>";

            EmailService.sendEmail(clientEmail, "⏰ Rappel: Bon Cadeau Expirant - " + codeUnique, corps);
            System.out.println("✅ Alerte expiration envoyée à " + clientNom);
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi alerte expiration: " + e.getMessage());
        }
    }

    /**
     * Envoyer une notification de rédemption de bon
     * Appelé quand un bon est rédimé en magasin
     */
    public static void notifyRedemption(String codeBon, String magasinNom, double montant) {
        try {
            String managerEmail = "manager@intermart.mu";

            String corps = "<h2>🛍️ Bon Cadeau Rédimé en Magasin</h2>" +
                    "<p>Un bon cadeau vient d'être rédimé avec succès dans l'un de nos points de vente.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Code Bon:</strong></td>" +
                    "<td><code>" + codeBon + "</code></td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Magasin:</strong></td>" +
                    "<td>" + magasinNom + "</td>" +
                    "</tr>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Montant Rédimé:</strong></td>" +
                    "<td>Rs " + String.format("%,.2f", montant) + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Date/Heure:</strong></td>" +
                    "<td>" + LocalDateTime.now().format(DATE_FORMATTER) + "</td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='color: #10B981; margin-top: 15px;'><strong>✓ Status:</strong> Rédemption enregistrée avec succès.</p>";

            EmailService.sendEmail(managerEmail, "✅ Bon Cadeau Rédimé - " + codeBon, corps);
            System.out.println("✅ Notification rédemption envoyée");
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notification rédemption: " + e.getMessage());
        }
    }

    /**
     * Envoyer une notification d'envoi de bons au client
     * Appelé quand les bons sont envoyés par email au client
     */
    public static void notifyBonsSent(String reference, String clientNom, String clientEmail, int nombreBons) {
        try {
            String corps = "<h2>📧 Vos Bons Cadeau Ont Été Envoyés!</h2>" +
                    "<p>Bonjour <strong>" + clientNom + "</strong>,</p>" +
                    "<p>Vos bons cadeau Intermart ont été générés et vous sont transmis par email.</p>" +
                    "<table border='1' cellpadding='10' style='border-collapse: collapse;'>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Référence:</strong></td>" +
                    "<td>" + reference + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td><strong>Nombre de Bons:</strong></td>" +
                    "<td>" + nombreBons + "</td>" +
                    "</tr>" +
                    "<tr style='background-color: #f0f0f0;'>" +
                    "<td><strong>Date d'Envoi:</strong></td>" +
                    "<td>" + LocalDateTime.now().format(DATE_FORMATTER) + "</td>" +
                    "</tr>" +
                    "</table>" +
                    "<p style='margin-top: 15px;'><strong>✓ Prochaine étape:</strong> Vous recevrez sous peu un email contenant vos bons en pièce jointe (format PDF).</p>" +
                    "<p style='color: #666; font-size: 12px;'>Chaque bon est à usage unique et doit être présenté en magasin pour validation.</p>";

            EmailService.sendEmail(clientEmail, "🎁 Vos Bons Cadeau Intermart - " + reference, corps);
            System.out.println("✅ Notification envoi bons envoyée au client");
        } catch (Exception e) {
            System.err.println("❌ Erreur envoi notification client: " + e.getMessage());
        }
    }
}