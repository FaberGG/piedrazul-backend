package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCambiosCitaResponse {

    private Long id;
    private LocalDate fechaAnterior;
    private LocalTime horaAnterior;
    private Long medicoAnteriorId;
    private LocalDate fechaNueva;
    private LocalTime horaNueva;
    private Long medicoNuevoId;
    private String motivo;
    private UUID modificadoPor;
    private LocalDateTime creadoEn;
}
