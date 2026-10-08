package com.example.smartpantrymanager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "smartpantry.db";
    private static final int DB_VERSION = 1;
    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pantry (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, quantity REAL NOT NULL, unit TEXT NOT NULL, expiry TEXT)");
        db.execSQL("CREATE TABLE recipes (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, steps TEXT NOT NULL)");
        db.execSQL("CREATE TABLE recipe_ingredients (id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "recipe_id INTEGER NOT NULL, name TEXT NOT NULL, quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL, FOREIGN KEY(recipe_id) REFERENCES recipes(id))");
        seedRecipes(db);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS recipe_ingredients");
        db.execSQL("DROP TABLE IF EXISTS recipes");
        db.execSQL("DROP TABLE IF EXISTS pantry");
        onCreate(db);
    }
    public long insertItem(String name, double quantity, String unit, String expiry) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("quantity", quantity);
        cv.put("unit", unit);
        cv.put("expiry", expiry);
        return getWritableDatabase().insert("pantry", null, cv);
    }
    public List<PantryItem> getAllItems() {
        List<PantryItem> list = new ArrayList<>();
        Cursor c = getReadableDatabase().query("pantry", null, null, null, null, null, "name COLLATE NOCASE");
        while (c.moveToNext()) {
            list.add(new PantryItem(c.getLong(0), c.getString(1), c.getDouble(2),
                    c.getString(3), c.getString(4)));
        }
        c.close();
        return list;
    }
    public PantryItem getItem(long id) {
        Cursor c = getReadableDatabase().query("pantry", null, "id=?",
                new String[]{String.valueOf(id)}, null, null, null);
        PantryItem item = null;
        if (c.moveToFirst()) {
            item = new PantryItem(c.getLong(0), c.getString(1), c.getDouble(2),
                    c.getString(3), c.getString(4));
        }
        c.close();
        return item;
    }
    public int updateItem(long id, String name, double quantity, String unit, String expiry) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("quantity", quantity);
        cv.put("unit", unit);
        cv.put("expiry", expiry);
        return getWritableDatabase().update("pantry", cv, "id=?", new String[]{String.valueOf(id)});
    }
    public int deleteItem(long id) {
        return getWritableDatabase().delete("pantry", "id=?", new String[]{String.valueOf(id)});
    }
    public List<Recipe> getAllRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("recipes", null, null, null, null, null, "name");
        while (c.moveToNext()) {
            Recipe r = new Recipe(c.getLong(0), c.getString(1), c.getString(2));
            loadIngredients(db, r);
            recipes.add(r);
        }
        c.close();
        return recipes;
    }
    public Recipe getRecipe(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query("recipes", null, "id=?", new String[]{String.valueOf(id)},
                null, null, null);
        Recipe r = null;
        if (c.moveToFirst()) {
            r = new Recipe(c.getLong(0), c.getString(1), c.getString(2));
            loadIngredients(db, r);
        }
        c.close();
        return r;
    }
    private void loadIngredients(SQLiteDatabase db, Recipe r) {
        Cursor c = db.query("recipe_ingredients", null, "recipe_id=?",
                new String[]{String.valueOf(r.getId())}, null, null, null);
        while (c.moveToNext()) {
            r.getIngredients().add(new RecipeIngredient(c.getString(2), c.getDouble(3), c.getString(4)));
        }
        c.close();
    }
    private void addRecipe(SQLiteDatabase db, String name, String steps, String... ingredients) {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("steps", steps);
        long recipeId = db.insert("recipes", null, cv);
        for (String s : ingredients) {
            String[] p = s.split("\\|");
            ContentValues ic = new ContentValues();
            ic.put("recipe_id", recipeId);
            ic.put("name", p[0]);
            ic.put("quantity", Double.parseDouble(p[1]));
            ic.put("unit", p[2]);
            db.insert("recipe_ingredients", null, ic);
        }
    }    private void seedRecipes(SQLiteDatabase db) {
        addRecipe(db, "Creamy Tuscan Chicken",
                "1. Season the chicken and sear it in olive oil until golden, then set aside.\n2. Fry the garlic and sun-dried tomatoes for a minute.\n3. Pour in the cream, add salt, and return the chicken.\n4. Simmer 10 minutes until cooked through.",
                "chicken breast|400|g", "garlic|3|pcs", "sun dried tomato|50|g", "cream|150|ml", "olive oil|1|tbsp", "salt|1|tsp");
        addRecipe(db, "Creamy Pesto Orzotto",
                "1. Bring the stock to a simmer.\n2. Toast the orzo in a pan, then add the stock a little at a time, stirring.\n3. When tender, stir in the pesto and grated cheese.",
                "orzo|200|g", "pesto|3|tbsp", "cheese|40|g", "vegetable stock|500|ml");
        addRecipe(db, "Cajun Chicken Alfredo",
                "1. Rub the sliced chicken with Cajun spice and fry until cooked.\n2. Boil the pasta and drain.\n3. Warm the cream with the cheese until smooth.\n4. Toss everything together.",
                "pasta|250|g", "chicken breast|300|g", "cream|200|ml", "cheese|50|g", "cajun spice|2|tsp");
        addRecipe(db, "Garlicky Shrimp and Veggies",
                "1. Chop the pepper, broccoli and onion and spread on a baking tray.\n2. Toss with olive oil, garlic and the shrimp.\n3. Roast at 200C for 15 to 20 minutes.",
                "shrimp|300|g", "bell pepper|1|pcs", "broccoli|200|g", "onion|1|pcs", "garlic|2|pcs", "olive oil|1|tbsp");
        addRecipe(db, "Coconut-Lime Chickpea Skillet",
                "1. Fry the chopped onion and garlic until soft.\n2. Add the drained chickpeas and coconut milk.\n3. Simmer 10 minutes and finish with lime juice.",
                "chickpea|400|g", "coconut milk|200|ml", "lime|1|pcs", "onion|1|pcs", "garlic|2|pcs");
        addRecipe(db, "BBQ Chicken Bread Pizza",
                "1. Mix shredded cooked chicken with BBQ sauce.\n2. Split the bread, spread the chicken on top and add cheese and onion.\n3. Bake at 200C for 10 minutes until melted.",
                "french bread|1|pcs", "chicken breast|200|g", "bbq sauce|3|tbsp", "cheese|60|g", "onion|1|pcs");
        addRecipe(db, "Black Bean Tostadas",
                "1. Crisp the tortillas in a hot pan or oven.\n2. Warm the beans with a little salt and mash lightly.\n3. Top with beans, cheese, sliced avocado and a squeeze of lime.",
                "tortilla|4|pcs", "black bean|200|g", "cheese|60|g", "avocado|1|pcs", "lime|1|pcs");
        addRecipe(db, "Chicken and Broccoli",
                "1. Slice the chicken and fry it in a hot pan.\n2. Add garlic and ginger, then the broccoli.\n3. Pour in the soy sauce and cook until the broccoli is just tender.",
                "chicken breast|300|g", "broccoli|250|g", "garlic|2|pcs", "ginger|1|tbsp", "soy sauce|2|tbsp");
        addRecipe(db, "Cacio e Pepe",
                "1. Boil the pasta, keeping a cup of the water.\n2. Toast the pepper in a dry pan.\n3. Add pasta and a splash of pasta water, then stir in the cheese until creamy.",
                "pasta|200|g", "cheese|80|g", "black pepper|2|tsp");
        addRecipe(db, "Korean-Style Beef",
                "1. Cut the beef into chunks and cover with soy sauce, sugar and crushed garlic.\n2. Simmer with the onion on low heat for about 1 hour until tender.\n3. Shred and serve over rice or in a sandwich.",
                "beef|500|g", "soy sauce|4|tbsp", "garlic|3|pcs", "sugar|2|tbsp", "onion|1|pcs");
        addRecipe(db, "Marry Me Chicken",
                "1. Sear the seasoned chicken and set aside.\n2. Fry garlic and sun-dried tomatoes, add the cream and cheese.\n3. Return the chicken and bake or simmer until cooked.",
                "chicken breast|400|g", "cream|150|ml", "sun dried tomato|50|g", "garlic|3|pcs", "cheese|40|g");
        addRecipe(db, "Honey-Garlic Glazed Salmon",
                "1. Mix honey, crushed garlic and soy sauce.\n2. Pan-fry the salmon in butter for 3 minutes per side.\n3. Pour in the glaze and cook until sticky.",
                "salmon|2|pcs", "honey|2|tbsp", "garlic|2|pcs", "soy sauce|1|tbsp", "butter|10|g");
        addRecipe(db, "Pasta alla Gricia",
                "1. Fry the bacon until crisp.\n2. Boil the pasta and keep some of the water.\n3. Toss pasta with bacon, pepper, cheese and a splash of pasta water.",
                "pasta|200|g", "bacon|100|g", "cheese|60|g", "black pepper|1|tsp");
        addRecipe(db, "Baked Cod with Tomatoes",
                "1. Place the cod in a dish with chopped tomatoes and garlic.\n2. Drizzle with olive oil and add lemon slices.\n3. Bake at 190C for 15 minutes.",
                "cod|2|pcs", "tomato|4|pcs", "garlic|2|pcs", "olive oil|2|tbsp", "lemon|1|pcs");
        addRecipe(db, "Creamy Lemon Herb Chicken",
                addRecipe(db, "Scrambled Eggs on Toast",
                        "1. Toast the bread.\n2. Whisk the eggs with salt.\n3. Melt the butter in a pan and scramble the eggs gently.\n4. Pile onto the toast.",
                        "egg|2|pcs", "bread|2|pcs", "butter|10|g", "salt|1|tsp");
        addRecipe(db, "Cheese Omelette",
                "1. Beat the eggs with salt.\n2. Melt butter in a pan and pour in the eggs.\n3. Add grated cheese, fold and serve.",
                "egg|3|pcs", "cheese|50|g", "butter|10|g", "salt|1|tsp");
        addRecipe(db, "Egg Fried Rice",
                "1. Fry chopped onion in oil.\n2. Push aside and scramble the eggs.\n3. Add cooked rice and soy sauce.\n4. Stir fry 5 minutes.",
                "rice|200|g", "egg|2|pcs", "onion|1|pcs", "soy sauce|2|tbsp", "oil|1|tbsp");
        addRecipe(db, "French Toast",
                "1. Whisk eggs, milk and sugar.\n2. Soak the bread slices.\n3. Fry in butter until golden on both sides.",
                "bread|4|pcs", "egg|2|pcs", "milk|100|ml", "sugar|1|tbsp", "butter|10|g");
        addRecipe(db, "Shakshuka",
                "1. Fry the chopped onion and garlic in oil until soft.\n2. Add chopped tomatoes and salt and simmer 10 minutes.\n3. Make small wells, crack in the eggs and cover.\n4. Cook until the whites are set.",
                "egg|3|pcs", "tomato|4|pcs", "onion|1|pcs", "garlic|2|pcs", "oil|1|tbsp", "salt|1|tsp");
        addRecipe(db, "Egg Mayo Sandwich",
                "1. Boil the eggs for 10 minutes, cool and peel.\n2. Mash with mayonnaise and a pinch of salt.\n3. Spread on the bread and close the sandwich.",
                "egg|3|pcs", "mayonnaise|2|tbsp", "bread|2|pcs", "salt|1|tsp");
        addRecipe(db, "Egg Fried Noodles",
                "1. Cook the noodles and drain.\n2. Scramble the eggs in hot oil.\n3. Add noodles and soy sauce and toss well.",
                "noodles|150|g", "egg|2|pcs", "soy sauce|1|tbsp", "oil|1|tbsp");
        addRecipe(db, "Cheesy Vegetable Frittata",
                "1. Fry the sliced onion and pepper in oil.\n2. Beat the eggs with salt and pour over the vegetables.\n3. Top with cheese and cook on low until set, or finish under the grill.",
                "egg|4|pcs", "onion|1|pcs", "bell pepper|1|pcs", "cheese|50|g", "oil|1|tbsp", "salt|1|tsp");
                "1. Brown the chicken in a pan.\n2. Add cream, lemon juice, garlic and herbs.\n3. Simmer on low for 20 minutes until tender.",
                "chicken breast|400|g", "cream|150|ml", "lemon|1|pcs", "garlic|2|pcs", "mixed herbs|1|tsp");
        addRecipe(db, "Marry Me Gnocchi",
                "1. Fry garlic and sun-dried tomatoes.\n2. Add the cream and cheese and stir until smooth.\n3. Add the gnocchi and cook until they float and the sauce coats them.",
                "gnocchi|400|g", "cream|150|ml", "sun dried tomato|50|g", "garlic|2|pcs", "cheese|40|g");
        addRecipe(db, "Pesto Ramen",
                "1. Cook the instant noodles and drain.\n2. Toss with pesto.\n3. Top with a fried or boiled egg.",
                "instant noodle|2|pcs", "pesto|2|tbsp", "egg|1|pcs");
        addRecipe(db, "Brown Sugar BBQ Chicken Rolls",
                "1. Cook the chicken in BBQ sauce and brown sugar until it falls apart.\n2. Shred it and pile into the rolls.",
                "chicken breast|400|g", "brown sugar|2|tbsp", "bbq sauce|4|tbsp", "bread roll|2|pcs");
        addRecipe(db, "One-Pot Lazy Lasagna",
                "1. Brown the mince in a large pot.\n2. Add the pasta sauce and broken pasta with a little water.\n3. Simmer until the pasta is tender, then top with cheese until melted.",
                "pasta|250|g", "mince|300|g", "pasta sauce|400|ml", "cheese|100|g");
        addRecipe(db, "Cube Steak with Gravy",
                "1. Coat the steak in flour and fry until browned.\n2. Add the sliced onion and stock and simmer into a gravy.\n3. Serve with boiled potatoes.",
                "cube steak|400|g", "flour|50|g", "onion|1|pcs", "beef stock|300|ml", "potato|4|pcs");
    }
}