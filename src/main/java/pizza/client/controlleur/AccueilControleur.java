package pizza.client.controlleur;

import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import pizza.client.models.ClientMqtt;
import pizza.client.models.PanierCommande;

/**
 * Contrôleur pour la vue d'accueil
 */
public class AccueilControleur {
    private static final Logger LOGGER = Logger.getLogger(AccueilControleur.class.getName());
    
    @FXML
    private Button btnCommander;
    
    @FXML
    private Label lblStatus;
    
    private ClientMqtt clientMqtt;
    private Stage stage;
    private PanierCommande panier;
    
    /**
     * Initialisation du contrôleur avec les dépendances nécessaires
     * Client MQTT pour la communication
     * Stage JavaFX principal
     */
    public void initialiser(ClientMqtt clientMqtt, Stage stage) {
        this.clientMqtt = clientMqtt;
        this.stage = stage;
        this.panier = new PanierCommande();
        
        // Vérification de la connexion MQTT
        verifierConnexion();
    }
    
    /**
     * Vérifie la connexion au broker MQTT
     */
    private void verifierConnexion() {
        if (clientMqtt.estConnecte()) {
            lblStatus.setText("Connecté au serveur");
            btnCommander.setDisable(false);
        } else {
            lblStatus.setText("Non connecté au serveur");
            btnCommander.setDisable(true);
            
            // Affiche une alerte
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Erreur de connexion");
            alert.setHeaderText("Impossible de se connecter au serveur");
            alert.setContentText("Vérifiez que le serveur MQTT est en cours d'exécution.");
            alert.showAndWait();
        }
    }
    
    /**
     * Action déclenchée lorsque l'utilisateur clique sur le bouton Commander
     */
    @FXML
    private void onCommanderClick(ActionEvent event) {
        // Désactive le bouton pendant le chargement
        btnCommander.setDisable(true);
        lblStatus.setText("Récupération du menu...");
        
        // Récupére le menu des pizzas
        clientMqtt.demanderMenu()
            .thenAccept(menu -> {
                if (menu == null) {
                    // Aucune réponse reçue
                    Platform.runLater(() -> {
                        lblStatus.setText("Aucune réponse du serveur");
                        btnCommander.setDisable(false);
                        
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Erreur de communication");
                        alert.setHeaderText("Aucune réponse du serveur");
                        alert.setContentText("Vérifiez que le serveur de pizzeria est en cours d'exécution.");
                        alert.showAndWait();
                    });
                    return;
                }
                
                // Menu reçu, charger l'écran de sélection des pizzas
                Platform.runLater(() -> {
                    try {
                        ouvrirEcranMenu(menu);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Erreur lors du chargement de l'écran de menu", e);
                        btnCommander.setDisable(false);
                        lblStatus.setText("Erreur lors du chargement du menu");
                    }
                });
            })
            .exceptionally(ex -> {
                // Erreur lors de la récupération du menu
                Platform.runLater(() -> {
                    LOGGER.log(Level.SEVERE, "Erreur lors de la récupération du menu", ex);
                    lblStatus.setText("Erreur de communication avec le serveur");
                    btnCommander.setDisable(false);
                    
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("Erreur de communication");
                    alert.setHeaderText("Impossible de récupérer le menu");
                    alert.setContentText("Une erreur s'est produite lors de la communication avec le serveur.");
                    alert.showAndWait();
                });
                return null;
            });
    }
    
    /**
     * Ouvre l'écran de sélection des pizzas
     */
    private void ouvrirEcranMenu(String menuSerialise) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pizza/client/vue/menu.fxml"));
        Parent root = loader.load();
        
        // Récupérer et initialiser le contrôleur
        MenuControleur controleur = loader.getController();
        controleur.initialiser(clientMqtt, stage, panier, menuSerialise);
        
        // Changer de scène
        Scene scene = new Scene(root, 800, 600);
        // Chargement sécurisé du CSS
        URL cssUrl = getClass().getClassLoader().getResource("css/style.css"); 
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            LOGGER.warning("CSS non trouvé !");
        }
        
            stage.setScene(scene);
            }
}

