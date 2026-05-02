package com.piedrazul.backend.agenda.api;

import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.agenda.internal.service.CitaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementación de la API pública del módulo AGENDA.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │  RESPONSABILIDAD DE ESTA CLASE                              │
 * │                                                             │
 * │  1. Implementar AgendaApi (contrato público del módulo).    │
 * │  2. Traducir datos INTERNOS (entidades JPA) a DTOs          │
 * │     PÚBLICOS antes de cruzar la frontera del módulo.        │
 * │  3. Delegar lógica compleja a CitaService/Disponibilidad.   │
 * │  4. NUNCA devolver entidades de dominio (Cita, Medico...).  │
 * └─────────────────────────────────────────────────────────────┘
 *
 * PATRÓN: Walking Skeleton — cada método compila y retorna un valor
 * seguro (no null). Los TODO marcan exactamente lo que implementar.
 */
@Service
@Transactional(readOnly = true)
public class AgendaFacade implements AgendaApi {

    private final CitaService citaService;

    public AgendaFacade(CitaService citaService) {
        this.citaService = citaService;
    }

    // ─────────────────────────────────────────────────────────────
    // AgendaApi — implementación // llamado a la capa de servicios
    // ─────────────────────────────────────────────────────────────

    @Override
    public ResumenCitasDto obtenerResumenCitas(LocalDate desde, LocalDate hasta) {
       return  citaService.obtenerResumenCitas(desde, hasta);
    }

    @Override
    public boolean tieneCitasFuturas(Long pacienteId) {
        return citaService.tieneCitasFuturas(pacienteId);
    }
}