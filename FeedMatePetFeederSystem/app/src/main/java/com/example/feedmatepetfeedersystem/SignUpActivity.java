package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

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

        mAuth = FirebaseAuth.getInstance();

        signupButton.setOnClickListener(v -> {
            String email = signupEmail.getText().toString().trim();
            String password = signupPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(SignUpActivity.this, "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) { // Firebase requires min 6 chars for password
                Toast.makeText(SignUpActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create user in Firebase Authentication
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Toast.makeText(SignUpActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                finish();
                            }
                        } else {
                            String errorMessage = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Unknown error occurred";

                            Toast.makeText(SignUpActivity.this,
                                    "Signup failed: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
