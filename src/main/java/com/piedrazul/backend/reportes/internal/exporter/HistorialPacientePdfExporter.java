package com.piedrazul.backend.reportes.internal.exporter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.piedrazul.backend.agenda.api.dto.CitaHistorialItemDto;
import com.piedrazul.backend.agenda.api.dto.HistorialPacienteDto;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

@Component
public class HistorialPacientePdfExporter {

    public byte[] export(HistorialPacienteDto historial) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font infoFont = new Font(Font.HELVETICA, 11, Font.NORMAL);

        document.add(new Paragraph("Historial de Citas del Paciente", titleFont));
        document.add(new Paragraph("Paciente: " + historial.getNombreCompleto(), infoFont));
        document.add(new Paragraph("Documento: " + historial.getDocumento(), infoFont));
        document.add(new Paragraph(" "));

        if (historial.getCitas() == null || historial.getCitas().isEmpty()) {
            document.add(new Paragraph("Sin citas registradas.", infoFont));
            document.close();
            return out.toByteArray();
        }

        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        try {
            table.setWidths(new float[]{2f, 2f, 3f, 2.5f, 3.5f, 3f, 4f});
        } catch (DocumentException ignored) { }

        Font headerFont = new Font(Font.HELVETICA, 9, Font.BOLD);
        for (String header : new String[]{"Fecha", "Hora", "Tipo Cita", "Estado", "Médico", "Especialidad", "Observaciones"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (CitaHistorialItemDto cita : historial.getCitas()) {
            table.addCell(cita.getFecha() != null ? cita.getFecha().toString() : "");
            table.addCell(cita.getHora() != null ? cita.getHora().toString() : "");
            table.addCell(cita.getTipoCita() != null ? cita.getTipoCita() : "");
            table.addCell(cita.getEstado() != null ? cita.getEstado() : "");
            table.addCell(cita.getMedicoNombre() != null ? cita.getMedicoNombre() : "");
            table.addCell(cita.getEspecialidad() != null ? cita.getEspecialidad() : "");
            table.addCell(cita.getObservaciones() != null ? cita.getObservaciones() : "");
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
}
