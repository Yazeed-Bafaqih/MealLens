// Same rules and messages as the Android MainActivity / RegisterActivity.
import AsyncStorage from "@react-native-async-storage/async-storage";
import { firebaseAuth, isFirebaseConfigured } from "./firebase";

export type AuthUser = { uid: string; email: string; displayName: string };

const USERS_KEY = "meallens.demoUsers";
const SESSION_KEY = "meallens.demoSession";

type DemoUser = AuthUser & { password: string };

async function readDemoUsers(): Promise<DemoUser[]> {
  try {
    const raw = await AsyncStorage.getItem(USERS_KEY);
    return raw ? (JSON.parse(raw) as DemoUser[]) : [];
  } catch {
    return [];
  }
}

function toUser(u: { uid: string; email: string | null; displayName: string | null }): AuthUser {
  return { uid: u.uid, email: u.email ?? "", displayName: u.displayName ?? "User" };
}

export const authMode: "firebase" | "demo" = isFirebaseConfigured ? "firebase" : "demo";

export async function register(name: string, email: string, password: string): Promise<AuthUser> {
  if (!name.trim() || !email.trim() || !password) throw new Error("Please fill in all fields");
  if (password.length < 6) throw new Error("Password must be at least 6 characters");

  if (firebaseAuth) {
    const { createUserWithEmailAndPassword, updateProfile } = await import("firebase/auth");
    const cred = await createUserWithEmailAndPassword(firebaseAuth, email.trim(), password);
    await updateProfile(cred.user, { displayName: name.trim() });
    return { uid: cred.user.uid, email: cred.user.email ?? "", displayName: name.trim() };
  }

  const users = await readDemoUsers();
  if (users.some((u) => u.email.toLowerCase() === email.trim().toLowerCase())) {
    throw new Error("The email address is already in use by another account.");
  }
  const user: DemoUser = { uid: String(Date.now()), email: email.trim(), displayName: name.trim(), password };
  await AsyncStorage.setItem(USERS_KEY, JSON.stringify([...users, user]));
  const session = { uid: user.uid, email: user.email, displayName: user.displayName };
  await AsyncStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export async function login(email: string, password: string): Promise<AuthUser> {
  if (!email.trim() || !password) throw new Error("Please enter email and password");

  if (firebaseAuth) {
    const { signInWithEmailAndPassword } = await import("firebase/auth");
    const cred = await signInWithEmailAndPassword(firebaseAuth, email.trim(), password);
    return toUser(cred.user);
  }

  const users = await readDemoUsers();
  const found = users.find((u) => u.email.toLowerCase() === email.trim().toLowerCase() && u.password === password);
  if (!found) throw new Error("The email or password is incorrect.");
  const user = { uid: found.uid, email: found.email, displayName: found.displayName };
  await AsyncStorage.setItem(SESSION_KEY, JSON.stringify(user));
  return user;
}

export async function logout(): Promise<void> {
  if (firebaseAuth) {
    const { signOut } = await import("firebase/auth");
    await signOut(firebaseAuth);
    return;
  }
  await AsyncStorage.removeItem(SESSION_KEY);
}

/** Subscribes to the current user. Returns an unsubscribe function. */
export function onAuthChange(cb: (user: AuthUser | null) => void): () => void {
  if (firebaseAuth) {
    const auth = firebaseAuth;
    let unsub = () => {};
    import("firebase/auth").then(({ onAuthStateChanged }) => {
      unsub = onAuthStateChanged(auth, (u) => cb(u ? toUser(u) : null));
    });
    return () => unsub();
  }
  AsyncStorage.getItem(SESSION_KEY)
    .then((raw) => cb(raw ? (JSON.parse(raw) as AuthUser) : null))
    .catch(() => cb(null));
  return () => {};
}
