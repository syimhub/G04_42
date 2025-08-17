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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
                                user.sendEmailVerification().addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        Toast.makeText(SignUpActivity.this,
                                                "Signup successful! Please check your email to verify your account.",
                                                Toast.LENGTH_LONG).show();

                                        // Save user info to Realtime Database
                                        FirebaseDatabase db = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
                                        DatabaseReference dbRef = db.getReference("users");
                                        String uid = user.getUid();
                                        User userProfile = new User(user.getEmail(), "user"); // default role: user

                                        dbRef.child(uid).setValue(userProfile).addOnCompleteListener(dbTask -> {
                                            if (dbTask.isSuccessful()) {
                                                Toast.makeText(SignUpActivity.this, "User saved successfully!", Toast.LENGTH_SHORT).show();
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
