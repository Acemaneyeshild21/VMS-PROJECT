package pkg.vms.controller;

import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.ClientDAO;
import pkg.vms.DAO.VoucherDAO;
import pkg.vms.EmailService;
import pkg.vms.VoucherPDFGenerator;

import javax.swing.SwingWorker;
import java.util.List;
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

    public void chargerDemandes(Consumer<List<VoucherDAO.DemandeComplet>> onSuccess,
                                 Consumer<String> onError) {
        new SwingWorker<List<VoucherDAO.DemandeComplet>, Void>() {
            @Override
            protected List<VoucherDAO.DemandeComplet> doInBackground() throws Exception {
                return VoucherDAO.getDemandesComplet();
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void chargerDetail(int demandeId,
                               Consumer<VoucherDAO.DemandeDetail> onSuccess,
                               Consumer<String> onError) {
        new SwingWorker<VoucherDAO.DemandeDetail, Void>() {
            @Override
            protected VoucherDAO.DemandeDetail doInBackground() throws Exception {
                return VoucherDAO.getDemandeDetail(demandeId);
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void chargerDonneesFormulaire(Consumer<FormData> onSuccess, Consumer<String> onError) {
        new SwingWorker<FormData, Void>() {
            @Override
            protected FormData doInBackground() throws Exception {
                return new FormData(
                    ClientDAO.getActiveClients(),
                    ClientDAO.getAllMagasins()
                );
            }
            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void creerDemande(int userId, int clientId, int nbBons, double valeurUnit,
                              String type, int magasinId, int validiteJours,
                              String motif, String emailDest,
                              Consumer<Integer> onSuccess, Consumer<String> onError) {
        new SwingWorker<Integer, Void>() {
            @Override
            protected Integer doInBackground() throws Exception {
                return VoucherDAO.createVoucherRequest(userId, clientId, nbBons, valeurUnit,
                        type, magasinId, validiteJours, motif, emailDest);
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

    /**
     * @param onProgress  called on EDT with {pctStr, label}
     * @param onSuccess   called on EDT with number of vouchers generated (email OK)
     * @param onError     called on EDT with error message (generation failed)
     * @param onEmailEchec called on EDT with nb vouchers generated (generation OK, email failed)
     */
    public void genererBons(int demandeId, int userId,
                             Consumer<String[]> onProgress,
                             Consumer<Integer> onSuccess,
                             Consumer<String> onError,
                             Consumer<Integer> onEmailEchec) {
        new SwingWorker<Integer, String[]>() {
            boolean emailFailed = false;

            @Override
            protected Integer doInBackground() throws Exception {
                publish(new String[]{"10", "Génération des bons en base de données…"});
                int nbBons = BonDAO.genererBons(demandeId, userId);

                publish(new String[]{"20", "Chargement des bons générés…"});
                List<BonDAO.BonInfo> bons = BonDAO.getBonsByDemande(demandeId);

                int total = bons.size();
                for (int i = 0; i < total; i++) {
                    BonDAO.BonInfo bon = bons.get(i);
                    int pct = 20 + 60 * (i + 1) / Math.max(total, 1);
                    publish(new String[]{String.valueOf(pct),
                            "PDF " + (i + 1) + "/" + total + " — " + bon.codeUnique});
                    VoucherPDFGenerator.PdfResult pdf = VoucherPDFGenerator.genererPDF(bon);
                    BonDAO.updatePdfPath(bon.bonId, pdf.filePath());
                    BonDAO.updatePdfData(bon.bonId, pdf.data());
                    bon.pdfPath = pdf.filePath();
                }

                publish(new String[]{"85", "Envoi des emails…"});
                try {
                    EmailService.envoyerBonsParEmail(demandeId, bons, userId);
                    VoucherDAO.marquerCommeEnvoye(demandeId, userId);
                } catch (Exception emailEx) {
                    emailFailed = true;
                    BonDAO.logEmailError(demandeId, emailEx.getMessage());
                    AuditDAO.logEnvoi(demandeId, false, userId, emailEx.getMessage());
                }

                publish(new String[]{"100", "Terminé !"});
                return nbBons;
            }

            @Override
            protected void process(List<String[]> chunks) {
                onProgress.accept(chunks.get(chunks.size() - 1));
            }

            @Override
            protected void done() {
                try {
                    int nb = get();
                    if (emailFailed) {
                        onEmailEchec.accept(nb);
                    } else {
                        onSuccess.accept(nb);
                    }
                } catch (Exception ex) {
                    onError.accept(ex.getMessage());
                }
            }
        }.execute();
    }

    public void renvoyerEmail(int demandeId, int userId,
                               Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<BonDAO.BonInfo> bons = BonDAO.getBonsByDemande(demandeId);
                EmailService.envoyerBonsParEmail(demandeId, bons, userId);
                VoucherDAO.marquerCommeEnvoye(demandeId, userId);
                BonDAO.resolveEmailErrors(demandeId);
                AuditDAO.logEnvoi(demandeId, true, userId, null);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    onSuccess.run();
                } catch (Exception ex) {
                    BonDAO.logEmailError(demandeId, "Renvoi échoué : " + ex.getMessage());
                    onError.accept(ex.getMessage());
                }
            }
        }.execute();
    }
}
