package com.piedrazul.backend.agenda.api;

import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;

import java.time.LocalDate;

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
}

