package com.piedrazul.backend.medicos.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionAgendaMedicoResponse {

    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private boolean activo;
    private List<DayOfWeek> diasAtencion;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Integer intervaloMinutos;
    private Integer capacidadDiaria;
}

