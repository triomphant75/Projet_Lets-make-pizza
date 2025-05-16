package pizza.client.models;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * Gère la communication MQTT pour le client
 */
public class ClientMqtt {
    private static final Logger LOGGER = Logger.getLogger(ClientMqtt.class.getName());
    
    private final MqttClient client;
    private final int qos;
    private final int timeout;
    private final Map<String, List<BiConsumer<String, String>>> listeners;
    private boolean estConnecte;
    
    /**
     * Constructeur pour initialiser le client MQTT
     */
    public ClientMqtt(Properties configuration) throws MqttException {
        // Configuration du client MQTT
        String adresseBroker = configuration.getProperty("mqtt.broker.url", "tcp://localhost:1883");
        String clientIdPrefix = configuration.getProperty("mqtt.client.id.prefix", "PizzaClient");
        String clientId = clientIdPrefix + "-" + UUID.randomUUID().toString();
        this.qos = Integer.parseInt(configuration.getProperty("mqtt.qos", "1"));
        this.timeout = Integer.parseInt(configuration.getProperty("client.timeout.ms", "10000"));
        
        this.listeners = new HashMap<>();
        this.estConnecte = false;
        
        // Création du client
        this.client = new MqttClient(adresseBroker, clientId, new MemoryPersistence());
        
        // Configuration de la connexion
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        
        // Définition des callbacks
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                LOGGER.log(Level.WARNING, "Connexion au broker MQTT perdue", cause);
                estConnecte = false;
                // La reconnexion est automatique grâce à setAutomaticReconnect(true)
            }
            
            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String contenu = new String(message.getPayload(), StandardCharsets.UTF_8);
                LOGGER.fine("Message reçu sur " + topic + ": " + contenu);
                
