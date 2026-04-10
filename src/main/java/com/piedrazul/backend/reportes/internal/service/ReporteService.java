package com.piedrazul.backend.reportes.internal.service;

import com.piedrazul.backend.agenda.api.AgendaApi;
import com.piedrazul.backend.agenda.api.dto.ResumenCitasDto;
import com.piedrazul.backend.reportes.internal.dto.ReporteCitasResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * IntelliJ puede mostrar "Could not autowire" aquí porque
     * {@code AgendaFacade} (la implementación {@code @Service}) está
     * en {@code agenda.service}, un sub-paquete privado del módulo.
     * En RUNTIME Spring resuelve el bean correctamente.
     * La arquitectura está verificada por {@code ModularityTest}.
     */
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public ReporteService(AgendaApi agendaApi) {
        this.agendaApi = agendaApi;
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
}

