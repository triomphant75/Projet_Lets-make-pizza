package pizza.client;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import pizza.client.controlleur.AccueilControleur;
import pizza.client.models.ClientMqtt;


/**
 Application principale du client pizza
 */
public class ClientApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ClientApp.class.getName());
    private static final String FICHIER_CONFIGURATION = "config.properties";
    private Properties configuration;
    private ClientMqtt clientMqtt;
    
    /**
      Point d'entrée de l'application
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void init() throws Exception {
        // Chargement de la configuration
        this.configuration = chargerConfiguration();
        
        // Initialisation du client MQTT
        this.clientMqtt = new ClientMqtt(configuration);
        
        LOGGER.info("Application initialisée avec succès");
    }
    
    @Override
public void start(Stage primaryStage) throws Exception {
    try {
        // Debug: Vérification des chemins de ressources
        URL fxmlUrl = getClass().getResource("/pizza/client/vue/accueil.fxml");
        URL cssUrl = getClass().getResource("/css/style.css");
        URL iconUrl = getClass().getResource("/images/pizza.png"); 
        
        LOGGER.info("Tentative de chargement FXML depuis: " + (fxmlUrl != null ? fxmlUrl.toString() : "NULL"));
        LOGGER.info("Tentative de chargement CSS depuis: " + (cssUrl != null ? cssUrl.toString() : "NULL"));

        if (iconUrl != null) {
                primaryStage.getIcons().add(new Image(iconUrl.toString()));
                LOGGER.info("Icône chargée avec succès");
            } 

        // 1. Chargement FXML avec vérification
        if (fxmlUrl == null) {
            throw new FileNotFoundException("FXML introuvable à /pizza/client/vue/accueil.fxml");
        }
        
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        Parent root = loader.load();
        
        // 2. Injection des dépendances avec vérification
        AccueilControleur controleur = loader.getController();
        if (controleur == null) {
            throw new IllegalStateException("Controller non initialisé dans le FXML");
        }
        controleur.initialiser(clientMqtt, primaryStage);
        
        // 3. Configuration de la scène
        Scene scene = new Scene(root, 800, 600);
        
        // 4. Chargement CSS avec fallback
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
            LOGGER.info("CSS chargé avec succès");
        } else {
            LOGGER.warning("CSS non trouvé à /css/style.css - L'application continuera sans style");
        }
        
        // 5. Configuration de la fenêtre
        primaryStage.setTitle("Pizz'App");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
        
        LOGGER.info("Interface utilisateur démarrée avec succès");
        
    } catch (Exception e) {
        LOGGER.severe("Échec du démarrage: " + e.getMessage());
        showErrorAlert("Erreur Critique", "Impossible de démarrer l'application", e.toString());
        Platform.exit();
    }
}

// Méthode utilitaire pour afficher les erreurs
private void showErrorAlert(String title, String header, String content) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle(title);
    alert.setHeaderText(header);
    alert.setContentText(content);
    alert.showAndWait();
}
    
    @Override
    public void stop() throws Exception {
        // Arrêt propre du client MQTT
        if (clientMqtt != null) {
            clientMqtt.deconnecter();
        }
        
        LOGGER.info("Application arrêtée");
    }
    
    /**
     * Charge les propriétés de configuration
       Puis retour les Propriétés chargées
     */
    private Properties chargerConfiguration() throws IOException {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(FICHIER_CONFIGURATION)) {
            if (input != null) {
                props.load(input);
                LOGGER.info("Configuration chargée depuis " + FICHIER_CONFIGURATION);
            } else {
                LOGGER.warning("Fichier de configuration introuvable, utilisation des valeurs par défaut");
                props = chargerConfigurationParDefaut();
            }
        }
        return props;
    }
    
    /**
     * Crée une configuration par défaut
     Puis retourne les Propriétés par défaut
     */
    private Properties chargerConfigurationParDefaut() {
        Properties props = new Properties();
        
        // Configuration MQTT
        props.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        props.setProperty("mqtt.client.id.prefix", "PizzaClient");
        props.setProperty("mqtt.qos", "1");
        
        // Configuration du client
        props.setProperty("client.timeout.ms", "10000");
        
        LOGGER.info("Configuration par défaut chargée");
        return props;
    }
}