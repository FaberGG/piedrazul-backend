package com.piedrazul.backend.pacientes.port;

import com.piedrazul.backend.auth.domain.Usuario;
import com.piedrazul.backend.pacientes.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.dto.PacienteSugerenciaResponse;
import org.springframework.modulith.NamedInterface;

import java.time.LocalDate;
import java.util.List;

@NamedInterface
public interface PacienteService {

    void crearPaciente(Usuario usuario, String documento, String nombres,
                       String apellidos, String celular, String correo,
                       LocalDate fechaNacimiento, String genero);

    boolean existePorDocumento(String documento);

    List<PacienteResponse> listarTodos();

    PacienteResponse buscarPorId(Long id);

    List<PacienteSugerenciaResponse> buscarPorDocumentoPrefijo(String documento, int limit);
}