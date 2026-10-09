package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, String> {
    Optional<CustomerEntity> findByUsernameIgnoreCase(String username);
    Optional<CustomerEntity> findByLicenseIdIgnoreCase(String licenseId);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByLicenseIdIgnoreCase(String licenseId);
    boolean existsByEmailIgnoreCase(String email);
}
