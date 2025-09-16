package com.example.feedmatepetfeedersystem;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ProfileAdminActivity extends AppCompatActivity {

    private static final String TAG = "ProfileAdminActivity";
    private static final String DB_URL =
            "https://feedmate-pet-feeder-system-default-rtdb.asia-southeast1.firebasedatabase.app/";

    private FirebaseAuth mAuth;
    private DatabaseReference adminRef;

    private TextView tvAdminName, tvAdminEmail;
    private EditText editAdminName, editAdminEmail;
    private ImageView imgAdmin;
    private LinearLayout nameButtonsLayout;

    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 2001;

    // ===== InputFilter for letters only =====
    private static final android.text.InputFilter LETTERS_ONLY_FILTER = (source, start, end, dest, dstart, dend) -> {
        for (int i = start; i < end; i++) {
            char c = source.charAt(i);
            if (!Character.isLetter(c) && c != ' ' && c != '-' && c != '\'') {
                return ""; // Reject any non-letter input
            }
        }
        return null;
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_admin);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        FirebaseDatabase db = FirebaseDatabase.getInstance(DB_URL);
        adminRef = db.getReference("users").child(currentUser.getUid());

        // UI
        tvAdminName = findViewById(R.id.tvAdminName);
        tvAdminEmail = findViewById(R.id.tvAdminEmail);
        imgAdmin = findViewById(R.id.imgAdmin);
        editAdminName = findViewById(R.id.editAdminName);
        editAdminEmail = findViewById(R.id.editAdminEmail);
        nameButtonsLayout = findViewById(R.id.nameButtonsLayout);

        TextInputLayout fullNameLayout = findViewById(R.id.fullNameLayout);

        // 🔒 Restrict Full Name to letters only
        editAdminName.setFilters(new android.text.InputFilter[]{LETTERS_ONLY_FILTER});

        // Display email
        if (currentUser.getEmail() != null) {
            tvAdminEmail.setText(currentUser.getEmail());
            editAdminEmail.setText(currentUser.getEmail());
        }

        // Pencil icon → enable editing and show buttons
        fullNameLayout.setEndIconOnClickListener(v -> {
            enableEditing(editAdminName);
            nameButtonsLayout.setVisibility(View.VISIBLE);
        });

        // Confirm button
        findViewById(R.id.btnConfirmName).setOnClickListener(v -> {
            String fullName = editAdminName.getText().toString().trim();
            if (!fullName.isEmpty()) {
                adminRef.child("fullName").setValue(fullName)
                        .addOnSuccessListener(unused -> {
                            tvAdminName.setText(fullName);
                            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                            editAdminName.setEnabled(false);
                            nameButtonsLayout.setVisibility(View.GONE);
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });

        // Cancel button
        findViewById(R.id.btnCancelName).setOnClickListener(v -> {
            editAdminName.setText(tvAdminName.getText().toString());
            editAdminName.setEnabled(false);
            nameButtonsLayout.setVisibility(View.GONE);
        });

        // Load data
        adminRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Map<String, Object> updates = new HashMap<>();
                if (!snapshot.hasChild("fullName")) updates.put("fullName", "");
                if (!snapshot.hasChild("email")) updates.put("email", currentUser.getEmail());
                if (!snapshot.hasChild("role")) updates.put("role", "admin");
                if (!updates.isEmpty()) adminRef.updateChildren(updates);

                String fullName = snapshot.child("fullName").getValue(String.class);
                String profileBase64 = snapshot.child("profileImageBase64").getValue(String.class);

                if (fullName != null) {
                    editAdminName.setText(fullName);
                    tvAdminName.setText(fullName);
                }

                if (profileBase64 != null && !profileBase64.isEmpty()) {
                    try {
                        byte[] decoded = Base64.decode(profileBase64, Base64.DEFAULT);
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        imgAdmin.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        imgAdmin.setImageResource(R.drawable.ic_user_placeholder);
                    }
                } else {
                    imgAdmin.setImageResource(R.drawable.ic_user_placeholder);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ProfileAdminActivity.this,
                        "Load error: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Load failed", error.toException());
            }
        });

        // Change Password button
        findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null && user.getEmail() != null) {
                mAuth.sendPasswordResetEmail(user.getEmail())
                        .addOnSuccessListener(unused ->
                                Toast.makeText(ProfileAdminActivity.this,
                                        "Password reset email sent to " + user.getEmail(),
                                        Toast.LENGTH_LONG).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(ProfileAdminActivity.this,
                                        "Failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show());
            } else {
                Toast.makeText(ProfileAdminActivity.this,
                        "No email found for this admin",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Profile picture → click to change
        imgAdmin.setOnClickListener(v -> openImageChooser());

        // Bottom navigation
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_admin);
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileAdminActivity.this, AdminDashboardActivity.class));
                finish();
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_logout) {
                logoutAdmin(); // ✅ centralized logout with toast
                return true;
            }
            return false;
        });
    }

    // Enable editing name
    private void enableEditing(EditText et) {
        et.setEnabled(true);
        et.requestFocus();
        et.setSelection(et.getText().length());
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
    }

    // Tap outside → hide keyboard but keep field enabled
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    // Pick image
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

    // Save as Base64
    private void saveImageAsBase64(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            adminRef.child("profileImageBase64").setValue(encodedImage)
                    .addOnSuccessListener(unused -> {
                        imgAdmin.setImageBitmap(bitmap);
                        Toast.makeText(this, "Profile picture updated", Toast.LENGTH_SHORT).show();
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

    private void logoutAdmin() {
        mAuth.signOut();
        Toast.makeText(ProfileAdminActivity.this, "Logged out", Toast.LENGTH_SHORT).show(); // ✅ Toast here
        Intent intent = new Intent(ProfileAdminActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
