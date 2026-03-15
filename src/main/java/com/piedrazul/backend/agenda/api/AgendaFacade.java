package com.piedrazul.backend.agenda.api;

import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

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

    private final CitaRepository citaRepository;

    public AgendaFacade(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // AgendaApi — implementación
    // ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR:
     *  1. Llamar a citaRepository.countByEstadoBetweenFechas(desde, hasta)
     *     que devuelve List<Object[]> con [estado, count] por fila.
     *  2. Iterar el resultado y asignar cada count a su campo del DTO.
     *  3. Calcular porcentajeOcupacion:
     *       totalSlots = suma de médicos activos × días del rango × slotsPerDía
     *       porcentaje = (totalCitas / totalSlots) * 100
     *     (simplificación aceptable para Sprint 1: usar totalCitas / totalAtendidas)
     */
    @Override
    public ResumenCitasDto obtenerResumenCitas(LocalDate desde, LocalDate hasta) {
        // TODO: consultar citaRepository.countByEstadoBetweenFechas(desde, hasta)
        //       y poblar los campos del DTO con los conteos reales.

        List<Object[]> conteos = citaRepository.countByEstadoBetweenFechas(desde, hasta);

        long programadas  = 0;
        long confirmadas  = 0;
        long atendidas    = 0;
        long canceladas   = 0;

        for (Object[] fila : conteos) {
            String estado = (String) fila[0];
            long   count  = (Long)   fila[1];
            switch (estado) {
                case "PROGRAMADA"  -> programadas = count;
                case "CONFIRMADA"  -> confirmadas  = count;
                case "ATENDIDA"    -> atendidas    = count;
                case "CANCELADA"   -> canceladas   = count;
            }
        }

        long total = programadas + confirmadas + atendidas + canceladas;

        // TODO Sprint 2: calcular porcentaje real contra slots disponibles de cada médico
        double porcentaje = 0.0;

        return ResumenCitasDto.builder()
                .desde(desde)
                .hasta(hasta)
                .totalCitas(total)
                .citasProgramadas(programadas)
                .citasConfirmadas(confirmadas)
                .citasAtendidas(atendidas)
                .citasCanceladas(canceladas)
                .porcentajeOcupacion(porcentaje)
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * IMPLEMENTAR: ya usa el método correcto del repositorio.
     * Verificar que el umbral "CANCELADA" sea consistente con los
     * estados usados en Cita.estado (ver EstadoCita si se crea un enum).
     */
    @Override
    public boolean tieneCitasFuturas(Long pacienteId) {
        return citaRepository
                .countByPacienteIdAndEstadoNotAndFechaGreaterThanEqual(
                        pacienteId, "CANCELADA", LocalDate.now()) > 0;
    }
}

