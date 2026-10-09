package com.example.vehiclerentalserviceplatform.persistence;

import com.example.vehiclerentalserviceplatform.fleet.Branch;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "branches")
public class BranchEntity {
    @Id private String branchId;
    private String name;
    private String address;
    private String phone;
    private String status;
    private String imagePath;

    protected BranchEntity() {}
    public BranchEntity(String branchId, String name, String address, String phone, boolean open) {
        this.branchId = branchId; this.name = name; this.address = address; this.phone = phone;
        this.status = open ? "OPEN" : "CLOSED";
    }
    public Branch toDomain() {
        Branch branch = new Branch(branchId, name, address, phone, "OPEN".equals(status));
        branch.setImagePath(imagePath);
        return branch;
    }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void update(String name, String address, String phone) { this.name=name; this.address=address; this.phone=phone; }
    public void setOpen(boolean open) { status = open ? "OPEN" : "CLOSED"; }
    public String getBranchId() { return branchId; }
}
