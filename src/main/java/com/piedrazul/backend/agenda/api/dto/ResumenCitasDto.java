package com.piedrazul.backend.agenda.api.dto;

import com.piedrazul.backend.agenda.api.AgendaApi;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de cruce de módulos: lo que el módulo AGENDA expone
 * al módulo REPORTES a través de {@link AgendaApi}.
 *
 * Este objeto NO es una entidad de dominio. Es un contrato de datos
 * intencionalmente simple para no acoplar los módulos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCitasDto {

    private LocalDate desde;
    private LocalDate hasta;
    private long totalCitas;
    private long citasProgramadas;
    private long citasAtendidas;
    private long citasCanceladas;
    private long citasInasistencias;
    /** Porcentaje de slots ocupados respecto al total disponible en el rango. */
    private double porcentajeOcupacion;
}

