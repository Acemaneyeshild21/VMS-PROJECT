package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.DAO.AuditDAO;
import pkg.vms.DAO.BonDAO;
import pkg.vms.DAO.SettingsDAO;
import pkg.vms.DAO.StatistiquesDAO;

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
        // Tendances (champs complémentaires pour le Dashboard)
        public int demandesAttente     = 0;
        public int demandesApprouvees  = 0;
        public int bonsExpirant30j     = 0;
        public int redemptionsToday    = 0;
        // Listes de détail
        public List<Object[]> statutRows        = new ArrayList<>();
        public List<Object[]> auditRows         = new ArrayList<>();
        public List<Object[]> topClientsRows    = new ArrayList<>();
        public List<Object[]> bonsExpirationRows = new ArrayList<>();
    }

    public void chargerDonnees(String role, int userId,
                               Consumer<StatsData> onSuccess,
                               Consumer<String> onError) {
        Task<StatsData> task = new Task<>() {
            @Override
            protected StatsData call() {
                StatsData data = new StatsData();

                try {
                    data.totalDemandes  = StatistiquesDAO.compterDemandes();
                    data.montantTotal   = StatistiquesDAO.sumMontantBons();
                    data.bonsActifs     = StatistiquesDAO.compterBonsActifs();
                    data.tauxRedemption = StatistiquesDAO.getTauxRedemption();
                    data.statutRows     = StatistiquesDAO.getStatsByStatut();
                    data.topClientsRows = StatistiquesDAO.getTopClients(5);

                    // Compter EN_ATTENTE et APPROUVE depuis statutRows
                    for (Object[] r : data.statutRows) {
                        String s = String.valueOf(r[0]);
                        int cnt  = ((Number) r[1]).intValue();
                        if ("EN_ATTENTE_PAIEMENT".equals(s)) data.demandesAttente    = cnt;
                        if ("APPROUVE".equals(s))             data.demandesApprouvees = cnt;
                    }
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
                    data.bonsExpirant30j = data.bonsExpirationRows.size();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return data;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
