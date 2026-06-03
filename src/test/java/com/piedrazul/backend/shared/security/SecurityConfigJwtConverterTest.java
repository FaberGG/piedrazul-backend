package com.piedrazul.backend.shared.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class SecurityConfigJwtConverterTest {

    private final SecurityConfig securityConfig = new SecurityConfig(new AppCorsProperties());

    @Test
    void mapeaRolesDesdeClaimTopLevelRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("roles", List.of("ADMIN", "offline_access"))
                .claim("preferred_username", "admin")
                .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);

        assertNotNull(auth);
        assertTrue(contieneAutoridad(auth.getAuthorities(), "ROLE_ADMIN"));
    }

    @Test
    void mantieneCompatibilidadConRealmAccessRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("MEDICO")))
                .claim("preferred_username", "medico")
                .build();

        AbstractAuthenticationToken auth = securityConfig.jwtAuthenticationConverter().convert(jwt);

        assertNotNull(auth);
        assertTrue(contieneAutoridad(auth.getAuthorities(), "ROLE_MEDICO"));
    }

    private boolean contieneAutoridad(Iterable<? extends GrantedAuthority> authorities, String value) {
        for (GrantedAuthority authority : authorities) {
            if (value.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}

