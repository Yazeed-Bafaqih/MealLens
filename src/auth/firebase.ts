// Firebase bootstrap. Reads the web config from EXPO_PUBLIC_FIREBASE_* variables
// (see .env.example). When no config is present, `firebaseAuth` is null and the
// app falls back to a local demo auth so the flow can still be tested.
import { Platform } from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { getApps, initializeApp } from "firebase/app";
import type { Auth } from "firebase/auth";

const config = {
  apiKey: process.env.EXPO_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.EXPO_PUBLIC_FIREBASE_PROJECT_ID,
  appId: process.env.EXPO_PUBLIC_FIREBASE_APP_ID,
};

export const isFirebaseConfigured = Boolean(config.apiKey && config.projectId);

function createAuth(): Auth | null {
  if (!isFirebaseConfigured) return null;
  const app = getApps()[0] ?? initializeApp(config);
  // eslint-disable-next-line @typescript-eslint/no-require-imports
  const authMod = require("firebase/auth");
  if (Platform.OS === "web") return authMod.getAuth(app);
  return authMod.initializeAuth(app, {
    persistence: authMod.getReactNativePersistence(AsyncStorage),
  });
}

export const firebaseAuth: Auth | null = createAuth();
