package pkg.vms.controller;

import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class RedemptionController {

    public void chargerMagasins(Consumer<List<ClientDAO.MagasinInfo>> onSuccess,
                                Consumer<String> onError) {
        new SwingWorker<List<ClientDAO.MagasinInfo>, Void>() {
            @Override
            protected List<ClientDAO.MagasinInfo> doInBackground() throws Exception {
                return ClientDAO.getAllMagasins();
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void redimerBon(String code, int magasinId, int userId,
                           Consumer<BonDAO.RedemptionResult> onResult,
                           Consumer<String> onError) {
        new SwingWorker<BonDAO.RedemptionResult, Void>() {
            @Override
            protected BonDAO.RedemptionResult doInBackground() throws Exception {
                return BonDAO.redimerBon(code, magasinId, userId);
            }
            @Override
            protected void done() {
                try {
                    onResult.accept(get());
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    onResult.accept(new BonDAO.RedemptionResult(
                        false,
                        "Erreur : " + (cause != null ? cause.getMessage() : ex.getMessage()),
                        0, BonDAO.RedemptionResult.ErrorType.UNKNOWN));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    onResult.accept(new BonDAO.RedemptionResult(
                        false, "Opération interrompue.", 0,
                        BonDAO.RedemptionResult.ErrorType.UNKNOWN));
                }
            }
        }.execute();
    }
}
