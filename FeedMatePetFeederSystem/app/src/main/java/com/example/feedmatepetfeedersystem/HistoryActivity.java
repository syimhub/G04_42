package com.example.feedmatepetfeedersystem;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar; // ✅ Correct import
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerHistory;
    private HistoryAdapter adapter;
    private List<History> historyList = new ArrayList<>();

    // 🔹 Toggle this to switch between Dummy vs Firebase
    private static final boolean USE_DUMMY = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Toolbar with back button
        MaterialToolbar toolbar = findViewById(R.id.toolbarHistory);
        toolbar.setNavigationOnClickListener(v -> finish());

        // RecyclerView setup
        recyclerHistory = findViewById(R.id.recyclerHistory);
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(historyList);
        recyclerHistory.setAdapter(adapter);

        // Load data based on mode
        if (USE_DUMMY) {
            loadDummyHistory();
        } else {
            loadHistory();
        }
    }

    private void loadDummyHistory() {
        historyList.clear();
        historyList.add(new History("2025-09-21 08:30", 80, 200));
        historyList.add(new History("2025-09-20 19:15", 60, 150));
        historyList.add(new History("2025-09-20 12:00", 40, 120));
        historyList.add(new History("2025-09-19 21:00", 20, 90));

        adapter.notifyDataSetChanged();
    }

    private void loadHistory() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("history");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                historyList.clear();

                DataSnapshot levelsSnap = snapshot.child("foodLevels");
                DataSnapshot weightsSnap = snapshot.child("foodWeights");

                for (DataSnapshot level : levelsSnap.getChildren()) {
                    String timestamp = level.getKey();
                    int foodLevel = level.getValue(Integer.class);

                    int foodWeight = 0;
                    if (weightsSnap.hasChild(timestamp)) {
                        foodWeight = weightsSnap.child(timestamp).getValue(Integer.class);
                    }

                    historyList.add(new History(timestamp, foodLevel, foodWeight));
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error if needed
            }
        });
    }
}
