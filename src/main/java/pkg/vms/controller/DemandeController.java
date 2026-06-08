package pkg.vms.controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.EmailService;
import pkg.vms.VoucherPDFGenerator;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class DemandeController {

    public static class FormData {
        public final List<ClientDAO.ClientInfo> clients;
        public final List<ClientDAO.MagasinInfo> magasins;

        public FormData(List<ClientDAO.ClientInfo> clients, List<ClientDAO.MagasinInfo> magasins) {
            this.clients = clients;
            this.magasins = magasins;
        }
    }

    public void chargerDonneesFormulaire(Consumer<FormData> onSuccess, Consumer<String> onError) {
        Task<FormData> task = new Task<>() {
            @Override
            protected FormData call() throws Exception {
                return new FormData(
                    ClientDAO.getActiveClients(),
                    ClientDAO.getAllMagasins()
                );
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void creerDemande(int userId, int clientId, int nbBons, double valeurUnit,
                              String type, int magasinId, int validiteJours,
                              String motif, String emailDest,
                              Consumer<Integer> onSuccess, Consumer<String> onError) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return VoucherDAO.createVoucherRequest(userId, clientId, nbBons, valeurUnit,
                        type, magasinId, validiteJours, motif, emailDest);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

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
     * @param onProgress   appelé sur le FX Thread avec {pctStr, label}
     * @param onSuccess    appelé sur le FX Thread avec le nombre de bons générés (email OK)
     * @param onError      appelé sur le FX Thread avec le message d'erreur (génération échouée)
     * @param onEmailEchec appelé sur le FX Thread avec nb bons générés (génération OK, email échoué)
     */
    public void genererBons(int demandeId, int userId,
                             Consumer<String[]> onProgress,
                             Consumer<Integer> onSuccess,
                             Consumer<String> onError,
                             Consumer<Integer> onEmailEchec) {

        boolean[] emailFailed = {false};

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                Platform.runLater(() -> onProgress.accept(new String[]{"10", "Génération des bons en base de données…"}));
                int nbBons = BonDAO.genererBons(demandeId, userId);

                Platform.runLater(() -> onProgress.accept(new String[]{"20", "Chargement des bons générés…"}));
                List<BonDAO.BonInfo> bons = BonDAO.getBonsByDemande(demandeId);

                int total = bons.size();
                for (int i = 0; i < total; i++) {
                    BonDAO.BonInfo bon = bons.get(i);
                    int pct = 20 + 60 * (i + 1) / Math.max(total, 1);
                    final String label = "PDF " + (i + 1) + "/" + total + " — " + bon.codeUnique;
                    final int finalPct = pct;
                    Platform.runLater(() -> onProgress.accept(new String[]{String.valueOf(finalPct), label}));
                    String pdfPath = VoucherPDFGenerator.genererPDF(bon);
                    BonDAO.updatePdfPath(bon.bonId, pdfPath);
                    try {
                        byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(pdfPath));
                        BonDAO.updatePdfData(bon.bonId, data);
                    } catch (Exception readEx) {
                        System.err.println("[DemandeController] PDF bytes non persistés : " + readEx.getMessage());
                    }
                    bon.pdfPath = pdfPath;
                }

                Platform.runLater(() -> onProgress.accept(new String[]{"85", "Envoi des emails…"}));
                String[] emailErr = {null};
                CountDownLatch latch = new CountDownLatch(1);
                EmailService.envoyerBonsParEmail(demandeId, bons, userId,
                        () -> latch.countDown(),
                        err -> { emailErr[0] = err; latch.countDown(); });
                latch.await();
                if (emailErr[0] != null) {
                    emailFailed[0] = true;
                    BonDAO.logEmailError(demandeId, emailErr[0]);
                } else {
                    VoucherDAO.marquerCommeEnvoye(demandeId, userId);
                }

                Platform.runLater(() -> onProgress.accept(new String[]{"100", "Terminé !"}));
                return nbBons;
            }
        };

        task.setOnSucceeded(e -> {
            if (emailFailed[0]) {
                onEmailEchec.accept(task.getValue());
            } else {
                onSuccess.accept(task.getValue());
            }
        });
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void renvoyerEmail(int demandeId, int userId,
                               Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<BonDAO.BonInfo> bons = BonDAO.getBonsByDemande(demandeId);
                String[] emailErr = {null};
                CountDownLatch latch = new CountDownLatch(1);
                EmailService.envoyerBonsParEmail(demandeId, bons, userId,
                        () -> latch.countDown(),
                        err -> { emailErr[0] = err; latch.countDown(); });
                latch.await();
                if (emailErr[0] != null) throw new Exception("Email échec : " + emailErr[0]);
                VoucherDAO.marquerCommeEnvoye(demandeId, userId);
                BonDAO.resolveEmailErrors(demandeId);
                AuditDAO.logEnvoi(demandeId, true, userId, null);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> {
            BonDAO.logEmailError(demandeId, "Renvoi échoué : " + task.getException().getMessage());
            onError.accept(task.getException().getMessage());
        });
        new Thread(task).start();
    }
}
