package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
        btnManualFeed.setOnClickListener(v -> {
            sendFeedCommand();
            Toast.makeText(this, "Dispensing food...", Toast.LENGTH_SHORT).show();
        });

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

        // Initialize food level indicators
        updateFoodLevels();
    }

    // 🔹 Centralized logout function
    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(0, 0);
        finish();
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
                        updateFoodLevels();
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

    private void sendFeedCommand() {
        // First move to feeding position (0 degrees)
        String feedUrl = "http://" + esp32Ip + "/position/0";

        Request feedRequest = new Request.Builder()
                .url(feedUrl)
                .build();

        httpClient.newCall(feedRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(UserDashboardActivity.this,
                                "Feeding failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // After feeding, wait a moment then return to home position
                try {
                    Thread.sleep(1500); // Wait 1.5 seconds for food to dispense
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Now send command to return to home position (90 degrees)
                String returnUrl = "http://" + esp32Ip + "/position/90";
                Request returnRequest = new Request.Builder()
                        .url(returnUrl)
                        .build();

                httpClient.newCall(returnRequest).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        runOnUiThread(() ->
                                Toast.makeText(UserDashboardActivity.this,
                                        "Return failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show()
                        );
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        runOnUiThread(() -> {
                            // Update food levels after feeding
                            updateFoodLevels();
                            Toast.makeText(UserDashboardActivity.this,
                                    "Feeding completed!",
                                    Toast.LENGTH_SHORT).show();
                        });
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

    private void updateFoodLevels() {
        // In a real app, you would get these values from the ESP32
        // For now, we'll use mock data
        runOnUiThread(() -> {
            tvFoodLevel.setText(isConnected ? "75%" : "Not connected");
            tvFoodWeight.setText(isConnected ? "250g" : "Not connected");
            tvNextFeedingTime.setText("12:00 PM"); // Example static time
        });
    }
}
