package pizza.serveur.models;

import java.util.List;
import java.util.Objects;

/**
 * Classe représentant les détails d'une pizza disponible au menu
 */
public class DetailsPizza {
    private final String nom;
    private final List<String> ingredients;
    private final int prix;

    /**
     * Constructeur
     * @param nom Nom de la pizza
     * @param ingredients Liste des ingrédients
     * @param prix Prix de la pizza
     */
    public DetailsPizza(String nom, List<String> ingredients, int prix) {
        this.nom = nom;
        this.ingredients = ingredients;
        this.prix = prix;
    }

    /**
     * @return Nom de la pizza
     */
    public String getNom() {
        return nom;
    }

    /**
     * @return Liste des ingrédients
     */
    public List<String> getIngredients() {
        return ingredients;
    }

    /**
     * @return Prix de la pizza
     */
    public int getPrix() {
        return prix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetailsPizza that = (DetailsPizza) o;
        return prix == that.prix && 
               Objects.equals(nom, that.nom) && 
               Objects.equals(ingredients, that.ingredients);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nom, ingredients, prix);
    }

    @Override
    public String toString() {
        return "DetailsPizza{" +
                "nom='" + nom + '\'' +
                ", ingredients=" + ingredients +
                ", prix=" + prix +
                '}';
    }
}