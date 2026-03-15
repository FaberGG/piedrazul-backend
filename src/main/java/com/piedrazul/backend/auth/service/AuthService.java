package com.piedrazul.backend.auth.service;

import com.piedrazul.backend.auth.dto.AuthResponse;
import com.piedrazul.backend.auth.dto.LoginRequest;
import com.piedrazul.backend.auth.dto.RegisterMedicoRequest;
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


    /**
     * Registra un nuevo médico. Solo accesible para ADMIN.
     * Crea en una transacción atómica: Usuario (rol MEDICO) + Medico vinculado.
     */
    AuthResponse registerMedico(RegisterMedicoRequest request);

    /**
     * Registra un nuevo médico. Solo accesible para ADMIN.
     * Crea en una transacción atómica: Usuario (rol MEDICO) + Medico vinculado.
     *
     * @param request datos de registro del médico
     * @return respuesta con token JWT
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si el username ya existe
     */

    /**
     * Registra un usuario ADMIN. Solo para uso en desarrollo.
     * ELIMINAR o proteger antes de producción.
     *
     * @param request credenciales (username + password)
     * @return respuesta con token JWT
     */
    AuthResponse registerAdmin(LoginRequest request);

}