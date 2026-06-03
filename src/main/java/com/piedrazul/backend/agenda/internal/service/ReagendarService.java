package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.HistorialCambiosCitaResponse;
import com.piedrazul.backend.agenda.internal.dto.ReagendarCitaRequest;

import java.util.List;

interface ReagendarService {
    CitaResponse reagendar(Long citaId, ReagendarCitaRequest request);
    List<HistorialCambiosCitaResponse> obtenerHistorial(Long citaId);
}
