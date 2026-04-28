package pkg.vms.controller;

import pkg.vms.RapportExcelService;

import javax.swing.SwingWorker;
import java.util.function.Consumer;

/**
 * Contrôleur pour la génération des rapports Excel.
 * Toute opération disk/SQL se fait dans {@code SwingWorker.doInBackground()}.
 */
public class RapportController {

    /**
     * Génère le rapport Excel complet (5 feuilles : Dashboard, Demandes, Bons, Clients, Config ODBC).
     *
     * @param filePath  chemin absolu du fichier .xlsx
     * @param onSuccess appelé sur l'EDT avec le chemin du fichier généré
     * @param onError   appelé sur l'EDT avec le message d'erreur
     */
    public void genererRapportComplet(String filePath,
                                       Consumer<String> onSuccess,
                                       Consumer<String> onError) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                RapportExcelService.genererRapportComplet(filePath);
                return filePath;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /** Génère uniquement la feuille Demandes. */
    public void genererRapportDemandes(String filePath,
                                        Consumer<String> onSuccess,
                                        Consumer<String> onError) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                RapportExcelService.genererRapportDemandes(filePath);
                return filePath;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /** Génère uniquement la feuille Bons. */
    public void genererRapportBons(String filePath,
                                    Consumer<String> onSuccess,
                                    Consumer<String> onError) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                RapportExcelService.genererRapportBons(filePath);
                return filePath;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /** Génère uniquement la feuille Clients. */
    public void genererRapportClients(String filePath,
                                       Consumer<String> onSuccess,
                                       Consumer<String> onError) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                RapportExcelService.genererRapportClients(filePath);
                return filePath;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    /**
     * Génère le rapport Bons proches d'expiration.
     *
     * @param joursMax  nombre de jours d'anticipation (ex: 30)
     */
    public void genererRapportExpiration(String filePath, int joursMax,
                                          Consumer<String> onSuccess,
                                          Consumer<String> onError) {
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                RapportExcelService.genererRapportExpiration(filePath, joursMax);
                return filePath;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
