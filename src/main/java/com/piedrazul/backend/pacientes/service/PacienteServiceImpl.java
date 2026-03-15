package com.piedrazul.backend.pacientes.service;

import com.piedrazul.backend.auth.domain.Usuario;
import com.piedrazul.backend.pacientes.domain.Paciente;
import com.piedrazul.backend.pacientes.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.port.PacienteService;
import com.piedrazul.backend.pacientes.repository.PacientesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PacienteServiceImpl implements PacienteService {

    private final PacientesRepository pacienteRepository;

    @Override
    public void crearPaciente(Usuario usuario, String documento, String nombres,
                              String apellidos, String celular, String correo,
                              LocalDate fechaNacimiento, String genero) {
        Paciente paciente = Paciente.builder()
                .usuario(usuario)
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
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));
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
}