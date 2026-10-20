# MealLens

MealLens is a native Android application (Java) that turns a meal photo and an optional caption into an editable ingredient list, a nutrition estimate and practical health insights. Recognition is performed by DeepSeek's vision model; the app calculates portion-scaled calories, macros, health score and flags from the returned per-100 g nutrition values.

## How it works

1. **Upload a photo** from the gallery (upload only — there is no camera capture) and optionally add a caption such as "chicken shawarma wrap, small portion, no fries".
2. **DeepSeek recognises the meal** with the `deepseek-flash` model and returns strict JSON: dish name, confidence, ingredients, estimated grams, per-100 g nutrition, health notes and a short summary.
3. **Confirm your meal** — review the AI result and caption, then edit grams, remove items or add missing ingredients. Calories update live.
4. **Meal summary** — total calories, health score, protein/carbs/fat with percentages, sugar/fiber/sodium, computed flags plus the model's notes, per-ingredient breakdown and **Save meal**.

The model estimates portions; every number is an approximation, not a medical measurement.

## Run the app

Open **this repository root** in Android Studio: `/Users/ahmed/AndroidStudioProjects/MealLens`.

1. Use an Android Studio release supporting Android Gradle Plugin **9.4.1**. The wrapper uses **Gradle 9.6.0**; the committed daemon JVM criteria request **JDK 25** (Java source compatibility is 11).
2. In **SDK Manager**, install **Android SDK Platform 37**, Android SDK Build-Tools and Platform-Tools. Accept any SDK license prompts.
3. Create the API key file described below.
4. Let Gradle sync. If the SDK is not detected, set its location in Android Studio or add `sdk.dir=/your/Android/sdk` to the ignored `local.properties` file. On this Mac the path is `/Users/ahmed/Library/Android/sdk`.
5. Start an emulator from **Device Manager** or connect an Android device with USB debugging enabled (Android 7 / API 24 or later).
6. Select the **app** run configuration, select the device, and click **Run ▶**.

From the command line:

