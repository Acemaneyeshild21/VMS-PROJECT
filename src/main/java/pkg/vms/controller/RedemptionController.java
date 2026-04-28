package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;

import java.util.List;
import java.util.function.Consumer;

public class RedemptionController {

    public void chargerMagasins(Consumer<List<ClientDAO.MagasinInfo>> onSuccess,
                                Consumer<String> onError) {
        Task<List<ClientDAO.MagasinInfo>> task = new Task<>() {
            @Override
            protected List<ClientDAO.MagasinInfo> call() throws Exception {
                return ClientDAO.getAllMagasins();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void redimerBon(String code, int magasinId, int userId,
                           Consumer<BonDAO.RedemptionResult> onResult,
                           Consumer<String> onError) {
        Task<BonDAO.RedemptionResult> task = new Task<>() {
            @Override
            protected BonDAO.RedemptionResult call() throws Exception {
                return BonDAO.redimerBon(code, magasinId, userId);
            }
        };
        task.setOnSucceeded(e -> onResult.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            onResult.accept(new BonDAO.RedemptionResult(
                false,
                "Erreur : " + (cause != null ? cause.getMessage() : "inconnue"),
                0, BonDAO.RedemptionResult.ErrorType.UNKNOWN));
        });
        new Thread(task).start();
    }
}
