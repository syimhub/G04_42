package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize UI
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);

        mAuth = FirebaseAuth.getInstance();

        // Login button clicked
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(LoginActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                                // Go to UserDashboardActivity after login
                                Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                                startActivity(intent);
                                finish(); // Prevent going back to login
                            }
                        } else {
                            String errorMessage = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error occurred";

                            Toast.makeText(LoginActivity.this,
                                    "Login failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
