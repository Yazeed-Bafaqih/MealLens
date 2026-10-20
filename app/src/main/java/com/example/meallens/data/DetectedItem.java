package com.example.meallens.data;

import java.io.Serializable;

/** One ingredient in a meal together with its confirmed weight. */
public class DetectedItem implements Serializable {
    public final String ingredientId;
    public int grams;

    public DetectedItem(String ingredientId, int grams) {
        this.ingredientId = ingredientId;
        this.grams = grams;
    }

    public static DetectedItem ofDefault(String ingredientId) {
        return new DetectedItem(ingredientId, Ingredients.get(ingredientId).defaultGrams);
    }

    public Ingredient ingredient() {
        return Ingredients.get(ingredientId);
    }

    public Nutrition nutrition() {
        return ingredient().per100g.scale(grams);
    }
}
