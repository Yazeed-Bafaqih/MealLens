import React from "react";
import { Alert, Image, Platform, Pressable, ScrollView, StyleSheet, Text, View } from "react-native";
import * as ImagePicker from "expo-image-picker";
import { Card } from "../components/ui";
import { colors, radius, spacing } from "../theme";
import { SavedMeal } from "../types";

const DEMO_IMAGE = require("../../assets/images/masoub.webp");

type Props = {
  userName: string;
  saved: SavedMeal[];
  onPicked: (uri: string | number) => void;
  onOpenSaved: (meal: SavedMeal) => void;
  onLogout: () => void;
};

function greeting() {
  const h = new Date().getHours();
  if (h < 12) return "Good morning,";
  if (h < 18) return "Good afternoon,";
  return "Good evening,";
}

export function HomeScreen({ userName, saved, onPicked, onOpenSaved, onLogout }: Props) {
  const [tab, setTab] = React.useState<"home" | "history">("home");

  function confirmLogout() {
    if (Platform.OS === "web") {
      if (window.confirm("Log out of MealLens?")) onLogout();
      return;
    }
    Alert.alert("Settings", `Signed in as ${userName}`, [
      { text: "Cancel", style: "cancel" },
      { text: "Log out", style: "destructive", onPress: onLogout },
    ]);
  }

  async function scan() {
    const perm = await ImagePicker.requestCameraPermissionsAsync();
    if (!perm.granted) return onPicked(DEMO_IMAGE);
    const res = await ImagePicker.launchCameraAsync({ quality: 0.7 });
    if (res.canceled) return;
    onPicked(res.assets[0].uri);
  }

  async function upload() {
    const res = await ImagePicker.launchImageLibraryAsync({ quality: 0.7, mediaTypes: ["images"] });
    if (res.canceled) return;
    onPicked(res.assets[0].uri);
  }

  return (
    <View style={styles.root}>
      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.topRow}>
          <View>
            <Text style={styles.greet}>{greeting()}</Text>
            <Text style={styles.name}>{userName}</Text>
          </View>
          <Pressable onPress={confirmLogout} style={styles.gear} hitSlop={8}>
            <Text style={{ fontSize: 18 }}>⚙️</Text>
          </Pressable>
        </View>
        <Text style={styles.tagline}>Small choices make a big difference for a healthier you.</Text>

        {tab === "home" ? (
          <>
            <Text style={styles.section}>Your meals</Text>
            <Pressable onPress={scan} style={({ pressed }) => [styles.scan, pressed && { opacity: 0.9 }]}>
              <View style={styles.scanIcon}>
                <Text style={{ fontSize: 30 }}>📷</Text>
              </View>
              <View style={{ flex: 1 }}>
                <Text style={styles.scanTitle}>Scan a meal</Text>
                <Text style={styles.scanSub}>Take a photo to analyse your food</Text>
              </View>
            </Pressable>
            <View style={styles.row}>
              <Pressable onPress={upload} hitSlop={8}>
                <Text style={styles.link}>Upload photo</Text>
              </Pressable>
              <Text style={styles.dot}>·</Text>
              <Pressable onPress={() => onPicked(DEMO_IMAGE)} hitSlop={8}>
                <Text style={styles.link}>Try demo meal</Text>
              </Pressable>
            </View>

            <View style={styles.divider} />
            <Text style={styles.section}>Recent meal</Text>
            {saved.length === 0 ? (
              <Card>
                <Text style={styles.empty}>No meals yet. Scan your first meal to see calories, macros and health flags.</Text>
              </Card>
            ) : (
              <RecentCard meal={saved[0]} onPress={() => onOpenSaved(saved[0])} />
            )}
          </>
        ) : (
          <>
            <Text style={styles.section}>History</Text>
            {saved.length === 0 ? (
              <Card>
                <Text style={styles.empty}>Saved meals will appear here.</Text>
              </Card>
            ) : (
              saved.map((m) => <RecentCard key={m.id} meal={m} onPress={() => onOpenSaved(m)} />)
            )}
          </>
        )}
      </ScrollView>

      <View style={styles.tabs}>
        <Tab label="Home" icon="🏠" active={tab === "home"} onPress={() => setTab("home")} />
        <Tab label="History" icon="🕒" active={tab === "history"} onPress={() => setTab("history")} />
      </View>
    </View>
  );
}

