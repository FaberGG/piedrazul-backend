package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.auth.internal.dto.LoginRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterMedicoRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterPacienteRequest;
import com.piedrazul.backend.shared.exception.BusinessRuleException;

/**
 * Contrato interno del servicio de autenticación (módulo AUTH).
 *
 * Implementación: {@link AuthServiceImpl}
 */
public interface AuthService {

    /**
     * Registra un nuevo paciente.
     * Crea en una transacción atómica: Usuario (rol PACIENTE) + Paciente vinculado.
     *
     * @throws BusinessRuleException si el username ya existe
     */
    void registerPaciente(RegisterPacienteRequest request);

    /**
     * Registra un nuevo médico. Solo accesible para ADMIN.
     * Crea en una transacción atómica: Usuario (rol MEDICO) + Medico vinculado.
     *
     * @throws BusinessRuleException si el username ya existe
     */
    void registerMedico(RegisterMedicoRequest request);

    /**
     * Registra un usuario ADMIN.
     *
     * @throws BusinessRuleException si el username ya existe
     */
    void registerAdmin(LoginRequest request);
}