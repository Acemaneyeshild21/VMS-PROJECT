package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.Client;
import pkg.vms.ExcelExportService;
import pkg.vms.DAO.ClientDAO;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClientController {

    public void chargerClients(Consumer<List<Client>> onSuccess, Consumer<String> onError) {
        Task<List<Client>> task = new Task<>() {
            @Override protected List<Client> call() throws Exception {
                return ClientDAO.getAllClients();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void rechercherClients(String query, Consumer<List<Client>> onSuccess, Consumer<String> onError) {
        Task<List<Client>> task = new Task<>() {
            @Override protected List<Client> call() throws Exception {
                return ClientDAO.searchClients(query);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void getClientById(int clientId, Consumer<Client> onSuccess, Consumer<String> onError) {
        Task<Client> task = new Task<>() {
            @Override protected Client call() throws Exception {
                return ClientDAO.getClientById(clientId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void creerClient(Client client, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return ClientDAO.addClient(client);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void modifierClient(Client client, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return ClientDAO.updateClient(client);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void desactiverClient(int clientId, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return ClientDAO.deactivateClient(clientId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void supprimerClient(int clientId, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        Task<Boolean> task = new Task<>() {
            @Override protected Boolean call() throws Exception {
                return ClientDAO.deleteClientPermanently(clientId);
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }

    public void exporterClients(String filePath, Runnable onSuccess, Consumer<String> onError) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                String[] columns = {"ID", "Nom", "Email", "Téléphone", "Société", "Création", "Actif"};
                List<Map<String, Object>> data = ClientDAO.getClientsForExport();
                ExcelExportService.exportData(filePath, "Clients", columns, data);
                return null;
            }
        };
        task.setOnSucceeded(e -> onSuccess.run());
        task.setOnFailed(e -> onError.accept(task.getException().getMessage()));
        new Thread(task).start();
    }
}
