package com.piedrazul.backend.agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta para una cita individual.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaResponse {

    private Long id;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String medicoNombre;
    private String especialidad;
    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private String observaciones;
}

