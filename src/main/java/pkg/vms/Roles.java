package pkg.vms;

/**
 * Constantes des rôles VMS — BTS SIO SLAM RP2, Session 2026.
 * Les valeurs correspondent exactement à la colonne utilisateur.role en base.
 * Utiliser ces constantes partout où un rôle est comparé (évite les magic strings).
 */
public final class Roles {

    // ── 4 rôles officiels du cahier des charges ──────────────────────────────

    /** Administrateur siège — accès complet à tous les modules. */
    public static final String ADMIN_SIEGE         = "Administrateur";

    /** Comptable — valide les paiements des demandes. Ne peut pas approuver. */
    public static final String COMPTABLE           = "Comptable";

    /** Approbateur — approuve les demandes payées. Ne peut pas valider paiement. */
    public static final String APPROBATEUR         = "Approbateur";

    /** Superviseur magasin — effectue les rédemptions en point de vente. */
    public static final String SUPERVISEUR_MAGASIN = "Superviseur_Magasin";

    // ── Rôles complémentaires ────────────────────────────────────────────────
    public static final String MANAGER       = "Manager";
    public static final String COLLABORATEUR = "Collaborateur";

    private Roles() {}

    // ── Helpers de vérification de permission ────────────────────────────────

    /** Peut valider les paiements (onglet Paiements du panneau Validation). */
    public static boolean peutValiderPaiement(String role) {
        return COMPTABLE.equalsIgnoreCase(role)
            || ADMIN_SIEGE.equalsIgnoreCase(role)
            || MANAGER.equalsIgnoreCase(role);
    }

    /** Peut approuver les demandes (onglet Approbations du panneau Validation). */
    public static boolean peutApprouver(String role) {
        return APPROBATEUR.equalsIgnoreCase(role)
            || ADMIN_SIEGE.equalsIgnoreCase(role)
            || MANAGER.equalsIgnoreCase(role);
    }

    /** Peut accéder au panneau Validation (au moins un des deux onglets). */
    public static boolean peutValider(String role) {
        return peutValiderPaiement(role) || peutApprouver(role);
    }

    /** Peut effectuer des rédemptions en magasin. */
    public static boolean peutRedimer(String role) {
        return SUPERVISEUR_MAGASIN.equalsIgnoreCase(role)
            || ADMIN_SIEGE.equalsIgnoreCase(role)
            || MANAGER.equalsIgnoreCase(role);
    }

    /** Accès complet — admin siège uniquement. */
    public static boolean isAdmin(String role) {
        return ADMIN_SIEGE.equalsIgnoreCase(role);
    }
}
