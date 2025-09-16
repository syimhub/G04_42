package com.example.feedmatepetfeedersystem;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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
    private Button btnConnectFeeder;
    private Button btnManualFeed;
    private TextView tvFoodLevel;
    private TextView tvFoodWeight;
    private TextView tvNextFeedingTime;
    private OkHttpClient httpClient;
    private boolean isConnected = false;
    private String esp32Ip = "10.114.113.127";

    private DatabaseReference deviceRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize views
        mAuth = FirebaseAuth.getInstance();
        btnConnectFeeder = findViewById(R.id.btnConnectFeeder);
        btnManualFeed = findViewById(R.id.btnManualFeed);
        tvFoodLevel = findViewById(R.id.tvFoodLevel);
        tvFoodWeight = findViewById(R.id.tvFoodWeight);
        tvNextFeedingTime = findViewById(R.id.tvNextFeedingTime);
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
        deviceRef = FirebaseDatabase.getInstance().getReference("devices").child(uid);

        // Set welcome message with user's name
        if (currentUser.getDisplayName() != null) {
            TextView tvWelcome = findViewById(R.id.tvWelcome);
            tvWelcome.setText("Welcome, " + currentUser.getDisplayName() + "!");
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

        // Start listening to device changes in Firebase
        listenToDeviceData();
    }

    // 🔹 Centralized logout function
    private void logoutUser() {
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

    // 🔹 Real-time listener for device data
    private void listenToDeviceData() {
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                // Update food level
                DataSnapshot foodSnapshot = snapshot.child("food").child("level");
                if (foodSnapshot.exists()) {
                    tvFoodLevel.setText(foodSnapshot.getValue(String.class) + "%");
                }

                // Update food weight (if you track weight separately)
                DataSnapshot weightSnapshot = snapshot.child("food").child("weight");
                if (weightSnapshot.exists()) {
                    tvFoodWeight.setText(weightSnapshot.getValue(String.class) + "g");
                }

                // Update next feeding time with default handling
                DataSnapshot nextFeedingSnapshot = snapshot.child("schedule").child("nextFeedingTime");
                String time = "--:--";
                if (nextFeedingSnapshot.exists()) {
                    String fetchedTime = nextFeedingSnapshot.getValue(String.class);
                    if (fetchedTime != null && !fetchedTime.isEmpty()) {
                        time = fetchedTime;
                    }
                }
                tvNextFeedingTime.setText(time);

                // Debug log
                Log.d("Firebase", "Next feeding time: " + time);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this,
                        "Failed to read device data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
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
            btnConnectFeeder.setText(connected ? "DISCONNECT" : "CONNECT TO FEEDER");
            btnConnectFeeder.setBackgroundTintList(
                    getResources().getColorStateList(
                            connected ? R.color.green : R.color.blue
                    )
            );
            btnManualFeed.setEnabled(connected);
        });
    }

}
