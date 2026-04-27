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

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de autenticación (módulo AUTH).
 *
 * DEPENDENCIAS:
 *  - usuarioRepository → buscar y persistir usuarios
 *  - pacientesApi      → registrar datos personales del paciente (módulo pacientes)
 *  - medicosApi        → registrar datos personales del medico (modulo medicos)
 *  - keycloakAdminService → crear usuario y asignar rol en Keycloak
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PacientesApi      pacientesApi;
    private final MedicosApi        medicosApi;
    private final KeycloakAdminService keycloakAdminService;

    public AuthServiceImpl(UsuarioRepository usuarioRepository,
                           PacientesApi pacientesApi,
                           MedicosApi medicosApi,
                           KeycloakAdminService keycloakAdminService) {
        this.usuarioRepository = usuarioRepository;
        this.pacientesApi      = pacientesApi;
        this.medicosApi        = medicosApi;
        this.keycloakAdminService  = keycloakAdminService;
    }

    //  login() se elimina completo, Keycloak lo maneja

    @Override
    public void registerPaciente(RegisterPacienteRequest request) {
        if (usuarioRepository.existsByUsername(request.getDocumento())) {
            throw new BusinessRuleException("El username ya está en uso");
        }
        validarPassword(request.getPassword(), request.getDocumento());

        String keycloakUserId = keycloakAdminService.crearUsuario(
                request.getDocumento(),
                request.getCorreo(),
                request.getPassword(),
                "PACIENTE"
        );

        Usuario usuario = Usuario.builder()
                .keycloakId(keycloakUserId)
                .username(request.getDocumento())
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
        validarPassword(request.getPassword(), request.getUsername());

        String keycloakUserId = keycloakAdminService.crearUsuario(
                request.getUsername(),
                null,
                request.getPassword(),
                "MEDICO"
        );

        Usuario usuario = Usuario.builder()
                .keycloakId(keycloakUserId)
                .username(request.getUsername())
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
        validarPassword(request.getPassword(), request.getUsername());

        String keycloakUserId = keycloakAdminService.crearUsuario(
                request.getUsername(),
                null,
                request.getPassword(),
                "ADMIN"
        );

        Usuario usuario = Usuario.builder()
                .keycloakId(keycloakUserId)
                .username(request.getUsername())
                .rol("ADMIN")
                .build();

        usuarioRepository.save(usuario);
    }

    private void validarPassword(String password, String username) {
        if (password == null || password.length() < 8) {
            throw new BusinessRuleException("La contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BusinessRuleException("La contraseña debe contener al menos una mayúscula");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new BusinessRuleException("La contraseña debe contener al menos un número");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
            throw new BusinessRuleException("La contraseña debe contener al menos un carácter especial");
        }
        if (password.contains(" ")) {
            throw new BusinessRuleException("La contraseña no debe contener espacios");
        }
        if (username != null && username.contains(" ")) {
            throw new BusinessRuleException("El nombre de usuario no debe contener espacios");
        }
    }

}