package com.example.vehiclerentalserviceplatform.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "staff_accounts")
public class StaffAccountEntity {
    @Id
    @Column(name = "user_id", length = 40)
    private String userId;
    @Column(name = "full_name", nullable = false, length = 140)
    private String fullName;
    @Column(nullable = false, unique = true, length = 80)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(name = "employee_type", nullable = false, length = 80)
    private String employeeType;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name="home_branch_id", length=30) private String homeBranchId;

    protected StaffAccountEntity() {}

    public StaffAccountEntity(String userId, String fullName, String username, String passwordHash,
                              String role, String employeeType, boolean active) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.employeeType = employeeType;
        this.active = active;
    }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public String getEmployeeType() { return employeeType; }
    public boolean isActive() { return active; }
    public String getHomeBranchId() { return homeBranchId; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(String role) { this.role = role; }
    public void setEmployeeType(String employeeType) { this.employeeType = employeeType; }
    public void setActive(boolean active) { this.active = active; }
    public void setHomeBranchId(String homeBranchId) { this.homeBranchId = homeBranchId; }
}
