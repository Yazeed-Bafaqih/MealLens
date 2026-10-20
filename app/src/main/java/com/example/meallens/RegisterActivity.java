package com.example.meallens;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.meallens.auth.AuthService;

/** Create-account screen. */
public class RegisterActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText registerEmailEditText;
    private EditText registerPasswordEditText;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        EdgeToEdge.apply(this);

        nameEditText = findViewById(R.id.nameEditText);
        registerEmailEditText = findViewById(R.id.registerEmailEditText);
        registerPasswordEditText = findViewById(R.id.registerPasswordEditText);
        signupButton = findViewById(R.id.signupButton);
        TextView loginText = findViewById(R.id.loginText);

        loginText.setOnClickListener(v -> finish());
        signupButton.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String name = nameEditText.getText().toString();
        String email = registerEmailEditText.getText().toString();
        String password = registerPasswordEditText.getText().toString();

        if (!password.isEmpty() && password.length() < 6) {
            registerPasswordEditText.setError("Password must be at least 6 characters");
            return;
        }

        signupButton.setEnabled(false);
        AuthService.register(this, name, email, password, new AuthService.Callback() {
            @Override
            public void onSuccess(AuthService.User user) {
                Toast.makeText(RegisterActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                signupButton.setEnabled(true);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
