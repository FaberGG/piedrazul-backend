package com.piedrazul.backend.auth.internal.service;

import com.piedrazul.backend.auth.internal.domain.Usuario;
import com.piedrazul.backend.auth.internal.dto.LoginRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterMedicoRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterPacienteRequest;
import com.piedrazul.backend.auth.internal.repository.UsuarioRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.RegistroMedicoDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
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
    private final PasswordEncoder   passwordEncoder;

    // ← ya no inyectas JwtService
    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PacientesApi pacientesApi,
                           MedicosApi medicosApi,
                           PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.pacientesApi      = pacientesApi;
        this.medicosApi        = medicosApi;
        this.passwordEncoder   = passwordEncoder;
    }

    // ← login() se elimina completo, Keycloak lo maneja

    @Override
    public void registerPaciente(RegisterPacienteRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("PACIENTE")
                .build();

        usuarioRepository.save(usuario);

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
    }

    @Override
    public void registerMedico(RegisterMedicoRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("MEDICO")
                .build();

        usuarioRepository.save(usuario);

        medicosApi.registrarMedicoConUsuario(
                RegistroMedicoDTO.builder()
                        .usuarioId(usuario.getId())
                        .nombres(request.getNombres())
                        .apellidos(request.getApellidos())
                        .especialidad(request.getEspecialidad())
                        .tipo(request.getTipo())
                        .build()
        );
    }

    @Override
    public void registerAdmin(LoginRequest request) {
        if (usuarioRepository.existsByUsername(request.getUsername())) {
            throw new BusinessRuleException("El username ya está en uso");
        }

        Usuario usuario = Usuario.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol("ADMIN")
                .build();

        usuarioRepository.save(usuario);
    }
}