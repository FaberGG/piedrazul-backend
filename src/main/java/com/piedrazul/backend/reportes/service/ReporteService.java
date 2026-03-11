package com.piedrazul.backend.reportes.service;

import com.piedrazul.backend.reportes.dto.ReporteCitasResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Servicio de reportes y consultas analíticas.
 * Solo lectura — no modifica estado del sistema.
 */
@Service
public class ReporteService {

    /**
     * Genera reporte de citas en un rango de fechas.
     */
    public ReporteCitasResponse generarReporteCitas(LocalDate desde, LocalDate hasta) {
        // TODO: consultar datos agregados de citas
        return null;
    }
}

