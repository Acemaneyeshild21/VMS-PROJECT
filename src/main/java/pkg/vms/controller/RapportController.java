package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.RapportExcelService;

import java.util.function.Consumer;

/**
 * Contrôleur pour la génération des rapports Excel (JavaFX Task).
 * Toute opération disk/SQL se fait dans {@code Task.call()}.
 */
public class RapportController {

    /**
     * Génère le rapport Excel complet (5 feuilles : Dashboard, Demandes, Bons, Clients, Config ODBC).
     *
     * @param filePath  chemin absolu du fichier .xlsx
     * @param onSuccess appelé sur le FX Thread avec le chemin du fichier généré
     * @param onError   appelé sur le FX Thread avec le message d'erreur
     */
    public void genererRapportComplet(String filePath,
                                       Consumer<String> onSuccess,
                                       Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                RapportExcelService.genererRapportComplet(filePath);
                return filePath;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Génère uniquement la feuille Demandes. */
    public void genererRapportDemandes(String filePath,
                                        Consumer<String> onSuccess,
                                        Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                RapportExcelService.genererRapportDemandes(filePath);
                return filePath;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Génère uniquement la feuille Bons. */
    public void genererRapportBons(String filePath,
                                    Consumer<String> onSuccess,
                                    Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                RapportExcelService.genererRapportBons(filePath);
                return filePath;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /** Génère uniquement la feuille Clients. */
    public void genererRapportClients(String filePath,
                                       Consumer<String> onSuccess,
                                       Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                RapportExcelService.genererRapportClients(filePath);
                return filePath;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    /**
     * Génère le rapport Bons proches d'expiration.
     *
     * @param joursMax nombre de jours d'anticipation (ex: 30)
     */
    public void genererRapportExpiration(String filePath, int joursMax,
                                          Consumer<String> onSuccess,
                                          Consumer<String> onError) {
        Task<String> task = new Task<>() {
            @Override protected String call() throws Exception {
                RapportExcelService.genererRapportExpiration(filePath, joursMax);
                return filePath;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
