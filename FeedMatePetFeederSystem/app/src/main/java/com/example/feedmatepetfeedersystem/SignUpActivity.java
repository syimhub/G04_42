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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
                Toast.makeText(SignUpActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(SignUpActivity.this, "Invalid password : Must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // ✅ Stricter email validation
            if (!isEmailValid(email)) {
                Toast.makeText(SignUpActivity.this, "Invalid email format or TLD", Toast.LENGTH_SHORT).show();
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

                                        // 🔹 Step 1: Use UID as feederId
                                        String feederId = uid;

                                        // 🔹 Step 2: Create user profile with feederId and default values for all fields
                                        User userProfile = new User(
                                                uid,
                                                "",                // fullName
                                                user.getEmail(),   // email
                                                "user",            // role
                                                feederId           // feederId
                                        );

                                        // 🔹 Step 3: Save user first
                                        dbRef.child(uid).setValue(userProfile).addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                // 🔹 Step 4: Initialize device node with default structure after user exists
                                                DatabaseReference deviceRef = db.getReference("devices").child(feederId);

                                                // Use LinkedHashMap to preserve insertion order
                                                Map<String, Object> defaultDevice = new LinkedHashMap<>();

                                                // Owner field (first child)
                                                defaultDevice.put("owner", uid);

                                                // Controls
                                                Map<String, Object> controls = new HashMap<>();
                                                controls.put("feedNow", false);
                                                defaultDevice.put("controls", controls);

                                                // Food
                                                Map<String, Object> food = new HashMap<>();
                                                food.put("level", 0);
                                                defaultDevice.put("food", food);

                                                // Sensor
                                                Map<String, Object> sensor = new HashMap<>();
                                                sensor.put("distance", 0.0);
                                                sensor.put("objectDetected", false);
                                                defaultDevice.put("sensor", sensor);

                                                // Servo
                                                Map<String, Object> servo = new HashMap<>();
                                                servo.put("angle", -1);
                                                defaultDevice.put("servo", servo);

                                                // System
                                                Map<String, Object> system = new HashMap<>();
                                                system.put("feedingInProgress", false);
                                                system.put("lastUpdate", "");
                                                system.put("status", 0);
                                                defaultDevice.put("system", system);

                                                // 🔹 Schedule: next feeding time
                                                Map<String, Object> schedule = new HashMap<>();
                                                schedule.put("nextFeedingTime", "12:00"); // default time
                                                defaultDevice.put("schedule", schedule);

                                                // Save device node with completion listener
                                                deviceRef.setValue(defaultDevice).addOnCompleteListener(deviceTask -> {
                                                    if (deviceTask.isSuccessful()) {
                                                        Toast.makeText(SignUpActivity.this, "Device node created successfully!", Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        Toast.makeText(SignUpActivity.this,
                                                                "Failed to create device node: " + deviceTask.getException(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });

                                                // Sign out and redirect to login
                                                mAuth.signOut();
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

    // 🔹 Stricter email validation method
    private boolean isEmailValid(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) return false;

        String domainPart = parts[1];
        if (!domainPart.contains(".")) return false;

        String tld = domainPart.substring(domainPart.lastIndexOf('.') + 1);

        String[] validTLDs = {"com", "net", "org", "edu", "gov", "mil", "info", "biz", "co", "io", "me", "xyz"};

        for (String valid : validTLDs) {
            if (tld.equalsIgnoreCase(valid)) return true;
        }

        return false;
    }
}
