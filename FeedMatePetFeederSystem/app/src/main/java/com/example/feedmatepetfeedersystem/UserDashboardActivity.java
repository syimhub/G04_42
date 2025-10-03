package com.example.feedmatepetfeedersystem;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private MaterialButton btnManualFeed;
    private MaterialButton btnHistory;
    private TextView tvFoodLevel;
    private TextView tvFoodWeight;
    private TextView tvNextFeedingTime;
    private TextView tvWelcome;
    private TextView tvLiveData;

    // Three separate feeding time boxes
    private TextView tvFeedingTime1, tvFeedingTime2, tvFeedingTime3;
    private ImageView btnEditTime1, btnEditTime2, btnEditTime3;

    private DatabaseReference deviceRef;
    private ValueEventListener deviceDataListener;

    private DatabaseReference userRef;
    private ValueEventListener userExistenceListener;

    private List<String> feedingTimes = new ArrayList<>();

    private final Handler countdownHandler = new Handler();
    private Runnable countdownRunnable;

    private boolean isEditingFeedingTime = false; // flag to indicate manual edit

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Initialize views
        mAuth = FirebaseAuth.getInstance();
        btnManualFeed = findViewById(R.id.btnManualFeed);
        btnHistory = findViewById(R.id.btnHistory);
        tvFoodLevel = findViewById(R.id.tvFoodLevel);
        tvFoodWeight = findViewById(R.id.tvFoodWeight);
        tvNextFeedingTime = findViewById(R.id.tvNextFeedingTime);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvLiveData = findViewById(R.id.tvLiveData);

        tvFeedingTime1 = findViewById(R.id.tvFeedingTime1);
        tvFeedingTime2 = findViewById(R.id.tvFeedingTime2);
        tvFeedingTime3 = findViewById(R.id.tvFeedingTime3);

        btnEditTime1 = findViewById(R.id.btnEditTime1);
        btnEditTime2 = findViewById(R.id.btnEditTime2);
        btnEditTime3 = findViewById(R.id.btnEditTime3);

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

        btnManualFeed.setOnClickListener(v -> feedNow());
        btnHistory.setOnClickListener(v -> startActivity(new Intent(UserDashboardActivity.this, HistoryActivity.class)));

        btnEditTime1.setOnClickListener(v -> editSingleFeedingTime(0));
        btnEditTime2.setOnClickListener(v -> editSingleFeedingTime(1));
        btnEditTime3.setOnClickListener(v -> editSingleFeedingTime(2));

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

        fetchFeederIdAndAttachListener();
        attachUserExistenceListener(currentUser.getUid());
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (deviceDataListener != null && deviceRef != null) {
            deviceRef.removeEventListener(deviceDataListener);
            deviceDataListener = null;
        }

        if (userExistenceListener != null && userRef != null) {
            userRef.removeEventListener(userExistenceListener);
            userExistenceListener = null;
        }

        if (countdownRunnable != null) countdownHandler.removeCallbacks(countdownRunnable);
    }

    private void logoutUser() {
        if (deviceDataListener != null && deviceRef != null) {
            deviceRef.removeEventListener(deviceDataListener);
            deviceDataListener = null;
        }
        if (userExistenceListener != null && userRef != null) {
            userRef.removeEventListener(userExistenceListener);
            userExistenceListener = null;
        }

        mAuth.signOut();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(0, 0);
        finish();
    }

    private void attachUserExistenceListener(String uid) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        userRef = database.getReference("users").child(uid);

        userExistenceListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(UserDashboardActivity.this,
                            "Your account has been removed by admin.",
                            Toast.LENGTH_LONG).show();
                    logoutUser();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("UserDashboard", "User existence check cancelled: " + error.getMessage());
            }
        };

        userRef.addValueEventListener(userExistenceListener);
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
                        new Handler().postDelayed(() ->
                                deviceRef.child("state").child("controls").child("feedNow").setValue(false), 3000);
                    } else {
                        Toast.makeText(this, "Failed to send feed command", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void editSingleFeedingTime(int index) {
        if (deviceRef == null || feedingTimes.isEmpty()) return;

        String current = feedingTimes.get(index);
        String[] split = current.split(":");
        int hour = Integer.parseInt(split[0]);
        int minute = Integer.parseInt(split[1]);

        TimePickerDialog picker = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> {
                    isEditingFeedingTime = true;
                    String formattedTime = String.format("%02d:%02d", hourOfDay, minute1);
                    feedingTimes.set(index, formattedTime);

                    sortTimes(feedingTimes);
                    updateFeedingTimeUI();

                    if (countdownRunnable != null) countdownHandler.removeCallbacks(countdownRunnable);
                    updateNextFeedingDisplay();

                    deviceRef.child("config").child("schedule").child("feedingTimes")
                            .setValue(feedingTimes)
                            .addOnCompleteListener(task -> {
                                isEditingFeedingTime = false;
                                if (task.isSuccessful())
                                    Toast.makeText(this, "Feeding time updated!", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(this, "Failed to update feeding time", Toast.LENGTH_SHORT).show();
                            });
                }, hour, minute, true);
        picker.show();
    }

    private void sortTimes(List<String> times) {
        long nowMinutes = System.currentTimeMillis() / 60000 % (24 * 60);
        times.sort((t1, t2) -> {
            long t1Min = toMinutes(t1);
            long t2Min = toMinutes(t2);
            long diff1 = (t1Min - nowMinutes + 24 * 60) % (24 * 60);
            long diff2 = (t2Min - nowMinutes + 24 * 60) % (24 * 60);
            return Long.compare(diff1, diff2);
        });
    }

    private long toMinutes(String time) {
        String[] split = time.split(":");
        int h = Integer.parseInt(split[0]);
        int m = Integer.parseInt(split[1]);
        return h * 60L + m;
    }

    private void updateFeedingTimeUI() {
        tvFeedingTime1.setText(feedingTimes.get(0));
        tvFeedingTime2.setText(feedingTimes.get(1));
        tvFeedingTime3.setText(feedingTimes.get(2));
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

                if (feederId == null || feederId.isEmpty()) feederId = uid;

                deviceRef = database.getReference("devices").child(feederId);
                listenToDeviceData();
            }

            @Override
            public void onCancelled(DatabaseError error) {
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
        if (deviceRef == null) return;

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
                    tvFeedingTime1.setText("--:--");
                    tvFeedingTime2.setText("--:--");
                    tvFeedingTime3.setText("--:--");
                    tvLiveData.setText("No device data available");
                    return;
                }

                try {
                    String foodLevel = "N/A";
                    if (snapshot.child("sensors").child("food").child("level").exists()) {
                        Object levelValue = snapshot.child("sensors").child("food").child("level").getValue();
                        if (levelValue instanceof Number) foodLevel = String.valueOf(((Number) levelValue).intValue());
                        else if (levelValue instanceof String) foodLevel = (String) levelValue;
                        tvFoodLevel.setText(foodLevel + "%");
                    } else tvFoodLevel.setText("N/A");

                    String foodWeight = "N/A";
                    if (snapshot.child("sensors").child("food").child("weight").exists()) {
                        Object weightValue = snapshot.child("sensors").child("food").child("weight").getValue();
                        if (weightValue instanceof Number) foodWeight = String.valueOf(((Number) weightValue).intValue());
                        else if (weightValue instanceof String) foodWeight = (String) weightValue;
                        tvFoodWeight.setText(foodWeight + "g");
                    } else tvFoodWeight.setText("N/A");

                    if (snapshot.child("config").child("schedule").child("feedingTimes").exists()) {
                        feedingTimes.clear();
                        for (DataSnapshot t : snapshot.child("config").child("schedule").child("feedingTimes").getChildren()) {
                            Object val = t.getValue();
                            if (val != null) feedingTimes.add(val.toString());
                        }
                        while (feedingTimes.size() < 3) feedingTimes.add("12:00");
                        updateFeedingTimeUI();
                        updateNextFeedingDisplay();
                    } else {
                        tvFeedingTime1.setText("--:--");
                        tvFeedingTime2.setText("--:--");
                        tvFeedingTime3.setText("--:--");
                        tvNextFeedingTime.setText("--:--");
                    }

                    boolean isFeeding = false;
                    if (snapshot.child("state").child("system").child("feedingInProgress").exists()) {
                        Object feedingValue = snapshot.child("state").child("system").child("feedingInProgress").getValue();
                        if (feedingValue instanceof Boolean) isFeeding = (Boolean) feedingValue;
                    }

                    StringBuilder liveData = new StringBuilder();
                    liveData.append("Food Level: ").append(foodLevel).append("%\n");
                    liveData.append("Food Weight: ").append(foodWeight).append("g\n");
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

    // ---------------------- NEW METHODS ----------------------

    private void updateNextFeedingDisplay() {
        if (feedingTimes.isEmpty()) {
            tvNextFeedingTime.setText("--:--");
            return;
        }

        Calendar now = Calendar.getInstance();
        int nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);

        String nextTime = "--:--";
        long minDiff = Long.MAX_VALUE;
        int closestIndex = -1;

        for (int i = 0; i < feedingTimes.size(); i++) {
            String t = feedingTimes.get(i);
            if (t == null || t.trim().isEmpty()) continue;

            long tMinutes = toMinutes(t.trim());
            long diff = (tMinutes - nowMinutes + 24 * 60) % (24 * 60);

            if (diff < minDiff) {
                minDiff = diff;
                nextTime = t.trim();
                closestIndex = i;
            }
        }

        long hours = minDiff / 60;
        long minutes = minDiff % 60;

        String timeDisplay;
        if (minDiff == 0) {
            timeDisplay = nextTime + "   |   Feeding now ⏳";
            tvNextFeedingTime.setText(timeDisplay);

            // Show quick message
            Toast.makeText(this, "Your pet is being fed now 🐾", Toast.LENGTH_SHORT).show();

            // After 5s, recalc next feeding time
            countdownHandler.postDelayed(this::updateNextFeedingDisplay, 5000);
        } else {
            timeDisplay = nextTime + "   |   Feeding in " +
                    (hours > 0 ? hours + "h " : "") +
                    (minutes > 0 ? minutes + "m" : "");
            tvNextFeedingTime.setText(timeDisplay);
        }

        // Reset all boxes and indicators
        findViewById(R.id.rowTime1).setBackgroundResource(R.drawable.time_box_bg);
        findViewById(R.id.rowTime2).setBackgroundResource(R.drawable.time_box_bg);
        findViewById(R.id.rowTime3).setBackgroundResource(R.drawable.time_box_bg);

        findViewById(R.id.tvUpcoming1).setVisibility(View.GONE);
        findViewById(R.id.tvUpcoming2).setVisibility(View.GONE);
        findViewById(R.id.tvUpcoming3).setVisibility(View.GONE);

        // Highlight + show indicator on closest time
        if (closestIndex == 0) {
            findViewById(R.id.rowTime1).setBackgroundResource(R.drawable.time_box_highlight);
            findViewById(R.id.tvUpcoming1).setVisibility(View.VISIBLE);
        } else if (closestIndex == 1) {
            findViewById(R.id.rowTime2).setBackgroundResource(R.drawable.time_box_highlight);
            findViewById(R.id.tvUpcoming2).setVisibility(View.VISIBLE);
        } else if (closestIndex == 2) {
            findViewById(R.id.rowTime3).setBackgroundResource(R.drawable.time_box_highlight);
            findViewById(R.id.tvUpcoming3).setVisibility(View.VISIBLE);
        }

        // Schedule refresh
        if (countdownRunnable != null) countdownHandler.removeCallbacks(countdownRunnable);
        countdownRunnable = this::updateNextFeedingDisplay;
        countdownHandler.postDelayed(countdownRunnable, 60000);
    }

}
