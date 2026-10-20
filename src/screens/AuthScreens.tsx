import React from "react";
import { ActivityIndicator, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { Button } from "../components/ui";
import { colors, radius, spacing } from "../theme";
import { AuthUser, authMode, login, register } from "../auth/authService";

type Props = { onAuthed: (user: AuthUser, message: string) => void };

function Brand() {
  return (
    <View style={styles.brand}>
      <Text style={styles.logo}>
        <Text style={{ color: colors.primary }}>Meal</Text>
        <Text style={{ color: colors.text }}>Lens</Text>
      </Text>
      <Text style={styles.tagline}>Good food. A brighter you.</Text>
    </View>
  );
}

function Field({ icon, placeholder, value, onChange, secure, keyboardType }: { icon: string; placeholder: string; value: string; onChange: (t: string) => void; secure?: boolean; keyboardType?: "email-address" | "default" }) {
  const [show, setShow] = React.useState(false);
  return (
    <View style={styles.field}>
      <Text style={styles.fieldIcon}>{icon}</Text>
      <TextInput
        style={styles.input}
        placeholder={placeholder}
        placeholderTextColor={colors.muted}
        value={value}
        onChangeText={onChange}
        secureTextEntry={secure && !show}
        autoCapitalize="none"
        keyboardType={keyboardType}
      />
      {secure ? (
        <Pressable onPress={() => setShow((s) => !s)} hitSlop={10}>
          <Text style={styles.eye}>{show ? "🙈" : "👁"}</Text>
        </Pressable>
      ) : null}
    </View>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <KeyboardAvoidingView style={{ flex: 1, backgroundColor: colors.bg }} behavior={Platform.OS === "ios" ? "padding" : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Brand />
        {children}
        {authMode === "demo" ? <Text style={styles.demo}>Demo auth mode: add Firebase keys in .env to use real accounts.</Text> : null}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

export function LoginScreen({ onAuthed, onCreateAccount }: Props & { onCreateAccount: () => void }) {
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState(false);

  async function submit() {
    setError(null);
    setBusy(true);
    try {
      const user = await login(email, password);
      onAuthed(user, "Login successful!");
    } catch (e: any) {
      setError(friendly(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell>
      <Text style={styles.title}>Welcome back</Text>
      <Text style={styles.subtitle}>Log in to continue your journey with better food choices.</Text>
      <Field icon="✉️" placeholder="Email address" value={email} onChange={setEmail} keyboardType="email-address" />
      <Field icon="🔒" placeholder="Password" value={password} onChange={setPassword} secure />
      <Pressable style={{ alignSelf: "flex-end" }} hitSlop={8}>
        <Text style={styles.link}>Forgot password?</Text>
      </Pressable>
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <View style={{ height: spacing.lg }} />
      {busy ? <ActivityIndicator color={colors.primary} /> : <Button label="Log in" onPress={submit} />}
      <View style={styles.orRow}>
        <View style={styles.rule} />
        <Text style={styles.or}>or</Text>
        <View style={styles.rule} />
      </View>
      <Text style={styles.footer}>
        Don't have an account?{" "}
        <Text style={styles.link} onPress={onCreateAccount}>
          Create account
        </Text>
      </Text>
    </Shell>
  );
}

export function RegisterScreen({ onAuthed, onLogin }: Props & { onLogin: () => void }) {
  const [name, setName] = React.useState("");
  const [email, setEmail] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [busy, setBusy] = React.useState(false);

  async function submit() {
    setError(null);
    setBusy(true);
    try {
      const user = await register(name, email, password);
      onAuthed(user, "Account created successfully!");
    } catch (e: any) {
      setError(friendly(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Shell>
      <Text style={styles.title}>Create account</Text>
      <Text style={styles.subtitle}>Join MealLens and start tracking your meals today.</Text>
      <Field icon="👤" placeholder="Full name" value={name} onChange={setName} />
      <Field icon="✉️" placeholder="Email address" value={email} onChange={setEmail} keyboardType="email-address" />
      <Field icon="🔒" placeholder="Password" value={password} onChange={setPassword} secure />
      {error ? <Text style={styles.error}>{error}</Text> : null}
      <View style={{ height: spacing.lg }} />
      {busy ? <ActivityIndicator color={colors.primary} /> : <Button label="Sign up" onPress={submit} />}
      <View style={{ height: spacing.xl }} />
      <Text style={styles.footer}>
        Already have an account?{" "}
        <Text style={styles.link} onPress={onLogin}>
          Log in
        </Text>
      </Text>
    </Shell>
  );
}

function friendly(e: any): string {
  const code: string = e?.code ?? "";
  if (code.includes("invalid-credential") || code.includes("wrong-password") || code.includes("user-not-found")) return "The email or password is incorrect.";
  if (code.includes("email-already-in-use")) return "The email address is already in use by another account.";
  if (code.includes("invalid-email")) return "The email address is badly formatted.";
  if (code.includes("network")) return "Network error. Check your connection.";
  return e?.message ?? "Something went wrong";
}

const styles = StyleSheet.create({
  content: { padding: spacing.lg, paddingTop: 48, paddingBottom: 48 },
  brand: { alignItems: "center", marginBottom: spacing.xl },
  logo: { fontSize: 36, fontWeight: "800", letterSpacing: -0.5 },
  tagline: { color: colors.muted, marginTop: 4 },
  title: { fontSize: 26, fontWeight: "700", color: colors.text, textAlign: "center" },
  subtitle: { color: colors.muted, textAlign: "center", marginTop: 6, marginBottom: spacing.lg, lineHeight: 20, paddingHorizontal: 20 },
  field: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: 14,
    height: 54,
    marginBottom: spacing.sm,
    gap: 10,
  },
  fieldIcon: { fontSize: 16, opacity: 0.6 },
  input: { flex: 1, fontSize: 16, color: colors.text },
  eye: { fontSize: 16, opacity: 0.6 },
  link: { color: colors.primary, fontWeight: "600" },
  error: { color: colors.danger, marginTop: spacing.sm, textAlign: "center" },
  orRow: { flexDirection: "row", alignItems: "center", gap: 12, marginVertical: spacing.lg },
  rule: { flex: 1, height: 1, backgroundColor: colors.border },
  or: { color: colors.muted },
  footer: { textAlign: "center", color: colors.muted },
  demo: { textAlign: "center", color: colors.muted, fontSize: 12, marginTop: spacing.xl },
});
