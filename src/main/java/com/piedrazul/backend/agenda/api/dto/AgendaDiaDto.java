package com.piedrazul.backend.agenda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaDiaDto {
    private Long medicoId;
    private String medicoNombre;
    private String especialidad;
    private LocalDate fecha;
    private List<CitaDiaDto> citas;
}