package com.example.meallens.data;

/** Nutrition totals. Values are grams except kcal and sodium (mg). */
public class Nutrition {
    public double kcal;
    public double protein;
    public double carbs;
    public double fat;
    public double fiber;
    public double sugar;
    public double sodium;

    public Nutrition() {}

    public Nutrition(double kcal, double protein, double carbs, double fat,
                     double fiber, double sugar, double sodium) {
        this.kcal = kcal;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.sugar = sugar;
        this.sodium = sodium;
    }

    /** Returns this per-100 g reference scaled to the given weight. */
    public Nutrition scale(double grams) {
        double f = grams / 100.0;
        return new Nutrition(kcal * f, protein * f, carbs * f, fat * f,
                fiber * f, sugar * f, sodium * f);
    }

    public void add(Nutrition o) {
        kcal += o.kcal;
        protein += o.protein;
        carbs += o.carbs;
        fat += o.fat;
        fiber += o.fiber;
        sugar += o.sugar;
        sodium += o.sodium;
    }
}
