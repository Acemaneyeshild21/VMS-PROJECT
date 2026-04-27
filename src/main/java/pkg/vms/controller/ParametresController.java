package pkg.vms.controller;

import pkg.vms.ExcelExportService;
import pkg.vms.DAO.*;

import javax.swing.SwingWorker;
import java.util.*;
import java.util.function.Consumer;

public class ParametresController {

    // ── User profile ─────────────────────────────────────────────────────────

    public void chargerProfil(int userId, Consumer<UserDAO.UserProfile> onSuccess, Consumer<String> onError) {
        new SwingWorker<UserDAO.UserProfile, Void>() {
            @Override protected UserDAO.UserProfile doInBackground() throws Exception {
                return UserDAO.getUserProfile(userId);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void mettreAJourProfil(int userId, String username, String email,
                                   Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return UserDAO.updateProfile(userId, username, email);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void changerMotDePasse(int userId, String ancien, String nouveau,
                                   Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return UserDAO.updatePassword(userId, ancien, nouveau);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Users management ─────────────────────────────────────────────────────

    public void chargerUtilisateurs(Consumer<List<UserDAO.UserProfile>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<UserDAO.UserProfile>, Void>() {
            @Override protected List<UserDAO.UserProfile> doInBackground() throws Exception {
                return UserDAO.getAllUsers();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void chargerRolesDisponibles(Consumer<List<String>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<String>, Void>() {
            @Override protected List<String> doInBackground() throws Exception {
                return UserDAO.getAllRoles();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void creerUtilisateur(String username, String email, String password, String role,
                                  Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return UserDAO.registerUser(username, email, password, role);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void changerRoleUtilisateur(int uid, String newRole, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                UserDAO.updateUserRole(uid, newRole);
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── DB monitoring ─────────────────────────────────────────────────────────

    public void chargerStatsDB(Consumer<StatistiquesDAO.DbStats> onSuccess, Consumer<String> onError) {
        new SwingWorker<StatistiquesDAO.DbStats, Void>() {
            @Override protected StatistiquesDAO.DbStats doInBackground() throws Exception {
                return StatistiquesDAO.getDBStats();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void vacuumDB(Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                StatistiquesDAO.vacuumDB();
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void testerConnexionDB(Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return DBconnect.testConnection();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Email config ─────────────────────────────────────────────────────────

    public void chargerConfigEmail(Consumer<SettingsDAO.EmailSettings> onSuccess, Consumer<String> onError) {
        new SwingWorker<SettingsDAO.EmailSettings, Void>() {
            @Override protected SettingsDAO.EmailSettings doInBackground() throws Exception {
                return SettingsDAO.getEmailSettings();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void sauvegarderConfigEmail(SettingsDAO.EmailSettings settings,
                                        Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return SettingsDAO.updateEmailSettings(settings);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Audit logs ────────────────────────────────────────────────────────────

    public void chargerLogs(String role, int userId, Consumer<List<String[]>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<String[]>, Void>() {
            @Override protected List<String[]> doInBackground() throws Exception {
                return AuditDAO.getAuditTrail(role, userId);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Societies ─────────────────────────────────────────────────────────────

    public void chargerSocietes(Consumer<List<SocieteDAO.Societe>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<SocieteDAO.Societe>, Void>() {
            @Override protected List<SocieteDAO.Societe> doInBackground() throws Exception {
                return SocieteDAO.getAllSocietes();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void ajouterSociete(String nom, String adresse, String tel, String email,
                                Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                SocieteDAO.addSociete(nom, adresse, tel, email);
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void supprimerSociete(int id, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                SocieteDAO.deleteSociete(id);
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Magasins ──────────────────────────────────────────────────────────────

    public void chargerMagasins(Consumer<List<MagasinDAO.Magasin>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<MagasinDAO.Magasin>, Void>() {
            @Override protected List<MagasinDAO.Magasin> doInBackground() throws Exception {
                return MagasinDAO.getAllMagasins();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void ajouterMagasin(String nom, String adresse, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return MagasinDAO.addMagasin(nom, adresse, null);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Config bons ───────────────────────────────────────────────────────────

    public void chargerConfigBons(Consumer<Map<String, String>> onSuccess, Consumer<String> onError) {
        new SwingWorker<Map<String, String>, Void>() {
            @Override protected Map<String, String> doInBackground() throws Exception {
                Map<String, String> m = new LinkedHashMap<>();
                String[] keys = {"bon_validite_defaut", "bon_type_defaut", "bon_entreprise", "bon_format_qr", "bon_signature"};
                for (String k : keys) {
                    String v = SettingsDAO.getSetting(k);
                    m.put(k, v != null ? v : "");
                }
                return m;
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void sauvegarderConfigBons(Map<String, String> settings, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                for (Map.Entry<String, String> e : settings.entrySet())
                    SettingsDAO.updateSetting(e.getKey(), e.getValue());
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    // ── Exports ───────────────────────────────────────────────────────────────

    public void exporterDemandes(String filePath, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] cols = {"ID", "Référence", "Facture", "Client", "Montant", "Nb Bons", "Statut", "Date"};
                ExcelExportService.exportData(filePath, "Demandes", cols, BonDAO.getDemandesForExport());
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void exporterBons(String filePath, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] cols = {"ID", "Code Unique", "Valeur", "Statut", "Émission", "Réf Demande", "Client"};
                ExcelExportService.exportData(filePath, "Bons", cols, BonDAO.getBonsForExport());
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void exporterClients(String filePath, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] cols = {"ID", "Nom", "Email", "Téléphone", "Société", "Création", "Actif"};
                ExcelExportService.exportData(filePath, "Clients", cols, ClientDAO.getClientsForExport());
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void exporterBonsExpiration(String filePath, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String seuilStr = SettingsDAO.getSetting("bon_expiration_seuil_jours");
                int seuil = (seuilStr != null && !seuilStr.isEmpty()) ? Integer.parseInt(seuilStr) : 30;
                String[] cols = {"ID", "Code Unique", "Valeur", "Réf Demande", "Client",
                                 "Émission", "Expiration", "Jours Restants"};
                ExcelExportService.exportData(filePath, "Bons Expiration", cols,
                        BonDAO.getBonsProchesExpirationForExport(seuil));
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
