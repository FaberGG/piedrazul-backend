package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaRequest;
import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralRequest;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralResponse;

import java.util.List;

public interface ConfiguracionAgendaService {
    ConfiguracionAgendaResponse obtenerConfiguracion();
    ConfiguracionAgendaResponse actualizarVentana(ConfiguracionAgendaRequest request);
    DiaNoLaboralResponse agregarDiaNoLaboral(DiaNoLaboralRequest request);
    void eliminarDiaNoLaboral(Long id);
    List<DiaNoLaboralResponse> listarDiasNoLaborales();
}