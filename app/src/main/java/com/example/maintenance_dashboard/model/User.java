package com.example.maintenance_dashboard.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String fName;
    private String email;
    private String role;

    public User() {
        // Required for Firestore toObject
    }

    public User(String fName, String email, String role) {
        this.fName = fName;
        this.email = email;
        this.role = role;
    }

    public String getfName() {
        return fName;
    }

    public void setfName(String fName) {
        this.fName = fName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
