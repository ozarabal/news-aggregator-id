package com.app.news_aggregator.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig mengkonfigurasi CORS agar frontend (localhost:5173) dapat
 * mengakses API backend (localhost:8080) dari browser.
 *
 * Tanpa ini, browser akan memblokir semua request cross-origin karena
 * preflight OPTIONS request akan mendapat respons 403.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173",   // Vite dev server (default)
                        "http://localhost:5174",   // Vite dev server (fallback port)
                        "http://localhost:4173"    // Vite preview
                )
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
