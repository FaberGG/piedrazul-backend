package com.piedrazul.backend.agenda.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Contrato interno del servicio de disponibilidad horaria (módulo AGENDA).
 * Calcula slots libres/ocupados a partir de {@code ConfiguracionMedico}.
 *
 * Implementación: {@link DisponibilidadServiceImpl}
 */
public interface DisponibilidadService {

    /**
     * Calcula todos los horarios disponibles de un médico en una fecha.
     * Un slot está disponible si: existe en la configuración del médico
     * Y no hay una cita activa (no CANCELADA) en ese horario.
     *
     * @param medicoId ID del médico
     * @param fecha    fecha a consultar
     * @return lista de horas disponibles ordenada ascendentemente; vacía si no hay slots
     * @throws com.piedrazul.backend.shared.exception.ResourceNotFoundException si el médico no tiene configuración
     */
    List<LocalTime> calcularHorariosDisponibles(Long medicoId, LocalDate fecha);

    /**
     * Verifica si un slot específico está disponible para un médico.
     * Versión optimizada de {@link #calcularHorariosDisponibles} para una sola hora.
     *
     * @param medicoId ID del médico
     * @param fecha    fecha de la cita
     * @param hora     hora de la cita
     * @return {@code true} si el slot está libre
     */
    boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora);
}


