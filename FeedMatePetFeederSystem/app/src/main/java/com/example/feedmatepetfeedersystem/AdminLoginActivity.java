package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminEmail, adminPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    // 🔹 Stricter email regex pattern
    private static final Pattern STRICT_EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

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

        // Back button
        adminBackButton.setOnClickListener(v -> finish());

        // ✅ Forgot Password logic (dialog, only for admins)
        adminForgotPassword.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(AdminLoginActivity.this);
            builder.setTitle("Reset Password");

            final EditText input = new EditText(AdminLoginActivity.this);
            input.setHint("Enter your admin email");
            input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            builder.setView(input);

            builder.setPositiveButton("Submit", null); // overridden later
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dlg -> {
                Button submitBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                submitBtn.setOnClickListener(v1 -> {
                    String email = input.getText().toString().trim();

                    if (email.isEmpty()) {
                        input.setError("Email is required");
                        input.requestFocus();
                        return;
                    }

                    if (!STRICT_EMAIL_PATTERN.matcher(email).matches()) {
                        input.setError("Invalid email format");
                        input.requestFocus();
                        return;
                    }

                    FirebaseDatabase db = FirebaseDatabase.getInstance(
                            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                    DatabaseReference usersRef = db.getReference("users");

                    usersRef.orderByChild("email").equalTo(email)
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snapshot) {
                                    if (!snapshot.exists()) {
                                        input.setError("Email does not exist. Please contact system admin.");
                                        input.requestFocus();
                                        return;
                                    }

                                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                        String role = userSnapshot.child("role").getValue(String.class);

                                        if ("admin".equalsIgnoreCase(role)) {
                                            mAuth.sendPasswordResetEmail(email)
                                                    .addOnCompleteListener(resetTask -> {
                                                        if (resetTask.isSuccessful()) {
                                                            Toast.makeText(AdminLoginActivity.this,
                                                                    "Password reset link sent to your email.",
                                                                    Toast.LENGTH_LONG).show();
                                                            dialog.dismiss(); // ✅ close only on success
                                                        } else {
                                                            String error = (resetTask.getException() != null) ?
                                                                    resetTask.getException().getMessage() : "Unknown error";
                                                            Toast.makeText(AdminLoginActivity.this,
                                                                    "Failed to send reset link: " + error,
                                                                    Toast.LENGTH_LONG).show();
                                                        }
                                                    });
                                        } else {
                                            input.setError("This email does not belong to an admin account.");
                                            input.requestFocus();
                                        }
                                        break;
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError error) {
                                    Toast.makeText(AdminLoginActivity.this,
                                            "Database error: " + error.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                });
            });

            dialog.show();
        });
    }

    private void adminLogin() {
        String email = adminEmail.getText().toString().trim();
        String password = adminPassword.getText().toString().trim();

        if (email.isEmpty() && password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean badEmail = !STRICT_EMAIL_PATTERN.matcher(email).matches();
        boolean badPassword = password.length() < 6;

        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (badEmail) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (badPassword) {
            Toast.makeText(this, "Invalid password : Must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        if (badEmail && badPassword) {
            Toast.makeText(this, "Invalid email format and password", Toast.LENGTH_SHORT).show();
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
                            // Correct admin
                            Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, AdminDashboardActivity.class));
                            finish();
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
                    Toast.makeText(this, "Account or password error", Toast.LENGTH_SHORT).show();
                } else if (e instanceof FirebaseAuthInvalidUserException) {
                    Toast.makeText(this, "Email not registered or user not allowed", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Login failed: " + (e != null ? e.getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
