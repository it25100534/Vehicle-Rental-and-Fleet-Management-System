package com.example.vehiclerentalserviceplatform.fleet;

public class Branch {
    private final String branchId;
    private String name;
    private String address;
    private String phone;
    private boolean open;
    private String imagePath;

    public Branch(String branchId, String name, String address, String phone, boolean open) {
        this.branchId = branchId;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.open = open;
    }

    public String getBranchId() { return branchId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public boolean isOpen() { return open; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setName(String name) { this.name = name; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setOpen(boolean open) { this.open = open; }

    @Override public String toString() {
        return String.join(",", branchId, name, address, phone, Boolean.toString(open));
    }
}
