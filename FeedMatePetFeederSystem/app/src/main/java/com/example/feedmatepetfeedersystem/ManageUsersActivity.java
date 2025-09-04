package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ManageUsersActivity extends AppCompatActivity {

    private RecyclerView recyclerAdmins, recyclerUsers;
    private UserAdapter adminAdapter, userAdapter;
    private final List<User> adminList = new ArrayList<>();
    private final List<User> userList = new ArrayList<>();
    private DatabaseReference usersRef;
    private ValueEventListener usersListener;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Match AdminDashboard: edge-to-edge
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);

        // Apply system insets padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar + back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Users");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> {
            Intent i = new Intent(ManageUsersActivity.this, AdminDashboardActivity.class);
            startActivity(i);
            finish();
        });

        // RecyclerViews
        recyclerAdmins = findViewById(R.id.recyclerAdmins);
        recyclerUsers = findViewById(R.id.recyclerUsers);
        recyclerAdmins.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));

        // Firebase
        usersRef = FirebaseDatabase.getInstance("https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // Adapters
        adminAdapter = new UserAdapter(adminList, null);
        userAdapter = new UserAdapter(userList, new UserAdapter.OnUserActionListener() {
            @Override
            public void onEdit(User user) {
                showEditDialog(user);
            }

            @Override
            public void onDelete(User user) {
                if (user.getUid() != null) {
                    usersRef.child(user.getUid()).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(ManageUsersActivity.this, "User deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(ManageUsersActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });

        recyclerAdmins.setAdapter(adminAdapter);
        recyclerUsers.setAdapter(userAdapter);

        loadUsersFromDatabase();
    }

    private void loadUsersFromDatabase() {
        usersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminList.clear();
                userList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    User user = ds.getValue(User.class);
                    if (user != null) {
                        user.setUid(ds.getKey());
                        if ("admin".equalsIgnoreCase(user.getRole())) {
                            adminList.add(user);
                        } else {
                            userList.add(user);
                        }
                    }
                }

                adminAdapter.notifyDataSetChanged();
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ManageUsersActivity.this,
                        "Failed to load users: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };
        usersRef.addValueEventListener(usersListener);
    }

    private void showEditDialog(User user) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit User Name");

        final EditText input = new EditText(this);
        input.setText(user.getFullName() != null ? user.getFullName() : "");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = input.getText().toString().trim();
            if (!newName.isEmpty() && user.getUid() != null) {
                usersRef.child(user.getUid()).child("fullName").setValue(newName)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(ManageUsersActivity.this, "User updated", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(ManageUsersActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener); // ✅ Clean up to prevent errors after logout
        }
    }
}
