package com.example.meallens;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Connect Java with XML
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);

        TextView createAccount =
                findViewById(R.id.createAccountText);

        // Open Register screen
        createAccount.setOnClickListener(v -> {
            Intent intent =
                    new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Login button
        loginButton.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String email =
                emailEditText.getText().toString().trim();

        String password =
                passwordEditText.getText().toString().trim();

        // Check empty fields
        if (TextUtils.isEmpty(email) ||
                TextUtils.isEmpty(password)) {

            Toast.makeText(
                    MainActivity.this,
                    "Please enter email and password",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // Firebase Login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(
                                MainActivity.this,
                                "Login successful!",
                                Toast.LENGTH_SHORT
                        ).show();

                        // Go to Home screen
                        Intent intent =
                                new Intent(
                                        MainActivity.this,
                                        HomeActivity.class
                                );

                        startActivity(intent);
                        finish();

                    } else {

                        String errorMessage = "Login failed";

                        if (task.getException() != null) {
                            errorMessage =
                                    task.getException().getMessage();
                        }

                        Toast.makeText(
                                MainActivity.this,
                                errorMessage,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }
}