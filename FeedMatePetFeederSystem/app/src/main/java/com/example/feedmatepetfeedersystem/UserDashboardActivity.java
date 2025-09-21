package com.example.feedmatepetfeedersystem;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private MaterialButton btnConnectFeeder;
    private MaterialButton btnManualFeed;
    private MaterialButton btnHistory;
    private TextView tvFoodLevel;
    private TextView tvFoodWeight;
    private TextView tvNextFeedingTime;
    private TextView tvWelcome;
    private TextView tvLiveData;

    private DatabaseReference deviceRef;
    private ValueEventListener deviceDataListener;

    // Device ID constant
    private static final String ESP32_DEVICE_ID = "HGxcHbt7LKMwCTsEDEivQqv5DO13";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize views
        mAuth = FirebaseAuth.getInstance();
        btnConnectFeeder = findViewById(R.id.btnConnectFeeder);
        btnManualFeed = findViewById(R.id.btnManualFeed);
        btnHistory = findViewById(R.id.btnHistory);
        tvFoodLevel = findViewById(R.id.tvFoodLevel);
        tvFoodWeight = findViewById(R.id.tvFoodWeight);
        tvNextFeedingTime = findViewById(R.id.tvNextFeedingTime);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLiveData = findViewById(R.id.tvLiveData);
        ImageView btnEditFeedingTime = findViewById(R.id.btnEditFeedingTime);

        // ENABLE THE FEED BUTTON IMMEDIATELY
        btnManualFeed.setEnabled(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
        deviceRef = database.getReference("devices").child(ESP32_DEVICE_ID);

        if (currentUser.getDisplayName() != null) {
            tvWelcome.setText("Welcome, " + currentUser.getDisplayName() + "!");
        } else {
            tvWelcome.setText("Welcome!");
        }

        // SIMPLIFIED CONNECTION - JUST TOGGLE UI STATE
        btnConnectFeeder.setOnClickListener(v -> {
            if (btnConnectFeeder.getText().toString().equals("SYNC DATA TO FEEDER")) {
                connectToFeeder();
            } else {
                disconnectFromFeeder();
            }
        });

        btnManualFeed.setOnClickListener(v -> feedNow());

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnEditFeedingTime.setOnClickListener(v -> setNextFeedingTime());

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) return true;
            else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileUserActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_logout) {
                logoutUser();
                return true;
            }
            return false;
        });

        // Start listening to device data immediately
        listenToDeviceData();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenToDeviceData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceDataListener != null) {
            deviceRef.removeEventListener(deviceDataListener);
        }
    }

    private void logoutUser() {
        if (deviceDataListener != null) {
            deviceRef.removeEventListener(deviceDataListener);
        }
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    private void feedNow() {
        if (deviceRef == null) return;

        Toast.makeText(this, "Dispensing food...", Toast.LENGTH_SHORT).show();

        // Send feed command to Firebase
        deviceRef.child("state").child("controls").child("feedNow").setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Feeding command sent!", Toast.LENGTH_SHORT).show();

                        // Reset the feed command after 3 seconds
                        new android.os.Handler().postDelayed(() -> {
                            deviceRef.child("state").child("controls").child("feedNow").setValue(false);
                        }, 3000);

                    } else {
                        Toast.makeText(this, "Failed to send feed command", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setNextFeedingTime() {
        if (deviceRef == null) return;

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                    tvNextFeedingTime.setText(formattedTime);

                    deviceRef.child("config").child("schedule").child("nextFeedingTime").setValue(formattedTime)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Next feeding time set!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to set feeding time", Toast.LENGTH_SHORT).show();
                                }
                            });
                }, 12, 0, false);

        timePicker.show();
    }

    private void listenToDeviceData() {
        deviceDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvFoodLevel.setText("No data");
                    tvFoodWeight.setText("No data");
                    tvNextFeedingTime.setText("--:--");
                    tvLiveData.setText("No device data available");
                    return;
                }

                try {
                    String foodLevel = "N/A";
                    if (snapshot.child("sensors").child("food").child("level").exists()) {
                        Object levelValue = snapshot.child("sensors").child("food").child("level").getValue();
                        if (levelValue instanceof Number) {
                            foodLevel = String.valueOf(((Number) levelValue).intValue());
                        } else if (levelValue instanceof String) {
                            foodLevel = (String) levelValue;
                        }
                        tvFoodLevel.setText(foodLevel + "%");
                    }

                    String foodWeight = "N/A";
                    if (snapshot.child("sensors").child("food").child("weight").exists()) {
                        Object weightValue = snapshot.child("sensors").child("food").child("weight").getValue();
                        if (weightValue instanceof Number) {
                            foodWeight = String.valueOf(((Number) weightValue).intValue());
                        } else if (weightValue instanceof String) {
                            foodWeight = (String) weightValue;
                        }
                        tvFoodWeight.setText(foodWeight + "g");
                    }

                    String nextFeedingTime = "--:--";
                    if (snapshot.child("config").child("schedule").child("nextFeedingTime").exists()) {
                        Object timeValue = snapshot.child("config").child("schedule").child("nextFeedingTime").getValue();
                        if (timeValue instanceof String) {
                            nextFeedingTime = (String) timeValue;
                        }
                        tvNextFeedingTime.setText(nextFeedingTime != null && !nextFeedingTime.isEmpty() ? nextFeedingTime : "--:--");
                    }

                    // Check if feeding is in progress
                    boolean isFeeding = false;
                    if (snapshot.child("state").child("system").child("feedingInProgress").exists()) {
                        Object feedingValue = snapshot.child("state").child("system").child("feedingInProgress").getValue();
                        if (feedingValue instanceof Boolean) {
                            isFeeding = (Boolean) feedingValue;
                        }
                    }

                    StringBuilder liveData = new StringBuilder();
                    liveData.append("Food Level: ").append(foodLevel).append("%\n");
                    liveData.append("Food Weight: ").append(foodWeight).append("g\n");
                    liveData.append("Next Feeding: ").append(nextFeedingTime).append("\n");
                    liveData.append("Feeding Status: ").append(isFeeding ? "IN PROGRESS" : "READY").append("\n");

                    tvLiveData.setText(liveData.toString());
                    Log.d("Firebase", "Data retrieved: " + snapshot.toString());

                } catch (Exception e) {
                    Log.e("Firebase", "Error parsing data: " + e.getMessage());
                    tvLiveData.setText("Error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvLiveData.setText("Database error: " + error.getMessage());
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        };

        deviceRef.addValueEventListener(deviceDataListener);
    }

    private void connectToFeeder() {
        // Since we're using Firebase, connection is always available
        updateConnectionUI(true);
        Toast.makeText(this, "Connected to feeder via Firebase!", Toast.LENGTH_SHORT).show();
    }

    private void disconnectFromFeeder() {
        updateConnectionUI(false);
        Toast.makeText(this, "Disconnected from feeder", Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionUI(boolean connected) {
        runOnUiThread(() -> {
            btnConnectFeeder.setText(connected ? "DISCONNECT FEEDER" : "SYNC DATA TO FEEDER");
            btnConnectFeeder.setBackgroundTintList(getResources().getColorStateList(connected ? R.color.green : R.color.blue));
        });
    }
}