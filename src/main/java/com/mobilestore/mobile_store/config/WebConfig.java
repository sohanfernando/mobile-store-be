package com.mobilestore.mobile_store.config;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.location:./uploads}")
    private String storageLocation;

    @Value("${app.storage.public-base-url:/uploads}")
    private String publicBaseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(storageLocation).toAbsolutePath().normalize().toUri().toString();
        String pattern = publicBaseUrl.endsWith("/") ? publicBaseUrl + "**" : publicBaseUrl + "/**";
        registry.addResourceHandler(pattern).addResourceLocations(location);
    }
}