function RecentCard({ meal, onPress }: { meal: SavedMeal; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.recent, pressed && { opacity: 0.9 }]}>
      <Image source={typeof meal.image === "string" ? { uri: meal.image } : meal.image} style={styles.recentImg} />
      <View style={{ flex: 1 }}>
        <Text style={styles.recentTitle}>{meal.analysis.title}</Text>
        <Text style={styles.recentSub} numberOfLines={2}>
          {meal.analysis.items.map((i) => i.ingredient.name).join(", ")}
        </Text>
        <Text style={styles.recentMeta}>
          {Math.round(meal.kcal)} kcal · {meal.time}
        </Text>
      </View>
      <Text style={styles.chev}>›</Text>
    </Pressable>
  );
}

function Tab({ label, icon, active, onPress }: { label: string; icon: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable onPress={onPress} style={styles.tab}>
      <Text style={{ fontSize: 20, opacity: active ? 1 : 0.5 }}>{icon}</Text>
      <Text style={[styles.tabLabel, active && { color: colors.primary, fontWeight: "600" }]}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.lg, paddingBottom: 120 },
  topRow: { flexDirection: "row", justifyContent: "space-between", alignItems: "flex-start" },
  greet: { fontSize: 22, color: colors.text },
  name: { fontSize: 30, fontWeight: "700", color: colors.text, marginTop: -2 },
  gear: { width: 40, height: 40, alignItems: "center", justifyContent: "center" },
  tagline: { color: colors.muted, marginTop: spacing.sm, fontSize: 15, lineHeight: 21 },
  section: { fontSize: 21, fontWeight: "700", color: colors.text, marginTop: spacing.xl, marginBottom: spacing.md },
  scan: {
    backgroundColor: colors.primary,
    borderRadius: radius.lg,
    padding: spacing.lg,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
  },
  scanIcon: { width: 64, height: 64, borderRadius: 18, backgroundColor: "rgba(255,255,255,0.15)", alignItems: "center", justifyContent: "center" },
  scanTitle: { color: "#fff", fontSize: 22, fontWeight: "700" },
  scanSub: { color: "rgba(255,255,255,0.85)", marginTop: 2, fontSize: 14 },
  row: { flexDirection: "row", justifyContent: "center", alignItems: "center", marginTop: spacing.md, gap: 10 },
  link: { color: colors.primary, fontWeight: "600", textDecorationLine: "underline", fontSize: 15 },
  dot: { color: colors.muted },
  divider: { height: 1, backgroundColor: colors.border, marginTop: spacing.lg },
  empty: { color: colors.muted, lineHeight: 20 },
  recent: {
    backgroundColor: colors.card,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.sm,
    flexDirection: "row",
    alignItems: "center",
    gap: spacing.md,
    marginBottom: spacing.sm,
  },
  recentImg: { width: 92, height: 92, borderRadius: 12, backgroundColor: colors.primarySoft },
  recentTitle: { fontSize: 17, fontWeight: "700", color: colors.text },
  recentSub: { color: colors.muted, marginTop: 2, fontSize: 13 },
  recentMeta: { color: colors.muted, marginTop: 6, fontSize: 12 },
  chev: { fontSize: 26, color: colors.muted, paddingHorizontal: 6 },
  tabs: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    flexDirection: "row",
    backgroundColor: colors.card,
    borderTopWidth: 1,
    borderTopColor: colors.border,
    paddingBottom: 24,
    paddingTop: 8,
  },
  tab: { flex: 1, alignItems: "center", gap: 2 },
  tabLabel: { fontSize: 12, color: colors.muted },
});
