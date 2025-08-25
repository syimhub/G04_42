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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminEmail, adminPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        adminEmail = findViewById(R.id.admin_email);
        adminPassword = findViewById(R.id.admin_password);
        Button adminLoginButton = findViewById(R.id.admin_login_button);
        Button adminBackButton = findViewById(R.id.admin_back_button);
        TextView adminForgotPassword = findViewById(R.id.admin_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        // Password visibility toggle
        adminPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (adminPassword.getRight()
                        - adminPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    int selection = adminPassword.getSelectionEnd();

                    if (isPasswordVisible) {
                        adminPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        adminPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_closed, 0);
                        isPasswordVisible = false;
                    } else {
                        adminPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        adminPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_open, 0);
                        isPasswordVisible = true;
                    }

                    adminPassword.setSelection(selection);
                    return true;
                }
            }
            return false;
        });

        // Admin login logic
        adminLoginButton.setOnClickListener(v -> adminLogin());

        adminBackButton.setOnClickListener(v -> finish());

        adminForgotPassword.setOnClickListener(v -> {
            String email = adminEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Enter your admin email to reset password", Toast.LENGTH_SHORT).show();
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
    }

    private void adminLogin() {
        String email = adminEmail.getText().toString().trim();
        String password = adminPassword.getText().toString().trim();

        if (email.isEmpty() && password.isEmpty()) {
            Toast.makeText(AdminLoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean badEmail = !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean badPassword = password.length() < 6;

        if (badEmail && badPassword) {
            Toast.makeText(AdminLoginActivity.this, "Invalid email format and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(AdminLoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (badEmail) {
            Toast.makeText(AdminLoginActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(AdminLoginActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (badPassword) {
            Toast.makeText(AdminLoginActivity.this, "Invalid password : Must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        // Attempt Firebase login
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // Check role in database
                    FirebaseDatabase db = FirebaseDatabase.getInstance(
                            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                    DatabaseReference userRef = db.getReference("users").child(user.getUid());

                    userRef.get().addOnCompleteListener(roleTask -> {
                        if (!roleTask.isSuccessful()) {
                            Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        DataSnapshot snapshot = roleTask.getResult();
                        String role = snapshot.child("role").getValue(String.class);

                        if ("admin".equalsIgnoreCase(role)) {
                            // Correct admin, check password length
                            if (password.length() >= 6) {
                                Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, AdminDashboardActivity.class));
                                finish();
                            }
                        } else {
                            // User trying to log in via Admin page
                            Toast.makeText(this, "Users are not allowed to login in Admin Login Page", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    });
                }
            } else {
                Exception e = task.getException();
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(this, "Wrong password", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseAuthInvalidUserException) {
                    Toast.makeText(this, "Users are not allowed to login in Admin Login Page", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Login failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
