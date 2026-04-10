package com.piedrazul.backend.pacientes.internal.service;

import com.piedrazul.backend.pacientes.internal.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.internal.dto.PacienteSugerenciaResponse;

import java.util.List;

public interface PacienteService {


    List<PacienteResponse> listarTodos();

    PacienteResponse buscarPorId(Long id);

    List<PacienteSugerenciaResponse> buscarPorDocumentoPrefijo(String documento, int limit);
}