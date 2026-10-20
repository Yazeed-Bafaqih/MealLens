# Meal recognition (DeepSeek vision)

MealLens sends one photo plus the user's caption to DeepSeek's OpenAI-compatible
Chat Completions endpoint and asks for a single JSON object. The app never sends
the user's account details.

## Request

- Endpoint: `https://api.deepseek.com/chat/completions`
- Model: `deepseek-flash` (cheap Flash tier; supports image input)
- Image: JPEG, longest side 1024 px, quality 84, inline base64 data URL, `detail: "low"`
- `response_format: {"type": "json_object"}`
- `thinking: {"type": "disabled"}` for lower latency/cost
- `max_tokens: 1800`, `temperature: 0.2`
- System prompt is stored in `MealRecognizer.SYSTEM_PROMPT`

Example user content:

```json
[
  { "type": "text", "text": "Analyze this meal photo.\n\nUser caption: \"chicken shawarma, small portion\"\n\nReturn the JSON object described in the system prompt." },
  { "type": "image_url", "image_url": { "url": "data:image/jpeg;base64,...", "detail": "low" } }
]
```

## System prompt

```text
You are MealLens, a careful nutrition-vision assistant. Identify the meal in the photo and break it into the visible ingredients. The user's caption is context, not a source of nutrition facts.

Rules:
- Identify only foods you can reasonably see or infer from the caption. If unsure, lower the confidence instead of inventing detail.
- Estimate the edible portion in grams for each ingredient as served. Use realistic portions; ground estimates in visual size, count, and the plate or utensils.
- Return nutrition per 100 g of each ingredient, not for the portion. Use standard reference values (USDA-style) for edible portions.
- kcal in kcal; protein, carbs, fat, fiber and sugar in grams; sodium in milligrams. Values must be non-negative numbers. Ensure kcal roughly agrees with macros (4/4/9), within reason.
- Use tags only from: gluten, dairy, nuts, high-sugar, high-gi, vegetarian, vegan, fried, processed. Add only tags that genuinely apply.
- health_notes: 2-4 short, practical notes grounded in the identified food and nutrition. Use levels info, ok, warn, danger. Do not give medical diagnoses or claim certainty. Mention allergens only when relevant.
- summary: one or two plain sentences describing the meal and its main nutritional trade-off.
- ingredient names: concise English name, Arabic name if you know it (otherwise empty string), and one food emoji. Use unique English names. Return at most 12 ingredients.
- If the photo contains no food, set dish.name_en to "No meal detected", confidence 0, ingredients [], and explain in summary.
- Return ONLY valid JSON matching the schema exactly. No markdown, no code fences, no extra keys.

JSON schema:
{
  "dish": {"name_en": string, "name_ar": string, "confidence": number between 0 and 1},
  "ingredients": [
    {
      "name_en": string,
      "name_ar": string,
      "emoji": string,
      "grams": number between 1 and 2000,
      "per_100g": {"kcal": number, "protein_g": number, "carbs_g": number, "fat_g": number, "fiber_g": number, "sugar_g": number, "sodium_mg": number},
      "tags": [string]
    }
  ],
  "health_notes": [{"level": "info|ok|warn|danger", "title": string, "detail": string}],
  "summary": string
}
```

## Response handling

`MealJsonParser` is Android-free and unit tested:

- strips Markdown fences or surrounding prose and parses the first `{...}` object;
- rejects empty ingredient lists and `"No meal detected"` with a user-facing message;
- clamps confidence to 0–1, grams to 1–2000, kcal to 0–900/100 g, macros to 0–100 g/100 g and sodium to 0–5000 mg/100 g;
- keeps only known tags and at most 12 ingredients / 6 health notes;
- tolerates missing text fields with safe defaults.

The UI scales per-100 g nutrition to the estimated portion. Totals, health score and
most flags are then computed deterministically in `Analyzer`; model `health_notes`
are merged in after the computed flags, de-duplicated by title.

## Failure behaviour

- Missing key: prompts the user to add `DEEPSEEK_API_KEY` to `.env` and rebuild.
- 401/402/429/5xx: short actionable messages; no raw key or stack trace is shown.
- Empty JSON-mode content: one automatic retry.
- Invalid/empty JSON: error card with **Try again**.
- Rotation during a request: `RecognitionViewModel` keeps the request and result, so
  the photo is not sent twice.
