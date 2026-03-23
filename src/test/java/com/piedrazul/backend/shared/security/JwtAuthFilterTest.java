package com.piedrazul.backend.shared.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JwtAuthFilterTest {

    @Test
    void allowsPreflightOptionsRequestWithoutAuthorizationHeader() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(mock(JwtService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        request.addHeader("Origin", "http://localhost:4200");
        request.addHeader("Access-Control-Request-Method", "POST");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotWriteUnauthorizedResponseWhenBearerHeaderIsMissing() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(mock(JwtService.class));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/pacientes");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }
}

