package com.piedrazul.backend.reportes.internal.exporter;
import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;

public interface AgendaExporter {
    byte[] export(AgendaDiaDto agenda);
    ExportFormat getExportFormat();
    String getContentType();
}