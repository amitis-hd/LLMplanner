package edu.tufts.hrilab.gui;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${cors.origin}")
    private String corsOrigin;

    // Helper method to convert comma-separated String to an array
    private String[] parseCorsOrigins() {
        return corsOrigin.split(",");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // This configuration tells Spring MVC that any requests to /images/** should be served from the file system path specified.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/home/hrilab/code/diarc-old/maps/elevator_lab_test/");
//        WebMvcConfigurer.super.addResourceHandlers(registry);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Allow specific origins or use "*" to allow all origins
        registry.addMapping("/images/**").allowedOrigins(parseCorsOrigins());
    }
}
