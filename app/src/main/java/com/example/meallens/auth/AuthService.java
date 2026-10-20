package com.example.meallens.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Registration / login / session.
 *
 * Uses Firebase Authentication when the app has a google-services.json
 * (same Firebase project as Sprint 1). Without it the app falls back to a
 * local demo mode that stores accounts in SharedPreferences, so the whole
 * flow can still be built and tested by anyone.
 */
public final class AuthService {

    public static final class User {
        public final String uid;
        public final String email;
        public final String displayName;

        User(String uid, String email, String displayName) {
            this.uid = uid;
            this.email = email;
            this.displayName = displayName;
        }
    }

    public interface Callback {
        void onSuccess(User user);
        void onError(String message);
    }

    private static final String PREFS = "meallens_demo_auth";
    private static final String KEY_USERS = "users";
    private static final String KEY_SESSION = "session";

    private AuthService() {}

    public static boolean isFirebase(Context ctx) {
        return !FirebaseApp.getApps(ctx.getApplicationContext()).isEmpty();
    }

    public static String modeLabel(Context ctx) {
        return isFirebase(ctx) ? "firebase" : "demo";
    }

    // ---------------------------------------------------------------- register

    public static void register(Context ctx, String name, String email, String password, Callback cb) {
        name = name.trim();
        email = email.trim();
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            cb.onError("Please fill in all fields");
            return;
        }
        if (password.length() < 6) {
            cb.onError("Password must be at least 6 characters");
            return;
        }

        if (isFirebase(ctx)) {
            final String displayName = name;
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    cb.onError(task.getException() != null
                            ? task.getException().getMessage() : "Registration failed");
                    return;
                }
                FirebaseUser fu = auth.getCurrentUser();
                if (fu == null) {
                    cb.onError("Registration failed");
                    return;
                }
                UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName).build();
                fu.updateProfile(req).addOnCompleteListener(t2 ->
                        cb.onSuccess(new User(fu.getUid(), fu.getEmail(), displayName)));
            });
            return;
        }

        try {
            SharedPreferences prefs = prefs(ctx);
            JSONArray users = new JSONArray(prefs.getString(KEY_USERS, "[]"));
            for (int i = 0; i < users.length(); i++) {
                if (users.getJSONObject(i).getString("email").equalsIgnoreCase(email)) {
                    cb.onError("The email address is already in use by another account.");
                    return;
                }
            }
            JSONObject u = new JSONObject();
            u.put("uid", String.valueOf(System.currentTimeMillis()));
            u.put("email", email);
            u.put("displayName", name);
            u.put("password", password);
            users.put(u);
            User user = new User(u.getString("uid"), email, name);
            prefs.edit()
                    .putString(KEY_USERS, users.toString())
                    .putString(KEY_SESSION, sessionJson(user))
                    .apply();
            cb.onSuccess(user);
        } catch (JSONException e) {
            cb.onError("Registration failed");
        }
    }

    // ------------------------------------------------------------------- login

    public static void login(Context ctx, String email, String password, Callback cb) {
        email = email.trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            cb.onError("Please enter email and password");
            return;
        }

        if (isFirebase(ctx)) {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                    cb.onError(task.getException() != null
                            ? task.getException().getMessage() : "Login failed");
                    return;
                }
                cb.onSuccess(toUser(auth.getCurrentUser()));
            });
            return;
        }

        try {
            SharedPreferences prefs = prefs(ctx);
            JSONArray users = new JSONArray(prefs.getString(KEY_USERS, "[]"));
            for (int i = 0; i < users.length(); i++) {
                JSONObject u = users.getJSONObject(i);
                if (u.getString("email").equalsIgnoreCase(email)
                        && u.getString("password").equals(password)) {
                    User user = new User(u.getString("uid"), u.getString("email"), u.getString("displayName"));
                    prefs.edit().putString(KEY_SESSION, sessionJson(user)).apply();
                    cb.onSuccess(user);
                    return;
                }
            }
            cb.onError("The email or password is incorrect.");
        } catch (JSONException e) {
            cb.onError("Login failed");
        }
    }

    // ----------------------------------------------------------------- session

    public static User currentUser(Context ctx) {
        if (isFirebase(ctx)) {
            FirebaseUser fu = FirebaseAuth.getInstance().getCurrentUser();
            return fu == null ? null : toUser(fu);
        }
        String raw = prefs(ctx).getString(KEY_SESSION, null);
        if (raw == null) return null;
        try {
            JSONObject o = new JSONObject(raw);
            return new User(o.getString("uid"), o.getString("email"), o.getString("displayName"));
        } catch (JSONException e) {
            return null;
        }
    }

    public static void logout(Context ctx) {
        if (isFirebase(ctx)) {
            FirebaseAuth.getInstance().signOut();
        } else {
            prefs(ctx).edit().remove(KEY_SESSION).apply();
        }
    }

    // ----------------------------------------------------------------- helpers

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static User toUser(FirebaseUser fu) {
        String name = fu.getDisplayName();
        return new User(fu.getUid(), fu.getEmail() == null ? "" : fu.getEmail(),
                name == null || name.isEmpty() ? "User" : name);
    }

    private static String sessionJson(User u) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("uid", u.uid);
        o.put("email", u.email);
        o.put("displayName", u.displayName);
        return o.toString();
    }
}
