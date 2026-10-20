package com.example.meallens.data;

import java.io.Serializable;

/** A meal the user saved from the summary screen. */
public class SavedMeal implements Serializable {
    public final String id;
    public final String imageUri;
    public final MealAnalysis analysis;
    public final double kcal;
    public final String time;

    public SavedMeal(String id, String imageUri, MealAnalysis analysis, double kcal, String time) {
        this.id = id;
        this.imageUri = imageUri;
        this.analysis = analysis;
        this.kcal = kcal;
        this.time = time;
    }
}
