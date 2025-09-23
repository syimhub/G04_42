package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            FirebaseDatabase db = FirebaseDatabase.getInstance(
                    "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference userRef = db.getReference("users").child(currentUser.getUid());

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();

                    // ✅ Extra check: block if account node has been deleted by admin
                    if (snapshot == null || !snapshot.exists()) {
                        Toast.makeText(MainActivity.this,
                                "Your account has been removed. Please contact support.",
                                Toast.LENGTH_LONG).show();
                        mAuth.signOut();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                        return;
                    }

                    String role = snapshot.child("role").getValue(String.class);

                    if ("admin".equalsIgnoreCase(role) && currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                        finish();
                    } else if ("user".equalsIgnoreCase(role) && currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, UserDashboardActivity.class));
                        finish();
                    } else {
                        // Unverified or invalid role → send to SignUpActivity
                        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
                        finish();
                    }
                } else {
                    // Database fetch failed → fallback to login
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            });
        } else {
            // No user logged in → go to login
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
}
