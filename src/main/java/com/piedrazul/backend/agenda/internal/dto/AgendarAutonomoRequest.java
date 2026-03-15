package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de solicitud para agendamiento autónomo por parte del paciente (RF3).
 * El paciente ya está autenticado; su ID se obtiene del SecurityContext.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgendarAutonomoRequest {

    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    private LocalTime hora;

    private String observaciones;
}

