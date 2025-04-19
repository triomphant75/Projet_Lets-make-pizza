package pizza.serveur.models;


/**
 * Énumération des différents états possibles d'une commande
 */
public enum StatutCommande {
    /**
     * La commande vient d'être reçue
     */
    RECUE,
    
    /**
     * La commande a été validée
     */
    VALIDEE,
    
    /**
     * Les pizzas sont en cours de préparation
     */
    EN_PREPARATION,
    
    /**
     * Les pizzas sont en train de cuire
     */
    EN_CUISSON,
    
    /**
     * La commande est en cours de livraison
     */
    EN_LIVRAISON,
    
    /**
     * La commande a été livrée
     */
    LIVREE,
    
    /**
     * La commande a été annulée
     */
    ANNULEE;

    /**
     * @return Le nom du statut en minuscules pour être utilisé dans les topics MQTT
     */
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}