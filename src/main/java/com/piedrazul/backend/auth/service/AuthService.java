package com.piedrazul.backend.auth.service;

import com.piedrazul.backend.auth.dto.AuthResponse;
import com.piedrazul.backend.auth.dto.LoginRequest;
import com.piedrazul.backend.auth.dto.RegisterPacienteRequest;
import org.springframework.stereotype.Service;

/**
 * Servicio de autenticación y registro de usuarios.
 */
@Service
public class AuthService {

    /**
     * Autentica un usuario y genera un token JWT.
     */
    public AuthResponse login(LoginRequest request) {
        // TODO: validar credenciales, generar JWT
        return null;
    }

    /**
     * Registra un nuevo paciente con cuenta de autogestión.
     */
    public AuthResponse registerPaciente(RegisterPacienteRequest request) {
        // TODO: crear Usuario + Paciente, generar JWT
        return null;
    }
}

