package pizza.client.controlleur;

import java.util.logging.Logger;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import pizza.client.models.ClientMqtt;
import pizza.client.models.PanierCommande;

/**
 * Contrôleur pour la vue de suivi de commande
 */
public class SuiviControleur {
    private static final Logger LOGGER = Logger.getLogger(SuiviControleur.class.getName());
    
    @FXML
    private Label lblStatut;
    
    @FXML
    private Label lblCommande;
    
    @FXML
    private ProgressBar progressBar;
    
    @FXML
    private ImageView imgPizza;
    
    private ClientMqtt clientMqtt;
    private Stage stage;
    private PanierCommande panier;
    private String idCommande;
    
    /**
     * Initialisation du contrôleur avec les dépendances nécessaires
        * Client MQTT pour la communication
        * Stage JavaFX principal
        * Panier de commande pour gérer les pizzas
        * ID de la commande pour le suivi
     */
    public void initialiser(ClientMqtt clientMqtt, Stage stage, PanierCommande panier, 
                           String idCommande, String statutInitial) {
        this.clientMqtt = clientMqtt;
        this.stage = stage;
        this.panier = panier;
        this.idCommande = idCommande;
        
        // Affiche les détails de la commande
        afficherDetailsCommande();
        
        // Mets à jour le statut initial s'il est fourni
        if (statutInitial != null && !statutInitial.isEmpty()) {
            mettreAJourStatut(statutInitial);
        } else {
            // Par défaut, on décide de commencer avec "validee"
            mettreAJourStatut("validee");
        }
    }
    
    /**
     * Affiche les détails de la commande (pizzas commandées)
     */
    private void afficherDetailsCommande() {
        StringBuilder sb = new StringBuilder();
        sb.append("Commande #").append(idCommande.substring(0, 8)).append("\n");
        
        panier.getPizzasSelectionnees().forEach((pizza, quantite) -> {
            sb.append(pizza).append(" x").append(quantite).append("\n");
        });
        
        lblCommande.setText(sb.toString());
    }
    
    /**
     * Met à jour l'affichage du statut de la commande
     */
    public void mettreAJourStatut(String statut) {
        LOGGER.info("Mise à jour du statut: " + statut);
        
        // Si le statut est vide, ne rien faire
        if (statut == null || statut.isEmpty()) {
            LOGGER.warning("Tentative de mise à jour avec un statut vide");
            return;
        }
        
        Platform.runLater(() -> {
            String etat = statut.toLowerCase().trim();
            String message = "";
            double progression = 0.0;
            
            switch (etat) {
                case "validee":
                    message = "✅ Commande validée";
                    progression = 0.2;
                    break;
                case "en_preparation":
                    message = "👨‍🍳 En préparation (" + panier.getNombreTotalPizzas() + " pizzas)";
                    progression = 0.4;
                    break;
                case "en_cuisson":
                    message = "🔥 En cuisson (" + panier.getNombreTotalPizzas() + " pizzas au four)";
                    progression = 0.7;
                    break;
                case "en_livraison":
                    message = "🚚 En livraison (arrive bientôt!)";
                    progression = 0.9;
                    break;
                default:
                    message = "❓ Statut: " + etat;
                    LOGGER.warning("Statut non reconnu: " + etat);
                    progression = 0.1; // Valeur par défaut
            }
            
            lblStatut.setText(message);
            
            // Animation fluide de la barre de progression
            Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(300), 
                new KeyValue(progressBar.progressProperty(), progression))
            );
            timeline.play();
        });
    }
}