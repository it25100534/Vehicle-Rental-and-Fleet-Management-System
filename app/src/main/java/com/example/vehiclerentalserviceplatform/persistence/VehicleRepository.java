package com.example.vehiclerentalserviceplatform.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleRepository extends JpaRepository<VehicleEntity, String> {
    long countByBranchIdIgnoreCase(String branchId);
    long countByCategoryCodeIgnoreCase(String categoryCode);

    @Query("select v from VehicleEntity v where v.retired = false and " +
           "(:query = '' or lower(v.vehicleId) like lower(concat('%', :query, '%')) " +
           "or lower(v.make) like lower(concat('%', :query, '%')) " +
           "or lower(v.model) like lower(concat('%', :query, '%'))) order by v.vehicleId")
    Page<VehicleEntity> searchActive(@Param("query") String query, Pageable pageable);
}
