package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
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
        ImageView catLogo = findViewById(R.id.topImage);

        mAuth = FirebaseAuth.getInstance();

        // 🔹 Long press on cat logo to redirect to AdminLogin
        catLogo.setOnLongClickListener(v -> {
            Toast.makeText(LoginActivity.this, "Redirecting to Admin Login...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(LoginActivity.this, AdminLoginActivity.class));
            return true;
        });

        // 🔹 Password visibility toggle
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

        // 🔹 Redirect to Signup
        signupRedirectText.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

        // 🔹 Login button logic
        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() && password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean badEmail = !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
            boolean badPassword = password.length() < 6;

            if (badEmail && badPassword) {
                Toast.makeText(LoginActivity.this, "Invalid email format and password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Email is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (badEmail) {
                Toast.makeText(LoginActivity.this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Password is required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (badPassword) {
                Toast.makeText(LoginActivity.this, "Invalid password : Must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // 🔹 Attempt Firebase login
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(signInTask -> {
                        if (signInTask.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                user.reload().addOnCompleteListener(reloadTask -> {
                                    // 🔹 Admin role check (use SAME DB URL and node name you use on signup)
                                    FirebaseDatabase db = FirebaseDatabase.getInstance(
                                            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                    DatabaseReference userRef = db.getReference("users").child(user.getUid());

                                    userRef.get().addOnCompleteListener(roleTask -> {
                                        if (!roleTask.isSuccessful()) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Database error: " +
                                                            (roleTask.getException() != null ? roleTask.getException().getMessage() : "Unknown"),
                                                    Toast.LENGTH_LONG).show();
                                            return;
                                        }

                                        DataSnapshot snapshot = roleTask.getResult();
                                        String role = snapshot.child("role").getValue(String.class);

                                        if ("admin".equalsIgnoreCase(role)) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Admins must log in through the Admin Login page.",
                                                    Toast.LENGTH_LONG).show();
                                            mAuth.signOut();
                                            return; // stop normal user flow
                                        }

                                        // Continue normal flow for non-admins
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
                                Toast.makeText(LoginActivity.this, "Wrong password", Toast.LENGTH_SHORT).show();
                            } else if (e instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(LoginActivity.this, "No account found with this email", Toast.LENGTH_SHORT).show();
                            } else {
                                String error = (e != null) ? e.getMessage() : "Unknown error";
                                Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }
}
