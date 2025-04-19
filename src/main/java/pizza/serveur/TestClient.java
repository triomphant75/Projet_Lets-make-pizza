package pizza.serveur;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestClient {
    public static void main(String[] args) {
        MqttClient client = null;
        try {
            // Configuration du client avec des timeouts plus longs
            String brokerUrl = "tcp://localhost:1883";
            String clientId = "TestClient-" + UUID.randomUUID();
            System.out.println("Tentative de connexion à " + brokerUrl + " avec l'ID client " + clientId);
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(30); // 30 secondes de timeout
            options.setKeepAliveInterval(60); // Ping toutes les 60 secondes
            options.setAutomaticReconnect(true); // Reconnexion automatique
            
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            // Configuration du callback avant la connexion
            final CountDownLatch connectLatch = new CountDownLatch(1);
            
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Connexion perdue: " + cause.getMessage());
                    cause.printStackTrace();
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    System.out.println("Message reçu sur " + topic + ": " + payload);
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("Message envoyé avec succès");
                }
            });
            
            // Connexion avec vérification
            System.out.println("Connexion au broker MQTT...");
            client.connect(options);
            
            if (!client.isConnected()) {
                System.out.println("Échec de connexion au broker MQTT");
                return;
            }
            
            System.out.println("Connecté avec succès au broker MQTT");
            
            // Test simple : S'abonner et publier sur un topic de test
            client.subscribe("test/topic", 1);
            System.out.println("Abonné au topic test/topic");
            
            MqttMessage testMessage = new MqttMessage("Test message".getBytes(StandardCharsets.UTF_8));
            testMessage.setQos(1);
            client.publish("test/topic", testMessage);
            System.out.println("Message publié sur test/topic");
            
            // Demander le menu
            System.out.println("Demande du menu...");
            client.subscribe("bcast/menu", 1);
            MqttMessage demande = new MqttMessage();
            demande.setQos(1);
            client.publish("bcast/i_am_ungry", demande);
            
            // Attendre un peu pour voir les réponses
            Thread.sleep(5000);
            
            // Déconnexion propre
            client.disconnect();
            System.out.println("Déconnexion réussie");
            
        } catch (MqttException e) {
            System.out.println("Erreur MQTT: " + e.getMessage() + " (code: " + e.getReasonCode() + ")");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Erreur générale: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Nettoyage
            if (client != null) {
                try {
                    if (client.isConnected()) {
                        client.disconnect();
                    }
                    client.close();
                    System.out.println("Client fermé proprement");
                } catch (MqttException e) {
                    System.out.println("Erreur lors de la fermeture du client: " + e.getMessage());
                }
            }
        }
    }
}