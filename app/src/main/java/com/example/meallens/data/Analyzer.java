package com.example.meallens.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Meal analysis logic. {@link #analyseImage} is a mock vision model that always
 * recognises the demo meal (masoub); everything after it works on the confirmed
 * ingredient list and is reusable once a real model is connected.
 */
public final class Analyzer {
    /** Simulated network / inference delay in milliseconds. */
    public static final long MOCK_DELAY_MS = 1400;

    private Analyzer() {}

    /** Mock recogniser: returns the demo meal regardless of the photo. */
    public static MealAnalysis analyseImage(String imageUri) {
        List<DetectedItem> items = Arrays.asList(
                DetectedItem.ofDefault("tamees"),
                DetectedItem.ofDefault("banana"),
                DetectedItem.ofDefault("cream"),
                DetectedItem.ofDefault("honey"),
                DetectedItem.ofDefault("cornflakes"),
                DetectedItem.ofDefault("cashew"));
        return new MealAnalysis("Masoub", "معصوب", 0.91, items);
    }

    public static Nutrition totals(List<DetectedItem> items) {
        Nutrition t = new Nutrition();
        for (DetectedItem it : items) t.add(it.nutrition());
        return t;
    }

    public static List<Flag> buildFlags(List<DetectedItem> items) {
        Nutrition t = totals(items);
        Set<String> tags = new HashSet<>();
        for (DetectedItem it : items) tags.addAll(it.ingredient().tags);
        List<Flag> flags = new ArrayList<>();

        if (t.sugar >= 25 || tags.contains(Ingredient.TAG_HIGH_GI)) {
            flags.add(new Flag(Flag.Level.DANGER, "Not suitable for diabetics",
                    String.format(Locale.US,
                            "About %d g of sugar plus fast carbs (bread, honey, corn flakes) may spike blood glucose.",
                            Math.round(t.sugar))));
        }
        if (t.kcal >= 600) {
            flags.add(new Flag(Flag.Level.WARN, "High calorie meal",
                    String.format(Locale.US, "Roughly %d%% of a 2000 kcal day in one plate.",
                            Math.round(t.kcal / 2000 * 100))));
        }
        if (t.fat >= 25) {
            flags.add(new Flag(Flag.Level.WARN, "High in fat",
                    "Cream and nuts contribute most of the fat. Reduce the cream to cut it."));
        }
        if (t.sodium >= 600) {
            flags.add(new Flag(Flag.Level.INFO, "Moderate sodium",
                    String.format(Locale.US, "About %d mg sodium, mostly from bread and corn flakes.",
                            Math.round(t.sodium))));
        }
        List<String> allergens = new ArrayList<>();
        if (tags.contains(Ingredient.TAG_GLUTEN)) allergens.add("gluten");
        if (tags.contains(Ingredient.TAG_DAIRY)) allergens.add("dairy");
        if (tags.contains(Ingredient.TAG_NUTS)) allergens.add("tree nuts");
        if (!allergens.isEmpty()) {
            flags.add(new Flag(Flag.Level.INFO, "Contains allergens",
                    "Contains " + String.join(", ", allergens) + "."));
        }
        if (t.fiber >= 5) {
            flags.add(new Flag(Flag.Level.OK, "Good source of fiber",
                    String.format(Locale.US, "%d g of fiber from banana and bread.", Math.round(t.fiber))));
        }
        if (t.protein >= 15) {
            flags.add(new Flag(Flag.Level.OK, "Decent protein",
                    String.format(Locale.US, "%d g of protein.", Math.round(t.protein))));
        }
        return flags;
    }

    /** Simple 5–95 score: penalises sugar, calories and fat; rewards fiber and protein. */
    public static int healthScore(List<DetectedItem> items) {
        Nutrition t = totals(items);
        double s = 80;
        s -= Math.max(0, t.sugar - 15) * 1.2;
        s -= Math.max(0, t.kcal - 500) / 20;
        s -= Math.max(0, t.fat - 20) * 0.8;
        s += Math.min(t.fiber, 8) * 1.5;
        s += Math.min(t.protein, 25) * 0.4;
        return (int) Math.max(5, Math.min(95, Math.round(s)));
    }
}
