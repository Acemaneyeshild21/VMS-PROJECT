package pkg.vms.controller;

import pkg.vms.DAO.AuthDAO;

import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginController {

    public void authenticate(String username, String password,
                             Consumer<AuthDAO.UserSession> onSuccess,
                             Consumer<String> onError) {
        new SwingWorker<AuthDAO.UserSession, Void>() {
            @Override
            protected AuthDAO.UserSession doInBackground() throws Exception {
                return AuthDAO.authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    AuthDAO.UserSession session = get();
                    if (session != null) {
                        onSuccess.accept(session);
                    } else {
                        onError.accept("Identifiants incorrects. Veuillez réessayer.");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause();
                    if (cause instanceof SQLException) {
                        onError.accept("Erreur de connexion à la base de données.");
                    } else {
                        onError.accept("Erreur inattendue : " + ex.getMessage());
                    }
                }
            }
        }.execute();
    }
}
