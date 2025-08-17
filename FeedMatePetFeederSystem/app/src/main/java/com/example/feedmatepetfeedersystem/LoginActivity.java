package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private Button loginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        TextView tvSignupRedirect = findViewById(R.id.signupRedirectText);
        TextView tvForgotPassword = findViewById(R.id.forgotPasswordText);

        mAuth = FirebaseAuth.getInstance();

        // Disable password & login until email is provided (enforces “enter email first”).
        loginPassword.setEnabled(false);
        loginButton.setEnabled(false);

        // Enable password only when email has text; enable login only when both have text.
        loginEmail.addTextChangedListener(simpleWatcher(() -> {
            boolean hasEmail = !loginEmail.getText().toString().trim().isEmpty();
            loginPassword.setEnabled(hasEmail);
            if (!hasEmail) {
                loginPassword.setText("");
                loginButton.setEnabled(false);
            } else {
                loginButton.setEnabled(!loginPassword.getText().toString().trim().isEmpty());
            }
        }));

        loginPassword.addTextChangedListener(simpleWatcher(() -> {
            boolean ready = !loginEmail.getText().toString().trim().isEmpty()
                    && !loginPassword.getText().toString().trim().isEmpty();
            loginButton.setEnabled(ready);
        }));

        // Go to Sign Up
        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

        // Forgot Password
        tvForgotPassword.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your email to reset password", Toast.LENGTH_SHORT).show();
                return;
            }
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Password reset link sent to your email.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to send reset email.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Login
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            // Validation per your rules
            if (email.isEmpty() && password.isEmpty()) {
                Toast.makeText(this, "Email and password is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty()) {
                Toast.makeText(this, "Email and password is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {

                                 if (!user.isEmailVerified()) {
                                     Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                     mAuth.signOut();
                                     return;
                                 }
                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, UserDashboardActivity.class));
                                finish();
                            }
                        } else {
                            // Map Firebase exceptions to your required messages
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) {
                                // Email not registered
                                Toast.makeText(this, "Email does not exists", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                // Usually wrong password (or bad format). Check error code if needed.
                                String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
                                if ("ERROR_WRONG_PASSWORD".equals(code)) {
                                    Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show();
                                } else {
                                    // Fallback for other credential errors
                                    Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    // Small helper to avoid verbose TextWatcher code
    private TextWatcher simpleWatcher(Runnable afterChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { afterChange.run(); }
        };
    }
}
