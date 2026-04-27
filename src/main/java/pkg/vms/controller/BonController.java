package pkg.vms.controller;

import pkg.vms.DAO.VoucherDAO;

import javax.swing.SwingWorker;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BonController {

    public static class BonView {
        public final int    id;
        public final String client;
        public final String email;
        public final String valeurFmt;
        public final String statut;
        public final String expirTag;  // ACTIF | MOIS | EXPIRE | INCONNU
        public final String expirStr;
        public final String action;    // "ARCHIVER" or ""

        BonView(int id, String client, String email, String valeurFmt,
                String statut, String expirTag, String expirStr, String action) {
            this.id       = id;
            this.client   = client;
            this.email    = email;
            this.valeurFmt = valeurFmt;
            this.statut   = statut;
            this.expirTag = expirTag;
            this.expirStr = expirStr;
            this.action   = action;
        }
    }

    public void chargerBons(Consumer<List<BonView>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<BonView>, Void>() {
            @Override
            protected List<BonView> doInBackground() throws Exception {
                List<VoucherDAO.BonSummaryRaw> raws = VoucherDAO.getBonsSummary();
                List<BonView> views = new ArrayList<>();
                LocalDate today = LocalDate.now();

                for (VoucherDAO.BonSummaryRaw r : raws) {
                    String expirTag = "INCONNU";
                    String expirStr = "—";

                    if (r.dateCreation != null && r.validiteJours > 0) {
                        LocalDate dateCreation = r.dateCreation.toLocalDateTime().toLocalDate();
                        LocalDate dateExpir    = dateCreation.plusDays(r.validiteJours);
                        long joursRestants = ChronoUnit.DAYS.between(today, dateExpir);
                        if (joursRestants < 0) {
                            expirTag = "EXPIRE";
                            expirStr = "Expiré (" + Math.abs(joursRestants) + "j)";
                        } else if (joursRestants <= 30) {
                            expirTag = "MOIS";
                            expirStr = joursRestants + " j restants";
                        } else {
                            expirTag = "ACTIF";
                            expirStr = joursRestants + " j restants";
                        }
                    }

                    String action = (expirTag.equals("EXPIRE") && !"ARCHIVE".equals(r.statut))
                            ? "ARCHIVER" : "";

                    views.add(new BonView(
                        r.id,
                        r.client  != null ? r.client : "—",
                        r.email   != null ? r.email  : "—",
                        String.format("Rs %,.0f", r.valeur),
                        r.statut  != null ? r.statut : "—",
                        expirTag,
                        expirStr,
                        action
                    ));
                }
                return views;
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

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
