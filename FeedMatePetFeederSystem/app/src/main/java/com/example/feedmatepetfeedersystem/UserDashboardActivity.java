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

        // ENABLE THE FEED BUTTON IMMEDIATELY (you can choose to enable only after connect)
        btnManualFeed.setEnabled(true);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (currentUser.getDisplayName() != null) {
            tvWelcome.setText("Welcome, " + currentUser.getDisplayName() + "!");
        } else {
            tvWelcome.setText("Welcome!");
        }

        // Buttons
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

        // IMPORTANT: fetch the user's feederId from /users/<uid>/feederId,
        // then set deviceRef = /devices/<feederId>, then call listenToDeviceData()
        fetchFeederIdAndAttachListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // don't start a second listener here — listenToDeviceData will be invoked after feederId fetch.
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceDataListener != null && deviceRef != null) {
            deviceRef.removeEventListener(deviceDataListener);
            deviceDataListener = null;
        }
    }

    private void logoutUser() {
        if (deviceDataListener != null && deviceRef != null) {
            deviceRef.removeEventListener(deviceDataListener);
            deviceDataListener = null;
        }
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    private void feedNow() {
        if (deviceRef == null) {
            Toast.makeText(this, "Device not initialized yet", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Dispensing food...", Toast.LENGTH_SHORT).show();

        deviceRef.child("state").child("controls").child("feedNow").setValue(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Feeding command sent!", Toast.LENGTH_SHORT).show();

                        // Reset the feed command after 3 seconds (optional)
                        new android.os.Handler().postDelayed(() -> {
                            deviceRef.child("state").child("controls").child("feedNow").setValue(false);
                        }, 3000);

                    } else {
                        Toast.makeText(this, "Failed to send feed command", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setNextFeedingTime() {
        if (deviceRef == null) {
            Toast.makeText(this, "Device not initialized yet", Toast.LENGTH_SHORT).show();
            return;
        }

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

    private void fetchFeederIdAndAttachListener() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );

        DatabaseReference userFeederRef = database.getReference("users").child(uid).child("feederId");
        userFeederRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String feederId = null;
                if (snapshot.exists()) {
                    Object v = snapshot.getValue();
                    if (v != null) feederId = v.toString();
                }

                // If feederId is missing, fallback to UID (if your signup uses UID as feederId)
                if (feederId == null || feederId.isEmpty()) {
                    feederId = uid;
                    Log.w("UserDashboard", "feederId missing for user; falling back to uid: " + uid);
                }

                Log.d("UserDashboard", "Using feederId: " + feederId + " for uid: " + uid);

                // Set deviceRef and start listening
                deviceRef = database.getReference("devices").child(feederId);
                listenToDeviceData();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("UserDashboard", "Failed to read feederId: " + error.getMessage());
                // Fallback: try to use UID as feederId
                String feederId = uid;
                FirebaseDatabase database = FirebaseDatabase.getInstance(
                        "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/"
                );
                deviceRef = database.getReference("devices").child(feederId);
                listenToDeviceData();
            }
        });
    }

    private void listenToDeviceData() {
        if (deviceRef == null) {
            Log.w("UserDashboard", "listenToDeviceData called but deviceRef is null");
            return;
        }

        // Remove existing listener if any
        if (deviceDataListener != null) {
            try { deviceRef.removeEventListener(deviceDataListener); } catch (Exception ignored) {}
            deviceDataListener = null;
        }

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
                    } else {
                        tvFoodLevel.setText("N/A");
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
                    } else {
                        tvFoodWeight.setText("N/A");
                    }

                    String nextFeedingTime = "--:--";
                    if (snapshot.child("config").child("schedule").child("nextFeedingTime").exists()) {
                        Object timeValue = snapshot.child("config").child("schedule").child("nextFeedingTime").getValue();
                        if (timeValue instanceof String) {
                            nextFeedingTime = (String) timeValue;
                        }
                        tvNextFeedingTime.setText(nextFeedingTime != null && !nextFeedingTime.isEmpty() ? nextFeedingTime : "--:--");
                    }

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
