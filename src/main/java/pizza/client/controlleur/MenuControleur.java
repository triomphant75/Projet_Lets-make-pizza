package pizza.client.controlleur;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.MqttException;

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
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import pizza.client.models.ClientMqtt;
import pizza.client.models.PanierCommande;

/**
 * Contrôleur pour la vue du menu
 */
public class MenuControleur {
    private static final Logger LOGGER = Logger.getLogger(MenuControleur.class.getName());
    
    @FXML
    private GridPane gridPizzas;
    
    @FXML
    private Button btnCommander;
    
    @FXML
    private Button btnRetour;
    
    @FXML
    private Label lblTotal;
    
    private ClientMqtt clientMqtt;
    private Stage stage;
    private PanierCommande panier;
    private Map<String, Map<String, Object>> menu;
    
    /**
     * Initialisation du contrôleur avec les dépendances nécessaires
     * Client MQTT pour la communication
     * Stage JavaFX principal
     * Panier de commande
     * Menu sérialisé reçu du serveur
     */
    public void initialiser(ClientMqtt clientMqtt, Stage stage, PanierCommande panier, String menuSerialise) {
        this.clientMqtt = clientMqtt;
        this.stage = stage;
        this.panier = panier;
        
        // Désérialiser le menu
        this.menu = PanierCommande.deserialiserMenu(menuSerialise);
        
        // Initialiser l'interface
        afficherPizzas();
        mettreAJourTotal();
        
        // Active/désactive le bouton de commande
        btnCommander.setDisable(true);
    }
    
