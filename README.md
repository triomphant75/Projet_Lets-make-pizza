# Let's Make Pizza 🍕

Un système distribué de commande de pizzas utilisant une architecture événementielle avec le protocole MQTT.

## 📋 Table des matières

- [Description](#description)
- [Architecture](#architecture)
- [Prérequis](#prérequis)
- [Installation](#installation)
- [Configuration](#configuration)
- [Lancement](#lancement)
- [Utilisation](#utilisation)
- [Dépannage](#dépannage)
- [Développement](#développement)

## 📖 Description 

Le projet "Let's Make Pizza" est un système distribué permettant de :
- Commander des pizzas via une interface graphique JavaFX.
- Suivre la préparation, la cuisson et la livraison en temps réel.
- Gérer plusieurs commandes simultanément.
- Communiquer de manière asynchrone via MQTT.

## 🏗️ Architecture

Le système est composé de deux applications principales :
- **Serveur (Pizzeria)** : Gère la réception des commandes, la préparation des pizzas et leurs statuts.
- **Client (Interface utilisateur)** : Interface graphique pour consulter le menu, passer commande et suivre l'avancement.

La communication se fait via un broker MQTT qui assure un découplage fort entre les composants.

## ✅ Prérequis

### Logiciels requis
- Java Development Kit (JDK) 11 ou supérieur
- Maven 3.6+ pour la gestion des dépendances
- Broker MQTT (Mosquitto recommandé)

### Systèmes supportés
- Windows 10/11
- macOS 10.14+
- Linux (Ubuntu 18.04+, CentOS 7+)

## 🚀 Installation

1. **Installation du broker MQTT**
   - **Ubuntu/Debian**
     ```bash
     sudo apt update
     sudo apt install mosquitto mosquitto-clients
     ```
   - **macOS** (avec Homebrew)
     ```bash
     brew install mosquitto
     ```
   - **Windows** : Téléchargez et installez Mosquitto depuis le site officiel : https://mosquitto.org/download/.
   - **Docker (Alternative)**
     ```bash
     docker run -it -p 1883:1883 eclipse-mosquitto
     ```

2. **Cloner le projet**
   ```bash
   git clone https://github.com/triomphant75/Projet_Lets-make-pizza.git
   cd Projet_Lets-make-pizza

3. **Compilation**

La commande suivante :

```bash
mvn clean package
 ```
- **Télécharger les dépendances**
- **Compiler le code source**
- **Créer les fichiers JAR dans le dossier** target/

## ⚙️ Configuration
Le fichier de configuration config.properties se trouve dans src/main/resources/.

Exemple de configuration :

properties
**Configuration du broker MQTT**
```bash
mqtt.broker.url=tcp://localhost:1883
mqtt.client.id.prefix=PizzaApp
mqtt.qos=1
```

**Configuration du serveur**
```bash
server.thread.pool.size=8
server.timeout.ms=30000
```
**Configuration du client**

client.timeout.ms=10000

- **Paramètres configurables**
  - mqtt.broker.url	URL du broker MQTT	**tcp://localhost:1883**
  - mqtt.qos	**Qualité de service MQTT	1**
  - server.thread.pool.size	**Taille du pool de threads serveur	8**
  - client.timeout.ms	**Timeout client en millisecondes	10000**

## 🎮 Lancement
1. **Démarrer le broker MQTT**
```bash
# Service système (Ubuntu/Debian)
sudo systemctl start mosquitto
```
```bash
# Ou directement
mosquitto -v
```
```bash
# Avec Docker
docker run -it -p 1883:1883 eclipse-mosquitto

```
2. **Démarrer le serveur (Pizzeria)**
```bash
java -jar target/serveur.jar

```
Vous devriez voir :

[INFO] Connexion établie avec le broker MQTT: tcp://localhost:1883

[INFO] Serveur pizzeria démarré et prêt à recevoir des commandes


3. **Démarrer le client**
```bash

java -jar target/client.jar
```

L'interface graphique s'ouvrira automatiquement.

## 📱 Utilisation

Interface client

**Écran d'accueil** : Cliquez sur "Passer commande !" pour commencer

**Menu** :

Sélectionnez les pizzas souhaitées avec les spinners de quantité

Le prix total s'affiche automatiquement

Cliquez sur **"Passer commande"** pour valider

**Suivi** : Suivez l'avancement de votre commande en temps réel

✅ Validée

👨‍🍳 En préparation

🔥 En cuisson

🚚 En livraison

**Livraison** : Confirmation de la livraison avec possibilité de retourner à l'accueil

Statuts des commandes

Statut	Description

**VALIDEE**	Commande acceptée par la pizzeria

**EN_PREPARATION**	Les pizzas sont en cours de préparation

**EN_CUISSON**	Les pizzas sont au four

**EN_LIVRAISON**	Les pizzas sont en cours de livraison

**LIVREE**	Commande livrée avec succès

### 🔧 Dépannage

Problèmes courants

Le client ne peut pas se connecter au serveur

```bash
# Vérifiez que le broker MQTT fonctionne
mosquitto_sub -h localhost -t "test"
```
```bash
# Dans un autre terminal
mosquitto_pub -h localhost -t "test" -m "Hello"
```
Aucune réponse du serveur

Vérifiez que le serveur est démarré

Vérifiez les logs du serveur pour d'éventuelles erreurs

Vérifiez la configuration MQTT dans **config.properties**

Interface graphique ne s'affiche pas

```bash
# Vérifiez la version de Java
java -version
```
```bash
# Vérifiez que JavaFX est disponible
java --list-modules | grep javafx
```
**Logs**

Les logs sont affichés dans la console. Pour un débogage plus poussé, modifiez le niveau de log :

```bash
#java
Logger.getLogger("").setLevel(Level.FINE);
```

## 👨‍💻 Développement

**Structure du projet**

lets-make-pizza/

├── src/

│   ├── main/

│   │   ├── java/

│   │   │   ├── pizza.serveur/    # Code du serveur

│   │   │   └── pizza.client/     # Code du client

│   │   └── resources/

│   │       ├── vue/              # Fichiers FXML

│   │       └── config.properties

├── target/                       # Fichiers compilés

├── pom.xml                      # Configuration Maven

└── README.md

## 📋 Topics MQTT utilisés

| Topic                           | Direction       | Description                          |
|---------------------------------|:---------------:|--------------------------------------|
| `bcast/i_am_ungry`             | Client → Serveur | Demande la liste des pizzas disponibles |
| `bcast/menu`                   | Serveur → Client | Envoie la liste des pizzas disponibles |
| `orders/{id}`                  | Client → Serveur | Nouvelle commande de pizzas          |
| `orders/{id}/status/validee`   | Serveur → Client | Commande validée                     |
| `orders/{id}/status/en_preparation` | Serveur → Client | Préparation commencée               |
| `orders/{id}/status/en_cuisson`| Serveur → Client | Cuisson commencée                    |
| `orders/{id}/status/en_livraison` | Serveur → Client | Livraison commencée                |
| `orders/{id}/delivery`         | Serveur → Client | Livraison terminée                   |
| `orders/{id}/cancelled`        | Serveur → Client | Commande annulée                     |

## 🤝 Contributeurs
**NZIKOU Triomphant** - Développement serveur

**ZOGBEMA Yahsé** - Interface d'accueil

**SOULAIMANA Warda** - Interfaces de suivi et livraison

**KONE Moustapha** - Interface de menu

## 📄 Licence
Ce projet a été développé dans le cadre du Master 1 MIAGE à l'Institut des Sciences du Digital Management et Cognition.





