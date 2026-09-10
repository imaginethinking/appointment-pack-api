package net.imaginethinking.appointmentpack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Creates the CORS rules used for browser requests from the configured frontend origins.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;

    /**
     * Reads the configured frontend origins and keeps the non blank values used for CORS.
     */
    public CorsConfig(
            @Value("${appointment-pack.cors.allowed-origins:http://localhost:4200}")
            String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::strip)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one CORS origin must be configured");
        }
    }

    /**
     * Allows the configured frontend origins to call the API with the supported methods and headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        configuration.setExposedHeaders(List.of("Location", "Content-Disposition"));

        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/v1/**", configuration);

        return source;
    }
}