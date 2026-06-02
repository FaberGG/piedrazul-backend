package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.TipoCita;

public record ContextoValidacionCita(
        Long pacienteId,
        TipoCita tipoCita,
        Long medicoId
) {}
