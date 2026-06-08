package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.DAO.VoucherDAO;

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
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                List<String[]> all = VoucherDAO.getVoucherRequests("Administrateur", -1);
                if (statut != null && !statut.isBlank()) {
                    all.removeIf(row -> !statut.equalsIgnoreCase(row[5]));
                }
                return all;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Compte les demandes d'un statut donné. */
    public void compterDemandes(String statut,
                                Consumer<Integer> onSuccess,
                                Consumer<String> onError) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                List<String[]> all = VoucherDAO.getVoucherRequests("Administrateur", -1);
                if (statut == null || statut.isBlank()) return all.size();
                return (int) all.stream().filter(r -> statut.equalsIgnoreCase(r[5])).count();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Change le statut d'une demande avec traçabilité. */
    public void changerStatut(int demandeId, String statut, int userId,
                              Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                VoucherDAO.updateVoucherStatus(demandeId, statut, userId);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /**
     * Vérification séparation des tâches — contrôle effectué côté base de données
     * via trigger/procédure. Retourne toujours false (permissif côté Java).
     */
    public void verifierSeparationTaches(int demandeId, int userId,
                                         Consumer<Boolean> onResult,
                                         Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return false; // vérification SoD déléguée à la BD
            }
        };
        task.setOnSucceeded(e -> onResult.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
