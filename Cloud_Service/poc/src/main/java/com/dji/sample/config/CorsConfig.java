package com.dji.sample.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("#{'${cors.allowed-origin-patterns}'.split(',')}")
    private List<String> allowedOriginPatterns;

    @Value("#{'${cors.allowed-methods}'.split(',')}")
    private List<String> allowedMethods;

    @Value("#{'${cors.allowed-headers}'.split(',')}")
    private List<String> allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private Boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private Long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOriginPatterns(
                normalizeValues(allowedOriginPatterns)
        );

        configuration.setAllowedMethods(
                normalizeValues(allowedMethods)
        );

        configuration.setAllowedHeaders(
                normalizeValues(allowedHeaders)
        );

        configuration.setAllowCredentials(
                allowCredentials
        );

        configuration.setMaxAge(
                maxAge
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }

    private List<String> normalizeValues(
            List<String> values
    ) {
        if (values == null) {
            return List.of();
        }

        List<String> normalized =
                new ArrayList<>();

        for (String value : values) {
            if (
                    value != null &&
                    !value.trim().isEmpty()
            ) {
                normalized.add(
                        value.trim()
                );
            }
        }

        return normalized;
    }
}