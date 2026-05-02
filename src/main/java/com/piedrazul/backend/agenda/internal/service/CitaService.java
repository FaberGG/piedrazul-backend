package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.api.AgendaApi;
import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;

import java.time.LocalDate;

/**
 * Contrato interno del servicio de citas (módulo AGENDA).
 *
 * Esta interfaz es PRIVADA al módulo — no debe ser importada
 * por ningún otro módulo. La comunicación externa va por {@link AgendaApi}.
 *
 * Implementación: {@link CitaServiceImpl}
 */
public interface CitaService {

    /**
     * RF1 — Lista la agenda de un médico en una fecha.
     * Incluye citas del día, slots disponibles y porcentaje de ocupación.
     *
     * @param medicoId ID del médico
     * @param fecha    día a consultar
     * @return agenda completa del médico para esa fecha
     * @throws com.piedrazul.backend.shared.exception.ResourceNotFoundException si el médico no existe
     */
    AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha);

    /**
     * RF2 — Crea una cita manual (agendador o médico).
     * Busca o crea el paciente por documento antes de persistir la cita.
     *
     * @param request datos del paciente + datos de la cita
     * @return cita creada con estado PROGRAMADA
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si el slot no está disponible
     * @throws com.piedrazul.backend.shared.exception.ResourceNotFoundException si el médico no existe o está inactivo
     */
    CitaResponse crearCitaManual(CrearCitaManualRequest request);

    /**
     * Obtiene el primer horario disponible para un medico desde una fecha dada.
     */
    PrimerHorarioDisponibleResponse obtenerPrimerHorarioDisponibleMedico(Long medicoId, LocalDate desde);

    /**
     * Obtiene el primer horario disponible global entre medicos activos.
     */
    PrimerHorarioDisponibleResponse obtenerPrimerHorarioDisponibleGlobal(LocalDate desde);

    /**
     * Consulta agregada para panel de agendamiento en frontend.
     */
    AgendaDinamicaResponse obtenerAgendaDinamica(Long medicoId, LocalDate fecha);

    /**
     * Inserta una cita prioritaria de 5 minutos inmediatamente despues de una cita de referencia,
     * recortando citas vecinas cuando sea factible.
     */
    CitaResponse crearCitaPrioritaria(CrearCitaPrioritariaRequest request);

    /**
     * RF3 — Agendamiento autónomo por parte del paciente.
     * El ID del paciente se extrae del SecurityContext (usuario autenticado).
     *
     * @param request datos de la cita solicitada
     * @return cita creada con estado PROGRAMADA
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si ya tiene 3 citas futuras activas
     * @throws com.piedrazul.backend.shared.exception.BusinessRuleException si el slot no está disponible
     */
    CitaResponse agendarAutonomo(AgendarAutonomoRequest request);

    /**
     * Resumen de citas estadistico por estado, calculado con una fecha de inicio y una fin (rango)
     *
     * @param desde fecha de inicio para el rango solicitado
     * @param hasta fecha de fin para el rango solicitado
     * @return conjunto del resumen de las citas en el rango dado con conteos por estado y global
    */
    ResumenCitasDto obtenerResumenCitas(LocalDate desde, LocalDate hasta);

    /**
     * Indíca si un paciente tiene citas programadas activas en una fecha futura a la actual del sistema
     *
     * @param pacienteId identificación del paciente solicitando la confirmación de la cita
     * @return true si tiene al menos una cita futura no cancelada, false de lo contrario.
     */
    boolean tieneCitasFuturas(Long pacienteId);
}



