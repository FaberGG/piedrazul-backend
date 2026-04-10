package com.piedrazul.backend.medicos.api;

import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.medicos.api.dto.RegistroMedicoDTO;

import java.util.List;

/**
 * Facade publico del modulo de medicos para consultas desde otros modulos.
 */
public interface MedicosApi {

    void registrarMedicoConUsuario(RegistroMedicoDTO request);

    HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId);

    MedicoResumenDTO obtenerResumenMedico(Long medicoId);

    List<MedicoResumenDTO> listarMedicosActivos();
}

