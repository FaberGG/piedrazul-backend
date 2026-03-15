package com.piedrazul.backend.medicos.api.dto;

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
public class HorarioAtencionDTO {
    LocalTime horaInicio;
    LocalTime horaFin;
    int intervaloMinutos;
    List<DayOfWeek> diasAtencion;
    boolean activo;
}
