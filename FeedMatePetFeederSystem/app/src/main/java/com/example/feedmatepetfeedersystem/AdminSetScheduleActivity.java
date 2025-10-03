package com.example.feedmatepetfeedersystem;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
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
import java.util.HashMap;
import java.util.List;

public class AdminSetScheduleActivity extends AppCompatActivity {

    private Spinner spinnerUsers;
    private RecyclerView recyclerFeedingTimes;
    private MaterialButton btnAddTime, btnSaveSchedule;
    private FeedingTimeAdapter adapter;

    private DatabaseReference dbRef;
    private HashMap<String, String> userFeederMap = new HashMap<>();
    private List<String> userNames = new ArrayList<>();
    private List<String> userIds = new ArrayList<>();

    private String selectedUserId = null;
    private String selectedFeederId = null;

    private ArrayList<String> feedingTimes = new ArrayList<>();

    private FirebaseUser currentUser;

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
        dbRef = FirebaseDatabase.getInstance().getReference();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Check authentication
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        spinnerUsers = findViewById(R.id.spinnerUsers);
        recyclerFeedingTimes = findViewById(R.id.recyclerFeedingTimes);
        btnAddTime = findViewById(R.id.btnAddTime);
        btnSaveSchedule = findViewById(R.id.btnSaveSchedule);

        recyclerFeedingTimes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FeedingTimeAdapter(feedingTimes);
        recyclerFeedingTimes.setAdapter(adapter);

        // Load users only after confirming admin UID
        loadUsers();

        btnAddTime.setOnClickListener(v -> showTimePicker());
        btnSaveSchedule.setOnClickListener(v -> saveSchedule());
    }

    private void loadUsers() {
        if (currentUser == null) return;

        dbRef.child("users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userNames.clear();
                userIds.clear();
                userFeederMap.clear();

                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    String uid = userSnap.getKey();
                    String fullName = userSnap.child("fullName").getValue(String.class);
                    String role = userSnap.child("role").getValue(String.class);
                    String feederId = userSnap.child("feederId").getValue(String.class);

                    // Only include regular users
                    if (fullName != null && "user".equals(role)) {
                        userNames.add(fullName);
                        userIds.add(uid);
                        userFeederMap.put(uid, feederId);
                    }
                }

                if (userNames.isEmpty()) {
                    Toast.makeText(AdminSetScheduleActivity.this, "No users found", Toast.LENGTH_SHORT).show();
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        AdminSetScheduleActivity.this,
                        android.R.layout.simple_spinner_dropdown_item,
                        userNames
                );
                spinnerUsers.setAdapter(adapter);

                spinnerUsers.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        selectedUserId = userIds.get(position);
                        selectedFeederId = userFeederMap.get(selectedUserId);

                        if (selectedFeederId == null || selectedFeederId.isEmpty()) {
                            feedingTimes.clear();
                            AdminSetScheduleActivity.this.adapter.notifyDataSetChanged();
                            Toast.makeText(AdminSetScheduleActivity.this, "Selected user has no feeder assigned", Toast.LENGTH_SHORT).show();
                        } else {
                            loadFeedingTimes();
                        }
                    }

                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
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
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(AdminSetScheduleActivity.this, "Error loading device: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (TimePicker view, int hourOfDay, int minuteOfHour) -> {
                    String formatted = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                    feedingTimes.add(formatted);
                    adapter.notifyItemInserted(feedingTimes.size() - 1);
                },
                hour, minute, true
        );
        timePickerDialog.show();
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
