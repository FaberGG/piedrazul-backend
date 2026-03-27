package com.piedrazul.backend.shared.security;

import org.springframework.stereotype.Service;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Servicio para generación y validación de tokens JWT.
 * Algoritmo: HMAC-SHA512 | Expiración: 24h
 */
@Service
public class JwtService {


    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private long expiration;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Genera un token JWT para el usuario autenticado.
     */
    public String generateToken(String username, Long userId, String rol) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Extrae el rol (subject) del token.
     */
    public String extractRol(String token) {
        return getClaims(token).get("rol", String.class);
    }

    /**
     * Valida si el token es válido y no ha expirado.
     */
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long extractUserId(String token) {
        return getClaims(token).get("userId", Long.class);
    }
}

