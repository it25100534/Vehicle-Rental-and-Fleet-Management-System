package com.example.vehiclerentalserviceplatform.customer.service;

import com.example.vehiclerentalserviceplatform.customer.model.RegularCustomer;
import com.example.vehiclerentalserviceplatform.customer.controller.CustomerController;
import com.example.vehiclerentalserviceplatform.persistence.CustomerEntity;
import com.example.vehiclerentalserviceplatform.persistence.CustomerRepository;
import com.example.vehiclerentalserviceplatform.security.PasswordHash;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ExtendedModelMap;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CustomerFileServiceSecurityTests {
    @Autowired CustomerFileService service;
    @Autowired CustomerRepository repository;
    @Autowired CustomerController customerController;
    @Test void registrationStoresAHashAndActiveStatus(){
        assertThat(service.registerCustomer(customer("sql-anna","LIC-SQL-100","Secure123"))).isTrue();
        CustomerEntity saved=repository.findByUsernameIgnoreCase("sql-anna").orElseThrow();
        assertThat(saved.isActive()).isTrue();assertThat(PasswordHash.isEncoded(saved.getPasswordHash())).isTrue();assertThat(saved.getPasswordHash()).doesNotContain("Secure123");assertThat(service.loginCustomer("sql-anna","Secure123")).isTrue();
    }
    @Test void legacyPasswordIsUpgradedAfterSuccessfulLogin(){
        repository.save(new CustomerEntity("sql-legacy","LIC-SQL-200","sql-legacy@example.com","0771234567","Oldpass1",true));
        assertThat(service.loginCustomer("sql-legacy","Oldpass1")).isTrue();
        assertThat(PasswordHash.isEncoded(repository.findByUsernameIgnoreCase("sql-legacy").orElseThrow().getPasswordHash())).isTrue();
    }
    @Test void resetRequiresAllIdentityFieldsAndDeactivationBlocksLogin(){
        service.registerCustomer(customer("sql-nimal","LIC-SQL-300","First123"));
        assertThat(service.resetPassword("sql-nimal","wrong@example.com","LIC-SQL-300","Second123")).isFalse();
        assertThat(service.resetPassword("sql-nimal","sql-nimal@example.com","LIC-SQL-300","Second123")).isTrue();
        assertThat(service.loginCustomer("sql-nimal","Second123")).isTrue();assertThat(service.deactivateCustomer("sql-nimal","Second123")).isTrue();assertThat(service.isActiveCustomer("sql-nimal")).isFalse();assertThat(service.loginCustomer("sql-nimal","Second123")).isFalse();
        ExtendedModelMap model = new ExtendedModelMap();
        assertThat(customerController.handleForgotPassword("sql-nimal","sql-nimal@example.com","LIC-SQL-300","Third123","Third123",model)).isEqualTo("forgot-password");
        assertThat(model.get("error")).asString().contains("Your account is disabled");
        ExtendedModelMap loginModel = new ExtendedModelMap();
        assertThat(customerController.handleLogin("sql-nimal","Second123",new org.springframework.mock.web.MockHttpServletRequest(),loginModel)).isEqualTo("login");
        assertThat(loginModel.get("error")).asString().contains("Your account is disabled");
    }
    private RegularCustomer customer(String u,String licence,String p){return new RegularCustomer(u,p,licence,u+"@example.com","0771234567");}
}
