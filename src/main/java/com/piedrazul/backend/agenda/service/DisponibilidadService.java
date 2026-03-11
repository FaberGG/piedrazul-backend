package com.piedrazul.backend.agenda.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Servicio para calcular disponibilidad de horarios de un médico.
 * Usa ConfiguracionMedico para determinar slots libres/ocupados.
 */
@Service
public class DisponibilidadService {

    /**
     * Calcula los horarios disponibles de un médico en una fecha.
     *
     * @param medicoId ID del médico
     * @param fecha    fecha a consultar
     * @return lista de horarios disponibles
     */
    public List<LocalTime> calcularHorariosDisponibles(Long medicoId, LocalDate fecha) {
        // TODO: obtener configuración del médico, generar slots, filtrar ocupados
        return List.of();
    }

    /**
     * Verifica si un horario específico está disponible para un médico.
     */
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        // TODO: verificar contra citas existentes no canceladas
        return false;
    }
}

