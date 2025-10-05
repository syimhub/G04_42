package com.example.feedmatepetfeedersystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;

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

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manage_users);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Toolbar
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
                if (user.getUid() == null) return;

                // Step 1: Fun confirmation dialog
                new AlertDialog.Builder(ManageUsersActivity.this)
                        .setTitle("Wait a second...")
                        .setMessage("Are you really sure you want to delete this user? 🤔\n\n" +
                                "Once you go down this path, there’s no coming back")
                        .setPositiveButton("I'm Sure", (funDialog, which) -> {
                            // Step 2: Serious confirmation dialog
                            new AlertDialog.Builder(ManageUsersActivity.this)
                                    .setTitle("Final Confirmation")
                                    .setMessage("This action will permanently remove the user and all related feeder data.\n\n" +
                                            "Do you want to proceed?")
                                    .setPositiveButton("Delete", (confirmDialog, w) -> {
                                        usersRef.child(user.getUid()).removeValue()
                                                .addOnSuccessListener(aVoid ->
                                                        Toast.makeText(ManageUsersActivity.this,
                                                                "User deleted successfully", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(ManageUsersActivity.this,
                                                                "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                    })
                                    .setNegativeButton("Cancel", (confirmDialog, w) -> confirmDialog.dismiss())
                                    .setCancelable(true)
                                    .show();
                        })
                        .setNegativeButton("Never mind", (dialog, which) -> dialog.dismiss())
                        .setCancelable(true)
                        .show();
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
        builder.setTitle("Edit User");

        // Inflate custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        // Bind fields
        EditText inputName = dialogView.findViewById(R.id.editFullName);
        RadioGroup inputRoleGroup = dialogView.findViewById(R.id.editRoleGroup);
        EditText inputPetName = dialogView.findViewById(R.id.editPetName);
        EditText inputPetAge = dialogView.findViewById(R.id.editPetAge);
        EditText inputPetBreed = dialogView.findViewById(R.id.editPetBreed);

        // Buttons from XML
        com.google.android.material.button.MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);
        com.google.android.material.button.MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);

        // Populate with current data
        inputName.setText(user.getFullName() != null ? user.getFullName() : "");
        inputPetName.setText(user.getPetName() != null ? user.getPetName() : "");
        inputPetAge.setText(user.getPetAge() != null ? user.getPetAge() : "");
        inputPetBreed.setText(user.getPetBreed() != null ? user.getPetBreed() : "");

        // Pre-select role
        if ("admin".equalsIgnoreCase(user.getRole())) {
            inputRoleGroup.check(R.id.radioAdmin);
        } else {
            inputRoleGroup.check(R.id.radioUser);
        }

        AlertDialog dialog = builder.create();

        // Handle Save
        btnSave.setOnClickListener(v -> {
            String newName = inputName.getText().toString().trim();
            String newPetName = inputPetName.getText().toString().trim();
            String newPetAge = inputPetAge.getText().toString().trim();
            String newPetBreed = inputPetBreed.getText().toString().trim();

            // Get role
            int selectedRoleId = inputRoleGroup.getCheckedRadioButtonId();
            String newRole = (selectedRoleId == R.id.radioAdmin) ? "admin" : "user";

            if (user.getUid() != null) {
                DatabaseReference userRef = usersRef.child(user.getUid());
                userRef.child("fullName").setValue(newName);
                userRef.child("role").setValue(newRole);
                userRef.child("petName").setValue(newPetName);
                userRef.child("petAge").setValue(newPetAge);
                userRef.child("petBreed").setValue(newPetBreed)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(ManageUsersActivity.this, "User updated", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(ManageUsersActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });

        // Handle Cancel
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (usersRef != null && usersListener != null) {
            usersRef.removeEventListener(usersListener);
        }
    }
}
