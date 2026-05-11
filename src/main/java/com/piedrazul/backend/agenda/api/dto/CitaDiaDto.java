package com.piedrazul.backend.agenda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaDiaDto {
    private String pacienteNombre;
    private String pacienteDocumento;
    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private String observaciones;
}