package com.piedrazul.backend.agenda.internal.event;

import java.time.LocalDate;

/**
 * Internal event fired when the appointment list for a doctor/day changes.
 */
public record AgendaDinamicaChangedEvent(
        Long medicoId,
        LocalDate fecha,
        Long citaId,
        String accion
) {
}

