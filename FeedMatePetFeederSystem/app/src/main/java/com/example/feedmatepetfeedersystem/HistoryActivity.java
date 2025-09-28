package com.example.feedmatepetfeedersystem;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private HistoryAdapter historyAdapter;
    private List<Object> historyList; // initialized properly in onCreate
    private DatabaseReference historyRef;
    private ValueEventListener historyListener;
    private TextView txtNoHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_history);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Feeding History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // RecyclerView + Adapter setup
        recyclerHistory = findViewById(R.id.recyclerViewHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));

        txtNoHistory = findViewById(R.id.txtNoHistory);

        historyList = new ArrayList<>(); // initialize here
        historyAdapter = new HistoryAdapter(historyList);
        recyclerHistory.setAdapter(historyAdapter);

        // Firebase reference (use logged-in user’s feederId)
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("users").child(uid);

            userRef.get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists() && snapshot.child("feederId").getValue() != null) {
                    String feederId = snapshot.child("feederId").getValue(String.class);
                    historyRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("devices").child(feederId).child("history");
                    loadHistory();
                } else {
                    Toast.makeText(this, "No feeder linked to your account.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadHistory() {
        historyListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();

                // Collect all dates first
                List<String> dates = new ArrayList<>();
                for (DataSnapshot dateSnap : snapshot.getChildren()) {
                    String date = dateSnap.getKey();
                    if (date != null) {
                        dates.add(date);
                    }
                }

                // Sort dates descending (latest first)
                dates.sort((d1, d2) -> d2.compareTo(d1));

                for (String date : dates) {
                    DataSnapshot dateSnap = snapshot.child(date);
                    historyList.add(date); // add date header

                    // Collect all times under this date
                    List<String> times = new ArrayList<>();
                    for (DataSnapshot timeSnap : dateSnap.getChildren()) {
                        String time = timeSnap.getKey();
                        if (time != null) {
                            times.add(time);
                        }
                    }

                    // Sort times descending
                    times.sort((t1, t2) -> t2.compareTo(t1));

                    // Add each history entry
                    for (String time : times) {
                        DataSnapshot timeSnap = dateSnap.child(time);
                        String source = timeSnap.child("source").getValue(String.class);
                        Integer level = timeSnap.child("level").getValue(Integer.class);
                        Double weight = timeSnap.child("weight").getValue(Double.class);

                        if (source != null && level != null && weight != null) {
                            historyList.add(new History(date, time, source, level, weight));
                        }
                    }
                }

                historyAdapter.notifyDataSetChanged();

                // Show/hide "No history available" text
                if (historyList.isEmpty()) {
                    txtNoHistory.setVisibility(View.VISIBLE);
                } else {
                    txtNoHistory.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HistoryActivity.this, "Failed to load history: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        historyRef.addValueEventListener(historyListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (historyRef != null && historyListener != null) {
            historyRef.removeEventListener(historyListener);
        }
    }
}
