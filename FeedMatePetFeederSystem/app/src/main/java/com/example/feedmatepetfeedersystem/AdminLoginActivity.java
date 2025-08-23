package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText adminEmail, adminPassword;
    private Button adminLoginButton, adminBackButton;
    private TextView adminForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_login);

        adminEmail = findViewById(R.id.admin_email);
        adminPassword = findViewById(R.id.admin_password);
        adminLoginButton = findViewById(R.id.admin_login_button);
        adminBackButton = findViewById(R.id.admin_back_button);
        adminForgotPassword = findViewById(R.id.admin_forgot_password);

        mAuth = FirebaseAuth.getInstance();

        adminPassword.setEnabled(false);
        adminLoginButton.setEnabled(false);

        // Enable password/login button dynamically
        adminEmail.addTextChangedListener(simpleWatcher(() -> {
            boolean hasEmail = !adminEmail.getText().toString().trim().isEmpty();
            adminPassword.setEnabled(hasEmail);
            adminLoginButton.setEnabled(hasEmail && !adminPassword.getText().toString().trim().isEmpty());
        }));

        adminPassword.addTextChangedListener(simpleWatcher(() -> {
            boolean ready = !adminEmail.getText().toString().trim().isEmpty()
                    && !adminPassword.getText().toString().trim().isEmpty();
            adminLoginButton.setEnabled(ready);
        }));

        adminLoginButton.setOnClickListener(v -> adminLogin());
        adminBackButton.setOnClickListener(v -> finish());

        // Forgot password
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

    private TextWatcher simpleWatcher(Runnable afterChange) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { afterChange.run(); }
        };
    }

    private void adminLogin() {
        String email = adminEmail.getText().toString().trim();
        String password = adminPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password are required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            FirebaseDatabase db = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                            DatabaseReference dbRef = db.getReference("users");
                            String uid = user.getUid();

                            dbRef.child(uid).get().addOnCompleteListener(dbTask -> {
                                if (dbTask.isSuccessful() && dbTask.getResult().exists()) {
                                    User userProfile = dbTask.getResult().getValue(User.class);
                                    if (userProfile != null && "admin".equals(userProfile.role)) {
                                        Toast.makeText(this, "Admin login successful!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, AdminDashboardActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Access denied: Not an admin account", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                    }
                                } else {
                                    Toast.makeText(this, "Failed to read user info", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                }
                            });
                        }
                    } else {
                        Toast.makeText(this, "Admin login failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
