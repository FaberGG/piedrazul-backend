package com.piedrazul.backend.auth.service;

import com.piedrazul.backend.auth.dto.AuthResponse;
import com.piedrazul.backend.auth.dto.LoginRequest;
import com.piedrazul.backend.auth.dto.RegisterPacienteRequest;
import com.piedrazul.backend.auth.repository.UsuarioRepository;
import com.piedrazul.backend.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de autenticación (módulo AUTH).
 *
 * DEPENDENCIAS:
 *  - usuarioRepository → buscar y persistir usuarios
 *  - jwtService        → generar token JWT tras autenticación exitosa
 *  - passwordEncoder   → verificar/hashear contraseñas con BCrypt
 *
 * NOTA IMPORTANTE — transaccionalidad en registerPaciente:
 *  El método crea dos entidades en una sola transacción (Usuario + Paciente).
 *  Si falla la creación del Paciente, el Usuario también debe hacer rollback.
 *  Usar @Transactional en el método o en la clase (ya aplicado a nivel clase).
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService        = jwtService;
        this.passwordEncoder   = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * PASOS A IMPLEMENTAR:
     *  1. usuarioRepository.findByUsername(request.username)
     *       → lanzar BadCredentialsException si no existe
     *  2. passwordEncoder.matches(request.password, usuario.password)
     *       → lanzar BadCredentialsException si no coincide
     *  3. Verificar usuario.estado == "ACTIVO"
     *       → lanzar BusinessRuleException("Usuario inactivo") si no
     *  4. jwtService.generateToken(usuario.username, usuario.id, usuario.rol)
     *  5. Retornar AuthResponse con token, username y rol
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        throw new UnsupportedOperationException("TODO AUTH: implementar login");
    }

    /**
     * {@inheritDoc}
     *
     * PASOS A IMPLEMENTAR:
     *  1. usuarioRepository.existsByUsername(request.username)
     *       → lanzar BusinessRuleException("Username ya existe") si true
     *  2. Crear Usuario:
     *       password = passwordEncoder.encode(request.password)
     *       rol = "PACIENTE", estado = "ACTIVO"
     *  3. usuarioRepository.save(usuario)
     *  4. Crear Paciente vinculado al Usuario recién creado
     *       (usar PacienteRepository — requiere inyectarlo si no está aquí)
     *  5. jwtService.generateToken(usuario.username, usuario.id, "PACIENTE")
     *  6. Retornar AuthResponse
     *
     * TODO: decidir si PacienteRepository debe inyectarse aquí o si
     *       debe crearse un evento que el módulo agenda escuche.
     *       (Opción B es más desacoplada: AuthService publica evento
     *        "PacienteRegistrado" y AgendaModule crea el Paciente.)
     */
    @Override
    public AuthResponse registerPaciente(RegisterPacienteRequest request) {
        throw new UnsupportedOperationException("TODO AUTH: implementar registerPaciente");
    }
}

