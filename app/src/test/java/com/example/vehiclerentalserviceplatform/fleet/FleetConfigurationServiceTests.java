package com.example.vehiclerentalserviceplatform.fleet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FleetConfigurationServiceTests {
    @Autowired BranchService branches;
    @Autowired VehicleCategoryService categories;
    @Test void createsAndUpdatesBranchesInSql(){
        branches.save("BR-098","Jaffna City","10 Hospital Road Jaffna","0212345678");
        assertThat(branches.find("BR-098").getName()).isEqualTo("Jaffna City");assertThat(branches.setOpen("BR-098",false)).isTrue();assertThat(branches.isOpen("BR-098")).isFalse();
    }
    @Test void createsAndDisablesManagedCategoriesInSql(){
        categories.save("PREMIUM-TEST","Premium Travel");assertThat(categories.isActive("PREMIUM-TEST")).isTrue();assertThat(categories.setActive("PREMIUM-TEST",false)).isTrue();assertThat(categories.isActive("PREMIUM-TEST")).isFalse();
    }
}
