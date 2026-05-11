package com.piedrazul.backend.reportes.internal.exporter;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgendaExporterFactory {

    private final Map<ExportFormat, AgendaExporter> exporters;

    AgendaExporterFactory(List<AgendaExporter> exporterList){
        this.exporters = new HashMap<>();
        for(AgendaExporter exporter : exporterList){
            exporters.put(exporter.getExportFormat(), exporter);
        }
    }

    public AgendaExporter getAgendaExporter(ExportFormat exportFormat){
        return exporters.get(exportFormat);
    }
}