package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignUpActivity extends AppCompatActivity {

    private EditText signupEmail, signupPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false; // track password visibility

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        Button signupButton = findViewById(R.id.signup_button);
        TextView loginRedirectText = findViewById(R.id.loginRedirectText);

        mAuth = FirebaseAuth.getInstance();

        // 🔹 Handle eye icon toggle inside password field
        signupPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2; // right drawable index
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (signupPassword.getRight()
                        - signupPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    if (isPasswordVisible) {
                        // Hide password
                        signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        signupPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_closed, 0);
                        isPasswordVisible = false;
                    } else {
                        // Show password
                        signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        signupPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_open, 0);
                        isPasswordVisible = true;
                    }
                    signupPassword.setSelection(signupPassword.getText().length()); // keep cursor at end
                    return true;
                }
            }
            return false;
        });

        // 🔹 Redirect to login
        loginRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        // 🔹 Signup button logic
        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            if (email.isEmpty() && password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "An email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "A password is required", Toast.LENGTH_SHORT).show();
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
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        Toast.makeText(SignUpActivity.this,
                                                "Signup successful! Please check your email to verify your account.",
                                                Toast.LENGTH_LONG).show();

                                        // Save user info to Realtime Database
                                        FirebaseDatabase db = FirebaseDatabase.getInstance(
                                                "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                        DatabaseReference dbRef = db.getReference("users");
                                        String uid = user.getUid();
                                        User userProfile = new User(user.getEmail(), "user"); // default role: user

                                        dbRef.child(uid).setValue(userProfile).addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this,
                                                        "User saved successfully!", Toast.LENGTH_SHORT).show();
                                                mAuth.signOut(); // Sign out until verified
                                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(SignUpActivity.this,
                                                        "Failed to save user info: " +
                                                                (dbTask.getException() != null
                                                                        ? dbTask.getException().getMessage()
                                                                        : "Unknown error"),
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });

                                    } else {
                                        String errorMessage = (verifyTask.getException() != null)
                                                ? verifyTask.getException().getMessage()
                                                : "Failed to send verification email";
                                        Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else {
                            String errorMessage = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error occurred";

                            Toast.makeText(SignUpActivity.this, "Signup failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
