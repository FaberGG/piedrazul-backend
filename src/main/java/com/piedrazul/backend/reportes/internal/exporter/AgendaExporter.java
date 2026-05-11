package com.piedrazul.backend.reportes.internal.exporter;
import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;

public abstract class AgendaExporter {
    public abstract byte[] export(AgendaDiaDto agenda);
    public abstract ExportFormat getExportFormat();
    public abstract String getContentType();
}