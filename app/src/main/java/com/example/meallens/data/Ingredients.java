package com.example.meallens.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Per-100 g nutrition reference used by the mock analyser (USDA-style values). */
public final class Ingredients {
    private static final Map<String, Ingredient> ALL = new LinkedHashMap<>();

    static {
        put(new Ingredient("tamees", "Tamees bread", "خبز تميس", "🫓", 90,
                new Nutrition(270, 9, 52, 3, 2, 2, 450),
                Ingredient.TAG_GLUTEN, Ingredient.TAG_HIGH_GI));
        put(new Ingredient("banana", "Banana", "موز", "🍌", 120,
                new Nutrition(89, 1.1, 23, 0.3, 2.6, 12, 1),
                Ingredient.TAG_HIGH_SUGAR));
        put(new Ingredient("cream", "Cream (qishta)", "قشطة", "🥛", 40,
                new Nutrition(340, 2.5, 3, 36, 0, 3, 35),
                Ingredient.TAG_DAIRY));
        put(new Ingredient("honey", "Honey", "عسل", "🍯", 20,
                new Nutrition(304, 0.3, 82, 0, 0, 82, 4),
                Ingredient.TAG_HIGH_SUGAR, Ingredient.TAG_HIGH_GI));
        put(new Ingredient("cornflakes", "Corn flakes", "كورن فليكس", "🥣", 25,
                new Nutrition(357, 7, 84, 0.4, 3, 8, 729),
                Ingredient.TAG_HIGH_GI));
        put(new Ingredient("cashew", "Cashews", "كاجو", "🥜", 20,
                new Nutrition(553, 18, 30, 44, 3.3, 6, 12),
                Ingredient.TAG_NUTS));
    }

    private Ingredients() {}

    private static void put(Ingredient i) {
        ALL.put(i.id, i);
    }

    public static Ingredient get(String id) {
        return ALL.get(id);
    }

    public static List<Ingredient> all() {
        return new ArrayList<>(ALL.values());
    }
}
