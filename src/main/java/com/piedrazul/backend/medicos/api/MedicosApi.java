package com.piedrazul.backend.medicos.api;

import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;

/**
 * Facade publico del modulo de medicos para consultas desde otros modulos.
 */
public interface MedicosApi {

    HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId);
}

