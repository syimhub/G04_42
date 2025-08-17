package com.example.feedmatepetfeedersystem;

public class User {
    public String email;
    public String role;

    // Default constructor required for Firebase
    public User() { }

    public User(String email, String role) {
        this.email = email;
        this.role = role;
    }
}
