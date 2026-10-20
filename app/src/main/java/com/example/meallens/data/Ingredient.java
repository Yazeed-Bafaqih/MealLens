package com.example.meallens.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** A food the analyser can recognise, with per-100 g nutrition and diet tags. */
public class Ingredient implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TAG_GLUTEN = "gluten";
    public static final String TAG_DAIRY = "dairy";
    public static final String TAG_NUTS = "nuts";
    public static final String TAG_HIGH_SUGAR = "high-sugar";
    public static final String TAG_HIGH_GI = "high-gi";
    public static final String TAG_VEGETARIAN = "vegetarian";
    public static final String TAG_VEGAN = "vegan";
    public static final String TAG_FRIED = "fried";
    public static final String TAG_PROCESSED = "processed";

    public final String id;
    public final String name;
    public final String nameAr;
    public final String emoji;
    public final int defaultGrams;
    public final Nutrition per100g;
    public final List<String> tags;

    public Ingredient(String id, String name, String nameAr, String emoji, int defaultGrams,
                      Nutrition per100g, String... tags) {
        this.id = id;
        this.name = name;
        this.nameAr = nameAr == null ? "" : nameAr;
        this.emoji = emoji == null || emoji.isEmpty() ? "🍽️" : emoji;
        this.defaultGrams = defaultGrams;
        this.per100g = per100g;
        this.tags = new ArrayList<>(Arrays.asList(tags));
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
