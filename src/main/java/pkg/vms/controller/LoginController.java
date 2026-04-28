package pkg.vms.controller;

import pkg.vms.DAO.AuthDAO;
import pkg.vms.DAO.AuthDAO.CompteBloqueException;

import javax.swing.SwingWorker;
import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginController {

    /**
     * Authentifie un utilisateur de façon asynchrone.
     *
     * @param onSuccess  appelé avec la session si connexion réussie
     * @param onError    appelé avec un message d'erreur (mauvais mdp, DB error)
     * @param onBloque   appelé avec le message de verrouillage si le compte est bloqué
     *                   (Fix B — séparé de onError pour affichage spécifique dans LoginForm)
     */
    public void authenticate(String username, String password,
                             Consumer<AuthDAO.UserSession> onSuccess,
                             Consumer<String> onError,
                             Consumer<String> onBloque) {
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
                        // Mauvais mot de passe — null sans exception
                        onError.accept("Identifiants incorrects. Veuillez réessayer.");
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (cause instanceof CompteBloqueException blocked) {
                        // Fix B — compte verrouillé : callback dédié
                        onBloque.accept(blocked.getMessage());
                    } else if (cause instanceof SQLException) {
                        onError.accept("Erreur de connexion à la base de données.");
                    } else {
                        onError.accept("Erreur inattendue : " + cause.getMessage());
                    }
                }
            }
        }.execute();
    }

    /** Surcharge de compatibilité (sans callback onBloque — onError reçoit tout). */
    public void authenticate(String username, String password,
                             Consumer<AuthDAO.UserSession> onSuccess,
                             Consumer<String> onError) {
        authenticate(username, password, onSuccess, onError, onError);
    }
}
