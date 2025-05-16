package pizza.client.controlleur;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import pizza.client.models.ClientMqtt;
import pizza.client.models.PanierCommande;

/**
 * Contrôleur pour la vue de livraison
 */
public class LivraisonControleur {
    private static final Logger LOGGER = Logger.getLogger(LivraisonControleur.class.getName());
    
    @FXML
    private Label lblMessage;
    
    @FXML
    private Label lblDetails;
    
    @FXML
    private Button btnAccueil;
    
    @FXML
    private ImageView imgPizza;
    
    private ClientMqtt clientMqtt;
    private Stage stage;
    private PanierCommande panier;
    
    /**
     * Initialisation du contrôleur avec les dépendances nécessaires
     * Client MQTT pour la communication
     * Stage JavaFX principal
     * Panier de commande pour gérer les pizzas
     * 
     */
    public void initialiser(ClientMqtt clientMqtt, Stage stage, PanierCommande panier, 
                           String idCommande, String infoLivraison) {
        this.clientMqtt = clientMqtt;
        this.stage = stage;
        this.panier = panier;
        
        // Affiche les détails de la livraison
        afficherDetailsLivraison(idCommande, infoLivraison);
    }
    
    /**
     * Affiche les détails de la livraison
     */
    private void afficherDetailsLivraison(String idCommande, String infoLivraison) {
        // Extraire le nombre de pizzas livrées
        int nombreLivrees;
        try {
            nombreLivrees = Integer.parseInt(infoLivraison.trim());
        } catch (NumberFormatException e) {
            nombreLivrees = panier.getNombreTotalPizzas();
        }
        
        int nombreTotal = panier.getNombreTotalPizzas();
        
        lblMessage.setText("Commande livrée !");
        lblDetails.setText("Votre commande #" + idCommande.substring(0, 8) + 
                          " a été livrée.\n" + nombreLivrees + " pizza(s) sur " + 
                          nombreTotal + " ont été livrées.");
    }
    
    /**
     * Action déclenchée lorsque l'utilisateur clique sur le bouton Accueil
     */
    @FXML
    private void onAccueilClick(ActionEvent event) {
        try {
            // Vide le panier
            panier.vider();
            
            // Retourne à l'écran d'accueil
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/pizza/client/vue/accueil.fxml"));
            Parent root = loader.load();
            
            AccueilControleur controleur = loader.getController();
            controleur.initialiser(clientMqtt, stage);
            
            Scene scene = new Scene(root, 800, 600);
            URL cssUrl = getClass().getResource("/css/style.css");  // Chemin relatif au classpath
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                LOGGER.warning("Fichier CSS non trouvé !");
            }
                        
            stage.setScene(scene);
            
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du retour à l'écran d'accueil", e);
        }
    }
}
