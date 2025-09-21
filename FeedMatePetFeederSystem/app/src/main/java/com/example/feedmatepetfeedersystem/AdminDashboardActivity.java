package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView tvUserCount;
    private DatabaseReference usersRef;
    private ValueEventListener userCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Counter TextView
        tvUserCount = findViewById(R.id.tvUserCount);
        loadUserCount();

        // Manage Users button
        MaterialButton btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageUsers.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        });

        // History button
        MaterialButton btnFoodHistory = findViewById(R.id.btnFoodHistory);
        btnFoodHistory.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(AdminDashboardActivity.this, ProfileAdminActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_logout) {
                // ✅ Centralized logout
                logoutAdmin();
                return true;
            }
            return false;
        });
    }

    private void loadUserCount() {
        userCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null && !"admin".equalsIgnoreCase(user.getRole())) {
                        count++;
                    }
                }
                tvUserCount.setText(String.valueOf(count));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUserCount.setText("0");
            }
        };
        usersRef.addValueEventListener(userCountListener);
    }

    // ✅ Centralized logout method (always shows toast)
    private void logoutAdmin() {
        mAuth.signOut();
        Toast.makeText(AdminDashboardActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && userCountListener != null) {
            usersRef.removeEventListener(userCountListener); // ✅ Clean up listener
        }
    }
}
