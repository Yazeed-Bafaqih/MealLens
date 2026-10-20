import { MealAnalysis } from "./data/analyzer";

export type SavedMeal = {
  id: string;
  image: string | number;
  analysis: MealAnalysis;
  kcal: number;
  time: string;
};
