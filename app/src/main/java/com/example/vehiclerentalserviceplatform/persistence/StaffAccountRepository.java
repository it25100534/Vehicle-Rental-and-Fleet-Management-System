package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffAccountRepository extends JpaRepository<StaffAccountEntity, String> {
    Optional<StaffAccountEntity> findByUsernameIgnoreCaseAndActiveTrue(String username);
    boolean existsByUsernameIgnoreCase(String username);
    List<StaffAccountEntity> findAllByActiveTrueOrderByFullNameAsc();
}
