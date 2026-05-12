package com.piedrazul.backend.reportes.internal.exporter;

import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;
import com.piedrazul.backend.agenda.api.dto.CitaDiaDto;
import org.springframework.stereotype.Component;

@Component
public class CsvAgendaExporter implements AgendaExporter {

    @Override
    public byte[] export(AgendaDiaDto agenda) {

        StringBuilder csv = new StringBuilder();

    // Header
        csv.append("Medico,Especialidad,Fecha\n");
        csv.append(agenda.getMedicoNombre()).append(",")
                .append(agenda.getEspecialidad()).append(",")
                .append(agenda.getFecha()).append("\n");

    // Empty line
        csv.append("\n");

    // Column headers
        csv.append("Hora,Paciente,Documento,Estado,Observaciones\n");

    // Rows
        for (CitaDiaDto cita : agenda.getCitas()) {
            csv.append(cita.getHora()).append(",")
                    .append(cita.getPacienteNombre()).append(",")
                    .append(cita.getPacienteDocumento()).append(",")
                    .append(cita.getEstado()).append(",")
                    .append(cita.getObservaciones()).append("\n");
        }

        return csv.toString().getBytes();
    }

    @Override
    public ExportFormat getExportFormat() {return ExportFormat.CSV;}

    @Override
    public String getContentType() {return "text/csv";}
}
