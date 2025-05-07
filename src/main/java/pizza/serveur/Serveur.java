package pizza.serveur;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import pizza.serveur.models.Commande;
import pizza.serveur.models.DetailsPizza;
import pizza.serveur.models.StatutCommande;
import pizza.serveur.service.ConfigurationUtil;



/**
 * Classe principale du serveur de pizzeria gérant la communication MQTT
 * et l'orchestration des commandes
 */
public class Serveur {
    private static final Logger LOGGER = Logger.getLogger(Serveur.class.getName());
    
    private final MqttClient client;
    private final Pizzaiolo pizzaiolo;
    private final ThreadPoolExecutor executeur;
    private final Map<String, Commande> commandesActives;
    private final FormatMessage formatMessage;
    private final Properties configuration;
    
    public static void main(String[] args) {
        try {
            Serveur serveur = new Serveur();
            serveur.demarrer();
            
            // Attendre l'arrêt du programme
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    serveur.arreter();
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Erreur lors de l'arrêt du serveur", e);
                }
            }));
            
            // Garder le programme en vie
            Thread.currentThread().join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur fatale", e);
            System.exit(1);
        }
    }
    
    public Serveur() throws MqttException, IOException {
        this.configuration = ConfigurationUtil.chargerConfiguration();
        this.formatMessage = new FormatMessageImpl();
        this.pizzaiolo = new Pizzaiolo();
        this.commandesActives = new ConcurrentHashMap<>();
        
        // Configuration du pool de threads
        int nbCoeurs = Runtime.getRuntime().availableProcessors();
        this.executeur = new ThreadPoolExecutor(
                nbCoeurs, 
                nbCoeurs * 2,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, "Pizzeria-Worker");
                    t.setDaemon(true);
                    return t;
                }
        );
        
        // Initialisation du client MQTT
        String adresseBroker = configuration.getProperty("mqtt.broker.url", "tcp://localhost:1883");
        String clientId = "Pizzeria-" + UUID.randomUUID().toString();
        this.client = new MqttClient(adresseBroker, clientId, new MemoryPersistence());
        
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        
        client.connect(options);
        LOGGER.info("Connexion établie avec le broker MQTT: " + adresseBroker);
    }
    
    public void demarrer() throws MqttException {
        LOGGER.info("Démarrage du serveur pizzeria...");
        
        // Configuration des callbacks
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LOGGER.log(Level.WARNING, "Connexion au broker MQTT perdue", cause);
                // La reconnexion est automatique grâce à setAutomaticReconnect(true)
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String contenu = new String(message.getPayload(), StandardCharsets.UTF_8);
                LOGGER.fine("Message reçu sur " + topic + ": " + contenu);
                
                executeur.submit(() -> {
                    try {
                        traiterMessage(topic, contenu);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Erreur lors du traitement du message", e);
                    }
                });
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Pas d'action nécessaire à la livraison des messages
            }
        });
        
        // Abonnement aux topics
        client.subscribe("bcast/i_am_ungry", 1);
        client.subscribe("orders/+", 1);
        
        LOGGER.info("Serveur pizzeria démarré et prêt à recevoir des commandes");
    }
    
    public void arreter() throws MqttException {
        LOGGER.info("Arrêt du serveur pizzeria...");
        
        // Arrêt des tâches en cours
        executeur.shutdown();
        try {
            if (!executeur.awaitTermination(5, TimeUnit.SECONDS)) {
                executeur.shutdownNow();
            }
        } catch (InterruptedException e) {
            executeur.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Déconnexion du broker MQTT
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
        
        LOGGER.info("Serveur pizzeria arrêté");
    }
    
    private void traiterMessage(String topic, String contenu) throws MqttException {
        if (topic.equals("bcast/i_am_ungry")) {
            // Demande de menu
            envoyerMenu();
        } else if (topic.startsWith("orders/")) {
            // Nouvelle commande
            String idCommande = topic.substring("orders/".length());
            traiterCommande(idCommande, contenu);
        }
    }
    
    private void envoyerMenu() throws MqttException {
        List<DetailsPizza> menu = pizzaiolo.getListePizzas()
                .stream()
                .map(p -> new DetailsPizza(p.nom(), 
                        p.ingredients().stream()
                                .map(Enum::toString)
                                .collect(Collectors.toList()), 
                        p.prix()))
                .collect(Collectors.toList());
        
        String menuSerialise = formatMessage.serialiserMenu(menu);
        MqttMessage message = new MqttMessage(menuSerialise.getBytes(StandardCharsets.UTF_8));
        message.setQos(1);
        
        client.publish("bcast/menu", message);
        LOGGER.info("Menu envoyé");
    }
    
    private void traiterCommande(String idCommande, String contenuCommande) throws MqttException {
        // Déserialiser la commande
        Map<String, Integer> pizzasCommandees;
        try {
            pizzasCommandees = formatMessage.deserialiserCommande(contenuCommande);
        } catch (Exception e) {
            LOGGER.warning("Format de commande invalide: " + e.getMessage());
            annulerCommande(idCommande);
            return;
        }
        
        // Vérifier la validité de la commande
        if (!validerCommande(pizzasCommandees)) {
            LOGGER.warning("Commande invalide: " + pizzasCommandees);
            annulerCommande(idCommande);
            return;
        }
        
        // Créer et démarrer le traitement de la commande
        Commande nouvelleCommande = new Commande(idCommande, pizzasCommandees);
        commandesActives.put(idCommande, nouvelleCommande);
        
        // Commande validée, envoyer la notification
        envoyerStatut(idCommande, StatutCommande.VALIDEE);
        
        // Démarrer le traitement asynchrone
        executeur.submit(() -> traiterCommandeValidee(nouvelleCommande));
    }
    
    private boolean validerCommande(Map<String, Integer> commande) {
        // Vérifier que toutes les pizzas existent
        Set<String> pizzasDisponibles = pizzaiolo.getListePizzas().stream()
                .map(Pizzaiolo.DetailsPizza::nom)
                .collect(Collectors.toSet());
        
        boolean toutesLesPizzasExistent = commande.keySet().stream()
                .allMatch(pizzasDisponibles::contains);
        
        if (!toutesLesPizzasExistent) {
            return false;
        }
        
        // Vérifier les quantités
        boolean quantitesValides = commande.values().stream()
                .allMatch(quantite -> quantite >= 0 && quantite < 10);
        
        return quantitesValides;
    }
    
    private void annulerCommande(String idCommande) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        
        client.publish("orders/" + idCommande + "/cancelled", message);
        LOGGER.info("Commande " + idCommande + " annulée");
    }
    
    private void envoyerStatut(String idCommande, StatutCommande statut) throws MqttException {
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        
        client.publish("orders/" + idCommande + "/status/" + statut.toString().toLowerCase(), message);
        LOGGER.info("Statut de la commande " + idCommande + " mis à jour: " + statut);
    }
    
    private void traiterCommandeValidee(Commande commande) {
        try {
            LOGGER.info("Début du traitement de la commande " + commande.getId());
            
            // Étape 1: Préparation
            envoyerStatut(commande.getId(), StatutCommande.EN_PREPARATION);
            List<Pizzaiolo.Pizza> pizzasPreparees = preparerPizzas(commande);
            
            // Étape 2: Cuisson
            envoyerStatut(commande.getId(), StatutCommande.EN_CUISSON);
            List<Pizzaiolo.Pizza> pizzasCuites = cuirePizzas(pizzasPreparees);
            
            // Étape 3: Livraison
            envoyerStatut(commande.getId(), StatutCommande.EN_LIVRAISON);
            livrerPizzas(commande.getId(), pizzasCuites.size());
            
            // Nettoyage
            commandesActives.remove(commande.getId());
            LOGGER.info("Commande " + commande.getId() + " terminée et livrée");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du traitement de la commande " + commande.getId(), e);
            try {
                annulerCommande(commande.getId());
            } catch (MqttException ex) {
                LOGGER.log(Level.SEVERE, "Impossible d'annuler la commande " + commande.getId(), ex);
            }
        }
    }
    
    private List<Pizzaiolo.Pizza> preparerPizzas(Commande commande) {
        List<Pizzaiolo.Pizza> pizzasPreparees = new ArrayList<>();
        
        // Récupérer les détails des pizzas commandées
        Map<String, Integer> pizzasCommandees = commande.getPizzasCommandees();
        List<Pizzaiolo.DetailsPizza> menuComplet = pizzaiolo.getListePizzas();
        
        // Pour chaque type de pizza dans la commande
        for (Map.Entry<String, Integer> entry : pizzasCommandees.entrySet()) {
            String nomPizza = entry.getKey();
            int quantite = entry.getValue();
            
            // Trouver les détails de cette pizza
            Pizzaiolo.DetailsPizza detailsPizza = menuComplet.stream()
                    .filter(p -> p.nom().equals(nomPizza))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Pizza inconnue: " + nomPizza));
            
            // Préparer le nombre demandé de cette pizza
            for (int i = 0; i < quantite; i++) {
                try {
                    Pizzaiolo.Pizza pizzaPreparee = pizzaiolo.preparer(detailsPizza);
                    pizzasPreparees.add(pizzaPreparee);
                    LOGGER.fine("Pizza préparée: " + pizzaPreparee.nom());
                } catch (IllegalStateException e) {
                    // Le pizzaiolo est occupé, on attend avant de réessayer
                    LOGGER.fine("Attente pour préparer la pizza: " + e.getMessage());
                    try {
                        Thread.sleep(500);
                        i--; // On réessaie cette pizza
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Préparation interrompue", ex);
                    }
                }
            }
        }
        
        return pizzasPreparees;
    }
    
    private List<Pizzaiolo.Pizza> cuirePizzas(List<Pizzaiolo.Pizza> pizzasPreparees) {
        List<Pizzaiolo.Pizza> pizzasCuites = new ArrayList<>();
        if (pizzasPreparees.isEmpty()) {
            return pizzasCuites;
        }
    
        // Division en lots de 6 pizzas maximum
        int tailleLot = Pizzaiolo.MAX_PIZZAS_AU_FOUR;
        List<List<Pizzaiolo.Pizza>> lots = new ArrayList<>();
    
        for (int i = 0; i < pizzasPreparees.size(); i += tailleLot) {
            int fin = Math.min(i + tailleLot, pizzasPreparees.size());
            lots.add(new ArrayList<>(pizzasPreparees.subList(i, fin)));
        }
    
        LOGGER.info("Division en " + lots.size() + " lot(s) pour cuisson");
    
        // Cuire chaque lot
        for (List<Pizzaiolo.Pizza> lot : lots) {
            boolean lotCuit = false;
            
            while (!lotCuit && !Thread.currentThread().isInterrupted()) {
                try {
                    LOGGER.info("Tentative de cuisson pour lot de " + lot.size() + " pizza(s)");
                    List<Pizzaiolo.Pizza> resultatCuisson = pizzaiolo.cuire(lot);
                    pizzasCuites.addAll(resultatCuisson);
                    lotCuit = true;
                    LOGGER.info("Lot cuit avec succès : " + resultatCuisson.size() + " pizza(s)");
                } catch (IllegalStateException e) {
                    LOGGER.warning("Four occupé - Attente...");
                    try {
                        // Attente progressive avec vérification d'interruption
                        Thread.sleep(1000); // 1 seconde entre les tentatives
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        LOGGER.warning("Cuisson interrompue pendant l'attente");
                        break;
                    }
                }
            }
    
            if (!lotCuit) {
                throw new IllegalStateException("Cuisson annulée (interruption ou erreur)");
            }
        }
    
        // Validation finale
        if (pizzasCuites.size() != pizzasPreparees.size()) {
            throw new IllegalStateException("Incohérence détectée : " + 
                pizzasPreparees.size() + " pizzas préparées mais " + 
                pizzasCuites.size() + " cuites");
        }
    
        return pizzasCuites;
    }
    
    private void livrerPizzas(String idCommande, int nombrePizzas) throws MqttException {
        // Simulation de la livraison (temps variable)
        try {
            int tempsLivraison = 1500 + new Random().nextInt(500);
            Thread.sleep(tempsLivraison);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Livraison interrompue", e);
        }
        
        // Envoi de la notification de livraison
        String message = formatMessage.serialiserLivraison(nombrePizzas);
        MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(1);
        
        client.publish("orders/" + idCommande + "/delivery", mqttMessage);
        LOGGER.info("Livraison de " + nombrePizzas + " pizzas pour la commande " + idCommande);
    }
}