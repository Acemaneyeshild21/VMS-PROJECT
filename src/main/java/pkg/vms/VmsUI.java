package pkg.vms;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/**
 * Helpers UI partagés entre toutes les vues JavaFX.
 * Garantit une cohérence visuelle sans dupliquer le code dans chaque vue.
 */
public final class VmsUI {

    private VmsUI() {}

    // ── Badges de statut ─────────────────────────────────────────────────────

    /**
     * Retourne un Label stylisé (badge arrondi) pour un statut métier.
     * Les classes CSS (.badge-*) sont définies dans vms.css.
     *
     * @param statut   valeur brute BD  (GENERE, ENVOYE, APPROUVE, PAYE, REJETE…)
     * @param display  texte affiché   (peut être identique au statut ou localisé)
     */
    public static Label statusBadge(String statut, String display) {
        Label l = new Label(display == null ? statut : display);
        l.getStyleClass().add("badge");
        l.getStyleClass().add(badgeClass(statut));
        return l;
    }

    /** Surcharge sans texte personnalisé — affiche le statut brut. */
    public static Label statusBadge(String statut) {
        return statusBadge(statut, frLabel(statut));
    }

    /** Conteneur centré contenant un badge — prêt pour setCellFactory. */
    public static HBox badgeCell(String statut) {
        HBox box = new HBox(statusBadge(statut));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(0, 0, 0, 0));
        return box;
    }

    // ── Empty states illustrés ───────────────────────────────────────────────

    /**
     * Panneau "empty state" avec émoji, titre, sous-titre et bouton d'action optionnel.
     *
     * <pre>
     *       📭
     *  Aucune demande
     *  Créez votre première demande
     *  [+ Nouvelle demande]
     * </pre>
     *
     * @param emoji       grand émoji illustratif
     * @param title       titre principal (gras)
     * @param subtitle    sous-titre (gris)
     * @param actionLabel texte du bouton CTA  (null = pas de bouton)
     * @param onAction    handler du bouton    (null = pas de bouton)
     */
    public static VBox emptyState(String emoji, String title, String subtitle,
                                  String actionLabel, EventHandler<ActionEvent> onAction) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(48, 24, 48, 24));
        box.setMaxWidth(360);

        Label ico = new Label(emoji);
        ico.setStyle("-fx-font-size:48;");

        Label ttl = new Label(title);
        ttl.setStyle("-fx-font-size:16;-fx-font-weight:bold;-fx-text-fill:#1e293b;");
        ttl.setWrapText(true);

        Label sub = new Label(subtitle);
        sub.setStyle("-fx-font-size:13;-fx-text-fill:#64748b;");
        sub.setWrapText(true);
        sub.setMaxWidth(300);

        box.getChildren().addAll(ico, ttl, sub);

        if (actionLabel != null && onAction != null) {
            Button btn = new Button(actionLabel);
            btn.getStyleClass().add("btn-primary");
            btn.setStyle("-fx-padding:9 20;");
            btn.setOnAction(onAction);
            box.getChildren().add(btn);
        }

        // Conteneur centré dans l'espace disponible
        StackPane wrapper = new StackPane(box);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setPrefHeight(300);

        // On retourne le VBox directement (à placer dans un StackPane centré)
        return box;
    }

    /** Surcharge sans bouton d'action. */
    public static VBox emptyState(String emoji, String title, String subtitle) {
        return emptyState(emoji, title, subtitle, null, null);
    }

    // ── Mapping statut → classe CSS ──────────────────────────────────────────

    private static String badgeClass(String statut) {
        if (statut == null) return "badge-gray";
        return switch (statut.toUpperCase()) {
            case "ENVOYE", "ACTIF"                      -> "badge-green";
            case "APPROUVE", "GENERE", "VALIDE"         -> "badge-blue";
            case "PAYE", "EN_ATTENTE_PAIEMENT"          -> "badge-yellow";
            case "REJETE", "ANNULE", "EXPIRE"           -> "badge-red";
            case "UTILISE", "REDIME"                    -> "badge-purple";
            case "ARCHIVE"                              -> "badge-gray";
            default                                     -> "badge-gray";
        };
    }

    // ── Labels français ──────────────────────────────────────────────────────

    private static String frLabel(String statut) {
        if (statut == null) return "—";
        return switch (statut.toUpperCase()) {
            case "EN_ATTENTE_PAIEMENT" -> "En attente";
            case "PAYE"                -> "Payé";
            case "APPROUVE"            -> "Approuvé";
            case "GENERE"              -> "Généré";
            case "ENVOYE"              -> "Envoyé";
            case "REJETE"              -> "Rejeté";
            case "ANNULE"              -> "Annulé";
            case "ARCHIVE"             -> "Archivé";
            case "ACTIF"               -> "Actif";
            case "EXPIRE"              -> "Expiré";
            case "UTILISE"             -> "Utilisé";
            case "REDIME"              -> "Rédemptié";
            default                    -> statut;
        };
    }
}
