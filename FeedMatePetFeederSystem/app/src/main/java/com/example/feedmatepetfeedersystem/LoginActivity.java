package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
        ImageView topImage = findViewById(R.id.topImage);
        topImage.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, AdminLoginActivity.class);
            startActivity(intent);
        });

        loginPassword.setEnabled(false);
        loginButton.setEnabled(false);

        loginEmail.addTextChangedListener(simpleWatcher(() -> {
            boolean hasEmail = !loginEmail.getText().toString().trim().isEmpty();
            loginPassword.setEnabled(hasEmail);
            loginButton.setEnabled(hasEmail && !loginPassword.getText().toString().trim().isEmpty());
        }));

        loginPassword.addTextChangedListener(simpleWatcher(() -> {
            boolean ready = !loginEmail.getText().toString().trim().isEmpty()
                    && !loginPassword.getText().toString().trim().isEmpty();
            loginButton.setEnabled(ready);
        }));

        tvSignupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            finish();
        });

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

        loginButton.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            String password = loginPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
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

                                FirebaseDatabase db = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                DatabaseReference dbRef = db.getReference("users");
                                String uid = user.getUid();

                                dbRef.child(uid).get().addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        DataSnapshot snapshot = dbTask.getResult();
                                        if (snapshot.exists()) {
                                            User userProfile = snapshot.getValue(User.class);
                                            if (userProfile != null) {
                                                String role = userProfile.role;

                                                if ("admin".equals(role)) {
                                                    Toast.makeText(this, "Admins must use the admin login page.", Toast.LENGTH_LONG).show();
                                                    mAuth.signOut();
                                                    return;
                                                }

                                                Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(this, UserDashboardActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(this, "Failed to read user info", Toast.LENGTH_SHORT).show();
                                            }
                                        } else {
                                            Toast.makeText(this, "User info not found in database", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(this, "Failed to read user info", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, "Email does not exist", Toast.LENGTH_LONG).show();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                Toast.makeText(this, "Wrong password", Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Login failed", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });
    }

    private TextWatcher simpleWatcher(Runnable afterChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { afterChange.run(); }
        };
    }
}