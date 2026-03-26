package com.piedrazul.backend.auth.api;

import com.piedrazul.backend.auth.api.dto.UsuarioInfoDto;

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
 */
public interface AuthApi {

    Optional<UsuarioInfoDto> findByUsername(String username);

    Optional<UsuarioInfoDto> findById(Long usuarioId); // ← nuevo

    boolean existeUsuarioActivo(Long usuarioId);
}