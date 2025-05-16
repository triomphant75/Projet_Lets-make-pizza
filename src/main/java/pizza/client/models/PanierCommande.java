package pizza.client.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Représente le panier de commande de l'utilisateur
 */
public class PanierCommande {
    private final String id;
    private final Map<String, Integer> pizzasSelectionnees;
    
    /**
     * Constructeur par défaut, génère un ID unique
     */
    public PanierCommande() {
        this.id = UUID.randomUUID().toString();
        this.pizzasSelectionnees = new HashMap<>();
    }
    
    /**
     * Ajoute une pizza au panier
     */
    public void ajouterPizza(String nomPizza, int quantite) {
        if (quantite > 0) {
            pizzasSelectionnees.put(nomPizza, quantite);
        } else {
            pizzasSelectionnees.remove(nomPizza);
        }
    }
    
    /**
     * Retire une pizza du panier
     */
    public void retirerPizza(String nomPizza) {
        pizzasSelectionnees.remove(nomPizza);
    }
    
    /**
     * Vérifie si le panier est vide
     */
    public boolean estVide() {
        return pizzasSelectionnees.isEmpty() || getNombreTotalPizzas() == 0;
    }
    
    /**
     * Vérifie si une pizza est déjà dans le panier
     */
    public boolean contientPizza(String nomPizza) {
        return pizzasSelectionnees.keySet().stream()
                .anyMatch(nom -> nom.equalsIgnoreCase(nomPizza));
    }
    /**
     * Calcule le nombre total de pizzas dans le panier
     */
    public int getNombreTotalPizzas() {
        return pizzasSelectionnees.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }
    
    /**
     * Vide le panier
     */
    public void vider() {
        pizzasSelectionnees.clear();
    }
    
    /**
     Map des pizzas sélectionnées et leurs quantités
     */
    public Map<String, Integer> getPizzasSelectionnees() {
        return new HashMap<>(pizzasSelectionnees);
    }
    
    /**
     returne Identifiant unique de la commande
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sérialise le panier au format attendu par le serveur
     * returne Chaîne sérialisée (format: nom:quantité|nom2:quantité2|...)
     */
    public String serialiser() {
        StringBuilder sb = new StringBuilder();
        
        pizzasSelectionnees.forEach((nom, quantite) -> {
            if (quantite > 0) {
                sb.append(nom).append(":").append(quantite).append("|");
            }
        });
        
        // Supprimer le dernier séparateur
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    /**
     * Désérialise une liste de pizzas au format du serveur
     */
    public static Map<String, Map<String, Object>> deserialiserMenu(String menuSerialise) {
        Map<String, Map<String, Object>> menu = new HashMap<>();
        
        String[] pizzas = menuSerialise.split("\\|");
        for (String pizza : pizzas) {
            String[] details = pizza.split(";");
            if (details.length == 3) {
                String nom = details[0];
                String[] ingredients = details[1].split(",");
                int prix;
                
                try {
                    prix = Integer.parseInt(details[2]);
                } catch (NumberFormatException e) {
                    prix = 0;
                }
                
                Map<String, Object> detailsPizza = new HashMap<>();
                detailsPizza.put("nom", nom);
                detailsPizza.put("ingredients", ingredients);
                detailsPizza.put("prix", prix);
                
                menu.put(nom, detailsPizza);
            }
        }
        
        return menu;
    }
}