package com.example.feedmatepetfeedersystem;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminFeederStatusHistoryActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private SetScheduleUserAdapter adapter;
    private DatabaseReference dbRef;
    private FirebaseUser currentUser;

    private List<User> userList = new ArrayList<>();
    private HashMap<String, String> userFeederMap = new HashMap<>();

    private String selectedUserId = null;
    private String selectedFeederId = null;
    private String selectedUserName = null;

    private TextView tvSelectUserPrompt, tvFeederStatus, tvHistoryList;
    private View layoutFeederStatus;
    private ProgressBar progressBar;

    // ✅ New: Sensor section TextViews
    private TextView textFoodLevel, textFoodWeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feeder_status_history_admin);

        // ✅ Prevent toolbar clipping
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase setup
        dbRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // UI Components
        rvUsers = findViewById(R.id.recyclerUsers);
        tvSelectUserPrompt = findViewById(R.id.textSelectPrompt);
        layoutFeederStatus = findViewById(R.id.layoutFeederStatus);
        tvFeederStatus = findViewById(R.id.tvFeederStatus);
        tvHistoryList = findViewById(R.id.tvHistoryList);
        progressBar = findViewById(R.id.progressBar);

        // ✅ New sensor TextViews
        textFoodLevel = findViewById(R.id.textFoodLevel);
        textFoodWeight = findViewById(R.id.textFoodWeight);

        layoutFeederStatus.setVisibility(View.GONE);
        tvSelectUserPrompt.setVisibility(View.VISIBLE);

        // Recycler setup
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SetScheduleUserAdapter(userList, (user, position) -> {
            selectedUserId = user.getUid();
            selectedFeederId = userFeederMap.get(selectedUserId);
            selectedUserName = user.getFullName();

            if (selectedFeederId == null || selectedFeederId.isEmpty()) {
                Toast.makeText(this, "Selected user has no feeder assigned", Toast.LENGTH_SHORT).show();
                layoutFeederStatus.setVisibility(View.GONE);
                findViewById(R.id.layoutHistory).setVisibility(View.GONE);
                tvSelectUserPrompt.setVisibility(View.VISIBLE);
            } else {
                tvSelectUserPrompt.setVisibility(View.GONE);
                layoutFeederStatus.setVisibility(View.VISIBLE);
                findViewById(R.id.layoutHistory).setVisibility(View.VISIBLE);
                loadFeederStatus();
                loadFeedingHistory();
                loadSensorData();
            }
        });
        rvUsers.setAdapter(adapter);

        // Load users
        loadUsers();
    }

    /** Load user list **/
    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        dbRef.child("users")
                .orderByChild("email")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        userList.clear();
                        userFeederMap.clear();

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String uid = userSnap.getKey();
                            String fullName = userSnap.child("fullName").getValue(String.class);
                            String email = userSnap.child("email").getValue(String.class);
                            String role = userSnap.child("role").getValue(String.class);
                            String feederId = userSnap.child("feederId").getValue(String.class);

                            if (fullName != null && "user".equals(role)) {
                                User user = new User();
                                user.setUid(uid);
                                user.setFullName(fullName);
                                user.setEmail(email);
                                user.setRole(role);
                                userList.add(user);
                                userFeederMap.put(uid, feederId);
                            }
                        }

                        progressBar.setVisibility(View.GONE);

                        if (userList.isEmpty()) {
                            Toast.makeText(AdminFeederStatusHistoryActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AdminFeederStatusHistoryActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Load current feeder status **/
    private void loadFeederStatus() {
        if (selectedFeederId == null) return;

        dbRef.child("devices").child(selectedFeederId).child("state")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tvFeederStatus.setText("No feeder status available.");
                            return;
                        }

                        Boolean feedingInProgress = snapshot.child("system").child("feedingInProgress").getValue(Boolean.class);
                        String lastUpdate = snapshot.child("system").child("lastUpdate").getValue(String.class);
                        Long status = snapshot.child("system").child("status").getValue(Long.class);

                        String statusText = "🟢 Feeder Status\n" +
                                "• Feeding In Progress: " + ((feedingInProgress != null && feedingInProgress) ? "Yes" : "No") + "\n" +
                                "• Last Update: " + (lastUpdate != null ? lastUpdate : "N/A") + "\n" +
                                "• Status Code: " + (status != null ? status : 0);

                        tvFeederStatus.setText(statusText);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvFeederStatus.setText("Error loading feeder status: " + error.getMessage());
                    }
                });
    }


    /** Load last 3 feeding records (grouped by date, latest first) **/
    private void loadFeedingHistory() {
        if (selectedFeederId == null) return;

        dbRef.child("devices").child(selectedFeederId).child("history")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tvHistoryList.setText("No recent feeding history available.");
                            return;
                        }

                        class Record {
                            String date, time, source;
                            Long level, weight;
                            Record(String d, String t, String s, Long l, Long w) {
                                date = d; time = t; source = s; level = l; weight = w;
                            }
                        }

                        List<Record> allRecords = new ArrayList<>();

                        // Collect all records
                        for (DataSnapshot dateSnap : snapshot.getChildren()) {
                            String date = dateSnap.getKey();
                            for (DataSnapshot timeSnap : dateSnap.getChildren()) {
                                String time = timeSnap.getKey();
                                Long level = timeSnap.child("level").getValue(Long.class);
                                Long weight = timeSnap.child("weight").getValue(Long.class);
                                String source = timeSnap.child("source").getValue(String.class);
                                allRecords.add(new Record(date, time, source, level, weight));
                            }
                        }

                        // Sort by date + time (ascending)
                        allRecords.sort((a, b) -> (a.date + a.time).compareTo(b.date + b.time));

                        // Keep only the last 3 overall
                        int start = Math.max(allRecords.size() - 3, 0);
                        List<Record> latestThree = allRecords.subList(start, allRecords.size());

                        // Group by date (latest first)
                        HashMap<String, List<Record>> grouped = new HashMap<>();
                        for (Record r : latestThree) {
                            if (!grouped.containsKey(r.date)) grouped.put(r.date, new ArrayList<>());
                            grouped.get(r.date).add(r);
                        }

                        // Sort dates descending (latest first)
                        List<String> sortedDates = new ArrayList<>(grouped.keySet());
                        sortedDates.sort((a, b) -> b.compareTo(a));

                        // Build formatted text
                        StringBuilder sb = new StringBuilder("📜 Latest Feeding History:\n");

                        for (String date : sortedDates) {
                            // Convert "YYYY-MM-DD" to readable format
                            String readableDate = formatReadableDate(date);
                            sb.append("\n📅 ").append(readableDate).append("\n");

                            // Sort times descending (latest first)
                            List<Record> dayRecords = grouped.get(date);
                            dayRecords.sort((a, b) -> b.time.compareTo(a.time));

                            for (Record r : dayRecords) {
                                sb.append("   ⏰ ").append(r.time)
                                        .append("\n      Source: ").append(r.source != null ? r.source : "N/A")
                                        .append("\n      Food Level After Feeding: ").append(r.level != null ? r.level : 0).append("%")
                                        .append("\n      Dispensed Weight: ").append(r.weight != null ? r.weight : 0).append("g\n");
                            }
                        }

                        tvHistoryList.setText(sb.toString());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvHistoryList.setText("Error loading history: " + error.getMessage());
                    }
                });
    }

    /** Convert YYYY-MM-DD → "Month DD, YYYY" **/
    private String formatReadableDate(String date) {
        try {
            java.text.SimpleDateFormat src = new java.text.SimpleDateFormat("yyyy-MM-dd");
            java.text.SimpleDateFormat dest = new java.text.SimpleDateFormat("MMMM d, yyyy");
            java.util.Date parsed = src.parse(date);
            return dest.format(parsed);
        } catch (Exception e) {
            return date; // fallback if format mismatch
        }
    }


    /** ✅ Live auto-updating sensor data **/
    private void loadSensorData() {
        if (selectedFeederId == null) return;

        dbRef.child("devices").child(selectedFeederId).child("sensors")
                .addValueEventListener(new ValueEventListener() { // <-- changed from addListenerForSingleValueEvent
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long level = snapshot.child("food").child("level").getValue(Long.class);
                        Long weight = snapshot.child("food").child("weight").getValue(Long.class);

                        textFoodLevel.setText("Current Food Level: " + (level != null ? level : 0) + "%");
                        textFoodWeight.setText("Current Food Weight: " + (weight != null ? weight : 0) + "g");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminFeederStatusHistoryActivity.this,
                                "Failed to load sensor data: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
