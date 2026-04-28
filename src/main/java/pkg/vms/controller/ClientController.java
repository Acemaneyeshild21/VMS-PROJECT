package pkg.vms.controller;

import pkg.vms.Client;
import pkg.vms.ExcelExportService;
import pkg.vms.DAO.ClientDAO;

import javax.swing.SwingWorker;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ClientController {

    public void chargerClients(Consumer<List<Client>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<Client>, Void>() {
            @Override protected List<Client> doInBackground() throws Exception {
                return ClientDAO.getAllClients();
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void rechercherClients(String query, Consumer<List<Client>> onSuccess, Consumer<String> onError) {
        new SwingWorker<List<Client>, Void>() {
            @Override protected List<Client> doInBackground() throws Exception {
                return ClientDAO.searchClients(query);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void getClientById(int clientId, Consumer<Client> onSuccess, Consumer<String> onError) {
        new SwingWorker<Client, Void>() {
            @Override protected Client doInBackground() throws Exception {
                return ClientDAO.getClientById(clientId);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void creerClient(Client client, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return ClientDAO.addClient(client);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void modifierClient(Client client, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return ClientDAO.updateClient(client);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void desactiverClient(int clientId, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return ClientDAO.deactivateClient(clientId);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void supprimerClient(int clientId, Consumer<Boolean> onSuccess, Consumer<String> onError) {
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws Exception {
                return ClientDAO.deleteClientPermanently(clientId);
            }
            @Override protected void done() {
                try { onSuccess.accept(get()); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }

    public void exporterClients(String filePath, Runnable onSuccess, Consumer<String> onError) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                String[] columns = {"ID", "Nom", "Email", "Téléphone", "Société", "Création", "Actif"};
                List<Map<String, Object>> data = ClientDAO.getClientsForExport();
                ExcelExportService.exportData(filePath, "Clients", columns, data);
                return null;
            }
            @Override protected void done() {
                try { get(); onSuccess.run(); }
                catch (Exception ex) { onError.accept(ex.getMessage()); }
            }
        }.execute();
    }
}
