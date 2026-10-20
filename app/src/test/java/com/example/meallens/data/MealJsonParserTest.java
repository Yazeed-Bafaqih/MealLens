package com.example.meallens.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class MealJsonParserTest {
    private static final String SAMPLE = "{"
            + "\"dish\":{\"name_en\":\"Chicken shawarma plate\",\"name_ar\":\"شاورما دجاج\",\"confidence\":0.87},"
            + "\"ingredients\":["
            + "{\"name_en\":\"Chicken shawarma\",\"name_ar\":\"شاورما\",\"emoji\":\"🌯\",\"grams\":120,"
            + "\"per_100g\":{\"kcal\":200,\"protein_g\":25,\"carbs_g\":5,\"fat_g\":10,\"fiber_g\":0,"
            + "\"sugar_g\":1,\"sodium_mg\":500},"
            + "\"tags\":[\"gluten\",\"high sugar\",\"not-a-real-tag\"]},"
            + "{\"name_en\":\"White rice\",\"name_ar\":\"أرز\",\"emoji\":\"🍚\",\"grams\":150,"
            + "\"per_100g\":{\"kcal\":130,\"protein_g\":2.7,\"carbs_g\":28,\"fat_g\":0.3,"
            + "\"fiber_g\":0.4,\"sugar_g\":0.1,\"sodium_mg\":1},\"tags\":[]}"
            + "],"
            + "\"health_notes\":["
            + "{\"level\":\"warn\",\"title\":\"High sodium\",\"detail\":\"The shawarma is salty.\"},"
            + "{\"level\":\"ok\",\"title\":\"Good protein\",\"detail\":\"Chicken adds protein.\"}"
            + "],"
            + "\"summary\":\"A protein-rich chicken plate with rice.\""
            + "}";

    @Test
    public void parsesDishIngredientsNotesAndSummary() {
        MealAnalysis analysis = MealJsonParser.parse(SAMPLE);

        assertEquals("Chicken shawarma plate", analysis.title);
        assertEquals("شاورما دجاج", analysis.titleAr);
        assertEquals(0.87, analysis.confidence, 0.0001);
        assertEquals("A protein-rich chicken plate with rice.", analysis.summary);
        assertEquals(2, analysis.items.size());
        assertEquals(270, analysis.totalGrams());

        DetectedItem chicken = analysis.items.get(0);
        assertEquals("ai-0-chicken-shawarma", chicken.ingredientId);
        assertEquals("Chicken shawarma", chicken.ingredient().name);
        assertEquals(120, chicken.grams);
        assertEquals(240, chicken.nutrition().kcal, 0.001);
        assertEquals(30, chicken.nutrition().protein, 0.001);
        assertTrue(chicken.ingredient().hasTag(Ingredient.TAG_GLUTEN));
        assertTrue(chicken.ingredient().hasTag(Ingredient.TAG_HIGH_SUGAR));
        assertFalse(chicken.ingredient().hasTag("not-a-real-tag"));

        assertEquals("🌯", chicken.ingredient().emoji);
        assertEquals(2, analysis.aiFlags.size());
        assertEquals(Flag.Level.WARN, analysis.aiFlags.get(0).level);
        assertEquals("High sodium", analysis.aiFlags.get(0).title);
        assertEquals(Flag.Level.OK, analysis.aiFlags.get(1).level);
    }

    @Test
    public void acceptsMarkdownFencesAroundJson() {
        MealAnalysis analysis = MealJsonParser.parse("```json\n" + SAMPLE + "\n```");
        assertEquals("Chicken shawarma plate", analysis.title);
    }

    @Test
    public void clampsPortionsAndNutritionToSaneBounds() {
        String json = "{\"dish\":{\"name_en\":\"Test\",\"name_ar\":\"\",\"confidence\":4},"
                + "\"ingredients\":[{\"name_en\":\"Test food\",\"name_ar\":\"\",\"emoji\":\"\","
                + "\"grams\":5000,\"per_100g\":{\"kcal\":-50,\"protein_g\":999,\"carbs_g\":-1,"
                + "\"fat_g\":1000,\"fiber_g\":-3,\"sugar_g\":500,\"sodium_mg\":99999},\"tags\":[]}],"
                + "\"health_notes\":[],\"summary\":\"\"}";

        MealAnalysis analysis = MealJsonParser.parse(json);

        assertEquals(1, analysis.confidence, 0.0001);
        DetectedItem item = analysis.items.get(0);
        assertEquals(2000, item.grams);
        assertEquals(0, item.ingredient().per100g.kcal, 0.001);
        assertEquals(100, item.ingredient().per100g.protein, 0.001);
        assertEquals(0, item.ingredient().per100g.carbs, 0.001);
        assertEquals(100, item.ingredient().per100g.fat, 0.001);
        assertEquals(5000, item.ingredient().per100g.sodium, 0.001);
        assertEquals("🍽️", item.ingredient().emoji);
    }

    @Test
    public void rejectsPayloadWithoutIngredients() {
        try {
            MealJsonParser.parse("{\"dish\":{\"name_en\":\"No meal detected\"},"
                    + "\"ingredients\":[],\"summary\":\"Nothing here.\"}");
            fail("Expected an empty meal to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("No meal was detected in this photo. Try a clearer picture with the food in view.",
                    expected.getMessage());
        }
    }

    @Test
    public void rejectsMalformedJson() {
        try {
            MealJsonParser.parse("not json at all");
            fail("Expected malformed JSON to be rejected");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }
}
