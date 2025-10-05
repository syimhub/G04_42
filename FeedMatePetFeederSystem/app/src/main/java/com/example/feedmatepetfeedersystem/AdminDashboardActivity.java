package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView tvUserCount;
    private DatabaseReference usersRef;
    private ValueEventListener userCountListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // Redirect to login if not authenticated
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Initialize Firebase references
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
        usersRef = database.getReference("users");

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TextView
        tvUserCount = findViewById(R.id.tvUserCount);

        // Verify admin role first
        verifyAdminRole();

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
                logoutAdmin();
                return true;
            }
            return false;
        });
    }

    private void verifyAdminRole() {
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();
        DatabaseReference currentUserRef = usersRef.child(currentUserId).child("role");

        currentUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String role = snapshot.getValue(String.class);
                    if ("admin".equals(role)) {
                        Log.d("AdminDashboard", "✅ User is admin, loading data...");
                        // User is admin, load the data
                        loadUserCount();
                        setupButtons();
                    } else {
                        Log.e("AdminDashboard", "❌ User is not admin, role: " + role);
                        Toast.makeText(AdminDashboardActivity.this, "Access denied: Admin privileges required", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(AdminDashboardActivity.this, UserDashboardActivity.class));
                        finish();
                    }
                } else {
                    Log.e("AdminDashboard", "❌ User role not found");
                    Toast.makeText(AdminDashboardActivity.this, "User role not found", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminDashboard", "❌ Error verifying admin role: " + error.getMessage());
                Toast.makeText(AdminDashboardActivity.this, "Error verifying permissions", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupButtons() {
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

        // Set Schedule button
        MaterialButton btnSetSchedule = findViewById(R.id.btnSetSchedule);
        btnSetSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminSetScheduleActivity.class);
            startActivity(intent);
        });

        // Manual Feed button
        MaterialButton btnManualFeed = findViewById(R.id.btnManualFeed);
        btnManualFeed.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboardActivity.this, AdminManualFeedActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserCount() {
        userCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        if (ds.hasChild("role")) {
                            String role = ds.child("role").getValue(String.class);
                            if (role != null && "user".equalsIgnoreCase(role)) {
                                count++;
                            }
                        } else {
                            count++;
                        }
                    }
                    tvUserCount.setText(String.valueOf(count));
                    Log.d("AdminDashboard", "User count: " + count);
                } catch (Exception e) {
                    Log.e("AdminDashboard", "Error counting users: " + e.getMessage());
                    tvUserCount.setText("0");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                tvUserCount.setText("0");
                Log.e("AdminDashboard", "User count error: " + error.getMessage());
                Toast.makeText(AdminDashboardActivity.this, "Error loading user count: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        usersRef.addValueEventListener(userCountListener);
    }

    private void logoutAdmin() {
        if (usersRef != null && userCountListener != null) {
            usersRef.removeEventListener(userCountListener);
        }

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
            usersRef.removeEventListener(userCountListener);
        }
    }
}
