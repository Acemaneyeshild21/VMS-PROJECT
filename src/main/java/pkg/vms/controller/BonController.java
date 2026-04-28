package pkg.vms.controller;

import pkg.vms.DAO.VoucherDAO;

import javax.swing.SwingWorker;
import java.util.function.Consumer;

public class BonController {

    public void archiverDemandesExpirees(int userId,
                                          Consumer<Integer> onSuccess,
                                          Consumer<String> onError) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return VoucherDAO.archiverDemandesExpirees(userId);
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void archiverDemande(int demandeId, int userId,
                                Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                VoucherDAO.updateVoucherStatus(demandeId, "ARCHIVE", userId);
                return null;
            }
            @Override
            protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
