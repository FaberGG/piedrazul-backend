package com.piedrazul.backend.pacientes.internal.service;

import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.ActualizarPacienteDTO;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.pacientes.internal.domain.Paciente;
import com.piedrazul.backend.pacientes.internal.repository.PacientesRepository;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import com.piedrazul.backend.shared.util.NombreNormalizadorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementacion del facade publico del modulo Pacientes.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PacientesFacade implements PacientesApi {

    private final PacientesRepository pacientesRepository;

    @Override
    public PacienteResumenDTO registrarPacienteConUsuario(RegistroPacienteDTO request) {
        if (request.getUsuarioId() == null) {
            throw new BusinessRuleException("El usuarioId es obligatorio para registrar un paciente");
        }

        if (pacientesRepository.findByUsuarioId(request.getUsuarioId()).isPresent()) {
            throw new BusinessRuleException("El usuario ya tiene un paciente vinculado");
        }

        if (pacientesRepository.existsByDocumento(request.getDocumento())) {
            throw new BusinessRuleException("El documento ya esta registrado");
        }

        Paciente paciente = pacientesRepository.save(
                Paciente.builder()
                        .usuarioId(request.getUsuarioId())
                        .documento(request.getDocumento())
                        .nombres(request.getNombres())
                        .apellidos(request.getApellidos())
                        .celular(request.getCelular())
                        .correo(request.getCorreo())
                        .fechaNacimiento(request.getFechaNacimiento())
                        .genero(request.getGenero())
                        .build()
        );

        return toResumen(paciente);
    }

    @Override
    public PacienteResumenDTO obtenerOCrearPorDocumento(RegistroPacienteDTO request) {
        Paciente paciente = pacientesRepository.findByDocumento(request.getDocumento())
                .orElseGet(() -> pacientesRepository.save(
                        Paciente.builder()
                                .documento(request.getDocumento())
                                .nombres(request.getNombres())
                                .apellidos(request.getApellidos())
                                .celular(request.getCelular())
                                .correo(request.getCorreo())
                                .fechaNacimiento(request.getFechaNacimiento())
                                .genero(request.getGenero())
                                .build()
                ));

        return toResumen(paciente);
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResumenDTO obtenerResumenPorId(Long pacienteId) {
        Paciente paciente = pacientesRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", pacienteId));

        return toResumen(paciente);
    }

    @Override
    @Transactional(readOnly = true)
    public PacienteResumenDTO buscarPorUsuarioId(UUID usuarioId) {
        Paciente paciente = pacientesRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", usuarioId));

        return toResumen(paciente);
    }

    @Override
    public PacienteResumenDTO actualizarDatosPaciente(Long pacienteId, ActualizarPacienteDTO datos) {
        Paciente paciente = pacientesRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", pacienteId));

        if (datos.getNombres() != null) paciente.setNombres(NombreNormalizadorUtil.normalizar(datos.getNombres()));
        if (datos.getApellidos() != null) paciente.setApellidos(NombreNormalizadorUtil.normalizar(datos.getApellidos()));
        if (datos.getDocumento() != null) paciente.setDocumento(datos.getDocumento().trim());
        if (datos.getCelular() != null) paciente.setCelular(datos.getCelular().trim());
        if (datos.getCorreo() != null) paciente.setCorreo(datos.getCorreo().trim().toLowerCase());

        return toResumen(pacientesRepository.save(paciente));
    }

    private PacienteResumenDTO toResumen(Paciente paciente) {
        return PacienteResumenDTO.builder()
                .id(paciente.getId())
                .documento(paciente.getDocumento())
                .nombres(paciente.getNombres())
                .apellidos(paciente.getApellidos())
                .celular(paciente.getCelular())
                .correo(paciente.getCorreo())
                .build();
    }
}

