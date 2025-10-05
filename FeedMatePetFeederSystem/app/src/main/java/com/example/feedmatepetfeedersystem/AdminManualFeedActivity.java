package com.example.feedmatepetfeedersystem;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

public class AdminManualFeedActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private SetScheduleUserAdapter adapter;
    private DatabaseReference dbRef;
    private FirebaseUser currentUser;

    private List<User> userList = new ArrayList<>();
    private HashMap<String, String> userFeederMap = new HashMap<>();

    private String selectedUserId = null;
    private String selectedFeederId = null;
    private String selectedUserName = null;

    private TextView tvSelectUserPrompt, tvLatestHistory, tvFeedStatus;
    private View layoutManualFeed;
    private MaterialButton btnFeedNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manual_feed_admin);

        // ✅ Prevent toolbar clipping under status bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
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
        rvUsers = findViewById(R.id.rvUsers);
        tvSelectUserPrompt = findViewById(R.id.tvSelectUserPrompt);
        layoutManualFeed = findViewById(R.id.layoutManualFeed);
        tvLatestHistory = findViewById(R.id.tvLatestHistory);
        tvFeedStatus = findViewById(R.id.tvFeedStatus);
        btnFeedNow = findViewById(R.id.btnFeedNow);

        layoutManualFeed.setVisibility(View.GONE);
        tvSelectUserPrompt.setVisibility(View.VISIBLE);

        // Recycler setup
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SetScheduleUserAdapter(userList, (user, position) -> {
            selectedUserId = user.getUid();
            selectedFeederId = userFeederMap.get(selectedUserId);
            selectedUserName = user.getFullName();

            if (selectedFeederId == null || selectedFeederId.isEmpty()) {
                Toast.makeText(AdminManualFeedActivity.this, "Selected user has no feeder assigned", Toast.LENGTH_SHORT).show();
                layoutManualFeed.setVisibility(View.GONE);
                tvSelectUserPrompt.setVisibility(View.VISIBLE);
            } else {
                tvSelectUserPrompt.setVisibility(View.GONE);
                layoutManualFeed.setVisibility(View.VISIBLE);
                loadLatestHistory();
            }
        });
        rvUsers.setAdapter(adapter);

        // Load users
        loadUsers();

        // ✅ Feed Now with confirmation dialog
        btnFeedNow.setOnClickListener(v -> {
            if (selectedFeederId == null) {
                Toast.makeText(this, "Select a user first", Toast.LENGTH_SHORT).show();
                return;
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Manual Feeding")
                    .setMessage("Are you sure you want to start feeding now?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        tvFeedStatus.setText("Status: Feeding in progress...");

                        dbRef.child("devices").child(selectedFeederId)
                                .child("state").child("controls").child("feedNow")
                                .setValue(true)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Feeding started for " + selectedUserName, Toast.LENGTH_SHORT).show();
                                    logFeedingHistory();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Failed to start feeding: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .show();
        });
    }

    /** Load user list **/
    private void loadUsers() {
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

                        if (userList.isEmpty()) {
                            Toast.makeText(AdminManualFeedActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                        }
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminManualFeedActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Load latest feeding history **/
    private void loadLatestHistory() {
        if (selectedFeederId == null) return;

        dbRef.child("devices").child(selectedFeederId).child("history")
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            tvLatestHistory.setText("No feeding history available.");
                            return;
                        }

                        for (DataSnapshot dateSnap : snapshot.getChildren()) {
                            String latestDate = dateSnap.getKey();
                            DataSnapshot lastTimeSnap = null;

                            for (DataSnapshot timeSnap : dateSnap.getChildren()) {
                                lastTimeSnap = timeSnap;
                            }

                            if (lastTimeSnap != null) {
                                String latestTime = lastTimeSnap.getKey();
                                Long level = lastTimeSnap.child("level").getValue(Long.class);
                                String source = lastTimeSnap.child("source").getValue(String.class);
                                Long weight = lastTimeSnap.child("weight").getValue(Long.class);

                                String text = "Latest Feeding:\n" +
                                        "• Date: " + latestDate + "\n" +
                                        "• Time: " + latestTime + "\n" +
                                        "• Source: " + (source != null ? source : "N/A") + "\n" +
                                        "• Level: " + (level != null ? level : 0) + "%\n" +
                                        "• Weight: " + (weight != null ? weight : 0) + "g";

                                tvLatestHistory.setText(text);
                            } else {
                                tvLatestHistory.setText("No feeding records found for " + latestDate);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        tvLatestHistory.setText("Error loading history: " + error.getMessage());
                    }
                });
    }

    /** Log feeding action into history **/
    private void logFeedingHistory() {
        if (selectedFeederId == null) return;

        long currentMillis = System.currentTimeMillis();
        String date = android.text.format.DateFormat.format("yyyy-MM-dd", currentMillis).toString();
        String time = android.text.format.DateFormat.format("HH:mm", currentMillis).toString();

        HashMap<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("level", 0);
        historyEntry.put("source", "admin");
        historyEntry.put("weight", 0);

        dbRef.child("devices")
                .child(selectedFeederId)
                .child("history")
                .child(date)
                .child(time)
                .setValue(historyEntry)
                .addOnSuccessListener(aVoid -> {
                    tvFeedStatus.setText("Status: Idle");
                    loadLatestHistory();
                    Toast.makeText(AdminManualFeedActivity.this,
                            "Feeding record added under " + date + " at " + time,
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(AdminManualFeedActivity.this,
                                "Failed to log history: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