```bash
./gradlew assembleDebug            # build the APK
./gradlew testDebugUnitTest        # run the unit tests (JSON parser + nutrition engine)
./gradlew lintDebug                # Android lint
./gradlew connectedDebugAndroidTest # UI regression tests (running emulator/device required)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## DeepSeek API key

Meal recognition needs a DeepSeek API key.

1. Create `.env` in the repository root (it is git-ignored):

   ```bash
   DEEPSEEK_API_KEY=sk-your-key-here
   ```

   `.env.example` shows the expected shape. You can also export `DEEPSEEK_API_KEY` in the build environment.
2. **Rebuild** after changing the key. Gradle reads `.env` at configuration time and injects the value into `BuildConfig.DEEPSEEK_API_KEY`.
3. Without a key the confirm screen shows a clear error with **Try again**; the rest of the app still runs.

### Model, cost and security

- Model: **`deepseek-flash`** (`https://api.deepseek.com/chat/completions`), the cheap Flash tier. It supports image input via base64, JSON output and a thinking-mode toggle.
- Requests disable thinking, use `detail: "low"`, cap output at 1800 tokens and send a 1024 px JPEG. A typical scan is a fraction of a cent; see [DeepSeek pricing](https://api-docs.deepseek.com/quick_start/pricing).
- **Security warning:** the key is compiled into the APK, so anyone with the APK can extract it. This is acceptable only for this local prototype. For production, put a small backend proxy between the app and DeepSeek, keep the key on the server, and add authentication/rate limiting. Never commit `.env` or share a debug APK containing a real key.
- **Privacy:** the selected photo (downscaled) and the caption are sent to DeepSeek to produce the analysis. Do not upload images you would not share with that service.

The request/response contract and system prompt are documented in [docs/recognition.md](docs/recognition.md).

## Accounts (Firebase Authentication)

Registration and login use Firebase Authentication (the same Firebase project as Sprint 1).

1. Firebase Console → Project settings → Your apps → Android app `com.example.meallens` → download `google-services.json`.
2. Put it at `app/google-services.json` (it is git-ignored).
3. Rebuild. The Google Services plugin is applied automatically when the file exists.

Without the file the app runs in a local **demo auth mode** (accounts stored on the device with SharedPreferences) so the whole flow can be built and tested by anyone. The login screen shows which mode is active.

## Screens

| Screen | What it does |
|---|---|
| Log in / Create account | Email + password, full name on sign up, same validation and messages as Sprint 1 |
| Home | Upload a meal photo, add an optional caption, analyze; recent meal and History tab; log out from ⚙ |
| Confirm your meal | Loading state, error + retry, AI dish/confidence/caption, editable grams per ingredient (− / + or typed), remove / add ingredients, live kcal |
| Meal summary | AI summary, total kcal, health score, protein / carbs / fat with percentages, sugar / fiber / sodium, computed + AI health flags, per-ingredient breakdown, Save meal |

Screenshots: `docs/screens/10-home-upload.png`, `11-confirm-ai.png`, `12-summary-ai.png` and `13-summary-ai-flags.png` show the current upload + AI flow. The older `01`–`09` screenshots are from Sprint 2 and still show the camera entry point.

## Project structure

```
app/src/main/java/com/example/meallens/
  MainActivity.java            login (opens Home directly when a session exists)
  RegisterActivity.java        create account
  HomeActivity.java            upload + caption entry point, recent meals, history
  ConfirmActivity.java         AI result, error/retry, portion editing, add / remove
  SummaryActivity.java         calories, score, macros, flags, breakdown, save
  RecognitionViewModel.java    background recognition that survives rotation
  MealRecognizer.java          DeepSeek HTTP client, image encoding, error mapping
  ImageUtil.java               photo loading (downscale + EXIF rotation)
  EdgeToEdge.java              system-bar insets for Android 15+
  auth/AuthService.java        Firebase Auth with local demo fallback
  data/MealJsonParser.java     strict JSON -> MealAnalysis, with bounds checking
  data/Analyzer.java           totals, flags, health score, test-only demo fixture
  data/Ingredients.java        built-in per-100 g reference table for manual adds
  data/MealStore.java          saved meals for the session
app/src/test/.../AnalyzerTest.java
app/src/test/.../MealJsonParserTest.java
docs/recognition.md            prompt, schema and parsing rules
```

## Recognition contract

The model must return a single JSON object; the parser also tolerates fenced JSON. Nutrition is per **100 g** so portions remain editable:

```json
{
  "dish": { "name_en": "Chicken shawarma plate", "name_ar": "شاورما دجاج", "confidence": 0.87 },
  "ingredients": [{
    "name_en": "Chicken shawarma", "name_ar": "شاورما", "emoji": "🌯", "grams": 120,
    "per_100g": { "kcal": 200, "protein_g": 25, "carbs_g": 5, "fat_g": 10,
                  "fiber_g": 0, "sugar_g": 1, "sodium_mg": 500 },
    "tags": ["gluten"]
  }],
  "health_notes": [{ "level": "warn", "title": "High sodium", "detail": "..." }],
  "summary": "A protein-rich chicken plate with rice."
}
```

The app:

- clamps portions to 1–2000 g and nutrition to sane ranges, and rejects empty/malformed results;
- scales per-100 g values to the portion, then totals them;
- computes a deterministic 5–95 health score and flags from the macros, allergens and tags;
- merges model health notes with the computed flags (de-duplicated by title).

Only tags `gluten`, `dairy`, `nuts`, `high-sugar`, `high-gi`, `vegetarian`, `vegan`, `fried` and `processed` are accepted. The full system prompt is in `MealRecognizer.SYSTEM_PROMPT`.

## Project progress

### Sprint 0 · Project initiation — completed
- Defined the idea, backlog and initial screen designs.

### Sprint 1 · Registration and authentication — completed
- Registration with name, email and password; login with Firebase Authentication.
- Input validation with clear success and error messages.

### Sprint 2 · Photo input, recognition and confirmation — prototype completed
- Home, confirmation and summary flows; nutrition engine, health flags and history.
- Firebase or local demo authentication.

### Sprint 3 · Real AI recognition — implemented
- Upload-only photo + caption flow; camera entry point removed.
- DeepSeek `deepseek-flash` vision integration with strict JSON output and bounded parsing.
- AI dish/confidence, AI summary and health notes surfaced in the UI.
- Loading, failure and retry states; recognition survives activity recreation without duplicate requests.
- Unit tests for the parser and nutrition engine; UI regression tests use a local demo fixture.

**Limits:** portions and nutrition are model estimates and can be wrong, especially for mixed dishes and unusual portions. The API key is embedded in the APK (prototype only). Saved meal history is in memory for the current app process and clears on logout or process termination. Firebase authentication requires your own `app/google-services.json`; otherwise accounts are local demo accounts.

### Next
- Move the API key to a backend proxy and add per-user rate limits.
- Persist meals in Firestore and sync across devices.
- Improve recognition with multi-photo input and a verified nutrition database lookup.
- Personalised health flags based on user goals and allergies.
