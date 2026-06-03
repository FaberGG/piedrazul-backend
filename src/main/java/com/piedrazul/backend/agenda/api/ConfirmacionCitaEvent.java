package com.piedrazul.backend.agenda.api;

public record ConfirmacionCitaEvent(
        String destinatario,
        String nombrePaciente,
        String fecha,
        String hora,
        String medicoNombre,
        String tipoCita
) {}
