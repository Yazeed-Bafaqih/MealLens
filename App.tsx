import React from "react";
import { ActivityIndicator, Animated, Platform, StyleSheet, Text, View } from "react-native";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context";
import { HomeScreen } from "./src/screens/HomeScreen";
import { ConfirmScreen } from "./src/screens/ConfirmScreen";
import { SummaryScreen } from "./src/screens/SummaryScreen";
import { LoginScreen, RegisterScreen } from "./src/screens/AuthScreens";
import { MealAnalysis, totals } from "./src/data/analyzer";
import { AuthUser, logout, onAuthChange } from "./src/auth/authService";
import { SavedMeal } from "./src/types";
import { colors } from "./src/theme";

type Route =
  | { name: "login" }
  | { name: "register" }
  | { name: "home" }
  | { name: "confirm"; image: string | number }
  | { name: "summary"; image: string | number; analysis: MealAnalysis; readOnly?: boolean };

export default function App() {
  const [user, setUser] = React.useState<AuthUser | null | undefined>(undefined);
  const [route, setRoute] = React.useState<Route>({ name: "login" });
  const [saved, setSaved] = React.useState<SavedMeal[]>([]);
  const [toast, setToast] = React.useState<string | null>(null);

  React.useEffect(() => {
    return onAuthChange((u) => {
      setUser(u);
      setRoute(u ? { name: "home" } : { name: "login" });
    });
  }, []);

  function showToast(message: string) {
    setToast(message);
    setTimeout(() => setToast(null), 2200);
  }

  function onAuthed(u: AuthUser, message: string) {
    setUser(u);
    setRoute({ name: "home" });
    showToast(message);
  }

  async function onLogout() {
    await logout();
    setUser(null);
    setSaved([]);
    setRoute({ name: "login" });
  }

  function save(image: string | number, analysis: MealAnalysis) {
    const now = new Date();
    const meal: SavedMeal = {
      id: String(now.getTime()),
      image,
      analysis,
      kcal: totals(analysis.items).kcal,
      time: `Today, ${now.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}`,
    };
    setSaved((s) => [meal, ...s]);
    setRoute({ name: "home" });
    showToast("Meal saved");
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={{ flex: 1, backgroundColor: colors.bg }} edges={["top"]}>
        <StatusBar style="dark" />
        {user === undefined && (
          <View style={styles.center}>
            <ActivityIndicator color={colors.primary} />
          </View>
        )}
        {user !== undefined && route.name === "login" && <LoginScreen onAuthed={onAuthed} onCreateAccount={() => setRoute({ name: "register" })} />}
        {user !== undefined && route.name === "register" && <RegisterScreen onAuthed={onAuthed} onLogin={() => setRoute({ name: "login" })} />}
        {user && route.name === "home" && (
          <HomeScreen
            userName={user.displayName || "User"}
            saved={saved}
            onPicked={(image) => setRoute({ name: "confirm", image })}
            onOpenSaved={(m) => setRoute({ name: "summary", image: m.image, analysis: m.analysis, readOnly: true })}
            onLogout={onLogout}
          />
        )}
        {user && route.name === "confirm" && (
          <ConfirmScreen
            image={route.image}
            onBack={() => setRoute({ name: "home" })}
            onEstimate={(analysis) => setRoute({ name: "summary", image: route.image, analysis })}
          />
        )}
        {user && route.name === "summary" && (
          <SummaryScreen
            image={route.image}
            analysis={route.analysis}
            readOnly={route.readOnly}
            onBack={() => (route.readOnly ? setRoute({ name: "home" }) : setRoute({ name: "confirm", image: route.image }))}
            onSave={() => save(route.image, route.analysis)}
          />
        )}
        {toast ? <Toast message={toast} /> : null}
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

function Toast({ message }: { message: string }) {
  const opacity = React.useRef(new Animated.Value(0)).current;
  React.useEffect(() => {
    Animated.timing(opacity, { toValue: 1, duration: 180, useNativeDriver: Platform.OS !== "web" }).start();
  }, [opacity]);
  return (
    <Animated.View style={[styles.toast, { opacity }]} pointerEvents="none">
      <Text style={styles.toastDot}>●</Text>
      <Text style={styles.toastText}>{message}</Text>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  center: { flex: 1, alignItems: "center", justifyContent: "center" },
  toast: {
    position: "absolute",
    bottom: 110,
    alignSelf: "center",
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    backgroundColor: "#fff",
    borderRadius: 999,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: colors.border,
    shadowColor: "#000",
    shadowOpacity: 0.08,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 4 },
    elevation: 3,
  },
  toastDot: { color: colors.primary, fontSize: 10 },
  toastText: { color: colors.text, fontWeight: "600", fontSize: 13 },
});
