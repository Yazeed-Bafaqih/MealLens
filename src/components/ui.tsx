import React from "react";
import { Pressable, StyleSheet, Text, View, ViewStyle } from "react-native";
import { colors, radius, spacing } from "../theme";

export function Button({
  label,
  onPress,
  variant = "primary",
  disabled,
}: {
  label: string;
  onPress: () => void;
  variant?: "primary" | "ghost";
  disabled?: boolean;
}) {
  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [
        styles.btn,
        variant === "primary" ? styles.btnPrimary : styles.btnGhost,
        pressed && { opacity: 0.85 },
        disabled && { opacity: 0.5 },
      ]}
    >
      <Text style={[styles.btnText, variant === "ghost" && { color: colors.primary }]}>{label}</Text>
    </Pressable>
  );
}

export function Card({ children, style }: { children: React.ReactNode; style?: ViewStyle }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

export function Header({ title, onBack, right }: { title: string; onBack?: () => void; right?: React.ReactNode }) {
  return (
    <View style={styles.header}>
      <Pressable onPress={onBack} hitSlop={12} style={styles.headerSide}>
        {onBack ? <Text style={styles.back}>‹</Text> : null}
      </Pressable>
      <Text style={styles.headerTitle}>{title}</Text>
      <View style={styles.headerSide}>{right}</View>
    </View>
  );
}

export function Pill({ text, tone = "neutral" }: { text: string; tone?: "neutral" | "ok" | "warn" | "danger" | "info" }) {
  const bg =
    tone === "ok" ? colors.okSoft : tone === "warn" ? colors.warnSoft : tone === "danger" ? colors.dangerSoft : tone === "info" ? colors.infoSoft : colors.primarySoft;
  const fg =
    tone === "ok" ? colors.ok : tone === "warn" ? colors.warn : tone === "danger" ? colors.danger : tone === "info" ? colors.info : colors.primary;
  return (
    <View style={[styles.pill, { backgroundColor: bg }]}>
      <Text style={[styles.pillText, { color: fg }]}>{text}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  btn: {
    height: 54,
    borderRadius: radius.md,
    alignItems: "center",
    justifyContent: "center",
  },
  btnPrimary: { backgroundColor: colors.primary },
  btnGhost: { backgroundColor: colors.primarySoft },
  btnText: { color: "#fff", fontSize: 17, fontWeight: "600" },
  card: {
    backgroundColor: colors.card,
    borderRadius: radius.md,
    borderWidth: 1,
    borderColor: colors.border,
    padding: spacing.md,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.sm,
  },
  headerSide: { width: 44, alignItems: "center", justifyContent: "center" },
  headerTitle: { flex: 1, textAlign: "center", fontSize: 18, fontWeight: "600", color: colors.text },
  back: { fontSize: 34, lineHeight: 36, color: colors.text, marginTop: -4 },
  pill: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999 },
  pillText: { fontSize: 12, fontWeight: "600" },
});
