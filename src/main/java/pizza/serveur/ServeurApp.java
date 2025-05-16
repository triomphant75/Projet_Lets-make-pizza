package pizza.serveur;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Point d'entrée pour l'application serveur de pizzeria
 */
public class ServeurApp {
    private static final Logger LOGGER = Logger.getLogger(ServeurApp.class.getName());

    /**
     * Point d'entrée principal
     */
    public static void main(String[] args) {
        LOGGER.info("Démarrage de l'application serveur de pizzeria...");
        
        try {
            // Créer et démarrer le serveur
            Serveur serveur = new Serveur();
            serveur.demarrer();
            
            // Ajouter un hook d'arrêt pour terminer proprement le serveur
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    LOGGER.info("Arrêt de l'application serveur...");
                    serveur.arreter();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'arrêt du serveur", e);
                }
            }));
            
            LOGGER.info("Serveur démarré et en attente de commandes");
            
            // Garder l'application en vie jusqu'à ce qu'elle soit interrompue
            Thread.currentThread().join();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale lors du démarrage du serveur", e);
            System.exit(1);
        }
    }
}