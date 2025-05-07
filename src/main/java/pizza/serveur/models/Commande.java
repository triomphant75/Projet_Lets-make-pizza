package pizza.serveur.models;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Classe représentant une commande en cours de traitement
 */
public class Commande {
    private final String id;
    private final Map<String, Integer> pizzasCommandees;
    private final LocalDateTime dateCreation;
    private StatutCommande statut;

    /**
     * Constructeur
     * 
     */
    public Commande(String id, Map<String, Integer> pizzasCommandees) {
        this.id = id;
        this.pizzasCommandees = pizzasCommandees;
        this.dateCreation = LocalDateTime.now();
        this.statut = StatutCommande.RECUE;
    }

    /**
     * return Identifiant de la commande
     */
    public String getId() {
        return id;
    }

    /**
     * return Map des pizzas commandées et leurs quantités
     */
    public Map<String, Integer> getPizzasCommandees() {
        return pizzasCommandees;
    }

    /**
     * return Date de création de la commande
     */
    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    /**
     * return Statut actuel de la commande
     */
    public StatutCommande getStatut() {
        return statut;
    }

    /**
     * Met à jour le statut de la commande
     * param statut Nouveau statut
     */
    public void setStatut(StatutCommande statut) {
        this.statut = statut;
    }

    /**
     * Calcule le nombre total de pizzas dans la commande
     * return Nombre total de pizzas
     */
    public int getNombreTotalPizzas() {
        return pizzasCommandees.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commande commande = (Commande) o;
        return Objects.equals(id, commande.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Commande{" +
                "id='" + id + '\'' +
                ", pizzasCommandees=" + pizzasCommandees +
                ", dateCreation=" + dateCreation +
                ", statut=" + statut +
                '}';
    }
}