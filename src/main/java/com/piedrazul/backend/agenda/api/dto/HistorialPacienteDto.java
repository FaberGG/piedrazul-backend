package com.piedrazul.backend.agenda.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialPacienteDto {
    private Long pacienteId;
    private String nombreCompleto;
    private String documento;
    private List<CitaHistorialItemDto> citas;
}
