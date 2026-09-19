package com.example.vehicalrentalserviceplatform.customer.model;

import com.example.vehicalrentalserviceplatform.model.User;

public class RegularCustomer extends User {

    private String email;
    private String phone;

    public RegularCustomer(String username, String password, String licenseId,
                           String email, String phone) {
        super(null, username, username, password, licenseId, "CUSTOMER");
        this.email = email;

        this.phone = (phone != null && phone.length() > 10)
                ? phone.substring(0, 10)
                : phone;
    }

    public RegularCustomer() {
        super();
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public void setEmail(String email) { this.email = email; }


    public void setPhone(String phone) {
        if (phone != null && phone.length() > 10) {
            this.phone = phone.substring(0, 10);
        } else {
            this.phone = phone;
        }
    }

    public String toFileString() {
        return "CUSTOMER," +
                getUsername() + "," +
                getPassword() + "," +
                getLicenseId() + "," +
                email + "," +
                phone;
    }

    @Override
    public String toString() {
        return "RegularCustomer{username='" + getUsername() + "', email='" + email + "'}";
    }
}