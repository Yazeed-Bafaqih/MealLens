import React from "react";
import { Image, ScrollView, StyleSheet, Text, View } from "react-native";
import { Button, Card, Header, Pill } from "../components/ui";
import { colors, radius, spacing } from "../theme";
import { Flag, MealAnalysis, buildFlags, healthScore, perItem, totals } from "../data/analyzer";

type Props = {
  image: string | number;
  analysis: MealAnalysis;
  readOnly?: boolean;
  onBack: () => void;
  onSave: () => void;
};

export function SummaryScreen({ image, analysis, readOnly, onBack, onSave }: Props) {
  const t = totals(analysis.items);
  const flags = buildFlags(analysis.items);
  const score = healthScore(analysis.items);
  const rows = perItem(analysis.items).sort((a, b) => b.n.kcal - a.n.kcal);
  const macroKcal = t.protein * 4 + t.carbs * 4 + t.fat * 9 || 1;

  return (
    <View style={styles.root}>
      <Header title="Meal summary" onBack={onBack} />
      <ScrollView contentContainerStyle={styles.content}>
        <Image source={typeof image === "string" ? { uri: image } : image} style={styles.hero} resizeMode="contain" />

        <View style={styles.kcalBlock}>
          <Text style={styles.kcal}>{Math.round(t.kcal)}</Text>
          <Text style={styles.kcalUnit}>kcal</Text>
        </View>
        <Text style={styles.estimated}>
          Estimated for {analysis.title} · {analysis.items.reduce((a, i) => a + i.grams, 0)} g total
        </Text>

        <View style={styles.scoreRow}>
          <ScoreRing score={score} />
          <View style={{ flex: 1 }}>
            <Text style={styles.scoreTitle}>Health score</Text>
            <Text style={styles.scoreSub}>
              {score >= 70 ? "Balanced meal." : score >= 45 ? "Enjoy occasionally. High in sugar and fat." : "Treat meal. Best kept small and infrequent."}
            </Text>
          </View>
        </View>

        <Card style={{ marginTop: spacing.lg }}>
          <View style={styles.macroBar}>
            <View style={[styles.macroSeg, { flex: (t.protein * 4) / macroKcal, backgroundColor: colors.primary }]} />
            <View style={[styles.macroSeg, { flex: (t.carbs * 4) / macroKcal, backgroundColor: colors.warn }]} />
            <View style={[styles.macroSeg, { flex: (t.fat * 9) / macroKcal, backgroundColor: colors.info }]} />
          </View>
          <MacroRow icon="🏋️" label="Protein" value={t.protein} color={colors.primary} pct={(t.protein * 4) / macroKcal} />
          <MacroRow icon="🌾" label="Carbs" value={t.carbs} color={colors.warn} pct={(t.carbs * 4) / macroKcal} />
          <MacroRow icon="💧" label="Fat" value={t.fat} color={colors.info} pct={(t.fat * 9) / macroKcal} last />
          <View style={styles.minorRow}>
            <Minor label="Sugar" value={`${Math.round(t.sugar)} g`} />
            <Minor label="Fiber" value={`${Math.round(t.fiber)} g`} />
            <Minor label="Sodium" value={`${Math.round(t.sodium)} mg`} />
          </View>
        </Card>

        <Text style={styles.section}>Health flags</Text>
        {flags.map((f, i) => (
          <FlagRow key={i} flag={f} />
        ))}

        <Text style={styles.section}>Ingredient breakdown</Text>
        <Card style={{ padding: 0 }}>
          {rows.map((r, i) => (
            <View key={r.ingredient.id} style={[styles.row, i < rows.length - 1 && styles.rowBorder]}>
              <Text style={{ fontSize: 20 }}>{r.ingredient.emoji}</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.rowName}>{r.ingredient.name}</Text>
                <Text style={styles.rowSub}>
                  {r.grams} g · P {r.n.protein.toFixed(0)} · C {r.n.carbs.toFixed(0)} · F {r.n.fat.toFixed(0)}
                </Text>
              </View>
              <Text style={styles.rowKcal}>{Math.round(r.n.kcal)} kcal</Text>
            </View>
          ))}
        </Card>

        <Text style={styles.note}>Estimates based on typical portions. Check foods and portions for a better estimate.</Text>
      </ScrollView>

      <View style={styles.footer}>
        {readOnly ? <Button label="Done" onPress={onBack} variant="ghost" /> : <Button label="Save meal" onPress={onSave} />}
      </View>
    </View>
  );
}

function ScoreRing({ score }: { score: number }) {
  const tone = score >= 70 ? colors.ok : score >= 45 ? colors.warn : colors.danger;
  return (
    <View style={[styles.ring, { borderColor: tone }]}>
      <Text style={[styles.ringText, { color: tone }]}>{score}</Text>
      <Text style={styles.ringSub}>/100</Text>
    </View>
  );
}

