# MealLens

MealLens is a native Android application (Java) that helps users keep track of their meals and make healthier food choices. A photo starts the log; the user confirms the foods and portions, then sees calories, macros and health flags.

## Run the app

Open the project in **Android Studio** (Ladybug or newer, JDK 17+), let Gradle sync, and run the `app` configuration on an emulator or device (minSdk 24, targetSdk 37).

From the command line:

```bash
./gradlew assembleDebug            # build the APK
./gradlew testDebugUnitTest        # run the unit tests
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

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
| Home | Scan a meal (camera), Upload photo (photo picker), Try demo meal, Recent meal, History tab, log out from ⚙ |
| Confirm your meal | Detected dish and confidence, editable grams per ingredient (− / + or typed), remove / add ingredients, live kcal |
| Meal summary | Total kcal, health score, protein / carbs / fat with percentages, sugar / fiber / sodium, health flags, per-ingredient breakdown, Save meal |

Screenshots are in `docs/screens/`.

## Project structure

```
app/src/main/java/com/example/meallens/
  MainActivity.java        login (opens Home directly when a session exists)
  RegisterActivity.java    create account
  HomeActivity.java        home + history tabs, camera / gallery / demo entry points
  ConfirmActivity.java     detected ingredients, portion editing, add / remove
  SummaryActivity.java     calories, score, macros, flags, breakdown, save
  ImageUtil.java           photo loading (downscale + EXIF rotation)
  EdgeToEdge.java          system-bar insets for Android 15+
  auth/AuthService.java    Firebase Auth with local demo fallback
  data/Ingredients.java    per-100 g nutrition table and ingredient tags
  data/Analyzer.java       analyseImage() (mock recognition), totals, flags, health score
  data/MealStore.java      saved meals for the session
app/src/test/.../AnalyzerTest.java   unit tests for totals, flags and score
docs/                      Sprint 2 presentation and screenshots
```

`Analyzer.analyseImage()` is the single integration point for a real food-recognition model: return the same `MealAnalysis` shape and the rest of the app works unchanged.

## Project progress

### Sprint 0 · Project initiation — completed
- Defined the idea, backlog and initial screen designs.

### Sprint 1 · Registration and authentication — completed
- Registration with name, email and password; login with Firebase Authentication.
- Input validation with clear success and error messages.

### Sprint 2 · Photo input, recognition and confirmation — completed
- Home screen with camera / photo-picker input and a demo meal.
- Confirm screen: detected dish, editable ingredients and portions.
- Meal summary: calories, macros, health score, health flags and ingredient breakdown; saved meals and history.
- Login / registration work with Firebase or in a local demo mode.
- Unit tests for the nutrition engine; manual test pass on a Pixel 7 emulator (API 36).
- Presentation: `docs/sprint2/MealLens_Sprint_2.pdf`.

### Next
- Replace the mock recogniser with a real AI vision model.
- Nutrition database lookup, cloud meal history (Firestore), personalised flags.
