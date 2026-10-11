# MealLens

MealLens is an Android application written in Java. The current project includes Firebase Authentication for account registration and sign-in.

## Requirements

- Android Studio
- JDK 11 or newer
- An Android SDK compatible with the versions configured in `app/build.gradle.kts`
- A Firebase project with Android package name `com.example.meallens`

## Setup

1. Clone or download this repository.
2. In the Firebase console, register an Android app using package name `com.example.meallens`.
3. Download `google-services.json` and place it in the `app/` directory.
4. Enable Email/Password authentication in Firebase Authentication.
5. Open the project in Android Studio, let Gradle sync, then run the `app` configuration.

`google-services.json`, local Android SDK paths, IDE settings, build outputs, and signing files are intentionally excluded from version control.
