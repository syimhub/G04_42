package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
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
            // Check role from database
            FirebaseDatabase db = FirebaseDatabase.getInstance(
                    "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference userRef = db.getReference("users").child(currentUser.getUid());

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    String role = snapshot.child("role").getValue(String.class);

                    if ("admin".equalsIgnoreCase(role) && currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, AdminDashboardActivity.class));
                        finish();
                    } else if ("user".equalsIgnoreCase(role) && currentUser.isEmailVerified()) {
                        startActivity(new Intent(MainActivity.this, UserDashboardActivity.class));
                        finish();
                    } else {
                        // Unverified users go to SignUpActivity
                        startActivity(new Intent(MainActivity.this, SignUpActivity.class));
                        finish();
                    }
                } else {
                    startActivity(new Intent(MainActivity.this, SignUpActivity.class));
                    finish();
                }
            });
        } else {
            // No user logged in
            startActivity(new Intent(MainActivity.this, SignUpActivity.class));
            finish();
        }
    }
}
