package pkg.vms.controller;

import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.SettingsDAO;
import pkg.vms.DAO.StatistiquesDAO;

import javax.swing.SwingWorker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class StatistiquesController {

    public static class StatsData {
        public int totalDemandes;
        public double montantTotal;
        public int bonsActifs;
        public double tauxRedemption;
        public List<Object[]> statutRows        = new ArrayList<>();
        public List<Object[]> auditRows         = new ArrayList<>();
        public List<Object[]> topClientsRows    = new ArrayList<>();
        public List<Object[]> bonsExpirationRows = new ArrayList<>();
    }

    public void chargerDonnees(String role, int userId,
                               Consumer<StatsData> onSuccess,
                               Consumer<String> onError) {
        new SwingWorker<StatsData, Void>() {
            @Override
            protected StatsData doInBackground() {
                StatsData data = new StatsData();

                try {
                    data.totalDemandes  = StatistiquesDAO.compterDemandes();
                    data.montantTotal   = StatistiquesDAO.sumMontantBons();
                    data.bonsActifs     = StatistiquesDAO.compterBonsActifs();
                    data.tauxRedemption = StatistiquesDAO.getTauxRedemption();
                    data.statutRows     = StatistiquesDAO.getStatsByStatut();
                    data.topClientsRows = StatistiquesDAO.getTopClients(5);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    List<String[]> trail = AuditDAO.getAuditTrail(role, userId);
                    int lim = Math.min(10, trail.size());
                    for (int i = 0; i < lim; i++) {
                        String[] r = trail.get(i);
                        data.auditRows.add(new Object[]{
                            r[1], r[2], r[0], r[3] != null ? r[3] : "—"
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                try {
                    String seuilStr = SettingsDAO.getSetting("bon_expiration_seuil_jours");
                    int seuil = (seuilStr != null && !seuilStr.isEmpty())
                            ? Integer.parseInt(seuilStr) : 30;
                    List<Map<String, Object>> bons =
                            BonDAO.getBonsProchesExpirationForExport(seuil);
                    for (Map<String, Object> bon : bons) {
                        data.bonsExpirationRows.add(new Object[]{
                            bon.get("Code Unique"), bon.get("Valeur"),
                            bon.get("Client"), bon.get("Expiration"), bon.get("Jours Restants")
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return data;
            }

            @Override
            protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
