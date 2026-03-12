package com.piedrazul.backend.auth;

import com.piedrazul.backend.auth.dto.UsuarioInfoDto;

import java.util.Optional;

/**
 * ══════════════════════════════════════════════════════════════
 *  INTERFAZ PÚBLICA DEL MÓDULO AUTH
 * ══════════════════════════════════════════════════════════════
 *
 * Punto de entrada ÚNICO para cualquier módulo externo que necesite
 * consultar información de usuarios.
 *
 * REGLA: Otros módulos SOLO inyectan AuthApi.
 *        Está PROHIBIDO importar auth.domain o auth.repository
 *        desde fuera de este módulo.
 *
 * @see com.piedrazul.backend.auth.service.AuthFacade
 */
public interface AuthApi {

    /**
     * Busca información básica de un usuario por su nombre de usuario.
     * El llamador recibe un DTO, nunca la entidad {@code Usuario}.
     *
     * @param username nombre de usuario
     * @return DTO con id, username y rol; vacío si no existe
     */
    Optional<UsuarioInfoDto> findByUsername(String username);

    /**
     * Verifica si un ID de usuario existe y está activo.
     *
     * @param usuarioId ID del usuario
     * @return {@code true} si existe y su estado es ACTIVO
     */
    boolean existeUsuarioActivo(Long usuarioId);
}

