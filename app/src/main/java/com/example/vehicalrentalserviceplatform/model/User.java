package com.example.vehicalrentalserviceplatform.model;

public class User {
    private String userId;
    private String fullName;
    private String username;
    private String password;
    private String licenseId;
    private String role;

    public User(String userId, String fullName, String username,
                String password, String licenseId, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.licenseId = licenseId;
        this.role = role;
    }

    public User() {}

    // Getters and setters for all fields
    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getLicenseId() { return licenseId; }
    public String getRole() { return role; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }
    public void setRole(String role) { this.role = role; }

    public boolean validatePassword(String input) {
        return this.password != null && this.password.equals(input);
    }

    @Override
    public String toString() {
        return "User{username='" + username + "', role='" + role + "'}";
    }
}