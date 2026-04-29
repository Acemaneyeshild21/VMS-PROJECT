package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.ExcelExportService;
import pkg.vms.DAO.*;

import java.util.*;
import java.util.function.Consumer;

public class ParametresController {

    // ── User profile ─────────────────────────────────────────────────────────

    public void chargerProfil(int userId, Consumer<UserDAO.UserProfile> onSuccess, Consumer<String> onError) {
        Task<UserDAO.UserProfile> task = new Task<>() {
            @Override protected UserDAO.UserProfile call() throws Exception {
                return UserDAO.getUserProfile(userId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void mettreAJourProfil(int userId, String username, String email,
                                   Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return UserDAO.updateProfile(userId, username, email);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void changerMotDePasse(int userId, String ancien, String nouveau,
                                   Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return UserDAO.updatePassword(userId, ancien, nouveau);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Users management ─────────────────────────────────────────────────────

    public void chargerUtilisateurs(Consumer<List<UserDAO.UserProfile>> onSuccess, Consumer<String> onError) {
        Task<List<UserDAO.UserProfile>> task = new Task<>() {
            @Override protected List<UserDAO.UserProfile> call() throws Exception {
                return UserDAO.getAllUsers();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void chargerRolesDisponibles(Consumer<List<String>> onSuccess, Consumer<String> onError) {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return UserDAO.getAllRoles();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void creerUtilisateur(String username, String email, String password, String role,
                                  Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return UserDAO.registerUser(username, email, password, role);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void supprimerUtilisateur(int userId, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                UserDAO.deleteUser(userId);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void changerRoleUtilisateur(int uid, String newRole, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                UserDAO.updateUserRole(uid, newRole);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── DB monitoring ─────────────────────────────────────────────────────────

    public void chargerStatsDB(Consumer<StatistiquesDAO.DbStats> onSuccess, Consumer<String> onError) {
        Task<StatistiquesDAO.DbStats> task = new Task<>() {
            @Override protected StatistiquesDAO.DbStats call() throws Exception {
                return StatistiquesDAO.getDBStats();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void vacuumDB(Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                StatistiquesDAO.vacuumDB();
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void testerConnexionDB(Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return DBconnect.testConnection();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void testerConnexionEmail(Consumer<Boolean> onSuccess, Consumer<String> onError) {
        pkg.vms.EmailService.testConnectionAsync(onSuccess, onError);
    }

    public void testerConnexionEmail(String host, int port, String user, String pass, boolean tls,
                                      Consumer<Boolean> onSuccess, Consumer<String> onError) {
        pkg.vms.EmailService.testConnectionAsync(host, port, user, pass, tls, onSuccess, onError);
    }

    // ── Email config ─────────────────────────────────────────────────────────

    public void chargerConfigEmail(Consumer<SettingsDAO.EmailSettings> onSuccess, Consumer<String> onError) {
        Task<SettingsDAO.EmailSettings> task = new Task<>() {
            @Override protected SettingsDAO.EmailSettings call() throws Exception {
                return SettingsDAO.getEmailSettings();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void sauvegarderConfigEmail(SettingsDAO.EmailSettings settings,
                                        Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return SettingsDAO.updateEmailSettings(settings);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Audit logs ────────────────────────────────────────────────────────────

    public void chargerLogs(String role, int userId, Consumer<List<String[]>> onSuccess, Consumer<String> onError) {
        Task<List<String[]>> task = new Task<>() {
            @Override protected List<String[]> call() throws Exception {
                return AuditDAO.getAuditTrail(role, userId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Societies ─────────────────────────────────────────────────────────────

    public void chargerSocietes(Consumer<List<SocieteDAO.Societe>> onSuccess, Consumer<String> onError) {
        Task<List<SocieteDAO.Societe>> task = new Task<>() {
            @Override protected List<SocieteDAO.Societe> call() throws Exception {
                return SocieteDAO.getAllSocietes();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void ajouterSociete(String nom, String adresse, String tel, String email,
                                Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                SocieteDAO.addSociete(nom, adresse, tel, email);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void supprimerSociete(int id, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                SocieteDAO.deleteSociete(id);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Magasins ──────────────────────────────────────────────────────────────

    public void chargerMagasins(Consumer<List<MagasinDAO.Magasin>> onSuccess, Consumer<String> onError) {
        Task<List<MagasinDAO.Magasin>> task = new Task<>() {
            @Override protected List<MagasinDAO.Magasin> call() throws Exception {
                return MagasinDAO.getAllMagasins();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void supprimerMagasin(int magasinId, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                MagasinDAO.deleteMagasin(magasinId);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void ajouterMagasin(String nom, String adresse, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return MagasinDAO.addMagasin(nom, adresse, null);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Config bons ───────────────────────────────────────────────────────────

    public void chargerConfigBons(Consumer<Map<String, String>> onSuccess, Consumer<String> onError) {
        Task<Map<String, String>> task = new Task<>() {
            @Override protected Map<String, String> call() throws Exception {
                Map<String, String> m = new LinkedHashMap<>();
                String[] keys = {"bon_validite_defaut", "bon_type_defaut", "bon_entreprise", "bon_format_qr", "bon_signature"};
                for (String k : keys) {
                    String v = SettingsDAO.getSetting(k);
                    m.put(k, v != null ? v : "");
                }
                return m;
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void sauvegarderConfigBons(Map<String, String> settings, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                for (Map.Entry<String, String> e : settings.entrySet())
                    SettingsDAO.updateSetting(e.getKey(), e.getValue());
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    // ── Exports ───────────────────────────────────────────────────────────────

    public void exporterDemandes(String filePath, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String[] cols = {"ID", "Référence", "Facture", "Client", "Montant", "Nb Bons", "Statut", "Date"};
                ExcelExportService.exportData(filePath, "Demandes", cols, BonDAO.getDemandesForExport());
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void exporterBons(String filePath, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String[] cols = {"ID", "Code Unique", "Valeur", "Statut", "Émission", "Réf Demande", "Client"};
                ExcelExportService.exportData(filePath, "Bons", cols, BonDAO.getBonsForExport());
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void exporterClients(String filePath, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String[] cols = {"ID", "Nom", "Email", "Téléphone", "Société", "Création", "Actif"};
                ExcelExportService.exportData(filePath, "Clients", cols, ClientDAO.getClientsForExport());
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void exporterBonsExpiration(String filePath, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String seuilStr = SettingsDAO.getSetting("bon_expiration_seuil_jours");
                int seuil = (seuilStr != null && !seuilStr.isEmpty()) ? Integer.parseInt(seuilStr) : 30;
                String[] cols = {"ID", "Code Unique", "Valeur", "Réf Demande", "Client",
                                 "Émission", "Expiration", "Jours Restants"};
                ExcelExportService.exportData(filePath, "Bons Expiration", cols,
                        BonDAO.getBonsProchesExpirationForExport(seuil));
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
