package pizza.serveur;

import java.util.List;
import java.util.Map;

import pizza.serveur.models.DetailsPizza;

/**
 * Interface définissant les fonctions de sérialisation/désérialisation des messages
 */
public interface FormatMessage {
    /**
     * Sérialise un menu de pizzas
     * @param menu Liste des pizzas disponibles
     * @return Chaîne sérialisée
     */
    String serialiserMenu(List<DetailsPizza> menu);
    
    /**
     * Désérialise une commande
     * @param message Message reçu du client
     * @return Map avec le nom des pizzas et leur quantité
     */
    Map<String, Integer> deserialiserCommande(String message);
    
    /**
     * Sérialise les informations de livraison
     * @param nombrePizzas Nombre de pizzas livrées
     * @return Chaîne sérialisée
     */
    String serialiserLivraison(int nombrePizzas);
}