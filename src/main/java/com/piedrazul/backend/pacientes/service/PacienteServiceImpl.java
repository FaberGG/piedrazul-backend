package com.piedrazul.backend.pacientes.service;

import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.pacientes.domain.Paciente;
import com.piedrazul.backend.pacientes.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.dto.PacienteSugerenciaResponse;
import com.piedrazul.backend.pacientes.port.PacienteService;
import com.piedrazul.backend.pacientes.repository.PacientesRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private static final int LIMITE_DEFAULT_SUGERENCIAS = 5;
    private static final int LIMITE_MAXIMO_SUGERENCIAS = 10;
    private static final int MIN_CARACTERES_BUSQUEDA = 2;

    private final PacientesRepository pacienteRepository;
    private final AuthApi authApi;

    @Override
    public void crearPaciente(Long usuarioId, String documento, String nombres,
                              String apellidos, String celular, String correo,
                              LocalDate fechaNacimiento, String genero) {

        if (usuarioId != null && !authApi.existeUsuarioActivo(usuarioId)) {
            throw new IllegalArgumentException(
                    "El usuario con id " + usuarioId + " no existe o no está activo"
            );
        }

        Paciente paciente = Paciente.builder()
                .usuarioId(usuarioId)
                .documento(documento)
                .nombres(nombres)
                .apellidos(apellidos)
                .celular(celular)
                .correo(correo)
                .fechaNacimiento(fechaNacimiento)
                .genero(genero)
                .build();

        pacienteRepository.save(paciente);
    }

    // ── sin cambios desde aquí ────────────────────────────────────

    @Override
    public boolean existePorDocumento(String documento) {
        return pacienteRepository.existsByDocumento(documento);
    }

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