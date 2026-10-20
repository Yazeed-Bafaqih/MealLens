package com.example.meallens.data;

import java.io.Serializable;

/** One ingredient in a meal together with its confirmed weight. */
public class DetectedItem implements Serializable {
    private static final long serialVersionUID = 1L;

    public final String ingredientId;
    private final Ingredient ingredient;
    public int grams;

    /** Item backed by an ingredient returned by the recognition API. */
    public DetectedItem(Ingredient ingredient, int grams) {
        if (ingredient == null) throw new IllegalArgumentException("ingredient == null");
        this.ingredient = ingredient;
        this.ingredientId = ingredient.id;
        this.grams = Math.max(0, grams);
    }

    /** Item backed by one of the built-in reference ingredients. */
    public DetectedItem(String ingredientId, int grams) {
        this(require(ingredientId), grams);
    }

    private static Ingredient require(String ingredientId) {
        Ingredient ingredient = Ingredients.get(ingredientId);
        if (ingredient == null) throw new IllegalArgumentException("Unknown ingredient " + ingredientId);
        return ingredient;
    }

    public static DetectedItem ofDefault(String ingredientId) {
        Ingredient ingredient = require(ingredientId);
        return new DetectedItem(ingredient, ingredient.defaultGrams);
    }

    public static DetectedItem ofDefault(Ingredient ingredient) {
        return new DetectedItem(ingredient, ingredient.defaultGrams);
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public Nutrition nutrition() {
        return ingredient.per100g.scale(grams);
    }
}
