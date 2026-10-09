package com.example.vehiclerentalserviceplatform.controller;

import com.example.vehiclerentalserviceplatform.fleet.BranchService;
import com.example.vehiclerentalserviceplatform.fleet.BranchImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class HomeRedesignTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired BranchService branches;
    @Autowired BranchImageStorage images;
    @jakarta.persistence.PersistenceContext jakarta.persistence.EntityManager entityManager;

    @Test void showsHomeWithCollapsedFaqAndNoAdminShell() throws Exception {
        mvc.perform(get("/" )).andExpect(status().isOk()).andExpect(view().name("home-redesign"))
                .andExpect(content().string(containsString("DRIVE CONFIDENT")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("class=\"faq-item\" open"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("admin-sidebar"))));
    }
    @Test void sharedChromeRendersAcrossPublicPages() throws Exception {
        for (String path : new String[]{"/about", "/catalog", "/contact", "/login"}) {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("driveease-header")))
                    .andExpect(content().string(containsString("driveease-footer")));
        }
        mvc.perform(get("/about")).andExpect(content().string(containsString("2 years")))
                .andExpect(content().string(containsString("dewasurendra.avif")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("data-stat="))));
        mvc.perform(get("/admin/dashboard").sessionAttr("loggedInAdmin", "test-admin").sessionAttr("role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(containsString("driveease-header"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("driveease-footer"))));
        mvc.perform(get("/admin/contact-enquiries").sessionAttr("loggedInAdmin", "test-admin").sessionAttr("role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("premium-table enquiry-table")))
                .andExpect(content().string(containsString("No enquiries yet")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("driveease-footer"))));
    }
    @Test void statisticsReflectBranchAndVehicleChanges() throws Exception {
        long before=jdbc.queryForObject("SELECT COUNT(*) FROM branches WHERE status='OPEN'",Long.class);
        branches.save("BR-096","Jaffna City","10 Hospital Road Jaffna","0212345678");
        entityManager.flush();
        mvc.perform(get("/api/home/stats")).andExpect(status().isOk()).andExpect(jsonPath("$.branches").value(before+1));
        branches.setOpen("BR-096",false);
        entityManager.flush();
        long fleet=jdbc.queryForObject("SELECT COUNT(*) FROM vehicles WHERE retired=FALSE",Long.class);
        jdbc.update("UPDATE vehicles SET retired=TRUE WHERE vehicle_id='CAR-0001'");
        mvc.perform(get("/api/home/stats")).andExpect(jsonPath("$.branches").value(before)).andExpect(jsonPath("$.vehicles").value(fleet-1));
    }
    @Test void fleetPagesReturnOnlyRequestedVehiclesAndReflectBranchClosures() throws Exception {
        mvc.perform(get("/api/vehicles/catalog-page").param("search", "corolla"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.vehicles[0].model").value(containsString("Corolla")));
        mvc.perform(get("/api/vehicles/catalog-page").param("search", "no-such-vehicle-model"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/vehicles/catalog-page").param("limit", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vehicles.length()").value(1))
                .andExpect(jsonPath("$.total").isNumber());
        mvc.perform(get("/api/vehicles/catalog-page").param("limit", "0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vehicles.length()").value(0));
        mvc.perform(get("/api/vehicles/inventory-page").param("size", "1")
                        .sessionAttr("loggedInAdmin", "test-admin").sessionAttr("role", "ADMIN"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.vehicles.length()").value(1));
        branches.setOpen("BR-001", false);
        mvc.perform(get("/api/vehicles/catalog-page").param("branches", "BR-001"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
    @Test void savesImageAndKeepsItWhenEditingWithoutAnotherUpload() throws Exception {
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20,20,BufferedImage.TYPE_INT_RGB),"png",buffer);
        MockMultipartFile image=new MockMultipartFile("image","branch.png","image/png",buffer.toByteArray());
        mvc.perform(multipart("/admin/branches/save").file(image).param("branchId","BR-095").param("name","Test Branch")
                .param("address","10 Main Street Jaffna").param("phone","0212345678")
                .sessionAttr("loggedInAdmin","test-admin").sessionAttr("role","ADMIN"))
                .andExpect(status().is3xxRedirection()).andExpect(flash().attributeExists("message"));
        String path=branches.find("BR-095").getImagePath();
        assertThat(path).startsWith("/branch-images/");
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("image/png"));
        branches.save("BR-095","Updated Branch","10 Main Street Jaffna","0212345678");
        assertThat(branches.find("BR-095").getImagePath()).isEqualTo(path);
        Files.delete(images.directory().resolve(path.substring(path.lastIndexOf('/')+1)));
    }
    @Test void rejectsNonImageUploadAndCustomerBranchWrites() throws Exception {
        MockMultipartFile bad=new MockMultipartFile("image","fake.png","image/png","not an image".getBytes());
        mvc.perform(multipart("/admin/branches/save").file(bad).param("branchId","BR-094").param("name","Bad Branch")
                .param("address","10 Main Street Jaffna").param("phone","0212345678")
                .sessionAttr("loggedInAdmin","test-admin").sessionAttr("role","ADMIN"))
                .andExpect(flash().attributeExists("error"));
        assertThat(branches.find("BR-094")).isNull();
        mvc.perform(multipart("/admin/branches/save").file(bad).sessionAttr("loggedInUser","test11"))
                .andExpect(status().isForbidden());
    }
}
