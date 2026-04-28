package pkg.vms;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

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
