package com.example.meallens;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText registerEmailEditText;
    private EditText registerPasswordEditText;
    private Button signupButton;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect Java with XML
        nameEditText = findViewById(R.id.nameEditText);
        registerEmailEditText = findViewById(R.id.registerEmailEditText);
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText);
        signupButton = findViewById(R.id.signupButton);

        TextView loginText = findViewById(R.id.loginText);

        // Return to Login screen
        loginText.setOnClickListener(v -> finish());

        // Create account
        signupButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String name = nameEditText.getText().toString().trim();
        String email = registerEmailEditText.getText().toString().trim();
        String password = registerPasswordEditText.getText().toString();

        // Check empty fields
        if (TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password)) {

            Toast.makeText(
                    this,
                    "Please fill in all fields",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Firebase requires at least 6 characters
        if (password.length() < 6) {
            registerPasswordEditText.setError(
                    "Password must be at least 6 characters"
            );
            return;
        }

        // Create Firebase account
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {

                            UserProfileChangeRequest profileUpdates =
                                    new UserProfileChangeRequest.Builder()
                                            .setDisplayName(name)
                                            .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {

                                        if (profileTask.isSuccessful()) {

                                            Toast.makeText(
                                                    RegisterActivity.this,
                                                    "Account created successfully!",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            finish();
                                        }
                                    });
                        }

                    } else {

                        String errorMessage = "Registration failed";

                        if (task.getException() != null) {
                            errorMessage = task.getException().getMessage();
                        }

                        Toast.makeText(
                                RegisterActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}