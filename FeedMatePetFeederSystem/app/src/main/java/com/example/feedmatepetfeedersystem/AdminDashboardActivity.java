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

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private TextView tvUserCount, tvFoodLevel, tvLowFoodAlert;
    private DatabaseReference usersRef, devicesRef;
    private ValueEventListener userCountListener, devicesDataListener;

    private List<String> deviceIds = new ArrayList<>();
    private int totalFoodLevel = 0;
    private int deviceCount = 0;
    private boolean hasLowFoodAlert = false;

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
        devicesRef = database.getReference("devices");

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize TextViews
        tvUserCount = findViewById(R.id.tvUserCount);
        tvFoodLevel = findViewById(R.id.tvFoodLevel);
        tvLowFoodAlert = findViewById(R.id.tvLowFoodAlert);

        // Set initial values
        tvFoodLevel.setText("0%");
        tvLowFoodAlert.setText("Food level: Connecting...");

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
                        loadDevicesData();
                        setupButtons();
                    } else {
                        Log.e("AdminDashboard", "❌ User is not admin, role: " + role);
                        Toast.makeText(AdminDashboardActivity.this, "Access denied: Admin privileges required", Toast.LENGTH_LONG).show();
                        // Redirect to user dashboard or login
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
            Toast.makeText(this, "Set Schedule feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Manual Feed button
        MaterialButton btnManualFeed = findViewById(R.id.btnManualFeed);
        btnManualFeed.setOnClickListener(v -> {
            Toast.makeText(this, "Manual Feed feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadUserCount() {
        userCountListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    int count = 0;
                    for (DataSnapshot ds : snapshot.getChildren()) {
                        // Check if this is a user node with role field
                        if (ds.hasChild("role")) {
                            String role = ds.child("role").getValue(String.class);
                            if (role != null && "user".equalsIgnoreCase(role)) {
                                count++;
                            }
                        } else {
                            // If no role field, count as user (for backward compatibility)
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

    private void loadDevicesData() {
        devicesDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d("AdminDashboard", "Devices data changed, total devices: " + snapshot.getChildrenCount());

                deviceIds.clear();
                totalFoodLevel = 0;
                deviceCount = 0;
                hasLowFoodAlert = false;

                // Check if any devices exist
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    Log.d("AdminDashboard", "No devices found in database");
                    updateUIForNoDevices();
                    return;
                }

                // Process each device
                for (DataSnapshot deviceSnapshot : snapshot.getChildren()) {
                    String deviceId = deviceSnapshot.getKey();
                    if (deviceId != null) {
                        deviceIds.add(deviceId);
                        processDeviceData(deviceSnapshot, deviceId);
                    }
                }

                Log.d("AdminDashboard", "Processed " + deviceCount + " devices with food data");
                updateAggregatedFoodLevel();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AdminDashboard", "Devices data error: " + error.getMessage());
                updateUIForError("Database error: " + error.getMessage());
                Toast.makeText(AdminDashboardActivity.this, "Error loading device data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // Add listener with error handling
        devicesRef.addValueEventListener(devicesDataListener);
    }

    private void processDeviceData(DataSnapshot deviceSnapshot, String deviceId) {
        try {
            Log.d("AdminDashboard", "Processing device: " + deviceId);

            int foodLevel = 0;
            boolean hasValidData = false;

            // Method 1: Try to get food level directly
            if (deviceSnapshot.hasChild("sensors") &&
                    deviceSnapshot.child("sensors").hasChild("food") &&
                    deviceSnapshot.child("sensors").child("food").hasChild("level")) {

                Object levelValue = deviceSnapshot.child("sensors").child("food").child("level").getValue();
                if (levelValue != null) {
                    if (levelValue instanceof Long) {
                        foodLevel = ((Long) levelValue).intValue();
                        hasValidData = true;
                    } else if (levelValue instanceof Integer) {
                        foodLevel = (Integer) levelValue;
                        hasValidData = true;
                    } else if (levelValue instanceof Double) {
                        foodLevel = ((Double) levelValue).intValue();
                        hasValidData = true;
                    } else if (levelValue instanceof String) {
                        try {
                            String levelStr = ((String) levelValue).replace("%", "").trim();
                            foodLevel = Integer.parseInt(levelStr);
                            hasValidData = true;
                        } catch (NumberFormatException e) {
                            Log.w("AdminDashboard", "Invalid food level format: " + levelValue);
                        }
                    }
                }
            }

            if (hasValidData) {
                Log.d("AdminDashboard", "Device " + deviceId + " food level: " + foodLevel + "%");

                // Check for low food alert
                if (foodLevel < 20) {
                    hasLowFoodAlert = true;
                }

                totalFoodLevel += foodLevel;
                deviceCount++;
            } else {
                Log.w("AdminDashboard", "No valid food data for device: " + deviceId);
                // Add default value for devices with no data
                totalFoodLevel += 0;
                deviceCount++;
            }

            // Check explicit low food alerts
            if (deviceSnapshot.hasChild("alerts") &&
                    deviceSnapshot.child("alerts").hasChild("lowFoodLevel")) {
                Object alertValue = deviceSnapshot.child("alerts").child("lowFoodLevel").getValue();
                if (alertValue instanceof Boolean && (Boolean) alertValue) {
                    hasLowFoodAlert = true;
                }
            }

        } catch (Exception e) {
            Log.e("AdminDashboard", "Error processing device " + deviceId + ": " + e.getMessage());
            // Still count this device but with 0 food level
            totalFoodLevel += 0;
            deviceCount++;
        }
    }

    private void updateAggregatedFoodLevel() {
        runOnUiThread(() -> {
            if (deviceCount == 0) {
                updateUIForNoData();
                return;
            }

            int averageFoodLevel = totalFoodLevel / deviceCount;

            // Update food level display
            tvFoodLevel.setText(averageFoodLevel + "%");

            // Color coding based on level
            if (averageFoodLevel < 20) {
                tvFoodLevel.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else if (averageFoodLevel < 50) {
                tvFoodLevel.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvFoodLevel.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            // Update alert message
            if (hasLowFoodAlert) {
                tvLowFoodAlert.setText("⚠️ Low food alert on one or more devices!");
                tvLowFoodAlert.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            } else {
                tvLowFoodAlert.setText("Food level: Normal (" + deviceCount + " devices)");
                tvLowFoodAlert.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }

            Log.d("AdminDashboard", "Final display - Average: " + averageFoodLevel + "%, Devices: " + deviceCount);
        });
    }

    private void updateUIForNoDevices() {
        runOnUiThread(() -> {
            tvFoodLevel.setText("0%");
            tvFoodLevel.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvLowFoodAlert.setText("Food level: No devices connected");
            tvLowFoodAlert.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });
    }

    private void updateUIForNoData() {
        runOnUiThread(() -> {
            tvFoodLevel.setText("0%");
            tvFoodLevel.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvLowFoodAlert.setText("Food level: No data available");
            tvLowFoodAlert.setTextColor(getResources().getColor(android.R.color.darker_gray));
        });
    }

    private void updateUIForError(String errorMessage) {
        runOnUiThread(() -> {
            tvFoodLevel.setText("0%");
            tvFoodLevel.setTextColor(getResources().getColor(android.R.color.darker_gray));
            tvLowFoodAlert.setText("Food level: " + errorMessage);
            tvLowFoodAlert.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        });
    }

    private void logoutAdmin() {
        // Remove listeners first
        if (usersRef != null && userCountListener != null) {
            usersRef.removeEventListener(userCountListener);
        }
        if (devicesRef != null && devicesDataListener != null) {
            devicesRef.removeEventListener(devicesDataListener);
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
        // Clean up listeners
        if (usersRef != null && userCountListener != null) {
            usersRef.removeEventListener(userCountListener);
        }
        if (devicesRef != null && devicesDataListener != null) {
            devicesRef.removeEventListener(devicesDataListener);
        }
    }
}