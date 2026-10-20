package com.example.meallens.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Turns the JSON returned by the DeepSeek vision model into the app's analysis
 * model. Kept free of Android imports so it can be unit tested on the JVM.
 *
 * <p>The model returns per-100 g nutrition for each identified ingredient; the
 * app scales those values to the estimated portion and derives totals, score and
 * deterministic health flags from them.</p>
 */
public final class MealJsonParser {
    public static final int MAX_INGREDIENTS = 12;
    public static final int MAX_NOTES = 6;

    private static final Set<String> ALLOWED_TAGS = new LinkedHashSet<>(Arrays.asList(
            Ingredient.TAG_GLUTEN,
            Ingredient.TAG_DAIRY,
            Ingredient.TAG_NUTS,
            Ingredient.TAG_HIGH_SUGAR,
            Ingredient.TAG_HIGH_GI,
            Ingredient.TAG_VEGETARIAN,
            Ingredient.TAG_VEGAN,
            Ingredient.TAG_FRIED,
            Ingredient.TAG_PROCESSED));

    private MealJsonParser() {}

    /**
     * @throws IllegalArgumentException when the payload is not usable JSON or the
     *                                  photo contained no recognisable food.
     */
    public static MealAnalysis parse(String raw) {
        String json = stripToJsonObject(raw);
        try {
            JSONObject root = new JSONObject(json);

            JSONObject dish = root.optJSONObject("dish");
            String title = text(dish, "name_en");
            String titleAr = text(dish, "name_ar");
            double confidence = clamp(number(dish, "confidence", 0.5), 0, 1);
            if (title.isEmpty()) title = "Meal";

            JSONArray ingredientsJson = root.optJSONArray("ingredients");
            if (ingredientsJson == null || ingredientsJson.length() == 0) {
                throw new IllegalArgumentException(
                        "No meal was detected in this photo. Try a clearer picture with the food in view.");
            }
            if ("no meal detected".equalsIgnoreCase(title)) {
                throw new IllegalArgumentException(
                        "No meal was detected in this photo. Try a clearer picture with the food in view.");
            }

            List<DetectedItem> items = new ArrayList<>();
            for (int i = 0; i < ingredientsJson.length() && items.size() < MAX_INGREDIENTS; i++) {
                JSONObject item = ingredientsJson.optJSONObject(i);
                if (item == null) continue;
                String name = text(item, "name_en");
                if (name.isEmpty()) continue;

                double grams = clamp(number(item, "grams", 100), 1, 2000);
                JSONObject per100 = item.optJSONObject("per_100g");
                Nutrition nutrition = new Nutrition(
                        clamp(number(per100, "kcal", 0), 0, 900),
                        clamp(number(per100, "protein_g", 0), 0, 100),
                        clamp(number(per100, "carbs_g", 0), 0, 100),
                        clamp(number(per100, "fat_g", 0), 0, 100),
                        clamp(number(per100, "fiber_g", 0), 0, 100),
                        clamp(number(per100, "sugar_g", 0), 0, 100),
                        clamp(number(per100, "sodium_mg", 0), 0, 5000));

                String id = "ai-" + i + "-" + slug(name);
                List<String> tags = parseTags(item.optJSONArray("tags"));
                Ingredient ingredient = new Ingredient(
                        id, name, text(item, "name_ar"), text(item, "emoji"),
                        (int) Math.round(grams), nutrition,
                        tags.toArray(new String[0]));
                items.add(new DetectedItem(ingredient, (int) Math.round(grams)));
            }

            if (items.isEmpty()) {
                throw new IllegalArgumentException(
                        "The meal was recognised but no usable ingredients came back. Please retry.");
            }

            List<Flag> aiFlags = parseFlags(root.optJSONArray("health_notes"));
            String summary = text(root, "summary");
            return new MealAnalysis(title, titleAr, confidence, items, summary, "", aiFlags);
        } catch (JSONException e) {
            throw new IllegalArgumentException("The recognition response was not valid JSON. Please retry.", e);
        }
    }

    private static String stripToJsonObject(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new IllegalArgumentException("The recognition service returned an empty response. Please retry.");
        }
        String text = raw.trim();
        if (text.startsWith("```")) {
            int newline = text.indexOf('\n');
            if (newline >= 0) text = text.substring(newline + 1);
            if (text.endsWith("```")) text = text.substring(0, text.length() - 3);
            text = text.trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("The recognition response was not valid JSON. Please retry.");
        }
        return text.substring(start, end + 1);
    }

    private static List<String> parseTags(JSONArray tagsJson) {
        Set<String> tags = new LinkedHashSet<>();
        if (tagsJson == null) return new ArrayList<>(tags);
        for (int i = 0; i < tagsJson.length(); i++) {
            String tag = tagsJson.optString(i, "").trim().toLowerCase(Locale.US)
                    .replace('_', '-');
            if (tag.equals("tree-nuts") || tag.equals("treenuts")) tag = Ingredient.TAG_NUTS;
            if (tag.equals("high sugar")) tag = Ingredient.TAG_HIGH_SUGAR;
            if (tag.equals("high gi")) tag = Ingredient.TAG_HIGH_GI;
            if (ALLOWED_TAGS.contains(tag)) tags.add(tag);
        }
        return new ArrayList<>(tags);
    }

    private static List<Flag> parseFlags(JSONArray notesJson) {
        List<Flag> flags = new ArrayList<>();
        if (notesJson == null) return flags;
        for (int i = 0; i < notesJson.length() && flags.size() < MAX_NOTES; i++) {
            JSONObject note = notesJson.optJSONObject(i);
            if (note == null) continue;
            String title = text(note, "title");
            String detail = text(note, "detail");
            if (title.isEmpty() && detail.isEmpty()) continue;
            if (title.isEmpty()) title = "Health note";
            flags.add(new Flag(level(text(note, "level")), title, detail));
        }
        return flags;
    }

    private static Flag.Level level(String raw) {
        switch (raw.toLowerCase(Locale.US)) {
            case "danger":
            case "high":
                return Flag.Level.DANGER;
            case "warn":
            case "warning":
            case "medium":
                return Flag.Level.WARN;
            case "ok":
            case "good":
                return Flag.Level.OK;
            default:
                return Flag.Level.INFO;
        }
    }

    private static String text(JSONObject object, String key) {
        if (object == null || !object.has(key) || object.isNull(key)) return "";
        return object.optString(key, "").trim();
    }

    private static double number(JSONObject object, String key, double fallback) {
        if (object == null || !object.has(key) || object.isNull(key)) return fallback;
        double value = object.optDouble(key, fallback);
        if (Double.isNaN(value) || Double.isInfinite(value)) return fallback;
        return value;
    }

    private static double clamp(double value, double min, double max) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }

    private static String slug(String name) {
        String slug = name.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-");
        while (slug.startsWith("-")) slug = slug.substring(1);
        while (slug.endsWith("-")) slug = slug.substring(0, slug.length() - 1);
        if (slug.isEmpty()) slug = "food";
        return slug.length() > 32 ? slug.substring(0, 32) : slug;
    }
}
