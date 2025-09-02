package com.example.feedmatepetfeedersystem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileUserActivity extends AppCompatActivity {

    private static final String TAG = "ProfileUserActivity";
    private static final String DB_URL =
            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    // UI
    private EditText editPetName, editPetAge, editPetBreed, editFullName, editEmail;
    private TextView tvUserName, tvUserEmail;
    private ImageView imgUser;

    // Action rows (Confirm/Cancel)
    private LinearLayout nameButtonsLayout, petNameButtonsLayout, petAgeButtonsLayout, petBreedButtonsLayout;

    // We’ll store last committed values (used for Cancel)
    private final Map<String, String> committedValues = new HashMap<>();

    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1001;

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

        // ===== Bind UI =====
        tvUserName = findViewById(R.id.tvUserName);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        imgUser = findViewById(R.id.imgUser);

        TextInputLayout petNameLayout = findViewById(R.id.petNameLayout);
        TextInputLayout petAgeLayout = findViewById(R.id.petAgeLayout);
        TextInputLayout petBreedLayout = findViewById(R.id.petBreedLayout);
        TextInputLayout fullNameLayout = findViewById(R.id.fullNameLayout);

        editPetName = findViewById(R.id.editPetName);
        editPetAge = findViewById(R.id.editPetAge);
        editPetBreed = findViewById(R.id.editPetBreed);
        editFullName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);

        nameButtonsLayout = findViewById(R.id.nameButtonsLayout);
        petNameButtonsLayout = findViewById(R.id.petNameButtonsLayout);
        petAgeButtonsLayout = findViewById(R.id.petAgeButtonsLayout);
        petBreedButtonsLayout = findViewById(R.id.petBreedButtonsLayout);

        // Show email from FirebaseAuth
        if (currentUser.getEmail() != null) {
            tvUserEmail.setText(currentUser.getEmail());
            editEmail.setText(currentUser.getEmail());
        }

        // ===== End icon (pencil) → enable edit & show buttons =====
        fullNameLayout.setEndIconOnClickListener(v ->
                startEditing("fullName", editFullName, nameButtonsLayout));
        petNameLayout.setEndIconOnClickListener(v ->
                startEditing("petName", editPetName, petNameButtonsLayout));
        petAgeLayout.setEndIconOnClickListener(v ->
                startEditing("petAge", editPetAge, petAgeButtonsLayout));
        petBreedLayout.setEndIconOnClickListener(v ->
                startEditing("petBreed", editPetBreed, petBreedButtonsLayout));

        // ===== Load data from DB =====
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();

                if (!snapshot.hasChild("petName")) updates.put("petName", "");
                if (!snapshot.hasChild("petAge")) updates.put("petAge", "");
                if (!snapshot.hasChild("petBreed")) updates.put("petBreed", "");
                if (!snapshot.hasChild("fullName")) updates.put("fullName", "");
                if (!snapshot.hasChild("role")) updates.put("role", "user");
                if (!snapshot.hasChild("email")) updates.put("email", mAuth.getCurrentUser().getEmail());

                if (!updates.isEmpty()) userRef.updateChildren(updates);

                String petName = snapshot.child("petName").getValue(String.class);
                String petAge = snapshot.child("petAge").getValue(String.class);
                String petBreed = snapshot.child("petBreed").getValue(String.class);
                String fullName = snapshot.child("fullName").getValue(String.class);
                String profileBase64 = snapshot.child("profileImageBase64").getValue(String.class);

                // Set texts
                if (petName != null) editPetName.setText(petName);
                if (petAge != null) editPetAge.setText(petAge);
                if (petBreed != null) editPetBreed.setText(petBreed);
                if (fullName != null) {
                    editFullName.setText(fullName);
                    tvUserName.setText(fullName);
                }

                // Cache committed values for Cancel
                committedValues.put("petName", editPetName.getText().toString());
                committedValues.put("petAge", editPetAge.getText().toString());
                committedValues.put("petBreed", editPetBreed.getText().toString());
                committedValues.put("fullName", editFullName.getText().toString());

                // Profile picture (Base64)
                if (profileBase64 != null && !profileBase64.isEmpty()) {
                    try {
                        byte[] decoded = android.util.Base64.decode(profileBase64, android.util.Base64.DEFAULT);
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        imgUser.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        imgUser.setImageResource(R.drawable.ic_user_placeholder);
                    }
                } else {
                    imgUser.setImageResource(R.drawable.ic_user_placeholder);
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

        // ===== Wire up Confirm/Cancel buttons for each field =====
        findViewById(R.id.btnConfirmName).setOnClickListener(v ->
                confirmField("fullName", editFullName, nameButtonsLayout));
        findViewById(R.id.btnCancelName).setOnClickListener(v ->
                cancelField("fullName", editFullName, nameButtonsLayout));

        findViewById(R.id.btnConfirmPetName).setOnClickListener(v ->
                confirmField("petName", editPetName, petNameButtonsLayout));
        findViewById(R.id.btnCancelPetName).setOnClickListener(v ->
                cancelField("petName", editPetName, petNameButtonsLayout));

        findViewById(R.id.btnConfirmPetAge).setOnClickListener(v ->
                confirmField("petAge", editPetAge, petAgeButtonsLayout));
        findViewById(R.id.btnCancelPetAge).setOnClickListener(v ->
                cancelField("petAge", editPetAge, petAgeButtonsLayout));

        findViewById(R.id.btnConfirmPetBreed).setOnClickListener(v ->
                confirmField("petBreed", editPetBreed, petBreedButtonsLayout));
        findViewById(R.id.btnCancelPetBreed).setOnClickListener(v ->
                cancelField("petBreed", editPetBreed, petBreedButtonsLayout));


        // 🔑 Change Password button (same behavior)
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            if (currentUser.getEmail() != null) {
                mAuth.sendPasswordResetEmail(currentUser.getEmail())
                        .addOnSuccessListener(unused ->
                                Toast.makeText(this,
                                        "Password reset email sent to " + currentUser.getEmail(),
                                        Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        "Failed to send reset email: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(this, "No email linked to account", Toast.LENGTH_SHORT).show();
            }
        });

        // Profile picture change (same Base64 flow)
        imgUser.setOnClickListener(v -> openImageChooser());

        // Bottom nav (IDs match your menu)
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, UserDashboardActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
    }

    // ===== Editing helpers =====
    private void startEditing(String key, EditText et, LinearLayout actionsRow) {
        // Keep field in edit mode even if user taps elsewhere (we do NOT disable on outside touch)
        et.setEnabled(true);
        et.requestFocus();
        et.setSelection(et.getText().length());
        actionsRow.setVisibility(View.VISIBLE);
        showKeyboard(et);
    }

    private void confirmField(String key, EditText et, LinearLayout actionsRow) {
        String value = et.getText().toString().trim();

        userRef.child(key).setValue(value)
                .addOnSuccessListener(unused -> {
                    committedValues.put(key, value); // update committed value
                    if ("fullName".equals(key)) {
                        tvUserName.setText(value);
                    }
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    et.setEnabled(false);
                    actionsRow.setVisibility(View.GONE);
                    hideKeyboard(et);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Update failed", e);
                });
    }

    private void cancelField(String key, EditText et, LinearLayout actionsRow) {
        String committed = committedValues.getOrDefault(key, "");
        et.setText(committed);
        et.setEnabled(false);
        actionsRow.setVisibility(View.GONE);
        hideKeyboard(et);
    }

    private void showKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // ===== Image picker → Base64 save (unchanged logic) =====
    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();

            String fileType = getContentResolver().getType(selectedImageUri);
            if (fileType != null && (fileType.equals("image/jpeg") || fileType.equals("image/png"))) {
                saveImageAsBase64(selectedImageUri);
            } else {
                Toast.makeText(this, "Wrong picture format", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveImageAsBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);

            userRef.child("profileImageBase64").setValue(encodedImage)
                    .addOnSuccessListener(unused -> {
                        imgUser.setImageBitmap(bitmap);
                        Toast.makeText(this, "Successfully changed", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Upload failed", e);
                    });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Image processing failed", Toast.LENGTH_LONG).show();
        }
    }

    // Note: We intentionally DO NOT override dispatchTouchEvent to disable fields on outside taps.
    // This keeps fields in edit mode until Confirm or Cancel is pressed (as requested).
}
