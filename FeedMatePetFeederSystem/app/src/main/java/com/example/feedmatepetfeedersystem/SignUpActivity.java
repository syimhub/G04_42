package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignUpActivity extends AppCompatActivity {

    private EditText signupEmail, signupPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        Button signupButton = findViewById(R.id.signup_button);
        TextView loginRedirectText = findViewById(R.id.loginRedirectText);

        mAuth = FirebaseAuth.getInstance();

        // Redirect to login when clicking "Already a user? Login"
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        // Handle signup button click
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Send verification email
                                user.sendEmailVerification()
                                        .addOnCompleteListener(verifyTask -> {
                                            if (verifyTask.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this,
                                                        "Signup successful! Please check your email to verify your account.",
                                                        Toast.LENGTH_LONG).show();

                                                mAuth.signOut(); // Sign out until verified
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                String errorMessage = (verifyTask.getException() != null)
                                                        ? verifyTask.getException().getMessage()
                                                        : "Failed to send verification email";
                                                Toast.makeText(SignUpActivity.this,
                                                        errorMessage,
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            String errorMessage = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error occurred";

                            Toast.makeText(SignUpActivity.this,
                                    "Signup failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
