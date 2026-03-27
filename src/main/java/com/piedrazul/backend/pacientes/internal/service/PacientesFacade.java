package com.piedrazul.backend.pacientes.internal.service;

import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;
import com.piedrazul.backend.pacientes.domain.Paciente;
import com.piedrazul.backend.pacientes.repository.PacientesRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion del facade publico del modulo Pacientes.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PacientesFacade implements PacientesApi {

    private final PacientesRepository pacientesRepository;

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
    public PacienteResumenDTO buscarPorUsuarioId(Long usuarioId) {
        Paciente paciente = pacientesRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", usuarioId));

        return toResumen(paciente);
    }

    private PacienteResumenDTO toResumen(Paciente paciente) {
        return PacienteResumenDTO.builder()
                .id(paciente.getId())
                .documento(paciente.getDocumento())
                .nombres(paciente.getNombres())
                .apellidos(paciente.getApellidos())
                .celular(paciente.getCelular())
                .build();
    }
}

