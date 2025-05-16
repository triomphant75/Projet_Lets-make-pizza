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
     */
    String serialiserMenu(List<DetailsPizza> menu);
    
    /**
     * Désérialise une commande
     */
    Map<String, Integer> deserialiserCommande(String message);
    
    /**
     * Sérialise les informations de livraison
     */
    String serialiserLivraison(int nombrePizzas);
}