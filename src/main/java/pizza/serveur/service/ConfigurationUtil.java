package pizza.serveur.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilitaire pour gérer la configuration du serveur
 */
public class ConfigurationUtil {
    private static final Logger LOGGER = Logger.getLogger(ConfigurationUtil.class.getName());
    private static final String FICHIER_CONFIGURATION = "config.properties";

    /**
     * Charge les propriétés de configuration depuis le fichier
     * @return Propriétés chargées ou propriétés par défaut si le fichier est absent
     * @throws IOException En cas d'erreur lors de la lecture du fichier
     */
    public static Properties chargerConfiguration() throws IOException {
        Properties props = new Properties();
        try (InputStream input = ConfigurationUtil.class.getClassLoader().getResourceAsStream(FICHIER_CONFIGURATION)) {
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
     * @return Propriétés par défaut
     */
    private static Properties chargerConfigurationParDefaut() {
        Properties props = new Properties();
        
        // Configuration MQTT
        props.setProperty("mqtt.broker.url", "tcp://localhost:1883");
        props.setProperty("mqtt.client.id.prefix", "PizzaApp");
        props.setProperty("mqtt.qos", "1");
        
        // Configuration du serveur
        props.setProperty("serveur.thread.pool.size", 
                String.valueOf(Runtime.getRuntime().availableProcessors()));
        props.setProperty("serveur.timeout.ms", "30000");
        
        LOGGER.info("Configuration par défaut chargée");
        return props;
    }

    /**
     * Récupère une valeur numérique depuis les propriétés
     * @param props Propriétés sources
     * @param cle Clé de la propriété
     * @param defaut Valeur par défaut
     * @return Valeur entière ou valeur par défaut en cas d'erreur
     */
    public static int getEntier(Properties props, String cle, int defaut) {
        try {
            String valeur = props.getProperty(cle);
            if (valeur != null && !valeur.isEmpty()) {
                return Integer.parseInt(valeur);
            }
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Format invalide pour la propriété " + cle, e);
        }
        return defaut;
    }

    /**
     * Récupère une valeur booléenne depuis les propriétés
     * @param props Propriétés sources
     * @param cle Clé de la propriété
     * @param defaut Valeur par défaut
     * @return Valeur booléenne ou valeur par défaut en cas d'absence
     */
    public static boolean getBoolean(Properties props, String cle, boolean defaut) {
        String valeur = props.getProperty(cle);
        if (valeur != null && !valeur.isEmpty()) {
            return Boolean.parseBoolean(valeur);
        }
        return defaut;
    }
}