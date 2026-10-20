package com.example.meallens.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Result of analysing a meal photo: dish name, confidence and the ingredient list. */
public class MealAnalysis implements Serializable {
    public final String title;
    public final String titleAr;
    public final double confidence;
    public final ArrayList<DetectedItem> items;

    public MealAnalysis(String title, String titleAr, double confidence, List<DetectedItem> items) {
        this.title = title;
        this.titleAr = titleAr;
        this.confidence = confidence;
        this.items = new ArrayList<>(items);
    }

    public int totalGrams() {
        int g = 0;
        for (DetectedItem it : items) g += it.grams;
        return g;
    }
}
