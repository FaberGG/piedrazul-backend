package com.piedrazul.backend.shared.security;

import org.springframework.stereotype.Service;

/**
 * Servicio para generación y validación de tokens JWT.
 * Algoritmo: HMAC-SHA512 | Expiración: 24h
 */
@Service
public class JwtService {

    /**
     * Genera un token JWT para el usuario autenticado.
     */
    public String generateToken(String username, Long userId, String rol) {
        // TODO: implementar generación de token con jjwt
        return null;
    }

    /**
     * Extrae el username (subject) del token.
     */
    public String extractUsername(String token) {
        // TODO: implementar extracción de claims
        return null;
    }

    /**
     * Valida si el token es válido y no ha expirado.
     */
    public boolean isTokenValid(String token) {
        // TODO: implementar validación
        return false;
    }
}

