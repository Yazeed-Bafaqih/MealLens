import React from "react";
import { ActivityIndicator, Image, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { Button, Card, Header, Pill } from "../components/ui";
import { colors, radius, spacing } from "../theme";
import { DetectedItem, MealAnalysis, analyseImage, totals } from "../data/analyzer";
import { INGREDIENTS } from "../data/nutrition";

type Props = {
  image: string | number;
  onBack: () => void;
  onEstimate: (analysis: MealAnalysis) => void;
};

export function ConfirmScreen({ image, onBack, onEstimate }: Props) {
  const [analysis, setAnalysis] = React.useState<MealAnalysis | null>(null);
  const [items, setItems] = React.useState<DetectedItem[]>([]);
  const [adding, setAdding] = React.useState(false);

  React.useEffect(() => {
    let alive = true;
    analyseImage(typeof image === "string" ? image : "demo").then((a) => {
      if (!alive) return;
      setAnalysis(a);
      setItems(a.items);
    });
    return () => {
      alive = false;
    };
  }, [image]);

  function setGrams(idx: number, grams: number) {
    setItems((prev) => prev.map((it, i) => (i === idx ? { ...it, grams: Math.max(0, Math.round(grams)) } : it)));
  }
  function remove(idx: number) {
    setItems((prev) => prev.filter((_, i) => i !== idx));
  }
  function add(id: string) {
    const ing = INGREDIENTS[id];
    setItems((prev) => [...prev, { ingredient: ing, grams: ing.defaultGrams }]);
    setAdding(false);
  }

  const missing = Object.values(INGREDIENTS).filter((ing) => !items.some((i) => i.ingredient.id === ing.id));
  const liveKcal = Math.round(totals(items).kcal);

  return (
    <View style={styles.root}>
      <Header title="Confirm your meal" onBack={onBack} />
      <ScrollView contentContainerStyle={styles.content}>
        <Image source={typeof image === "string" ? { uri: image } : image} style={styles.hero} resizeMode="contain" />

        {!analysis ? (
          <View style={styles.loading}>
            <ActivityIndicator color={colors.primary} />
            <Text style={styles.loadingText}>Analysing your photo…</Text>
          </View>
        ) : (
          <>
            <View style={styles.titleRow}>
              <View style={{ flex: 1 }}>
                <Text style={styles.detectedLabel}>Detected</Text>
                <Text style={styles.detected}>
                  {analysis.title} <Text style={styles.detectedAr}>{analysis.titleAr}</Text>
                </Text>
              </View>
              <Pill text={`${Math.round(analysis.confidence * 100)}% match`} tone="ok" />
            </View>

            <View style={styles.sectionRow}>
              <Text style={styles.section}>Edit items and portions</Text>
              <Text style={styles.live}>{liveKcal} kcal</Text>
            </View>

            {items.map((it, idx) => (
              <ItemRow key={it.ingredient.id} item={it} onGrams={(g) => setGrams(idx, g)} onRemove={() => remove(idx)} />
            ))}

            {adding ? (
              <Card style={{ marginTop: spacing.sm }}>
                <Text style={styles.addTitle}>Add an ingredient</Text>
                {missing.length === 0 ? (
                  <Text style={{ color: colors.muted }}>All known ingredients are already in the list.</Text>
                ) : (
                  missing.map((ing) => (
                    <Pressable key={ing.id} onPress={() => add(ing.id)} style={styles.addRow}>
                      <Text style={{ fontSize: 20 }}>{ing.emoji}</Text>
                      <Text style={styles.addName}>{ing.name}</Text>
                      <Text style={styles.addPlus}>+</Text>
                    </Pressable>
                  ))
                )}
                <Pressable onPress={() => setAdding(false)} style={{ marginTop: spacing.sm }}>
                  <Text style={styles.cancel}>Cancel</Text>
                </Pressable>
              </Card>
            ) : (
              <Pressable onPress={() => setAdding(true)} style={styles.addBtn}>
                <Text style={styles.addBtnText}>+ Add missing ingredient</Text>
              </Pressable>
            )}
          </>
        )}
      </ScrollView>

      <View style={styles.footer}>
        <Button label="Estimate nutrition" disabled={!analysis || items.length === 0} onPress={() => analysis && onEstimate({ ...analysis, items })} />
      </View>
    </View>
  );
}

function ItemRow({ item, onGrams, onRemove }: { item: DetectedItem; onGrams: (g: number) => void; onRemove: () => void }) {
  const [text, setText] = React.useState(String(item.grams));
  React.useEffect(() => setText(String(item.grams)), [item.grams]);
  const step = item.grams >= 100 ? 10 : 5;

  return (
    <View style={styles.item}>
      <View style={styles.itemIcon}>
        <Text style={{ fontSize: 22 }}>{item.ingredient.emoji}</Text>
      </View>
      <View style={styles.itemText}>
        <Text style={styles.itemName} numberOfLines={1}>{item.ingredient.name}</Text>
        <Text style={styles.itemSub}>{item.ingredient.nameAr}</Text>
      </View>
      <View style={styles.stepper}>
        <Pressable onPress={() => onGrams(item.grams - step)} style={styles.stepBtn} hitSlop={6}>
          <Text style={styles.stepText}>−</Text>
        </Pressable>
        <TextInput
          value={text}
          onChangeText={setText}
          onEndEditing={() => onGrams(Number(text) || 0)}
          keyboardType="number-pad"
          style={styles.input}
          selectTextOnFocus
        />
        <Text style={styles.unit}>g</Text>
        <Pressable onPress={() => onGrams(item.grams + step)} style={styles.stepBtn} hitSlop={6}>
          <Text style={styles.stepText}>+</Text>
        </Pressable>
      </View>
      <Pressable onPress={onRemove} hitSlop={10} style={styles.remove}>
        <Text style={styles.removeText}>✕</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: 120 },
  hero: { width: "100%", height: 240, borderRadius: radius.lg, backgroundColor: "#fff" },
  loading: { alignItems: "center", paddingVertical: spacing.xl, gap: spacing.sm },
  loadingText: { color: colors.muted },
  titleRow: { flexDirection: "row", alignItems: "center", marginTop: spacing.md, gap: spacing.sm },
  detectedLabel: { color: colors.muted, fontSize: 12, textTransform: "uppercase", letterSpacing: 0.6 },
  detected: { fontSize: 22, fontWeight: "700", color: colors.text },
  detectedAr: { fontSize: 16, color: colors.muted, fontWeight: "500" },
  sectionRow: { flexDirection: "row", alignItems: "baseline", justifyContent: "space-between", marginTop: spacing.lg, marginBottom: spacing.sm },
  section: { fontSize: 17, fontWeight: "700", color: colors.text },
  live: { color: colors.primary, fontWeight: "700" },
  item: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing.sm,
    marginBottom: spacing.sm,
    gap: spacing.sm,
  },
  itemIcon: { width: 42, height: 42, borderRadius: 12, backgroundColor: colors.primarySoft, alignItems: "center", justifyContent: "center" },
  itemText: { flex: 1, minWidth: 0, flexShrink: 1 },
  itemName: { fontSize: 15, fontWeight: "600", color: colors.text },
  itemSub: { fontSize: 12, color: colors.muted, textAlign: "left", writingDirection: "ltr" },
  stepper: { flexDirection: "row", alignItems: "center", flexShrink: 0, backgroundColor: colors.bg, borderRadius: 10, paddingHorizontal: 4 },
  stepBtn: { width: 30, height: 32, alignItems: "center", justifyContent: "center" },
  stepText: { fontSize: 18, color: colors.primary, fontWeight: "700" },
  input: { width: 46, textAlign: "right", fontSize: 15, fontWeight: "600", color: colors.text, paddingVertical: 4 },
  unit: { color: colors.muted, marginLeft: 2, fontSize: 13 },
  remove: { paddingHorizontal: 4 },
  removeText: { color: colors.muted, fontSize: 14 },
  addBtn: { alignItems: "center", paddingVertical: spacing.sm },
  addBtnText: { color: colors.primary, fontWeight: "600" },
  addTitle: { fontWeight: "700", marginBottom: spacing.sm, color: colors.text },
  addRow: { flexDirection: "row", alignItems: "center", gap: spacing.sm, paddingVertical: 8 },
  addName: { flex: 1, color: colors.text },
  addPlus: { color: colors.primary, fontSize: 20, fontWeight: "700" },
  cancel: { color: colors.muted, textAlign: "center" },
  footer: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    padding: spacing.md,
    paddingBottom: 28,
    backgroundColor: colors.bg,
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
});
