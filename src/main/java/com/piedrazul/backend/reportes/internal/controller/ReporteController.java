package com.piedrazul.backend.reportes.internal.controller;

import com.piedrazul.backend.reportes.internal.dto.ExportResult;
import com.piedrazul.backend.reportes.internal.dto.ReporteCitasResponse;
import com.piedrazul.backend.reportes.internal.exporter.ExportFormat;
import com.piedrazul.backend.reportes.internal.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

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
        return ResponseEntity.ok(reporteService.generarReporteCitas(desde, hasta));
    }

    @GetMapping("/citas/reporteDiario")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'ADMIN', 'MEDICO', 'TERAPISTA')")
    public ResponseEntity<byte[]> reporteDiario(
            @RequestParam LocalDate dia,
            @RequestParam Long medicoId,
            @RequestParam String exportFormat) {

        ExportFormat format = ExportFormat.valueOf(exportFormat.toUpperCase());
        ExportResult exportResult = reporteService.generarReporteDiario(dia, medicoId, format);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment; filename=agenda_" + dia + "." + exportFormat.toLowerCase());
        headers.set("Content-Type", exportResult.getContentType());

        return ResponseEntity.ok().headers(headers).body(exportResult.getData());
    }

    @GetMapping("/formatos")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'ADMIN', 'MEDICO', 'TERAPISTA')")
    public ResponseEntity<List<ExportFormat>> getExportFormatos(){
        return ResponseEntity.ok(Arrays.asList(ExportFormat.values()));
    }

    @GetMapping("/historial-paciente")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'TERAPISTA')")
    public ResponseEntity<byte[]> historialPaciente(@RequestParam Long pacienteId) {
        ExportResult result = reporteService.generarHistorialPaciente(pacienteId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment; filename=historial-paciente-" + pacienteId + ".pdf");
        headers.set("Content-Type", result.getContentType());
        return ResponseEntity.ok().headers(headers).body(result.getData());
    }

    @GetMapping("/agenda-dia-completa")
    @PreAuthorize("hasAnyRole('ADMIN', 'MEDICO', 'TERAPISTA', 'AGENDADOR')")
    public ResponseEntity<byte[]> agendaDiaCompleta(@RequestParam LocalDate dia) {
        ExportResult result = reporteService.generarAgendaDiaCompleta(dia);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Disposition", "attachment; filename=agenda-completa-" + dia + ".pdf");
        headers.set("Content-Type", result.getContentType());
        return ResponseEntity.ok().headers(headers).body(result.getData());
    }
}