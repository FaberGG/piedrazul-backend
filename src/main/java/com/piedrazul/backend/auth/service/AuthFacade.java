package com.piedrazul.backend.auth.service;

import com.piedrazul.backend.auth.AuthApi;
import com.piedrazul.backend.auth.dto.UsuarioInfoDto;
import com.piedrazul.backend.auth.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementación de la API pública del módulo AUTH.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  RESPONSABILIDAD DE ESTA CLASE                              │
 * │                                                             │
 * │  1. Implementar AuthApi (contrato público del módulo).      │
 * │  2. Exponer SOLO la información de identidad mínima:        │
 * │     id, username, rol, estado — NUNCA la contraseña.        │
 * │  3. Mapear Usuario (entidad interna) → UsuarioInfoDto        │
 * │     (DTO público) antes de cruzar la frontera del módulo.   │
 * └─────────────────────────────────────────────────────────────┘
 *
 * PATRÓN: Walking Skeleton — métodos funcionales desde el inicio.
 * El mapeo Usuario → UsuarioInfoDto ya está implementado aquí.
 */
@Service
@Transactional(readOnly = true)
public class AuthFacade implements AuthApi {

    private final UsuarioRepository usuarioRepository;

    public AuthFacade(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // AuthApi — implementación
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTADO: busca por username y mapea a UsuarioInfoDto.
     * No expone la contraseña ni ningún campo sensible de Usuario.
     *
     * AMPLIAR si se necesita: nombre completo, email, etc.
     * En ese caso añadir el campo a UsuarioInfoDto (no a Usuario directamente).
     */
    @Override
    public Optional<UsuarioInfoDto> findByUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .map(u -> UsuarioInfoDto.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .rol(u.getRol())
                        .estado(u.getEstado())
                        .build());
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTADO: verifica existencia y estado ACTIVO.
     *
     * AMPLIAR si se definen más estados (SUSPENDIDO, BLOQUEADO, etc.):
     * ajustar la condición de estado aquí, sin tocar los llamadores.
     */
    @Override
    public boolean existeUsuarioActivo(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .map(u -> "ACTIVO".equals(u.getEstado()))
                .orElse(false);
    }
}

