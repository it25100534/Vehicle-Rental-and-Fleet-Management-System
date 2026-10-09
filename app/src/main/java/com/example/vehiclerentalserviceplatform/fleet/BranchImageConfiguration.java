package com.example.vehiclerentalserviceplatform.fleet;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class BranchImageConfiguration implements WebMvcConfigurer {
    private final BranchImageStorage storage;
    public BranchImageConfiguration(BranchImageStorage storage) { this.storage = storage; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String uri = storage.directory().toUri().toString();
        registry.addResourceHandler("/branch-images/**").addResourceLocations(uri.endsWith("/") ? uri : uri + "/");
    }
}