    /**
     * Affiche les pizzas dans la grille
     */
    private void afficherPizzas() {
        int row = 0;
        
        // Vide la grille
        gridPizzas.getChildren().clear();
        
        // Ajoute les en-têtes
        gridPizzas.add(new Label("Pizza"), 0, 0);
        gridPizzas.add(new Label("Ingrédients"), 1, 0);
        gridPizzas.add(new Label("Prix"), 2, 0);
        gridPizzas.add(new Label("Quantité"), 3, 0);
        
        // Ajout chaque pizza
        for (String nomPizza : menu.keySet()) {
            row++;
            Map<String, Object> detailsPizza = menu.get(nomPizza);
            
            // Nom de la pizza
            Label lblNom = new Label(nomPizza);
            gridPizzas.add(lblNom, 0, row);
            
            // Ingrédients
            String[] ingredients = (String[]) detailsPizza.get("ingredients");
            String strIngredients = String.join(", ", ingredients);
            Label lblIngredients = new Label(strIngredients);
            lblIngredients.setWrapText(true);
            gridPizzas.add(lblIngredients, 1, row);
            
            // Prix
            int prix = (int) detailsPizza.get("prix");
            Label lblPrix = new Label(prix + " €");
            gridPizzas.add(lblPrix, 2, row);
            
            // Quantité
            Spinner<Integer> spinner = new Spinner<>();
            SpinnerValueFactory<Integer> valueFactory = 
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0);
            spinner.setValueFactory(valueFactory);
            spinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                panier.ajouterPizza(nomPizza, newValue);
                mettreAJourTotal();
            });
            gridPizzas.add(spinner, 3, row);
        }
    }
    
    /**
     * Met à jour l'affichage du total
     */
    private void mettreAJourTotal() {
        int nombrePizzas = panier.getNombreTotalPizzas();
        btnCommander.setDisable(nombrePizzas == 0);
        
        // Calculer le prix total
        int prixTotal = 0;
        Map<String, Integer> selections = panier.getPizzasSelectionnees();
        
        for (String nomPizza : selections.keySet()) {
            int quantite = selections.get(nomPizza);
            if (menu.containsKey(nomPizza)) {
                int prixUnitaire = (int) menu.get(nomPizza).get("prix");
                prixTotal += prixUnitaire * quantite;
            }
        }
        
        lblTotal.setText("Total: " + nombrePizzas + " pizza(s), " + prixTotal + " €");
    }
    
    /*
     * Action déclenchée lorsque l'utilisateur clique sur le bouton Commander
     */

    @FXML
    private void onCommanderClick(ActionEvent event) {
        if (panier.estVide()) {
            afficherAlerte("Panier vide", "Aucune pizza sélectionnée", 
                        "Veuillez sélectionner au moins une pizza.", AlertType.WARNING);
            return;
        }
        
        // Vérification de la pizza hawaïenne
        if (panier.contientPizza("hawaiana")) {
            afficherAlerte("Commande impossible", "Pizza non autorisée", 
                        "Désolé, notre pizzeria ne fait pas de pizzas à l'ananas !\n"
                        + "\"de l'ananas, sérieux ?\"", AlertType.ERROR);
            btnCommander.setDisable(false);
            return;
        }
        // Désactive le bouton pendant l'envoi
        btnCommander.setDisable(true);
        
        try {
            // Sérialise et envoie la commande
            String commandeSerialisee = panier.serialiser();
            String idCommande = panier.getId();
            
            clientMqtt.envoyerCommande(
                idCommande,
                commandeSerialisee,
                
                // Callback pour les changements de statut
                (statut) -> {
                    Platform.runLater(() -> {
                        try {
                            ouvrirEcranSuivi(idCommande, statut);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de l'écran de suivi", e);
                        }
                    });
                },
                
                // Callback pour l'annulation
                () -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("Commande annulée");
                        alert.setHeaderText("La commande a été annulée");
                        alert.setContentText("Votre commande n'a pas pu être validée par la pizzeria.");
                        alert.showAndWait();
                        btnCommander.setDisable(false);
                    });
                },
                
                // Callback pour la livraison
                (infoLivraison) -> {
                    Platform.runLater(() -> {
                        try {
                            ouvrirEcranLivraison(idCommande, infoLivraison);
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de l'écran de livraison", e);
                        }
                    });
                }
            );
            
        } catch (MqttException e) {
            afficherAlerte("Erreur de communication", "Impossible d'envoyer la commande", 
                        "Une erreur s'est produite lors de la communication avec le serveur.", 
                        AlertType.ERROR);
            btnCommander.setDisable(false);
        }
    }

    // Méthode helper pour simplifier les alertes
    private void afficherAlerte(String titre, String header, String content, AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(titre);
            alert.setHeaderText(header);
            alert.setContentText(content);
            alert.showAndWait();
        });
    }


    
    /**
     * Action déclenchée lorsque l'utilisateur clique sur le bouton Retour
     */
    @FXML
    private void onRetourClick(ActionEvent event) {
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
    
    /*
     * Ouvre l'écran de suivi de la commande
     */
    private void ouvrirEcranSuivi(String idCommande, String statutInitial) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pizza/client/vue/suivi.fxml"));
        Parent root = loader.load();
        
        SuiviControleur controleur = loader.getController();
        controleur.initialiser(clientMqtt, stage, panier, idCommande, statutInitial);
        
        Scene scene = new Scene(root, 800, 600);
        URL cssUrl = getClass().getResource("/css/style.css");  // Chemin relatif au classpath
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            LOGGER.warning("Fichier CSS non trouvé !");
        }
                stage.setScene(scene);
    }
    
    /*
     * Ouvre l'écran de livraison
     */
    private void ouvrirEcranLivraison(String idCommande, String infoLivraison) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/pizza/client/vue/livraison.fxml"));
        Parent root = loader.load();
        
        LivraisonControleur controleur = loader.getController();
        controleur.initialiser(clientMqtt, stage, panier, idCommande, infoLivraison);
        
        Scene scene = new Scene(root, 800, 600);
        URL cssUrl = getClass().getResource("/css/style.css");  // Chemin relatif au classpath
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            LOGGER.warning("Fichier CSS non trouvé !");
        }
        stage.setScene(scene);
    }
}