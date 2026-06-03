package com.piedrazul.backend.agenda.internal.event;

public record ConfirmacionCitaEvent(
        String destinatario,
        String nombrePaciente,
        String fecha,
        String hora,
        String medicoNombre,
        String tipoCita
) {}