                // Notifie tous les listeners pour ce topic
                synchronized (listeners) {
                    // Vérifie les listeners exacts
                    if (listeners.containsKey(topic)) {
                        for (BiConsumer<String, String> listener : listeners.get(topic)) {
                            listener.accept(topic, contenu);
                        }
                    }
                    
                    // Vérifie les listeners avec wildcard
                    for (String pattern : listeners.keySet()) {
                        if (matchTopic(pattern, topic)) {
                            for (BiConsumer<String, String> listener : listeners.get(pattern)) {
                                listener.accept(topic, contenu);
                            }
                        }
                    }
                }
            }
            
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Pas d'action nécessaire à la livraison des messages
            }
        });
        
        // Connexion au broker
        try {
            client.connect(options);
            estConnecte = true;
            LOGGER.info("Connexion établie avec le broker MQTT: " + adresseBroker);
        } catch (MqttException e) {
            LOGGER.log(Level.SEVERE, "Impossible de se connecter au broker MQTT: " + adresseBroker, e);
            throw e;
        }
    }
    
    /**
     * Vérifie si un topic correspond à un pattern (gestion des wildcards +, #)

     */
    private boolean matchTopic(String pattern, String topic) {
        // Implémentation simplifiée de la correspondance de topic MQTT
        // + pour un niveau, # pour plusieurs niveaux
        
        if (pattern.equals(topic)) {
            return true;
        }
        
        String[] patternParts = pattern.split("/");
        String[] topicParts = topic.split("/");
        
        // Si le pattern a plus de parties que le topic, il ne peut pas correspondre
        // sauf si la dernière partie est #
        if (patternParts.length > topicParts.length && 
            !(patternParts.length == topicParts.length + 1 && patternParts[patternParts.length - 1].equals("#"))) {
            return false;
        }
        
        // on Vérifie chaque partie du topic et du pattern
        // en tenant compte des wildcards
        for (int i = 0; i < patternParts.length; i++) {
            // Le # correspond à zéro ou plusieurs niveaux
            if (patternParts[i].equals("#")) {
                return true;
            }
            
            // Le + correspond à exactement un niveau
            if (patternParts[i].equals("+")) {
                continue;
            }
            
            // Si nous sommes arrivés à la fin du topic mais pas du pattern
            if (i >= topicParts.length) {
                return false;
            }
            
            // Comparaison exacte
            if (!patternParts[i].equals(topicParts[i])) {
                return false;
            }
        }
        
        // Si le topic a plus de parties que le pattern, il ne correspond pas
        return patternParts.length == topicParts.length;
    }
    
    /**
     * S'abonne à un topic et notifie le listener quand un message arrive
     */
    public void souscrire(String topic, BiConsumer<String, String> listener) throws MqttException {
        // Ajoute le listener
        synchronized (listeners) {
            if (!listeners.containsKey(topic)) {
                listeners.put(topic, new ArrayList<>());
            }
            listeners.get(topic).add(listener);
        }
        
        // S'abonne au topic
        client.subscribe(topic, qos);
        LOGGER.info("Abonnement au topic: " + topic);
    }
    
    /**
     * S'abonne à un topic et notifie le listener quand un message arrive
     */
    public void souscrire(String topic, Consumer<String> listener) throws MqttException {
        souscrire(topic, (t, message) -> listener.accept(message));
    }
    
    /**
     * S'abonne à plusieurs topics et notifie le listener quand un message arrive
     */
    public void souscrire(List<String> topics, Consumer<String> listener) throws MqttException {
        for (String topic : topics) {
            souscrire(topic, listener);
        }
    }
    
    /**
     * Se désabonne d'un topic
     */
    public void desabonner(String topic) throws MqttException {
        // Se désabonner du topic
        client.unsubscribe(topic);
        
        // Supprimer les listeners
        synchronized (listeners) {
            listeners.remove(topic);
        }
        
        LOGGER.info("Désabonnement du topic: " + topic);
    }
    
    /**
     * Publie un message sur un topic
     */
    public void publier(String topic, String message) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
        mqttMessage.setQos(qos);
        
        client.publish(topic, mqttMessage);
        LOGGER.info("Message publié sur: " + topic);
    }
    
    /**
     * Publie un message vide sur un topic
     */
    public void publier(String topic) throws MqttException {
        MqttMessage mqttMessage = new MqttMessage();
        mqttMessage.setQos(qos);
        
        client.publish(topic, mqttMessage);
        LOGGER.info("Message vide publié sur: " + topic);
    }
    
    /**
     * Demande le menu et attend la réponse
     */
    public CompletableFuture<String> demanderMenu() {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        try {
            // Prépare un latch pour attendre une réponse
            final CountDownLatch latch = new CountDownLatch(1);
            final String[] menuRecu = new String[1];
            
            // S'abonne pour recevoir le menu
            souscrire("bcast/menu", (topic, message) -> {
                menuRecu[0] = message;
                latch.countDown();
            });
            
            // Demande le menu
            publier("bcast/i_am_ungry");
            
            // Attendre la réponse dans un autre thread
            new Thread(() -> {
                try {
                    if (latch.await(timeout, TimeUnit.MILLISECONDS)) {
                        future.complete(menuRecu[0]);
                    } else {
                        future.complete(null);
                    }
                } catch (InterruptedException e) {
                    future.completeExceptionally(e);
                } finally {
                    try {
                        // Se désabonner après avoir reçu la réponse
                        desabonner("bcast/menu");
                    } catch (MqttException e) {
                        LOGGER.log(Level.WARNING, "Erreur lors du désabonnement", e);
                    }
                }
            }).start();
            
        } catch (MqttException e) {
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Extrait le statut à partir du topic

     */
    private String extraireStatutDuTopic(String topic) {
        String[] segments = topic.split("/");
        // Format attendu: orders/[id]/status/[statut]
        if (segments.length >= 4) {
            return segments[3];
        }
        return "";
    }
    
    /**
     * Envoie une commande et s'abonne pour suivre son avancement
     */
    public void envoyerCommande(String idCommande, String commande, 
                              Consumer<String> onStatutChange, 
                              Runnable onCancel, 
                              Consumer<String> onDelivery) throws MqttException {
        
        // S'abonner pour suivre les statuts
        souscrire("orders/" + idCommande + "/status/#", (topic, message) -> {
            // Extraire le statut depuis le topic et/ou le contenu
            String statut = extraireStatutDuTopic(topic);
            if (statut.isEmpty() && !message.isEmpty()) {
                statut = message;
            }
            LOGGER.info("Statut reçu pour commande " + idCommande + ": " + statut);
            onStatutChange.accept(statut);
        });
        
        // S'abonner pour l'annulation
        souscrire("orders/" + idCommande + "/cancelled", (topic, message) -> {
            LOGGER.warning("Commande " + idCommande + " annulée");
            try {
                nettoyerAbonnements(idCommande);
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Erreur lors du nettoyage des abonnements", e);
            }
            onCancel.run();
        });
        
        // S'abonner pour la livraison
        souscrire("orders/" + idCommande + "/delivery", (topic, message) -> {
            LOGGER.info("Commande " + idCommande + " livrée");
            try {
                nettoyerAbonnements(idCommande);
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Erreur lors du nettoyage des abonnements", e);
            }
            onDelivery.accept(message);
        });
        
        // Envoyer la commande
        publier("orders/" + idCommande, commande);
        LOGGER.info("Commande " + idCommande + " envoyée");
    }
    
    /**
     * Nettoie les abonnements pour une commande
     */
    private void nettoyerAbonnements(String idCommande) throws MqttException {
        desabonner("orders/" + idCommande + "/status/#");
        desabonner("orders/" + idCommande + "/cancelled");
        desabonner("orders/" + idCommande + "/delivery");
    }
    
    /**
     * Vérifie si le client est connecté au broker
     * @return true si connecté
     */
    public boolean estConnecte() {
        return estConnecte && client.isConnected();
    }
    
    /**
     * Déconnecte le client MQTT
     */
    public void deconnecter() {
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                LOGGER.info("Client MQTT déconnecté");
            } catch (MqttException e) {
                LOGGER.log(Level.WARNING, "Erreur lors de la déconnexion MQTT", e);
            }
        }
    }
}