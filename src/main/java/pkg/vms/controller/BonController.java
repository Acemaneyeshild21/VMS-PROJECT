package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.DAO.VoucherDAO;

import java.util.function.Consumer;

public class BonController {

    public void archiverDemandesExpirees(int userId,
                                          Consumer<Integer> onSuccess,
                                          Consumer<String> onError) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return VoucherDAO.archiverDemandesExpirees(userId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void archiverDemande(int demandeId, int userId,
                                Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                VoucherDAO.updateVoucherStatus(demandeId, "ARCHIVE", userId);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
