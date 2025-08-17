package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileAdminActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_admin);

        mAuth = FirebaseAuth.getInstance();

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_admin);
        bottomNav.setSelectedItemId(R.id.nav_profile); // Highlight Profile

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileAdminActivity.this, AdminDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true; // Already on profile
            } else if (id == R.id.nav_logout) {
                logoutAdmin();
                return true;
            }
            return false;
        });
    }

    private void logoutAdmin() {
        mAuth.signOut();
        Intent intent = new Intent(ProfileAdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
