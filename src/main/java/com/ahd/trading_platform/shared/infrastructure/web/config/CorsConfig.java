package com.ahd.trading_platform.shared.infrastructure.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * Allows frontend applications to call the API from different origins.
 *
 * NOTE: This is a permissive configuration for development.
 * In production, restrict allowed origins to specific domains.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        // Allow all origins (DEVELOPMENT ONLY - restrict in production)
        config.addAllowedOriginPattern("*");

        // Allow all headers
        config.addAllowedHeader("*");

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Expose headers that frontend can read
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        // Apply CORS configuration to all API endpoints
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);
    }
}
