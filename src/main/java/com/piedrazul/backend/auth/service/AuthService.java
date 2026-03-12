package com.piedrazul.backend.auth.service;

import com.piedrazul.backend.auth.dto.AuthResponse;
import com.piedrazul.backend.auth.dto.LoginRequest;
import com.piedrazul.backend.auth.dto.RegisterPacienteRequest;

/**
 * Contrato interno del servicio de autenticación (módulo AUTH).
 *
 * Implementación: {@link AuthServiceImpl}
 */
public interface AuthService {

    /**
     * Autentica un usuario con sus credenciales y genera un token JWT.
     *
     * @param request credenciales (username + password)
     * @return respuesta con token JWT, username y rol
     * @throws org.springframework.security.authentication.BadCredentialsException si las credenciales son inválidas
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si el usuario está inactivo
     */
    AuthResponse login(LoginRequest request);

    /**
     * Registra un nuevo paciente con cuenta de autogestión y genera su token JWT.
     * Crea en una transacción atómica: Usuario (rol PACIENTE) + Paciente vinculado.
     *
     * @param request datos de registro
     * @return respuesta con token JWT listo para usar
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si el username ya existe
     */
    AuthResponse registerPaciente(RegisterPacienteRequest request);
}


