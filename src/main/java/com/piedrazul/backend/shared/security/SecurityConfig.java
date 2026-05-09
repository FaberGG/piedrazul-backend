package com.piedrazul.backend.shared.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitingProperties.class)
public class SecurityConfig {

    private final AppCorsProperties corsProperties;

    public SecurityConfig(AppCorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitingFilter rateLimitingFilter) {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"No autorizado\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/register/paciente").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .addFilterBefore(rateLimitingFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Set<String> roles = extraerRoles(jwt);
            return roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .map(role -> role.trim())
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toList());
        });
        return converter;
    }

    private Set<String> extraerRoles(Jwt jwt) {
        Set<String> roles = new LinkedHashSet<>();

        agregarRoles(roles, jwt.getClaim("roles"));

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            agregarRoles(roles, realmAccess.get("roles"));
        }

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null) {
            for (Object clientData : resourceAccess.values()) {
                if (clientData instanceof Map<?, ?> clientMap) {
                    agregarRoles(roles, clientMap.get("roles"));
                }
            }
        }

        return roles;
    }

    private void agregarRoles(Set<String> sink, Object claimValue) {
        if (claimValue instanceof String role && !role.isBlank()) {
            sink.add(role);
            return;
        }

        if (claimValue instanceof Collection<?> values) {
            for (Object value : values) {
                if (value instanceof String role && !role.isBlank()) {
                    sink.add(role);
                }
            }
        }
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> cleanAllowedOrigins = clean(corsProperties.getAllowedOrigins());
        List<String> cleanAllowedOriginPatterns = clean(corsProperties.getAllowedOriginPatterns());
        if (!cleanAllowedOrigins.isEmpty()) configuration.setAllowedOrigins(cleanAllowedOrigins);
        if (!cleanAllowedOriginPatterns.isEmpty()) configuration.setAllowedOriginPatterns(cleanAllowedOriginPatterns);
        configuration.setAllowedMethods(clean(corsProperties.getAllowedMethods()));
        configuration.setAllowedHeaders(clean(corsProperties.getAllowedHeaders()));
        configuration.setExposedHeaders(clean(corsProperties.getExposedHeaders()));
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> clean(List<String> values) {
        if (values == null) return List.of();
        return values.stream().map(String::trim).filter(v -> !v.isEmpty()).toList();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        return JwtDecoders.fromIssuerLocation(issuerUri);
    }

    @Bean
    public RateLimitingFilter rateLimitingFilter(RateLimitingProperties properties) {
        List<RateLimitingFilter.RateLimitPolicy> publicPolicies = properties.getPublicPolicies().stream()
                .map(policy -> toPolicy(policy, true))
                .toList();

        List<RateLimitingFilter.RateLimitPolicy> pacientePolicies = properties.getPacientePolicies().stream()
                .map(policy -> toPolicy(policy, false))
                .toList();

        return new RateLimitingFilter(publicPolicies, pacientePolicies);
    }

    private RateLimitingFilter.RateLimitPolicy toPolicy(RateLimitingProperties.Policy policy, boolean isPublic) {
        HttpMethod method = HttpMethod.valueOf(policy.getMethod().toUpperCase(Locale.ROOT));
        if (isPublic) {
            return RateLimitingFilter.RateLimitPolicy.publicPolicy(
                    policy.getId(),
                    method,
                    policy.getPath(),
                    policy.getCapacity(),
                    policy.getWindow());
        }
        return RateLimitingFilter.RateLimitPolicy.pacientePolicy(
                policy.getId(),
                method,
                policy.getPath(),
                policy.getCapacity(),
                policy.getWindow());
    }

}