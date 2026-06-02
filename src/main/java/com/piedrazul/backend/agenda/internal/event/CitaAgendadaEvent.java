package com.piedrazul.backend.agenda.internal.event;
import java.time.LocalDateTime;

public record CitaAgendadaEvent(
    String pacienteId,
    String phone,       // puede ser null
    String email,       // puede ser null
    String medicoNombre,
    LocalDateTime fecha
) {}
