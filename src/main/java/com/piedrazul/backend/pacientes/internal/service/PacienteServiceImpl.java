package com.piedrazul.backend.pacientes.internal.service;

import com.piedrazul.backend.pacientes.internal.domain.Paciente;
import com.piedrazul.backend.pacientes.internal.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.internal.dto.PacienteSugerenciaResponse;
import com.piedrazul.backend.pacientes.internal.repository.PacientesRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private static final int LIMITE_DEFAULT_SUGERENCIAS = 5;
    private static final int LIMITE_MAXIMO_SUGERENCIAS = 10;
    private static final int MIN_CARACTERES_BUSQUEDA = 2;

    private final PacientesRepository pacienteRepository;

    @Override
    public List<PacienteResponse> listarTodos() {
        return pacienteRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PacienteResponse buscarPorId(Long id) {
        Paciente paciente = pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", id));
        return toResponse(paciente);
    }

    @Override
    public List<PacienteSugerenciaResponse> buscarPorDocumentoPrefijo(String documento, int limit) {
        String prefijo = documento == null ? "" : documento.trim();
        if (prefijo.length() < MIN_CARACTERES_BUSQUEDA) {
            return List.of();
        }

        int limiteAplicado = limit > 0 ? Math.min(limit, LIMITE_MAXIMO_SUGERENCIAS) : LIMITE_DEFAULT_SUGERENCIAS;

        return pacienteRepository
                .findByDocumentoStartingWithOrderByDocumentoAsc(prefijo, PageRequest.of(0, limiteAplicado))
                .stream()
                .map(this::toSugerenciaResponse)
                .toList();
    }

    @Override
    public PacienteResponse buscarPorUsuarioId(UUID usuarioId) {
        Paciente paciente = pacienteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", usuarioId));
        return toResponse(paciente);
    }

    @Override
    public PacienteResponse buscarPorKeycloakId(String keycloakId) {
        Paciente paciente = pacienteRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", keycloakId));
        return toResponse(paciente);
    }

    private PacienteResponse toResponse(Paciente p) {
        return PacienteResponse.builder()
                .id(p.getId())
                .documento(p.getDocumento())
                .nombres(p.getNombres())
                .apellidos(p.getApellidos())
                .celular(p.getCelular())
                .correo(p.getCorreo())
                .fechaNacimiento(p.getFechaNacimiento())
                .genero(p.getGenero())
                .build();
    }

    private PacienteSugerenciaResponse toSugerenciaResponse(Paciente paciente) {
        return PacienteSugerenciaResponse.builder()
                .id(paciente.getId())
                .documento(paciente.getDocumento())
                .nombresCompletos((paciente.getNombres() + " " + paciente.getApellidos()).trim())
                .build();
    }
}