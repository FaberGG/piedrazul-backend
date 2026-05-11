package com.piedrazul.backend.reportes.internal.exporter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;
import com.piedrazul.backend.agenda.api.dto.CitaDiaDto;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.awt.Color;

@Component
public class PdfAgendaExporter extends AgendaExporter {

    @Override
    public byte[] export(AgendaDiaDto agenda) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        // Header
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        document.add(new Paragraph("Médico: " + agenda.getMedicoNombre(), titleFont));
        document.add(new Paragraph("Especialidad: " + agenda.getEspecialidad()));
        document.add(new Paragraph("Fecha: " + agenda.getFecha()));
        document.add(new Paragraph(" "));

        // Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);

        // Column headers
        for (String header : new String[]{"Hora", "Paciente", "Documento", "Estado", "Observaciones"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Rows
        for (CitaDiaDto cita : agenda.getCitas()) {
            table.addCell(cita.getHora().toString());
            table.addCell(cita.getPacienteNombre());
            table.addCell(cita.getPacienteDocumento());
            table.addCell(cita.getEstado().toString());
            table.addCell(cita.getObservaciones() != null ? cita.getObservaciones() : "");
        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    @Override
    public ExportFormat getExportFormat() { return ExportFormat.PDF; }

    @Override
    public String getContentType() { return "application/pdf"; }
}