package com.piedrazul.backend.reportes.internal.service;

import com.piedrazul.backend.agenda.api.AgendaApi;
import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;
import com.piedrazul.backend.agenda.api.dto.HistorialPacienteDto;
import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.reportes.internal.dto.ReporteCitasResponse;
import com.piedrazul.backend.reportes.internal.exporter.AgendaDiaCompletaPdfExporter;
import com.piedrazul.backend.reportes.internal.exporter.AgendaExporter;
import com.piedrazul.backend.reportes.internal.exporter.AgendaExporterFactory;
import com.piedrazul.backend.reportes.internal.exporter.ExportFormat;
import com.piedrazul.backend.reportes.internal.exporter.HistorialPacientePdfExporter;
import com.piedrazul.backend.reportes.internal.dto.ExportResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import java.time.LocalDate;

/**
 * Servicio de reportes y consultas analíticas.
 * Solo lectura — no modifica estado del sistema.
 *
 * PATRÓN MODULAR:
 *  Este servicio SOLO se comunica con el módulo AGENDA a través de
 *  {@link AgendaApi}. Está PROHIBIDO inyectar CitaRepository,
 *  CitaService ni ninguna otra clase interna del módulo agenda.
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    /**
     * Contrato público del módulo AGENDA — única dependencia permitida hacia él.
     * Ver {@link AgendaApi}.
     */
    private final AgendaApi agendaApi;
    private final AgendaExporterFactory agendaExporterFactory;
    private final HistorialPacientePdfExporter historialPacientePdfExporter;
    private final AgendaDiaCompletaPdfExporter agendaDiaCompletaPdfExporter;

    /**
     * IntelliJ puede mostrar "Could not autowire" aquí porque
     * {@code AgendaFacade} (la implementación {@code @Service}) está
     * en {@code agenda.service}, un sub-paquete privado del módulo.
     * En RUNTIME Spring resuelve el bean correctamente.
     * La arquitectura está verificada por {@code ModularityTest}.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ReporteService(AgendaApi agendaApi, AgendaExporterFactory agendaExporterFactory,
                          HistorialPacientePdfExporter historialPacientePdfExporter,
                          AgendaDiaCompletaPdfExporter agendaDiaCompletaPdfExporter) {
        this.agendaApi = agendaApi;
        this.agendaExporterFactory = agendaExporterFactory;
        this.historialPacientePdfExporter = historialPacientePdfExporter;
        this.agendaDiaCompletaPdfExporter = agendaDiaCompletaPdfExporter;
    }

    /**
     * Genera reporte de citas en un rango de fechas.
     * Obtiene los datos a través de AgendaApi y los mapea al DTO de presentación.
     */
    public ReporteCitasResponse generarReporteCitas(LocalDate desde, LocalDate hasta) {
        ResumenCitasDto resumen = agendaApi.obtenerResumenCitas(desde, hasta);

        // TODO: mapear resumen → ReporteCitasResponse (puede usar MapStruct)
        return ReporteCitasResponse.builder()
                .desde(resumen.getDesde())
                .hasta(resumen.getHasta())
                .totalCitas(resumen.getTotalCitas())
                .citasAtendidas(resumen.getCitasAtendidas())
                .citasCanceladas(resumen.getCitasCanceladas())
                .citasProgramadas(resumen.getCitasProgramadas())
                .porcentajeOcupacion(resumen.getPorcentajeOcupacion())
                .build();
    }

    /**
     * Genera el binario del reporte diario con patron strategy.
     *
     * @param dia es la fecha selecionada para el reporte.
     * @param medicoId es el medico para filtrar el reporte.
     * @param exportFormat es el tipo the formato que se usará determinado por factory,
     * según el tipo de archivo solicitado.
     * @return reporte segun medico y fecha en binario especifico.
     */
    public ExportResult generarReporteDiario(LocalDate dia, Long medicoId, ExportFormat exportFormat) {
        AgendaDiaDto agendaDiaDto = agendaApi.obtenerAgendaDia(dia, medicoId);
        AgendaExporter agendaExporter = agendaExporterFactory.getAgendaExporter(exportFormat);
        return new ExportResult(agendaExporter.export(agendaDiaDto), agendaExporter.getContentType());
    }

    public ExportResult generarHistorialPaciente(Long pacienteId) {
        HistorialPacienteDto historial = agendaApi.obtenerHistorialPaciente(pacienteId);
        return new ExportResult(historialPacientePdfExporter.export(historial), "application/pdf");
    }

    public ExportResult generarAgendaDiaCompleta(LocalDate dia) {
        List<AgendaDiaDto> agendas = agendaApi.obtenerAgendaDiaCompleta(dia);
        return new ExportResult(agendaDiaCompletaPdfExporter.export(dia, agendas), "application/pdf");
    }
}

