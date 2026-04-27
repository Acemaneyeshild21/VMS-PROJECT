package pkg.vms.controller;

import pkg.vms.DAO.VoucherDAO;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.function.Consumer;

public class ValidationController {

    public void chargerDemandes(String statut,
                                Consumer<List<VoucherDAO.DemandeComplet>> onSuccess,
                                Consumer<String> onError) {
        new SwingWorker<List<VoucherDAO.DemandeComplet>, Void>() {
            @Override
            protected List<VoucherDAO.DemandeComplet> doInBackground() throws Exception {
                return VoucherDAO.getDemandesParStatut(statut);
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void compterDemandes(String statut,
                                Consumer<Integer> onSuccess,
                                Consumer<String> onError) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return VoucherDAO.compterDemandesParStatut(statut);
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

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

    public void verifierSeparationTaches(int demandeId, int userId,
                                         Consumer<Boolean> onResult,
                                         Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return VoucherDAO.aValidePaiement(demandeId, userId);
            }
            @Override
            protected void done() {
                try { onResult.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
