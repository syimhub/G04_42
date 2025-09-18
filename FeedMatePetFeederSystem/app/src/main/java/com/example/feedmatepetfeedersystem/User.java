package com.example.feedmatepetfeedersystem;

public class User {
    private String uid;
    private String fullName;
    private String email;
    private String role;
    private String feederId;
    private String petName;
    private String petAge;
    private String petBreed;
    private String profileImageUrl;

    // 🔹 Default constructor required for Firebase
    public User() {}

    // 🔹 Minimal constructor (signup only: required info)
    public User(String uid, String fullName, String email, String role, String feederId) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;

        // ✅ Ensure feederId is always set (same as UID at signup)
        this.feederId = feederId;

        // Initialize optional fields with defaults (not null)
        this.petName = "";
        this.petAge = "";
        this.petBreed = "";
        this.profileImageUrl = "";
    }

    // 🔹 Full constructor (all fields)
    public User(String uid, String fullName, String email, String role, String feederId,
                String petName, String petAge, String petBreed, String profileImageUrl) {
        this.uid = uid;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.feederId = feederId;
        this.petName = petName != null ? petName : "";
        this.petAge = petAge != null ? petAge : "";
        this.petBreed = petBreed != null ? petBreed : "";
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
    }

    // 🔹 Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFeederId() { return feederId; }
    public void setFeederId(String feederId) { this.feederId = feederId; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetAge() { return petAge; }
    public void setPetAge(String petAge) { this.petAge = petAge; }

    public String getPetBreed() { return petBreed; }
    public void setPetBreed(String petBreed) { this.petBreed = petBreed; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
}
