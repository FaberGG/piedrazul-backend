package com.piedrazul.backend.agenda.api;

import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;
import com.piedrazul.backend.agenda.api.dto.HistorialPacienteDto;
import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;

import java.time.LocalDate;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *  INTERFAZ PÚBLICA DEL MÓDULO AGENDA
 * ══════════════════════════════════════════════════════════════
 *
 * Punto de entrada ÚNICO para cualquier módulo externo que necesite
 * datos del módulo Agenda.
 *
 * REGLA: Otros módulos SOLO inyectan AgendaApi.
 *        Está PROHIBIDO importar agenda.domain, agenda.repository
 *        o agenda.service desde fuera de este módulo.
 *
 * Analogía REST:
 *   Microservicios  → HTTP endpoint en AgendaController
 *   Monolito modular → este método Java (in-process call)
 *
 * @see AgendaFacade
 */
public interface AgendaApi {

    /**
     * Resumen estadístico de citas en un rango de fechas.
     * Consumido por el módulo {@code reportes} para reportes analíticos.
     * El llamador recibe únicamente un DTO, nunca entidades de dominio.
     *
     * @param desde fecha inicio del rango (inclusive)
     * @param hasta fecha fin del rango (inclusive)
     * @return resumen con totales por estado y porcentaje de ocupación
     */
    ResumenCitasDto obtenerResumenCitas(LocalDate desde, LocalDate hasta);

    /**
     * Verifica si un paciente tiene citas futuras activas.
     *
     * @param pacienteId ID del paciente
     * @return {@code true} si tiene al menos una cita futura no cancelada
     */
    boolean tieneCitasFuturas(Long pacienteId);

    /**
     * Entrega la data necesaria para las citas de un médico por dia.
     *
     * @param dia fecha selecionada para las citas programadas.
     * @param medicoId medico para filtrar las citas.
     * @return información condensada para la lista del medico por dia.
     */
    AgendaDiaDto obtenerAgendaDia(LocalDate dia, Long medicoId);

    /**
     * Historial completo de citas de un paciente, ordenado por fecha descendente.
     * Consumido por el módulo {@code reportes} para generar el PDF de historial.
     *
     * @param pacienteId ID del paciente
     * @return datos del paciente + lista de todas sus citas
     */
    HistorialPacienteDto obtenerHistorialPaciente(Long pacienteId);

    /**
     * Agenda completa del día — todas las citas de todos los médicos para {@code dia},
     * agrupadas por médico y ordenadas por hora.
     *
     * @param dia fecha a consultar
     * @return lista de agendas por médico (puede estar vacía si no hay citas)
     */
    List<AgendaDiaDto> obtenerAgendaDiaCompleta(LocalDate dia);
}

