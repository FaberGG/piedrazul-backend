package com.piedrazul.backend.agenda.service;

import com.piedrazul.backend.agenda.dto.AgendaResponse;
import com.piedrazul.backend.agenda.dto.CitaResponse;
import com.piedrazul.backend.agenda.dto.CrearCitaManualRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Servicio principal de gestión de citas médicas.
 * Reglas de negocio: validación de disponibilidad, límites de citas, concurrencia.
 */
@Service
public class CitaService {

    /**
     * Lista la agenda de un médico en una fecha determinada (RF1).
     */
    public AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha) {
        // TODO: consultar citas, calcular slots, porcentaje de ocupación
        return null;
    }

    /**
     * Crea una cita manual para un paciente (RF2).
     */
    public CitaResponse crearCitaManual(CrearCitaManualRequest request) {
        // TODO: buscar/crear paciente, validar disponibilidad, persistir cita
        return null;
    }

    /**
     * Agendamiento autónomo por parte del paciente (RF3).
     */
    public CitaResponse agendarAutonomo(Long pacienteId, Long medicoId, LocalDate fecha, String hora) {
        // TODO: validar límite de 3 citas futuras, disponibilidad, concurrencia
        return null;
    }
}

