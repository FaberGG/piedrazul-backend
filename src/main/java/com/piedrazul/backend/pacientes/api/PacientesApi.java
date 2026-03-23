package com.piedrazul.backend.pacientes.api;

import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.RegistroPacienteDTO;

/**
 * Contrato publico del modulo de pacientes para consumo entre modulos.
 */
public interface PacientesApi {

    PacienteResumenDTO obtenerOCrearPorDocumento(RegistroPacienteDTO request);

    PacienteResumenDTO obtenerResumenPorId(Long pacienteId);

    PacienteResumenDTO buscarPorUsuarioId(Long usuarioId);
}

