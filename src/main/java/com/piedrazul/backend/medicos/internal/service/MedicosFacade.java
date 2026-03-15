package com.piedrazul.backend.medicos.internal.service;

import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementacion minima del facade de medicos para habilitar inyeccion de MedicosApi.
 */
@Service
@Transactional(readOnly = true)
public class MedicosFacade implements MedicosApi {

    @Override
    public HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId) {
        // Skeleton temporal: por defecto el medico queda sin horario activo.
        return HorarioAtencionDTO.builder()
                .activo(false)
                .build();
    }
}

