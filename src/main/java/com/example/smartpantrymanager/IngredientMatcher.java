package com.example.smartpantrymanager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
public class IngredientMatcher {
    private static final Set<String> FILLER_WORDS = new HashSet<>(Arrays.asList(
            "fresh", "large", "small", "medium", "big", "ripe", "raw", "chopped",
            "sliced", "diced", "grated", "frozen", "whole", "fillet", "fillets"));
    private static final Map<String, String> ALIASES = new HashMap<>();
    private static final Map<String, List<String>> SUBSTITUTES = new HashMap<>();

    static {
        alias("pasta", "spaghetti", "penne", "macaroni", "fusilli", "linguine", "tagliatelle");
        alias("cheese", "parmesan", "cheddar", "mozzarella", "pecorino",
                "parmesan cheese", "cheddar cheese", "mozzarella cheese", "pecorino cheese");
        alias("shrimp", "prawn");
        alias("bell pepper", "capsicum", "red pepper", "green pepper", "yellow pepper",
                "red bell pepper", "green bell pepper", "yellow bell pepper");
        alias("tomato", "cherry tomato", "plum tomato", "roma tomato");
        alias("mince", "ground beef", "minced beef", "beef mince");
        alias("chicken breast", "chicken");           // "chicken fillets" -> "chicken" -> "chicken breast"
        alias("oil", "cooking oil", "sunflower oil", "vegetable oil", "canola oil");
        alias("soy sauce", "soya sauce", "soy");
        alias("bbq sauce", "barbecue sauce", "braai sauce");
        alias("garlic", "garlic clove", "clove garlic");
        alias("cream", "double cream", "heavy cream", "whipping cream", "single cream");
        alias("milk", "full cream milk", "low fat milk", "skim milk", "long life milk");
        alias("bread", "white bread", "brown bread", "bread slice");
        alias("french bread", "baguette");
        alias("bread roll", "roll", "bun");
        alias("instant noodle", "ramen", "ramen noodle");
        alias("chickpea", "garbanzo bean");
        alias("sugar", "white sugar", "caster sugar", "castor sugar", "granulated sugar");
        alias("salt", "table salt", "sea salt");
        alias("flour", "plain flour", "cake flour", "all purpose flour", "self raising flour", "bread flour");
        alias("mayonnaise", "mayo");
        alias("bacon", "bacon rasher", "streaky bacon", "bacon strip");
        alias("ginger", "ginger root");
        alias("black pepper", "ground black pepper", "black peppercorn");
        alias("cajun spice", "cajun seasoning");
        alias("sun dried tomato", "sundried tomato");
        alias("vegetable stock", "veg stock");
        SUBSTITUTES.put("oil", Arrays.asList("olive oil"));
    }

    private static void alias(String canonical, String... variants) {
        for (String v : variants) ALIASES.put(v, canonical);
    }
    public static List<Recipe> strictMatches(List<Recipe> recipes, List<PantryItem> pantry) {
        // Total what the user has, per (name + unit type).
        Map<String, Double> have = new HashMap<>();
        for (PantryItem item : pantry) {
            String key = normalizeName(item.getName()) + "|" + baseUnit(item.getUnit());
            double amount = toBase(item.getQuantity(), item.getUnit());
            Double existing = have.get(key);
            have.put(key, (existing == null ? 0 : existing) + amount);
        }

        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (canMake(recipe, have)) result.add(recipe);
        }
        return result;
    }

    private static boolean canMake(Recipe recipe, Map<String, Double> have) {
        for (RecipeIngredient ing : recipe.getIngredients()) {
            String name = normalizeName(ing.getName());
            String unitType = baseUnit(ing.getUnit());
            double owned = amountOwned(have, name, unitType);
            double needed = toBase(ing.getQuantity(), ing.getUnit());
            // Missing ingredient or not enough -> reject the whole recipe.
            if (owned + 0.000001 < needed) return false;
        }
        return true;
    }
    private static double amountOwned(Map<String, Double> have, String name, String unitType) {
        double total = valueOf(have.get(name + "|" + unitType));
        List<String> subs = SUBSTITUTES.get(name);
        if (subs != null) {
            for (String sub : subs) total += valueOf(have.get(sub + "|" + unitType));
        }
        return total;
    }

    private static double valueOf(Double d) {
        return d == null ? 0 : d;
    }

    public static String normalizeName(String name) {
        // 1. lower-case, hyphens to spaces, remove anything that is not a letter
        String n = name.toLowerCase(Locale.ROOT).replace('-', ' ')
                .replaceAll("[^a-z ]", " ").trim().replaceAll("\\s+", " ");

        // 2. drop filler words
        StringBuilder sb = new StringBuilder();
        for (String word : n.split(" ")) {
            if (word.isEmpty() || FILLER_WORDS.contains(word)) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(word);
        }
        n = singularize(sb.toString());

        // 3. map alternative names to the canonical one
        String canonical = ALIASES.get(n);
        return canonical != null ? canonical : n;
    }
    private static String singularize(String n) {
        if (n.endsWith("ies") && n.length() > 4) {
            return n.substring(0, n.length() - 3) + "y";
        } else if (n.endsWith("oes") && n.length() > 4) {
            return n.substring(0, n.length() - 2);
        } else if (n.endsWith("s") && !n.endsWith("ss") && !n.endsWith("us") && n.length() > 3) {
            return n.substring(0, n.length() - 1);
        }
        return n;
    }
    public static String baseUnit(String unit) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "g":
            case "kg":
                return "g";
            case "ml":
            case "l":
            case "tsp":
            case "tbsp":
            case "cup":
                return "ml";
            default:
                return "pcs";
        }
    }
    public static double toBase(double qty, String unit) {
        switch (unit.toLowerCase(Locale.ROOT)) {
            case "kg":   return qty * 1000;
            case "l":    return qty * 1000;
            case "tsp":  return qty * 5;
            case "tbsp": return qty * 15;
            case "cup":  return qty * 240;
            default:     return qty;
        }
    }
}