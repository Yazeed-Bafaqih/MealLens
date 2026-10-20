package com.example.meallens.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AnalyzerTest {

    private static boolean hasFlag(List<Flag> flags, String title) {
        for (Flag f : flags) if (f.title.equals(title)) return true;
        return false;
    }

    @Test
    public void demoMealIsMasoubWithSixIngredients() {
        MealAnalysis a = Analyzer.analyseImage("demo");
        assertEquals("Masoub", a.title);
        assertEquals(0.91, a.confidence, 0.001);
        assertEquals(6, a.items.size());
        assertEquals("tamees", a.items.get(0).ingredientId);
        assertEquals(90, a.items.get(0).grams);
        assertEquals(315, a.totalGrams());
    }

    @Test
    public void scalingUsesPer100gValues() {
        Nutrition n = Ingredients.get("banana").per100g.scale(200);
        assertEquals(178, n.kcal, 0.001);
        assertEquals(24, n.sugar, 0.001);
    }

    @Test
    public void demoMealTotalsMatchExpoVersion() {
        Nutrition t = Analyzer.totals(Analyzer.analyseImage("demo").items);
        assertEquals(746, Math.round(t.kcal));
        assertEquals(16, Math.round(t.protein));
        assertEquals(119, Math.round(t.carbs));
        assertEquals(26, Math.round(t.fat));
        assertEquals(37, Math.round(t.sugar));
        assertEquals(6, Math.round(t.fiber));
        assertEquals(606, Math.round(t.sodium));
    }

    @Test
    public void removingHoneyLowersCalories() {
        List<DetectedItem> items = new ArrayList<>(Analyzer.analyseImage("demo").items);
        items.removeIf(i -> i.ingredientId.equals("honey"));
        assertEquals(686, Math.round(Analyzer.totals(items).kcal));
    }

    @Test
    public void demoMealRaisesExpectedFlags() {
        List<Flag> flags = Analyzer.buildFlags(Analyzer.analyseImage("demo").items);
        assertTrue(hasFlag(flags, "Not suitable for diabetics"));
        assertTrue(hasFlag(flags, "High calorie meal"));
        assertTrue(hasFlag(flags, "High in fat"));
        assertTrue(hasFlag(flags, "Moderate sodium"));
        assertTrue(hasFlag(flags, "Contains allergens"));
        assertTrue(hasFlag(flags, "Good source of fiber"));
        assertTrue(hasFlag(flags, "Decent protein"));
        assertEquals(Flag.Level.DANGER, flags.get(0).level);
    }

    @Test
    public void smallBananaOnlyMealHasNoDangerFlag() {
        List<DetectedItem> items = new ArrayList<>();
        items.add(new DetectedItem("banana", 100));
        List<Flag> flags = Analyzer.buildFlags(items);
        assertFalse(hasFlag(flags, "Not suitable for diabetics"));
        assertFalse(hasFlag(flags, "High calorie meal"));
        assertFalse(hasFlag(flags, "Contains allergens"));
    }

    @Test
    public void healthScoreStaysWithinBounds() {
        int demo = Analyzer.healthScore(Analyzer.analyseImage("demo").items);
        assertEquals(52, demo);
        List<DetectedItem> huge = new ArrayList<>();
        huge.add(new DetectedItem("honey", 1000));
        assertEquals(5, Analyzer.healthScore(huge));
        List<DetectedItem> none = new ArrayList<>();
        assertTrue(Analyzer.healthScore(none) >= 5 && Analyzer.healthScore(none) <= 95);
    }

    @Test
    public void mealStoreKeepsNewestFirst() {
        MealStore.clear();
        MealAnalysis a = Analyzer.analyseImage("demo");
        MealStore.add(new SavedMeal("1", "demo", a, 700, "Today, 1:00 PM"));
        MealStore.add(new SavedMeal("2", "demo", a, 650, "Today, 2:00 PM"));
        assertEquals("2", MealStore.latest().id);
        assertNotNull(MealStore.find("1"));
        assertEquals(2, MealStore.all().size());
        MealStore.clear();
    }
}
