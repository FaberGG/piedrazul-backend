package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de respuesta para recomendar el primer hueco disponible de agenda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrimerHorarioDisponibleResponse {

    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private LocalDate fecha;
    private LocalTime hora;
    private Integer intervaloMinutos;
}

