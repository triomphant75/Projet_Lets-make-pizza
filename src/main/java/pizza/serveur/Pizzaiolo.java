package pizza.serveur;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Pizzaiolo {
   public static final int MAX_PIZZAS_EN_PREPARATION = 4;
   public static final int MAX_PIZZAS_AU_FOUR = 6;
   private final AtomicBoolean fourDisponible;
   private AtomicInteger nbPreparationsRestantes;
   private final boolean quantique;

   public Pizzaiolo() {
      this(false);
   }

   public Pizzaiolo(boolean quantique) {
      this.fourDisponible = new AtomicBoolean(true);
      this.nbPreparationsRestantes = new AtomicInteger(4);
      this.quantique = quantique;
      if (this.quantique) {
         System.err.println("\u001b[31m");
         System.err.println("▄▄▄▄   ▄▄▄     ▐▌▗▞▀▚▖     ▄▄▄▄ █  ▐▌▗▞▀▜▌▄▄▄▄     ■  ▄  ▄▄▄▄ █  ▐▌▗▞▀▚▖    ▗▞▀▜▌▗▞▀▘   ■  ▄ ▗▞▀▀▘");
         System.err.println("█ █ █ █   █    ▐▌▐▛▀▀▘    █   █ ▀▄▄▞▘▝▚▄▟▌█   █ ▗▄▟▙▄▖▄ █   █ ▀▄▄▞▘▐▛▀▀▘    ▝▚▄▟▌▝▚▄▖▗▄▟▙▄▖▄ ▐▌   ");
         System.err.println("█   █ ▀▄▄▄▀ ▗▞▀▜▌▝▚▄▄▖    ▀▄▄▄█           █   █   ▐▌  █ ▀▄▄▄█      ▝▚▄▄▖               ▐▌  █ ▐▛▀▘ ");
         System.err.println("            ▝▚▄▟▌             █                   ▐▌  █     █                          ▐▌  █ ▐▌   ");
         System.err.println("                              ▀                   ▐▌        ▀                          ▐▌         ");
         System.err.println("\u001b[39m");
      }

   }

   public List<Pizzaiolo.DetailsPizza> getListePizzas() {
      return Arrays.asList(new Pizzaiolo.DetailsPizza("margherita", Arrays.asList(Pizzaiolo.Ingredient.SAUCE_TOMATE, Pizzaiolo.Ingredient.MOZARELLA, Pizzaiolo.Ingredient.BASILIC), 5), new Pizzaiolo.DetailsPizza("quattro stagioni", Arrays.asList(Pizzaiolo.Ingredient.SAUCE_TOMATE, Pizzaiolo.Ingredient.MOZARELLA, Pizzaiolo.Ingredient.JAMBON, Pizzaiolo.Ingredient.CHAMPIGNONS, Pizzaiolo.Ingredient.POIVRON, Pizzaiolo.Ingredient.ARTICHAUT, Pizzaiolo.Ingredient.OLIVES), 9), new Pizzaiolo.DetailsPizza("reine", Arrays.asList(Pizzaiolo.Ingredient.SAUCE_TOMATE, Pizzaiolo.Ingredient.MOZARELLA, Pizzaiolo.Ingredient.JAMBON, Pizzaiolo.Ingredient.CHAMPIGNONS, Pizzaiolo.Ingredient.OLIVES), 7), new Pizzaiolo.DetailsPizza("napoli", Arrays.asList(Pizzaiolo.Ingredient.SAUCE_TOMATE, Pizzaiolo.Ingredient.ANCHOIS, Pizzaiolo.Ingredient.OLIVES), 5), new Pizzaiolo.DetailsPizza("hawaiana", Arrays.asList(Pizzaiolo.Ingredient.SAUCE_TOMATE, Pizzaiolo.Ingredient.FROMAGE, Pizzaiolo.Ingredient.JAMBON, Pizzaiolo.Ingredient.ANANAS), 8));
   }

   public Pizzaiolo.Pizza preparer(Pizzaiolo.DetailsPizza pizza) {
      if (pizza.ingredients().contains(Pizzaiolo.Ingredient.ANANAS)) {
         throw new IllegalArgumentException("de l'ananas, sérieux ?");
      } else if (!this.quantique && this.nbPreparationsRestantes.getAndUpdate((val) -> {
         return val > 0 ? val - 1 : val;
      }) == 0) {
         throw new IllegalStateException("trop de pizza déjà en préparation");
      } else {
         try {
            Thread.sleep((long)((int)Math.floor(Math.random() * 1000.0D) + 2000));
         } catch (InterruptedException var3) {
         }

         if (!this.quantique) {
            this.nbPreparationsRestantes.updateAndGet((val) -> {
               return val + 1;
            });
         }

         return new Pizzaiolo.Pizza(pizza.nom(), Pizzaiolo.Statut.PRETE);
      }
   }

   public List<Pizzaiolo.Pizza> cuire(List<Pizzaiolo.Pizza> pizzas) {
      if (!pizzas.stream().allMatch((p) -> {
         return p.statut() == Pizzaiolo.Statut.PRETE;
      })) {
         throw new IllegalArgumentException("au moins une pizza n'est pas prête");
      } else if (!this.quantique && !this.fourDisponible.getAndSet(false)) {
         throw new IllegalStateException("le four est déjà occupé !");
      } else {
         try {
            Thread.sleep((long)((int)Math.floor(Math.random() * 2000.0D) + 3000));
         } catch (InterruptedException var3) {
         }

         if (!this.quantique) {
            this.fourDisponible.set(true);
         }

         return (List)pizzas.stream().map((pizza) -> {
            return new Pizzaiolo.Pizza(pizza.nom(), Pizzaiolo.Statut.CUITE);
         }).collect(Collectors.toList());
      }
   }

   public static record DetailsPizza(String nom, List<Pizzaiolo.Ingredient> ingredients, int prix) {
      public DetailsPizza(String nom, List<Pizzaiolo.Ingredient> ingredients, int prix) {
         this.nom = nom;
         this.ingredients = ingredients;
         this.prix = prix;
      }

      public String nom() {
         return this.nom;
      }

      public List<Pizzaiolo.Ingredient> ingredients() {
         return this.ingredients;
      }

      public int prix() {
         return this.prix;
      }
   }

   public static enum Ingredient {
      SAUCE_TOMATE,
      TOMATES_CERISES,
      MOZARELLA,
      BASILIC,
      ANCHOIS,
      OLIVES,
      FROMAGE,
      JAMBON,
      CHAMPIGNONS,
      POIVRON,
      ARTICHAUT,
      ANANAS;

      public String toString() {
         return this.name().toLowerCase().replace('_', ' ');
      }

      // $FF: synthetic method
      private static Pizzaiolo.Ingredient[] $values() {
         return new Pizzaiolo.Ingredient[]{SAUCE_TOMATE, TOMATES_CERISES, MOZARELLA, BASILIC, ANCHOIS, OLIVES, FROMAGE, JAMBON, CHAMPIGNONS, POIVRON, ARTICHAUT, ANANAS};
      }
   }

   public static class Pizza {
      private final Pizzaiolo.Statut statut;
      private final String nom;

      private Pizza(String nom, Pizzaiolo.Statut statut) {
         this.statut = statut;
         this.nom = nom;
      }

      public Pizzaiolo.Statut statut() {
         return this.statut;
      }

      public String nom() {
         return this.nom;
      }
   }

   public static enum Statut {
      PRETE,
      CUITE;

      // $FF: synthetic method
      private static Pizzaiolo.Statut[] $values() {
         return new Pizzaiolo.Statut[]{PRETE, CUITE};
      }
   }
}
