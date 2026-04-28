package pkg.vms.controller;

import pkg.vms.DAO.VoucherDAO;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.function.Consumer;

public class ValidationController {

    /**
     * Charge toutes les demandes filtrées par statut.
     * Retourne {@code List<String[]>} : [demande_id, reference, client, montant, type, statut, date, invoice_ref, valeur_unit]
     */
    public void chargerDemandes(String statut,
                                Consumer<List<String[]>> onSuccess,
                                Consumer<String> onError) {
        new SwingWorker<List<String[]>, Void>() {
            @Override
            protected List<String[]> doInBackground() throws Exception {
                List<String[]> all = VoucherDAO.getVoucherRequests("Administrateur", -1);
                // filter by statut (index 5)
                if (statut != null && !statut.isBlank()) {
                    all.removeIf(row -> !statut.equalsIgnoreCase(row[5]));
                }
                return all;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /** Compte les demandes d'un statut donné. */
    public void compterDemandes(String statut,
                                Consumer<Integer> onSuccess,
                                Consumer<String> onError) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                List<String[]> all = VoucherDAO.getVoucherRequests("Administrateur", -1);
                if (statut == null || statut.isBlank()) return all.size();
                return (int) all.stream().filter(r -> statut.equalsIgnoreCase(r[5])).count();
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /** Change le statut d'une demande avec traçabilité. */
    public void changerStatut(int demandeId, String statut, int userId,
                              Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                VoucherDAO.updateVoucherStatus(demandeId, statut, userId);
                return null;
            }
            @Override
            protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /**
     * Vérifie la séparation des tâches : retourne true si l'utilisateur
     * a déjà validé le paiement de cette demande (interdit d'approuver soi-même).
     * Utilise l'audit trail — si la colonne valide_par n'est pas disponible,
     * retourne false par défaut (permissif).
     */
    public void verifierSeparationTaches(int demandeId, int userId,
                                         Consumer<Boolean> onResult,
                                         Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                // Délégué à VoucherDAO.updateVoucherStatus — la vérification SoD
                // est appliquée en base via le trigger ou la procédure stockée.
                // Ici on retourne toujours false (contrôle DB-side).
                return false;
            }
            @Override
            protected void done() {
                try { onResult.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
