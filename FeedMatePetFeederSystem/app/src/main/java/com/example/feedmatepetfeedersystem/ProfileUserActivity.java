package com.example.feedmatepetfeedersystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ProfileUserActivity extends AppCompatActivity {

    private static final String TAG = "ProfileUserActivity";

    // ✅ Use your full database URL (copy from console > Data tab)
    private static final String DB_URL =
            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private EditText editPetName, editPetAge, editPetBreed, editFullName, editEmail;
    private TextView tvUserName, tvUserEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_user);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }


        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        userRef = db.getReference("users").child(currentUser.getUid());

        // UI references
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);

        TextInputLayout petNameLayout = findViewById(R.id.petNameLayout);
        TextInputLayout petAgeLayout = findViewById(R.id.petAgeLayout);
        TextInputLayout petBreedLayout = findViewById(R.id.petBreedLayout);
        TextInputLayout fullNameLayout = findViewById(R.id.fullNameLayout);

        editPetName = findViewById(R.id.editPetName);
        editPetAge = findViewById(R.id.editPetAge);
        editPetBreed = findViewById(R.id.editPetBreed);
        editFullName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);

        // Display email in header
        if (currentUser.getEmail() != null) {
            tvUserEmail.setText(currentUser.getEmail());
            editEmail.setText(currentUser.getEmail());
        }

        // Enable edit on pencil click
        petNameLayout.setEndIconOnClickListener(v -> enableEditing(editPetName));
        petAgeLayout.setEndIconOnClickListener(v -> enableEditing(editPetAge));
        petBreedLayout.setEndIconOnClickListener(v -> enableEditing(editPetBreed));
        fullNameLayout.setEndIconOnClickListener(v -> enableEditing(editFullName));

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                if (!snapshot.hasChild("petName")) updates.put("petName", "");
                if (!snapshot.hasChild("petAge")) updates.put("petAge", "");
                if (!snapshot.hasChild("petBreed")) updates.put("petBreed", "");
                if (!snapshot.hasChild("fullName")) updates.put("fullName", "");
                if (!snapshot.hasChild("role")) updates.put("role", "user"); // keep role
                if (!snapshot.hasChild("email")) updates.put("email", mAuth.getCurrentUser().getEmail());

                if (!updates.isEmpty()) {
                    userRef.updateChildren(updates);
                }

                // 🔹 Now load into fields
                String petName = snapshot.child("petName").getValue(String.class);
                String petAge = snapshot.child("petAge").getValue(String.class);
                String petBreed = snapshot.child("petBreed").getValue(String.class);
                String fullName = snapshot.child("fullName").getValue(String.class);

                if (petName != null) editPetName.setText(petName);
                if (petAge != null) editPetAge.setText(petAge);
                if (petBreed != null) editPetBreed.setText(petBreed);
                if (fullName != null) {
                    editFullName.setText(fullName);
                    tvUserName.setText(fullName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileUserActivity.this,
                        "Load error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Load failed", error.toException());
            }
        });


        // ✅ Save changes
        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String petName = editPetName.getText().toString().trim();
            String petAge = editPetAge.getText().toString().trim();
            String petBreed = editPetBreed.getText().toString().trim();
            String fullName = editFullName.getText().toString().trim();

            Map<String, Object> updates = new HashMap<>();
            updates.put("email", currentUser.getEmail());
            updates.put("petName", petName);
            updates.put("petAge", petAge);
            updates.put("petBreed", petBreed);
            updates.put("fullName", fullName);

            userRef.updateChildren(updates)
                    .addOnSuccessListener(unused -> {
                        tvUserName.setText(fullName);
                        Toast.makeText(this, "Changes Saved", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Save failed", e);
                    });
        });

        // Bottom navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserDashboardActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });
    }

    private void enableEditing(EditText et) {
        et.setEnabled(true);
        et.requestFocus();
        et.setSelection(et.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }
}
