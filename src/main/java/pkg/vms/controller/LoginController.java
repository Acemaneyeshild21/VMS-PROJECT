package pkg.vms.controller;

import javafx.concurrent.Task;
import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.AuthDAO.CompteBloqueException;

import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginController {

    /**
     * Authentifie un utilisateur de façon asynchrone (JavaFX Task).
     *
     * @param onSuccess  appelé sur le FX Application Thread avec la session si connexion réussie
     * @param onError    appelé sur le FX Application Thread avec un message d'erreur
     * @param onBloque   appelé sur le FX Application Thread si le compte est verrouillé
     */
    public void authenticate(String username, String password,
                             Consumer<AuthDAO.UserSession> onSuccess,
                             Consumer<String> onError,
                             Consumer<String> onBloque) {

        Task<AuthDAO.UserSession> task = new Task<>() {
            @Override
            protected AuthDAO.UserSession call() throws Exception {
                return AuthDAO.authenticate(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            AuthDAO.UserSession session = task.getValue();
            if (session != null) {
                onSuccess.accept(session);
            } else {
                onError.accept("Identifiants incorrects. Veuillez réessayer.");
            }
        });

        task.setOnFailed(e -> {
            Throwable cause = task.getException();
            if (cause instanceof CompteBloqueException blocked) {
                onBloque.accept(blocked.getMessage());
            } else if (cause instanceof SQLException) {
                onError.accept("Erreur de connexion à la base de données.");
            } else {
                onError.accept("Erreur inattendue : " + (cause != null ? cause.getMessage() : "inconnue"));
            }
        });

        new Thread(task).start();
    }

    /** Surcharge de compatibilité (sans callback onBloque — onError reçoit tout). */
    public void authenticate(String username, String password,
                             Consumer<AuthDAO.UserSession> onSuccess,
                             Consumer<String> onError) {
        authenticate(username, password, onSuccess, onError, onError);
    }
}