function MacroRow({ icon, label, value, color, pct, last }: { icon: string; label: string; value: number; color: string; pct: number; last?: boolean }) {
  return (
    <View style={[styles.macroRow, !last && styles.rowBorder]}>
      <View style={[styles.swatch, { backgroundColor: color }]} />
      <Text style={{ fontSize: 16 }}>{icon}</Text>
      <Text style={styles.macroLabel}>{label}</Text>
      <Text style={styles.macroPct}>{Math.round(pct * 100)}%</Text>
      <Text style={styles.macroValue}>{Math.round(value)} g</Text>
    </View>
  );
}

function Minor({ label, value }: { label: string; value: string }) {
  return (
    <View style={{ flex: 1, alignItems: "center" }}>
      <Text style={styles.minorValue}>{value}</Text>
      <Text style={styles.minorLabel}>{label}</Text>
    </View>
  );
}

function FlagRow({ flag }: { flag: Flag }) {
  const bg = flag.level === "danger" ? colors.dangerSoft : flag.level === "warn" ? colors.warnSoft : flag.level === "ok" ? colors.okSoft : colors.infoSoft;
  const fg = flag.level === "danger" ? colors.danger : flag.level === "warn" ? colors.warn : flag.level === "ok" ? colors.ok : colors.info;
  const icon = flag.level === "danger" ? "⛔" : flag.level === "warn" ? "⚠️" : flag.level === "ok" ? "✅" : "ℹ️";
  return (
    <View style={[styles.flag, { backgroundColor: bg }]}>
      <Text style={{ fontSize: 18 }}>{icon}</Text>
      <View style={{ flex: 1 }}>
        <Text style={[styles.flagTitle, { color: fg }]}>{flag.title}</Text>
        <Text style={styles.flagDetail}>{flag.detail}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: colors.bg },
  content: { padding: spacing.md, paddingBottom: 120 },
  hero: { width: "100%", height: 240, borderRadius: radius.lg, backgroundColor: "#fff" },
  kcalBlock: { flexDirection: "row", alignItems: "baseline", justifyContent: "center", marginTop: spacing.lg, gap: 6 },
  kcal: { fontSize: 56, fontWeight: "800", color: colors.text, letterSpacing: -1 },
  kcalUnit: { fontSize: 22, color: colors.muted, fontWeight: "600" },
  estimated: { textAlign: "center", color: colors.muted, marginTop: -2 },
  scoreRow: { flexDirection: "row", alignItems: "center", gap: spacing.md, marginTop: spacing.lg },
  ring: { width: 68, height: 68, borderRadius: 34, borderWidth: 5, alignItems: "center", justifyContent: "center", backgroundColor: colors.card },
  ringText: { fontSize: 22, fontWeight: "800", lineHeight: 24 },
  ringSub: { fontSize: 10, color: colors.muted },
  scoreTitle: { fontWeight: "700", fontSize: 16, color: colors.text },
  scoreSub: { color: colors.muted, marginTop: 2, lineHeight: 19 },
  macroBar: { flexDirection: "row", height: 10, borderRadius: 6, overflow: "hidden", marginBottom: spacing.sm },
  macroSeg: { height: "100%" },
  macroRow: { flexDirection: "row", alignItems: "center", paddingVertical: 12, gap: 10 },
  swatch: { width: 8, height: 8, borderRadius: 4 },
  macroLabel: { flex: 1, fontSize: 16, color: colors.text },
  macroPct: { color: colors.muted, fontSize: 13, marginRight: 8 },
  macroValue: { fontSize: 17, fontWeight: "700", color: colors.text },
  minorRow: { flexDirection: "row", marginTop: spacing.sm, paddingTop: spacing.sm, borderTopWidth: 1, borderTopColor: colors.border },
  minorValue: { fontWeight: "700", color: colors.text },
  minorLabel: { color: colors.muted, fontSize: 12 },
  section: { fontSize: 17, fontWeight: "700", color: colors.text, marginTop: spacing.lg, marginBottom: spacing.sm },
  flag: { flexDirection: "row", gap: spacing.sm, padding: spacing.md, borderRadius: radius.md, marginBottom: spacing.sm, alignItems: "flex-start" },
  flagTitle: { fontWeight: "700", fontSize: 15 },
  flagDetail: { color: colors.text, opacity: 0.8, marginTop: 2, lineHeight: 19, fontSize: 13 },
  row: { flexDirection: "row", alignItems: "center", gap: spacing.sm, padding: spacing.md },
  rowBorder: { borderBottomWidth: 1, borderBottomColor: colors.border },
  rowName: { fontWeight: "600", color: colors.text },
  rowSub: { color: colors.muted, fontSize: 12, marginTop: 2 },
  rowKcal: { fontWeight: "700", color: colors.text },
  note: { color: colors.muted, fontSize: 12, textAlign: "center", marginTop: spacing.lg, lineHeight: 18 },
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
