package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.auth.internal.domain.Usuario;
import com.piedrazul.backend.auth.internal.dto.AuthResponse;
import com.piedrazul.backend.auth.internal.dto.LoginRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterMedicoRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterPacienteRequest;
import com.piedrazul.backend.auth.internal.repository.UsuarioRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.RegistroMedicoDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de autenticación (módulo AUTH).
 *
 * DEPENDENCIAS:
 *  - usuarioRepository → buscar y persistir usuarios
 *  - pacientesApi      → registrar datos personales del paciente (módulo pacientes)
 *  - medicosApi        → registrar datos personales del medico (modulo medicos)
 *  - jwtService        → generar token JWT tras autenticación exitosa
 *  - passwordEncoder   → verificar/hashear contraseñas con BCrypt
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacientesApi      pacientesApi;
    private final MedicosApi        medicosApi;
    private final JwtService        jwtService;
    private final PasswordEncoder   passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PacientesApi pacientesApi,
                           MedicosApi medicosApi,
                           JwtService jwtService,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacientesApi      = pacientesApi;
        this.medicosApi        = medicosApi;
        this.jwtService        = jwtService;
        this.passwordEncoder   = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * PASOS:
     *  1. Buscar usuario por username → BadCredentialsException si no existe
     *  2. Verificar password con BCrypt → BadCredentialsException si no coincide
     *  3. Verificar estado == "ACTIVO" → BusinessRuleException si inactivo
     *  4. Generar JWT con username, userId y rol
     *  5. Retornar AuthResponse
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // 1. Buscar usuario
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));

        // 2. Verificar password
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // 3. Verificar estado
        if (!"ACTIVO".equals(usuario.getEstado())) {
            throw new BusinessRuleException("Usuario inactivo");
        }

        // 4. Generar JWT
        String token = jwtService.generateToken(usuario.getUsername(), usuario.getId(), usuario.getRol());

        // 5. Retornar respuesta
        return AuthResponse.builder()
                .token(token)
                .userId(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .expiresIn(86400)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * PASOS:
     *  1. Verificar username único → BusinessRuleException si ya existe
     *  2. Crear Usuario con password encriptado, rol=PACIENTE, estado=ACTIVO
     *  3. Persistir Usuario
     *  4. Crear Paciente vinculado al Usuario via PacientesApi
     *  5. Generar JWT y retornar AuthResponse
     */
    @Override
    public AuthResponse registerPaciente(RegisterPacienteRequest request) {
        // 1. Verificar username único
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        // 2 y 3. Crear y persistir Usuario
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("PACIENTE")
                .build();

        usuarioRepository.save(usuario);

        // 4. Crear Paciente vinculado
        pacientesApi.registrarPacienteConUsuario(
                RegistroPacienteDTO.builder()
                        .usuarioId(usuario.getId())
                        .documento(request.getDocumento())
                        .nombres(request.getNombres())
                        .apellidos(request.getApellidos())
                        .celular(request.getCelular())
                        .correo(request.getCorreo())
                        .fechaNacimiento(request.getFechaNacimiento())
                        .genero(request.getGenero())
                        .build()
        );

        // 5. Generar JWT y retornar respuesta
        String token = jwtService.generateToken(usuario.getUsername(), usuario.getId(), usuario.getRol());

        return AuthResponse.builder()
                .token(token)
                .userId(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .expiresIn(86400)
                .build();
    }

    /**
     * PASOS:
     *  1. Verificar username único → BusinessRuleException si ya existe
     *  2. Crear Usuario con password encriptado, rol=MEDICO, estado=ACTIVO
     *  3. Persistir Usuario
     *  4. Crear Medico vinculado al Usuario via MedicosApi
     *  5. Generar JWT y retornar AuthResponse
     */
    @Override
    public AuthResponse registerMedico(RegisterMedicoRequest request) {
        // 1. Verificar username único
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        // 2 y 3. Crear y persistir Usuario
        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("MEDICO")
                .build();

        usuarioRepository.save(usuario);

        // 4. Crear Medico vinculado
        medicosApi.registrarMedicoConUsuario(
                RegistroMedicoDTO.builder()
                        .usuarioId(usuario.getId())
                        .nombres(request.getNombres())
                        .apellidos(request.getApellidos())
                        .especialidad(request.getEspecialidad())
                        .tipo(request.getTipo())
                        .build()
        );

        // 5. Generar JWT y retornar respuesta
        String token = jwtService.generateToken(usuario.getUsername(), usuario.getId(), usuario.getRol());

        return AuthResponse.builder()
                .token(token)
                .userId(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .expiresIn(86400)
                .build();
    }

    @Override
    public AuthResponse registerAdmin(LoginRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("ADMIN")
                .build();

        usuarioRepository.save(usuario);

        String token = jwtService.generateToken(usuario.getUsername(), usuario.getId(), usuario.getRol());

        return AuthResponse.builder()
                .token(token)
                .userId(usuario.getId())
                .username(usuario.getUsername())
                .rol(usuario.getRol())
                .expiresIn(86400)
                .build();
    }


}