package com.example.vehiclerentalserviceplatform;


import com.example.vehiclerentalserviceplatform.controller.DeleteAdminStaffController;
import com.example.vehiclerentalserviceplatform.controller.RegisterAdminStaffController;
import com.example.vehiclerentalserviceplatform.controller.UpdateAdminStaffController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import com.example.vehiclerentalserviceplatform.service.AdminStaffService;


@SpringBootApplication
public class VehicleRentalServicePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicleRentalServicePlatformApplication.class, args);
    }


    @Bean
    public ServletRegistrationBean<RegisterAdminStaffController> registerAdminStaffServletBean(AdminStaffService service) {
        return new ServletRegistrationBean<>(new RegisterAdminStaffController(service), "/registerAdminStaff");
    }

    @Bean
    public ServletRegistrationBean<DeleteAdminStaffController> deleteAdminStaffServletBean(AdminStaffService service) {
        return new ServletRegistrationBean<>(new DeleteAdminStaffController(service), "/deleteAdminStaff");
    }

    @Bean
    public ServletRegistrationBean<UpdateAdminStaffController> updateAdminStaffServletBean(AdminStaffService service) {
        return new ServletRegistrationBean<>(new UpdateAdminStaffController(service), "/updateAdminStaff");
    }

}
