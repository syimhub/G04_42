package com.example.feedmatepetfeedersystem;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.EdgeToEdge;
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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class AdminSetScheduleActivity extends AppCompatActivity {

    private RecyclerView rvUsers;
    private SetScheduleUserAdapter setScheduleUserAdapter;

    private DatabaseReference dbRef;
    private HashMap<String, String> userFeederMap = new HashMap<>();
    private List<User> userList = new ArrayList<>();
    private List<String> userIds = new ArrayList<>();

    private String selectedUserId = null;
    private String selectedFeederId = null;

    // 3 fixed TextViews for times
    private TextView tvFeedingTime1, tvFeedingTime2, tvFeedingTime3;

    // Buttons under each time row
    private View time1Buttons, time2Buttons, time3Buttons;

    private FirebaseUser currentUser;

    // Store feeding times
    private ArrayList<String> feedingTimes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable Edge-to-Edge mode
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_schedule_admin);

        // Apply window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Firebase
        dbRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        rvUsers = findViewById(R.id.rvUsers);

        // Feeding time views
        tvFeedingTime1 = findViewById(R.id.tvFeedingTime1);
        tvFeedingTime2 = findViewById(R.id.tvFeedingTime2);
        tvFeedingTime3 = findViewById(R.id.tvFeedingTime3);

        time1Buttons = findViewById(R.id.time1Buttons);
        time2Buttons = findViewById(R.id.time2Buttons);
        time3Buttons = findViewById(R.id.time3Buttons);

        // Setup user list recycler
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        setScheduleUserAdapter = new SetScheduleUserAdapter(userList, (user, position) -> {
            selectedUserId = user.getUid();
            selectedFeederId = userFeederMap.get(selectedUserId);

            if (selectedFeederId == null || selectedFeederId.isEmpty()) {
                feedingTimes.clear();
                clearDisplayedTimes();
                Toast.makeText(AdminSetScheduleActivity.this, "Selected user has no feeder assigned", Toast.LENGTH_SHORT).show();
            } else {
                loadFeedingTimes();
            }
        });
        rvUsers.setAdapter(setScheduleUserAdapter);

        // Load users
        loadUsers();

        // Setup editing listeners
        setupTimeBoxListeners();
    }

    private void loadUsers() {
        if (currentUser == null) {
            Log.e("AdminSetSchedule", "❌ currentUser is null, aborting loadUsers()");
            return;
        }

        Log.d("AdminSetSchedule", "⏳ loadUsers() called with orderByChild(email)");

        dbRef.child("users")
                .orderByChild("email")   // 🔑 Required for rules
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d("AdminSetSchedule", "✅ onDataChange triggered, children count = " + snapshot.getChildrenCount());

                        userList.clear();
                        userIds.clear();
                        userFeederMap.clear();

                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            String uid = userSnap.getKey();
                            String fullName = userSnap.child("fullName").getValue(String.class);
                            String email = userSnap.child("email").getValue(String.class);
                            String role = userSnap.child("role").getValue(String.class);
                            String feederId = userSnap.child("feederId").getValue(String.class);

                            Log.d("AdminSetSchedule", "📌 Found user: uid=" + uid + ", role=" + role + ", fullName=" + fullName);

                            if (fullName != null && "user".equals(role)) {
                                User user = new User();
                                user.setUid(uid);
                                user.setFullName(fullName);
                                user.setEmail(email);
                                user.setRole(role);

                                userList.add(user);
                                userIds.add(uid);
                                userFeederMap.put(uid, feederId);

                                Log.d("AdminSetSchedule", "➡️ Added user to list: " + fullName + " (" + uid + ")");
                            }
                        }

                        Log.d("AdminSetSchedule", "📊 Final userList size = " + userList.size());

                        if (userList.isEmpty()) {
                            Toast.makeText(AdminSetScheduleActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                        }

                        setScheduleUserAdapter.notifyDataSetChanged();
                        Log.d("AdminSetSchedule", "🔄 Adapter notified of data change");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("AdminSetSchedule", "❌ onCancelled: " + error.getMessage());
                        Toast.makeText(AdminSetScheduleActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void loadFeedingTimes() {
        if (selectedFeederId == null) return;

        dbRef.child("devices").child(selectedFeederId).child("config").child("schedule").child("feedingTimes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        feedingTimes.clear();
                        for (DataSnapshot timeSnap : snapshot.getChildren()) {
                            String time = timeSnap.getValue(String.class);
                            if (time != null) feedingTimes.add(time);
                        }
                        updateDisplayedTimes();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminSetScheduleActivity.this, "Error loading device: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupTimeBoxListeners() {
        setupTimeBox(tvFeedingTime1, time1Buttons, 0, R.id.btnConfirm1, R.id.btnCancel1);
        setupTimeBox(tvFeedingTime2, time2Buttons, 1, R.id.btnConfirm2, R.id.btnCancel2);
        setupTimeBox(tvFeedingTime3, time3Buttons, 2, R.id.btnConfirm3, R.id.btnCancel3);
    }

    private void setupTimeBox(TextView tvTime, View buttonsLayout, int index,
                              int confirmBtnId, int cancelBtnId) {
        tvTime.setOnClickListener(v -> {
            if (selectedFeederId == null) {
                Toast.makeText(this, "Select a user first", Toast.LENGTH_SHORT).show();
                return;
            }
            showTimePicker(index, tvTime, buttonsLayout);
        });

        buttonsLayout.findViewById(confirmBtnId).setOnClickListener(v -> confirmTime(tvTime, index, buttonsLayout));
        buttonsLayout.findViewById(cancelBtnId).setOnClickListener(v -> cancelEdit(buttonsLayout));
    }

    private void showTimePicker(int index, TextView tvTime, View buttonsLayout) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minuteOfHour) -> {
                    String formatted = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                    tvTime.setText(formatted);
                    buttonsLayout.setVisibility(View.VISIBLE);
                },
                hour, minute, true
        );
        timePickerDialog.show();
    }

    private void confirmTime(TextView tvTime, int index, View buttonsLayout) {
        String newTime = tvTime.getText().toString();
        if (feedingTimes.size() > index) {
            feedingTimes.set(index, newTime);
        } else {
            feedingTimes.add(newTime);
        }
        saveSchedule();
        buttonsLayout.setVisibility(View.GONE);
    }

    private void cancelEdit(View buttonsLayout) {
        buttonsLayout.setVisibility(View.GONE);
    }

    private void updateDisplayedTimes() {
        clearDisplayedTimes();
        if (feedingTimes.size() > 0) tvFeedingTime1.setText(feedingTimes.get(0));
        if (feedingTimes.size() > 1) tvFeedingTime2.setText(feedingTimes.get(1));
        if (feedingTimes.size() > 2) tvFeedingTime3.setText(feedingTimes.get(2));
    }

    private void clearDisplayedTimes() {
        tvFeedingTime1.setText("--:--");
        tvFeedingTime2.setText("--:--");
        tvFeedingTime3.setText("--:--");
    }

    private void saveSchedule() {
        if (selectedFeederId == null) {
            Toast.makeText(this, "No feeder selected", Toast.LENGTH_SHORT).show();
            return;
        }

        dbRef.child("devices").child(selectedFeederId).child("config").child("schedule").child("feedingTimes")
                .setValue(feedingTimes)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
