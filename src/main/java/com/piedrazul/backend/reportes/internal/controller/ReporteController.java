package com.piedrazul.backend.reportes.internal.controller;

import com.piedrazul.backend.reportes.internal.dto.ReporteCitasResponse;
import com.piedrazul.backend.reportes.internal.service.ReporteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller de reportes y estadísticas.
 * Acceso: AGENDADOR, ADMINISTRADOR.
 */
@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping("/citas")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'ADMIN')")
    public ResponseEntity<ReporteCitasResponse> reporteCitas(
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta) {
        // TODO: delegar al servicio
        return ResponseEntity.ok(reporteService.generarReporteCitas(desde, hasta));
    }
}

