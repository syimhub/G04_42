package com.example.feedmatepetfeedersystem;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String role;
    private String petName;
    private String petAge;
    private String petBreed;
    private String profileImageBase64;

    // Required empty constructor for Firebase
    public User() {}

    // Constructor for signup (minimum info)
    public User(String uid, String fullName, String email, String role) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.petName = "";
        this.petAge = "";
        this.petBreed = "";
        this.profileImageBase64 = "";
    }

    // Full constructor if needed later
    public User(String uid, String fullName, String email, String role,
                String petName, String petAge, String petBreed, String profileImageBase64) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.petName = petName;
        this.petAge = petAge;
        this.petBreed = petBreed;
        this.profileImageBase64 = profileImageBase64;
    }

    // Getters and setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetAge() { return petAge; }
    public void setPetAge(String petAge) { this.petAge = petAge; }

    public String getPetBreed() { return petBreed; }
    public void setPetBreed(String petBreed) { this.petBreed = petBreed; }

    public String getProfileImageBase64() { return profileImageBase64; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
}
