// Per-100g nutrition reference used by the mock analyser.
// Values are approximate, taken from USDA-style tables.
export type Nutrition = {
  kcal: number;
  protein: number;
  carbs: number;
  fat: number;
  fiber: number;
  sugar: number;
  sodium: number; // mg
};

export type Ingredient = {
  id: string;
  name: string;
  nameAr: string;
  emoji: string;
  defaultGrams: number;
  per100g: Nutrition;
  tags: ("gluten" | "dairy" | "nuts" | "high-sugar" | "high-gi" | "fried")[];
};

export const INGREDIENTS: Record<string, Ingredient> = {
  tamees: {
    id: "tamees",
    name: "Tamees bread",
    nameAr: "خبز تميس",
    emoji: "🫓",
    defaultGrams: 90,
    per100g: { kcal: 270, protein: 9, carbs: 52, fat: 3, fiber: 2, sugar: 2, sodium: 450 },
    tags: ["gluten", "high-gi"],
  },
  banana: {
    id: "banana",
    name: "Banana",
    nameAr: "موز",
    emoji: "🍌",
    defaultGrams: 120,
    per100g: { kcal: 89, protein: 1.1, carbs: 23, fat: 0.3, fiber: 2.6, sugar: 12, sodium: 1 },
    tags: ["high-sugar"],
  },
  cream: {
    id: "cream",
    name: "Cream (qishta)",
    nameAr: "قشطة",
    emoji: "🥛",
    defaultGrams: 40,
    per100g: { kcal: 340, protein: 2.5, carbs: 3, fat: 36, fiber: 0, sugar: 3, sodium: 35 },
    tags: ["dairy"],
  },
  honey: {
    id: "honey",
    name: "Honey",
    nameAr: "عسل",
    emoji: "🍯",
    defaultGrams: 20,
    per100g: { kcal: 304, protein: 0.3, carbs: 82, fat: 0, fiber: 0, sugar: 82, sodium: 4 },
    tags: ["high-sugar", "high-gi"],
  },
  cornflakes: {
    id: "cornflakes",
    name: "Corn flakes",
    nameAr: "كورن فليكس",
    emoji: "🥣",
    defaultGrams: 25,
    per100g: { kcal: 357, protein: 7, carbs: 84, fat: 0.4, fiber: 3, sugar: 8, sodium: 729 },
    tags: ["high-gi"],
  },
  cashew: {
    id: "cashew",
    name: "Cashews",
    nameAr: "كاجو",
    emoji: "🥜",
    defaultGrams: 20,
    per100g: { kcal: 553, protein: 18, carbs: 30, fat: 44, fiber: 3.3, sugar: 6, sodium: 12 },
    tags: ["nuts"],
  },
};

export function scale(n: Nutrition, grams: number): Nutrition {
  const f = grams / 100;
  return {
    kcal: n.kcal * f,
    protein: n.protein * f,
    carbs: n.carbs * f,
    fat: n.fat * f,
    fiber: n.fiber * f,
    sugar: n.sugar * f,
    sodium: n.sodium * f,
  };
}

export function sum(items: Nutrition[]): Nutrition {
  return items.reduce(
    (a, b) => ({
      kcal: a.kcal + b.kcal,
      protein: a.protein + b.protein,
      carbs: a.carbs + b.carbs,
      fat: a.fat + b.fat,
      fiber: a.fiber + b.fiber,
      sugar: a.sugar + b.sugar,
      sodium: a.sodium + b.sodium,
    }),
    { kcal: 0, protein: 0, carbs: 0, fat: 0, fiber: 0, sugar: 0, sodium: 0 }
  );
}
