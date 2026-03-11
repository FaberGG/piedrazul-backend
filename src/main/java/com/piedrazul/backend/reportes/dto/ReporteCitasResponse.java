package com.piedrazul.backend.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta para reporte de citas.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReporteCitasResponse {

    private LocalDate desde;
    private LocalDate hasta;
    private long totalCitas;
    private long citasAtendidas;
    private long citasCanceladas;
    private long citasProgramadas;
    private double porcentajeOcupacion;
}

