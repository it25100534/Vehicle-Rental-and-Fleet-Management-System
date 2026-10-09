package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customers")
public class CustomerEntity {
    @Id
    @Column(length = 80)
    private String username;
    @Column(name = "license_id", nullable = false, unique = true, length = 80)
    private String licenseId;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    @Column(nullable = false, length = 30)
    private String phone;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false)
    private boolean active = true;

    protected CustomerEntity() {}

    public CustomerEntity(String username, String licenseId, String email, String phone,
                          String passwordHash, boolean active) {
        this.username = username;
        this.licenseId = licenseId;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.active = active;
    }

    public String getUsername() { return username; }
    public String getLicenseId() { return licenseId; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isActive() { return active; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setActive(boolean active) { this.active = active; }
}
