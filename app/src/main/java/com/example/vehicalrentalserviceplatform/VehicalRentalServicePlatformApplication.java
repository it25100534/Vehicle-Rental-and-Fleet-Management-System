package com.example.vehicalrentalserviceplatform;


import com.example.vehicalrentalserviceplatform.controller.DeleteAdminStaffController;
import com.example.vehicalrentalserviceplatform.controller.RegisterAdminStaffController;
import com.example.vehicalrentalserviceplatform.controller.UpdateAdminStaffController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class VehicalRentalServicePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(VehicalRentalServicePlatformApplication.class, args);
    }


    @Bean
    public ServletRegistrationBean<RegisterAdminStaffController> registerAdminStaffServletBean() {
        return new ServletRegistrationBean<>(new RegisterAdminStaffController(), "/registerAdminStaff");
    }

    @Bean
    public ServletRegistrationBean<DeleteAdminStaffController> deleteAdminStaffServletBean() {
        return new ServletRegistrationBean<>(new DeleteAdminStaffController(), "/deleteAdminStaff");
    }

    @Bean
    public ServletRegistrationBean<UpdateAdminStaffController> updateAdminStaffServletBean() {
        return new ServletRegistrationBean<>(new UpdateAdminStaffController(), "/updateAdminStaff");
    }

}