package com.piedrazul.backend.shared.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String ROLE_PACIENTE = "ROLE_PACIENTE";
    private static final String JSON = "application/json";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<RateLimitPolicy> publicPolicies;
    private final List<RateLimitPolicy> pacientePolicies;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(List<RateLimitPolicy> publicPolicies, List<RateLimitPolicy> pacientePolicies) {
        this.publicPolicies = List.copyOf(publicPolicies);
        this.pacientePolicies = List.copyOf(pacientePolicies);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        boolean isPaciente = isAuthenticated && hasRole(authentication, ROLE_PACIENTE);

        RateLimitPolicy policy = resolvePolicy(request, isPaciente);
        if (policy == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String principalKey = policy.isPublic()
                ? "ip:" + resolveClientIp(request)
                : resolvePrincipalKey(authentication, request);
        String bucketKey = policy.id() + ":" + principalKey;

        Bucket bucket = buckets.computeIfAbsent(bucketKey, key -> Bucket.builder()
                .addLimit(policy.limit())
                .build());

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            filterChain.doFilter(request, response);
            return;
        }

        long waitSeconds = Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).getSeconds());
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(waitSeconds));
        response.setContentType(JSON);
        response.getWriter().write("{\"error\":\"Rate limit excedido\"}");
    }

    private RateLimitPolicy resolvePolicy(HttpServletRequest request, boolean isPaciente) {
        for (RateLimitPolicy policy : publicPolicies) {
            if (policy.matches(request, pathMatcher)) {
                return policy;
            }
        }
        if (isPaciente) {
            for (RateLimitPolicy policy : pacientePolicies) {
                if (policy.matches(request, pathMatcher)) {
                    return policy;
                }
            }
        }
        return null;
    }

    private boolean hasRole(Authentication authentication, String role) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    private String resolvePrincipalKey(Authentication authentication, HttpServletRequest request) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            if (jwt != null && jwt.getSubject() != null && !jwt.getSubject().isBlank()) {
                return "sub:" + jwt.getSubject();
            }
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return "name:" + authentication.getName();
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public record RateLimitPolicy(String id, HttpMethod method, String pathPattern, Bandwidth limit, boolean isPublic) {
        public static RateLimitPolicy publicPolicy(String id, HttpMethod method, String pathPattern, int capacity, Duration window) {
            return new RateLimitPolicy(id, method, pathPattern, Bandwidth.classic(capacity, Refill.greedy(capacity, window)), true);
        }

        public static RateLimitPolicy pacientePolicy(String id, HttpMethod method, String pathPattern, int capacity, Duration window) {
            return new RateLimitPolicy(id, method, pathPattern, Bandwidth.classic(capacity, Refill.greedy(capacity, window)), false);
        }

        boolean matches(HttpServletRequest request, AntPathMatcher matcher) {
            if (!method.matches(request.getMethod())) {
                return false;
            }
            return matcher.match(pathPattern, request.getRequestURI());
        }
    }
}

