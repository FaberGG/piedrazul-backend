package com.piedrazul.backend.reportes.internal.exporter;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.piedrazul.backend.agenda.api.dto.AgendaDiaDto;
import com.piedrazul.backend.agenda.api.dto.CitaDiaDto;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

@Component
public class AgendaDiaCompletaPdfExporter {

    public byte[] export(LocalDate dia, List<AgendaDiaDto> agendas) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont   = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font sectionFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font infoFont    = new Font(Font.HELVETICA, 10, Font.NORMAL);
        Font headerFont  = new Font(Font.HELVETICA, 9,  Font.BOLD);

        document.add(new Paragraph("Agenda Completa del Día", titleFont));
        document.add(new Paragraph("Fecha: " + dia, infoFont));
        document.add(new Paragraph(" "));

        if (agendas.isEmpty()) {
            document.add(new Paragraph("No hay citas programadas para este día.", infoFont));
            document.close();
            return out.toByteArray();
        }

        for (AgendaDiaDto agenda : agendas) {
            // Doctor section header
            Paragraph sectionTitle = new Paragraph(
                    agenda.getMedicoNombre() + " — " + agenda.getEspecialidad(), sectionFont);
            sectionTitle.setSpacingBefore(10);
            document.add(sectionTitle);

            if (agenda.getCitas() == null || agenda.getCitas().isEmpty()) {
                document.add(new Paragraph("  Sin citas para este día.", infoFont));
                document.add(new Paragraph(" "));
                continue;
            }

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            try {
                table.setWidths(new float[]{2f, 3.5f, 2.5f, 2.5f, 4f});
            } catch (DocumentException ignored) { }
            table.setSpacingBefore(4);
            table.setSpacingAfter(8);

            for (String header : new String[]{"Hora", "Paciente", "Documento", "Estado", "Observaciones"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(Color.LIGHT_GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            for (CitaDiaDto cita : agenda.getCitas()) {
                table.addCell(cita.getHora() != null ? cita.getHora().toString() : "");
                table.addCell(cita.getPacienteNombre() != null ? cita.getPacienteNombre() : "");
                table.addCell(cita.getPacienteDocumento() != null ? cita.getPacienteDocumento() : "");
                table.addCell(cita.getEstado() != null ? cita.getEstado() : "");
                table.addCell(cita.getObservaciones() != null ? cita.getObservaciones() : "");
            }

            document.add(table);
        }

        document.close();
        return out.toByteArray();
    }
}
