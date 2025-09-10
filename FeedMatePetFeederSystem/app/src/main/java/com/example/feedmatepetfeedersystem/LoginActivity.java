package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText loginEmail, loginPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        Button loginButton = findViewById(R.id.login_button);
        TextView signupRedirectText = findViewById(R.id.signupRedirectText);
        TextView forgotPasswordText = findViewById(R.id.forgotPasswordText);
        ImageView catLogo = findViewById(R.id.topImage);

        mAuth = FirebaseAuth.getInstance();

        // Long press on cat logo → Admin login
        catLogo.setOnLongClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Redirecting to Admin Login...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class));
            return true;
        });

        // Password visibility toggle
        loginPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (loginPassword.getRight()
                        - loginPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {

                    int selection = loginPassword.getSelectionEnd();

                    if (isPasswordVisible) {
                        loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_closed, 0);
                        isPasswordVisible = false;
                    } else {
                        loginPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        loginPassword.setCompoundDrawablesWithIntrinsicBounds(
                                R.drawable.baseline_lock_24, 0, R.drawable.ic_eye_open, 0);
                        isPasswordVisible = true;
                    }

                    loginPassword.setTypeface(loginEmail.getTypeface());
                    loginPassword.setSelection(selection);
                    return true;
                }
            }
            return false;
        });

        // Redirect to Signup
        signupRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

        // ✅ Forgot Password logic (fixed to keep dialog open on errors)
        forgotPasswordText.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle("Reset Password");

            final EditText input = new EditText(LoginActivity.this);
            input.setHint("Enter your email");
            input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            builder.setView(input);

            builder.setPositiveButton("Submit", null); // we’ll override later
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

                    if (!isEmailValid(email)) {
                        input.setError("Invalid email format or TLD");
                        input.requestFocus();
                        return;
                    }

                    FirebaseDatabase db = FirebaseDatabase.getInstance(
                            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                    DatabaseReference usersRef = db.getReference("users");

                    Query query = usersRef.orderByChild("email").equalTo(email);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            if (!snapshot.exists()) {
                                input.setError("Email does not exist. Please sign up first.");
                                input.requestFocus();
                                return;
                            }

                            for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                String role = userSnapshot.child("role").getValue(String.class);

                                if ("user".equalsIgnoreCase(role)) {
                                    mAuth.sendPasswordResetEmail(email)
                                            .addOnCompleteListener(resetTask -> {
                                                if (resetTask.isSuccessful()) {
                                                    Toast.makeText(LoginActivity.this,
                                                            "Password reset link sent to your email.",
                                                            Toast.LENGTH_LONG).show();
                                                    dialog.dismiss(); // ✅ close only on success
                                                } else {
                                                    String error = (resetTask.getException() != null) ?
                                                            resetTask.getException().getMessage() : "Unknown error";
                                                    Toast.makeText(LoginActivity.this,
                                                            "Failed to send reset link: " + error,
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            });
                                } else {
                                    input.setError("This email does not belong to a user account.");
                                    input.requestFocus();
                                }
                                break;
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            Toast.makeText(LoginActivity.this,
                                    "Database error: " + error.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
                });
            });

            dialog.show();
        });

        // Login button logic
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() && password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isEmailValid(email)) {
                Toast.makeText(LoginActivity.this, "Invalid email format or TLD", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(LoginActivity.this, "Invalid password : Must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.reload().addOnCompleteListener(reloadTask -> {
                                    FirebaseDatabase db = FirebaseDatabase.getInstance(
                                            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                    DatabaseReference userRef = db.getReference("users").child(user.getUid());

                                    userRef.child("role").get().addOnCompleteListener(roleTask -> {
                                        if (!roleTask.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Database error: " +
                                                            (roleTask.getException() != null ? roleTask.getException().getMessage() : "Unknown"),
                                                    Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        String role = roleTask.getResult().getValue(String.class);

                                        if ("admin".equalsIgnoreCase(role)) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Admins must log in through the Admin Login page.",
                                                    Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            return;
                                        }

                                        if (user.isEmailVerified()) {
                                            Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(LoginActivity.this, UserDashboardActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(LoginActivity.this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                        }
                                    });
                                });
                            }
                        } else {
                            Exception e = signInTask.getException();
                            if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
                                if ("ERROR_WRONG_PASSWORD".equals(code)) {
                                    Toast.makeText(LoginActivity.this,
                                            "Wrong password. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "Account or password error",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else if (e instanceof FirebaseAuthInvalidUserException) {
                                String code = ((FirebaseAuthInvalidUserException) e).getErrorCode();
                                if ("ERROR_USER_NOT_FOUND".equals(code)) {
                                    Toast.makeText(LoginActivity.this,
                                            "Email not registered. Please sign up first.",
                                            Toast.LENGTH_SHORT).show();
                                } else if ("ERROR_USER_DISABLED".equals(code)) {
                                    Toast.makeText(LoginActivity.this,
                                            "This account is disabled.",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(LoginActivity.this,
                                            "This account is no longer valid. Please contact support.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                String error = (e != null) ? e.getMessage() : "Unknown error";
                                Toast.makeText(LoginActivity.this,
                                        "Login failed: " + error,
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    private boolean isEmailValid(String email) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return false;
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
