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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

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

    private OkHttpClient httpClient;
    private boolean isConnected = false;
    private String esp32Ip = "10.114.113.127";

    private DatabaseReference deviceRef;
    private ValueEventListener deviceDataListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize views based on your XML
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
        httpClient = new OkHttpClient();

        // Disable feed button until connected
        btnManualFeed.setEnabled(false);

        // Firebase user check
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String uid = currentUser.getUid();

        // Initialize Firebase Database with your specific URL
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
        deviceRef = database.getReference("devices").child(uid);

        // Set welcome message with user's name
        if (currentUser.getDisplayName() != null) {
            tvWelcome.setText("Welcome, " + currentUser.getDisplayName() + "!");
        } else {
            tvWelcome.setText("Welcome!");
        }

        // Set up connection button
        btnConnectFeeder.setOnClickListener(v -> {
            if (!isConnected) {
                connectToFeeder();
            } else {
                disconnectFromFeeder();
            }
        });

        // Set up manual feed button
        btnManualFeed.setOnClickListener(v -> feedNow());

        // Set up history button (add your history functionality)
        btnHistory.setOnClickListener(v -> {
            // Add your history activity intent here
            Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show();
        });

        // Set up edit feeding time button
        btnEditFeedingTime.setOnClickListener(v -> setNextFeedingTime());

        // Bottom Navigation setup
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileUserActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_logout) {
                logoutUser();
                return true;
            }
            return false;
        });

        // Check Firebase connection status
        checkFirebaseConnection();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start listening to device changes in Firebase when activity is visible
        listenToDeviceData();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop listening to Firebase when activity is not visible
        if (deviceDataListener != null) {
            deviceRef.removeEventListener(deviceDataListener);
        }
    }

    // 🔹 Centralized logout function
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

    // 🔹 Feed Now function (writes to Firebase)
    private void feedNow() {
        if (deviceRef == null) return;

        Toast.makeText(this, "Dispensing food...", Toast.LENGTH_SHORT).show();

        deviceRef.child("controls").child("feedNow").setValue(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Feeding command sent!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Failed to send feed command", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔹 Set Next Feeding Time function
    private void setNextFeedingTime() {
        if (deviceRef == null) return;

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    String formattedTime = String.format("%02d:%02d", hourOfDay, minute);
                    tvNextFeedingTime.setText(formattedTime); // Update UI immediately

                    // Ensure schedule node exists
                    deviceRef.child("schedule").child("nextFeedingTime").setValue(formattedTime)
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

    // 🔹 Real-time listener for device data (FIXED DATA TYPE CONVERSION)
    private void listenToDeviceData() {
        deviceDataListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    tvFoodLevel.setText("Loading...");
                    tvFoodWeight.setText("Loading...");
                    tvNextFeedingTime.setText("Loading...");
                    tvLiveData.setText("No device data available");

                    // Create initial device structure if it doesn't exist
                    initializeDeviceData();
                    return;
                }

                try {
                    // Update food level - handle both String and Number types
                    String foodLevel = "N/A";
                    if (snapshot.child("food").child("level").exists()) {
                        Object levelValue = snapshot.child("food").child("level").getValue();
                        if (levelValue instanceof String) {
                            foodLevel = (String) levelValue;
                        } else if (levelValue instanceof Number) {
                            foodLevel = String.valueOf(((Number) levelValue).intValue());
                        }
                        tvFoodLevel.setText(foodLevel + "%");
                    } else {
                        tvFoodLevel.setText("N/A");
                    }

                    // Update food weight - handle both String and Number types
                    String foodWeight = "N/A";
                    if (snapshot.child("food").child("weight").exists()) {
                        Object weightValue = snapshot.child("food").child("weight").getValue();
                        if (weightValue instanceof String) {
                            foodWeight = (String) weightValue;
                        } else if (weightValue instanceof Number) {
                            foodWeight = String.valueOf(((Number) weightValue).intValue());
                        }
                        tvFoodWeight.setText(foodWeight + "g");
                    } else {
                        tvFoodWeight.setText("N/A");
                    }

                    // Update next feeding time
                    String nextFeedingTime = "--:--";
                    if (snapshot.child("schedule").child("nextFeedingTime").exists()) {
                        Object timeValue = snapshot.child("schedule").child("nextFeedingTime").getValue();
                        if (timeValue instanceof String) {
                            nextFeedingTime = (String) timeValue;
                        }
                        if (nextFeedingTime != null && !nextFeedingTime.isEmpty()) {
                            tvNextFeedingTime.setText(nextFeedingTime);
                        } else {
                            tvNextFeedingTime.setText("--:--");
                        }
                    } else {
                        tvNextFeedingTime.setText("--:--");
                    }

                    // Display all data in Live View
                    StringBuilder liveData = new StringBuilder();
                    liveData.append("Food Level: ").append(foodLevel).append("%\n");
                    liveData.append("Food Weight: ").append(foodWeight).append("g\n");
                    liveData.append("Next Feeding: ").append(nextFeedingTime).append("\n\n");

                    // Add raw Firebase data for debugging
                    liveData.append("Raw Firebase Data:\n");
                    liveData.append(snapshot.toString());

                    tvLiveData.setText(liveData.toString());
                    Log.d("Firebase", "Data retrieved successfully: " + snapshot.toString());

                } catch (Exception e) {
                    Log.e("Firebase", "Error parsing data: " + e.getMessage());
                    tvLiveData.setText("Error loading data: " + e.getMessage());
                    Toast.makeText(UserDashboardActivity.this, "Error reading device data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvLiveData.setText("Database error: " + error.getMessage());
                Toast.makeText(UserDashboardActivity.this,
                        "Failed to read device data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        };

        // Add the listener to the database reference
        deviceRef.addValueEventListener(deviceDataListener);
    }

    // Helper method to initialize device data structure if it doesn't exist
    private void initializeDeviceData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
            DatabaseReference userDeviceRef = database.getReference("devices").child(uid);

            // Create initial data structure
            userDeviceRef.child("food").child("level").setValue(0);
            userDeviceRef.child("food").child("weight").setValue(0);
            userDeviceRef.child("schedule").child("nextFeedingTime").setValue("--:--");
            userDeviceRef.child("controls").child("feedNow").setValue(false);

            Toast.makeText(this, "Initialized new device data", Toast.LENGTH_SHORT).show();
        }
    }

    // Check Firebase connection status
    private void checkFirebaseConnection() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/");
        DatabaseReference connectedRef = database.getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d("Firebase", "Connected to Firebase");
                    // Append to existing text instead of replacing
                    String currentText = tvLiveData.getText().toString();
                    if (!currentText.contains("Firebase: Connected")) {
                        tvLiveData.append("\nFirebase: Connected");
                    }
                } else {
                    Log.d("Firebase", "Not connected to Firebase");
                    String currentText = tvLiveData.getText().toString();
                    if (!currentText.contains("Firebase: Disconnected")) {
                        tvLiveData.append("\nFirebase: Disconnected");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("Firebase", "Listener was cancelled");
            }
        });
    }

    private void connectToFeeder() {
        String url = "http://" + esp32Ip + "/position/90"; // Test connection

        Request request = new Request.Builder()
                .url(url)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(UserDashboardActivity.this,
                            "Connection failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    updateConnectionUI(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        Toast.makeText(UserDashboardActivity.this,
                                "Connected to feeder!",
                                Toast.LENGTH_SHORT).show();
                        updateConnectionUI(true);
                    } else {
                        Toast.makeText(UserDashboardActivity.this,
                                "Connection failed",
                                Toast.LENGTH_SHORT).show();
                        updateConnectionUI(false);
                    }
                });
            }
        });
    }

    private void disconnectFromFeeder() {
        updateConnectionUI(false);
        Toast.makeText(this, "Disconnected from feeder", Toast.LENGTH_SHORT).show();
    }

    private void updateConnectionUI(boolean connected) {
        isConnected = connected;
        runOnUiThread(() -> {
            btnConnectFeeder.setText(connected ? "DISCONNECT FEEDER" : "SYNC DATA TO FEEDER");
            btnConnectFeeder.setBackgroundTintList(
                    getResources().getColorStateList(
                            connected ? R.color.green : R.color.blue
                    )
            );
            btnManualFeed.setEnabled(connected);
        });
    }
}