package pizza.serveur;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pizza.serveur.models.DetailsPizza;

/**
 * Implémentation du format de sérialisation/désérialisation
 * Format simple: CSV pour cet exemple
 */
public class FormatMessageImpl implements FormatMessage {
    
    @Override
    public String serialiserMenu(List<DetailsPizza> menu) {
        StringBuilder sb = new StringBuilder();
        
        // Format: nom;ingredient1,ingredient2,...;prix|nom2;...
        menu.forEach(pizza -> {
            sb.append(pizza.getNom()).append(";");
            sb.append(String.join(",", pizza.getIngredients())).append(";");
            sb.append(pizza.getPrix()).append("|");
        });
        
        // Supprimer le dernier séparateur
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    @Override
    public Map<String, Integer> deserialiserCommande(String message) {
        Map<String, Integer> commande = new HashMap<>();
        
        // Format: nom:quantité|nom2:quantité2|...
        String[] items = message.split("\\|");
        for (String item : items) {
            String[] parts = item.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Format de commande invalide: " + item);
            }
            
            String nomPizza = parts[0].trim();
            int quantite;
            try {
                quantite = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Quantité invalide: " + parts[1]);
            }
            
            commande.put(nomPizza, quantite);
        }
        
        return commande;
    }
    
    @Override
    public String serialiserLivraison(int nombrePizzas) {
        return String.valueOf(nombrePizzas);
    }
}
