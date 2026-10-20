# MealLens

MealLens is a mobile application that helps users keep track of their meals and make healthier food choices. A photo starts the log; the user confirms the foods and portions, then sees calories, macros and health flags.

The app is built with **Expo (React Native + TypeScript)** and runs on iOS, Android and the web from one codebase.

## Run the app

```bash
npm install
npx expo start          # press i (iOS Simulator), a (Android), w (web)
```

On a real phone install **Expo Go** and scan the QR code printed by `npx expo start`.

## Accounts (Firebase Authentication)

Registration and login use Firebase Authentication, the same Firebase project as Sprint 1, so existing accounts keep working.

1. Firebase Console → Project settings → Your apps → add a **Web app** (or open the existing one).
2. Copy `.env.example` to `.env` and fill in the keys.
3. Restart `npx expo start`.

Without a `.env` the app runs in a local **demo auth mode** (accounts stored on the device) so the full flow can still be tested.

## Screens

| Screen | What it does |
|---|---|
| Log in / Create account | Email + password, name on sign up, same validation and messages as Sprint 1 |
| Home | Scan a meal (camera), Upload photo, demo meal, recent meal, History tab, log out from ⚙ |
| Confirm your meal | Detected dish and confidence, editable grams per ingredient, add / remove ingredients, live kcal |
| Meal summary | Total kcal, health score, protein / carbs / fat, sugar / fiber / sodium, health flags, per-ingredient breakdown, Save meal |

## Project structure

```
App.tsx                 navigation and auth gate
src/auth/               Firebase bootstrap + auth service (demo fallback)
src/screens/            AuthScreens, HomeScreen, ConfirmScreen, SummaryScreen
src/data/nutrition.ts   per-100 g nutrition table and ingredient tags
src/data/analyzer.ts    analyseImage() (mock recognition), totals, flags, score
src/components/ui.tsx   Button, Card, Header, Pill
docs/                   Sprint 2 presentation and screenshots
android-native/         Sprint 1 Android (Java + Firebase) prototype, kept for reference
```

`analyseImage()` in `src/data/analyzer.ts` is the single integration point for a real food-recognition model: return the same `MealAnalysis` shape and the rest of the app works unchanged.

## Project progress

### Sprint 0 · Project initiation — completed
- Defined the idea, backlog and initial screen designs.

### Sprint 1 · Registration and authentication — completed
- Registration with name, email and password; login with Firebase Authentication.
- Input validation with clear success and error messages.

### Sprint 2 · Photo input, recognition and confirmation — completed
- Moved the app to Expo (React Native) so it runs on iOS, Android and the web.
- Ported the login / registration screens and connected them to Firebase Auth (with a demo fallback).
- Home screen with camera / gallery input and a demo meal.
- Confirm screen: detected dish, editable ingredients and portions.
- Meal summary: calories, macros, health score, health flags and ingredient breakdown; saved meals and history.
- Presentation: `docs/sprint2/MealLens_Sprint_2.pdf`.

### Next
- Replace the mock recogniser with a real AI vision model.
- Nutrition database lookup, cloud meal history (Firestore), personalised flags.
