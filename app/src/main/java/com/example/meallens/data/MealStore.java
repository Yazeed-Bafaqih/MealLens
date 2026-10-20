package com.example.meallens.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory list of saved meals for the current session (newest first). */
public final class MealStore {
    private static final List<SavedMeal> MEALS = new ArrayList<>();

    private MealStore() {}

    public static void add(SavedMeal meal) {
        MEALS.add(0, meal);
    }

    public static List<SavedMeal> all() {
        return Collections.unmodifiableList(MEALS);
    }

    public static SavedMeal latest() {
        return MEALS.isEmpty() ? null : MEALS.get(0);
    }

    public static SavedMeal find(String id) {
        for (SavedMeal m : MEALS) if (m.id.equals(id)) return m;
        return null;
    }

    public static void clear() {
        MEALS.clear();
    }
}
