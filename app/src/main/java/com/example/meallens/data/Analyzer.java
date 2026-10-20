package com.example.meallens.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Nutrition analysis logic. Everything here works on the confirmed ingredient
 * list returned by DeepSeek (or added manually by the user).
 */
public final class Analyzer {
    private Analyzer() {}

    /**
     * Local fixture for the instrumented regression tests. It is no longer used
     * by the normal UI; real recognition lives in {@code MealRecognizer}.
     */
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
            String fastCarbs = namesWithTag(items, Ingredient.TAG_HIGH_GI);
            String detail = fastCarbs.isEmpty()
                    ? String.format(Locale.US, "About %d g of sugar may spike blood glucose.",
                            Math.round(t.sugar))
                    : String.format(Locale.US,
                            "About %d g of sugar plus fast carbs from %s may spike blood glucose.",
                            Math.round(t.sugar), fastCarbs);
            flags.add(new Flag(Flag.Level.DANGER, "Not suitable for diabetics", detail));
        }
        if (t.kcal >= 600) {
            flags.add(new Flag(Flag.Level.WARN, "High calorie meal",
                    String.format(Locale.US, "Roughly %d%% of a 2000 kcal day in one plate.",
                            Math.round(t.kcal / 2000 * 100))));
        }
        if (t.fat >= 25) {
            String topFat = topBy(items, n -> n.fat);
            String detail = topFat.isEmpty()
                    ? "This meal is high in fat. Reduce the fattiest ingredients to cut it."
                    : String.format(Locale.US,
                            "About %d g of fat. %s contributes most; reducing it will cut the total.",
                            Math.round(t.fat), topFat);
            flags.add(new Flag(Flag.Level.WARN, "High in fat", detail));
        }
        if (t.sodium >= 600) {
            flags.add(new Flag(Flag.Level.INFO, "Moderate sodium",
                    String.format(Locale.US, "About %d mg of sodium in this serving.",
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
            String topFiber = topBy(items, n -> n.fiber);
            String detail = topFiber.isEmpty()
                    ? String.format(Locale.US, "About %d g of fiber.", Math.round(t.fiber))
                    : String.format(Locale.US, "About %d g of fiber, mostly from %s.",
                            Math.round(t.fiber), topFiber);
            flags.add(new Flag(Flag.Level.OK, "Good source of fiber", detail));
        }
        if (t.protein >= 15) {
            flags.add(new Flag(Flag.Level.OK, "Decent protein",
                    String.format(Locale.US, "About %d g of protein.", Math.round(t.protein))));
        }
        return flags;
    }

    private static String namesWithTag(List<DetectedItem> items, String tag) {
        StringBuilder sb = new StringBuilder();
        for (DetectedItem item : items) {
            if (!item.ingredient().hasTag(tag)) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.ingredient().name);
        }
        return sb.toString();
    }

    /** Name of the ingredient contributing the most of a nutrient, or "" when empty. */
    private static String topBy(List<DetectedItem> items,
                                java.util.function.ToDoubleFunction<Nutrition> value) {
        DetectedItem best = null;
        double bestValue = 0;
        for (DetectedItem item : items) {
            double contribution = value.applyAsDouble(item.nutrition());
            if (contribution > bestValue) {
                bestValue = contribution;
                best = item;
            }
        }
        return best == null ? "" : best.ingredient().name;
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
