package com.piedrazul.backend.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO de respuesta para la agenda de un médico en una fecha (RF1).
 * Incluye lista de citas, horarios disponibles y porcentaje de ocupación.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaResponse {

    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private LocalDate fecha;
    private List<CitaResponse> citas;
    private List<String> horariosDisponibles;
    private int totalSlots;
    private int slotsOcupados;
    private double porcentajeOcupacion;
}

