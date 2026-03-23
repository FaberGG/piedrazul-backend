package com.piedrazul.backend.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    private static AppCorsProperties buildBaseProperties() {
        AppCorsProperties properties = new AppCorsProperties();
        properties.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        properties.setAllowedHeaders(List.of("*"));
        properties.setExposedHeaders(List.of("Authorization"));
        properties.setAllowCredentials(false);
        properties.setMaxAge(3600L);
        return properties;
    }

    @Test
    void allowsAnyOriginWhenPatternIsWildcard() {
        AppCorsProperties properties = buildBaseProperties();
        properties.setAllowedOrigins(List.of());
        properties.setAllowedOriginPatterns(List.of("*"));
        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class), properties);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("*");
        assertThat(cors.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
    }

    @Test
    void deniesCrossOriginWhenNoOriginsAreConfigured() {
        AppCorsProperties properties = buildBaseProperties();
        properties.setAllowedOrigins(List.of());
        properties.setAllowedOriginPatterns(List.of());
        SecurityConfig config = new SecurityConfig(mock(JwtAuthFilter.class), properties);

        CorsConfigurationSource source = config.corsConfigurationSource();
        CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login"));

        assertThat(cors).isNotNull();
        assertThat(cors.checkOrigin("http://localhost:4200")).isNull();
    }
}

