import { INGREDIENTS, Ingredient, Nutrition, scale, sum } from "./nutrition";

export type DetectedItem = { ingredient: Ingredient; grams: number };

export type MealAnalysis = {
  title: string;
  titleAr: string;
  confidence: number;
  items: DetectedItem[];
};

export type Flag = {
  level: "danger" | "warn" | "ok" | "info";
  title: string;
  detail: string;
};

/**
 * Mock vision model. In the real app this calls the backend with the photo.
 * For the proof of concept it always recognises the demo meal (masoub).
 */
export async function analyseImage(_uri: string): Promise<MealAnalysis> {
  await new Promise((r) => setTimeout(r, 1400));
  const pick = (id: string): DetectedItem => ({
    ingredient: INGREDIENTS[id],
    grams: INGREDIENTS[id].defaultGrams,
  });
  return {
    title: "Masoub",
    titleAr: "معصوب",
    confidence: 0.91,
    items: [
      pick("tamees"),
      pick("banana"),
      pick("cream"),
      pick("honey"),
      pick("cornflakes"),
      pick("cashew"),
    ],
  };
}

export function totals(items: DetectedItem[]): Nutrition {
  return sum(items.map((i) => scale(i.ingredient.per100g, i.grams)));
}

export function perItem(items: DetectedItem[]) {
  return items.map((i) => ({ ...i, n: scale(i.ingredient.per100g, i.grams) }));
}

export function buildFlags(items: DetectedItem[]): Flag[] {
  const t = totals(items);
  const tags = new Set(items.flatMap((i) => i.ingredient.tags));
  const flags: Flag[] = [];

  if (t.sugar >= 25 || tags.has("high-gi")) {
    flags.push({
      level: "danger",
      title: "Not suitable for diabetics",
      detail: `About ${Math.round(t.sugar)} g of sugar plus fast carbs (bread, honey, corn flakes) may spike blood glucose.`,
    });
  }
  if (t.kcal >= 600) {
    flags.push({
      level: "warn",
      title: "High calorie meal",
      detail: `Roughly ${Math.round((t.kcal / 2000) * 100)}% of a 2000 kcal day in one plate.`,
    });
  }
  if (t.fat >= 25) {
    flags.push({
      level: "warn",
      title: "High in fat",
      detail: "Cream and nuts contribute most of the fat. Reduce the cream to cut it.",
    });
  }
  if (t.sodium >= 600) {
    flags.push({
      level: "info",
      title: "Moderate sodium",
      detail: `About ${Math.round(t.sodium)} mg sodium, mostly from bread and corn flakes.`,
    });
  }
  const allergens: string[] = [];
  if (tags.has("gluten")) allergens.push("gluten");
  if (tags.has("dairy")) allergens.push("dairy");
  if (tags.has("nuts")) allergens.push("tree nuts");
  if (allergens.length) {
    flags.push({
      level: "info",
      title: "Contains allergens",
      detail: `Contains ${allergens.join(", ")}.`,
    });
  }
  if (t.fiber >= 5) {
    flags.push({
      level: "ok",
      title: "Good source of fiber",
      detail: `${Math.round(t.fiber)} g of fiber from banana and bread.`,
    });
  }
  if (t.protein >= 15) {
    flags.push({
      level: "ok",
      title: "Decent protein",
      detail: `${Math.round(t.protein)} g of protein.`,
    });
  }
  return flags;
}

export function healthScore(items: DetectedItem[]): number {
  const t = totals(items);
  let s = 80;
  s -= Math.max(0, t.sugar - 15) * 1.2;
  s -= Math.max(0, t.kcal - 500) / 20;
  s -= Math.max(0, t.fat - 20) * 0.8;
  s += Math.min(t.fiber, 8) * 1.5;
  s += Math.min(t.protein, 25) * 0.4;
  return Math.max(5, Math.min(95, Math.round(s)));
}
