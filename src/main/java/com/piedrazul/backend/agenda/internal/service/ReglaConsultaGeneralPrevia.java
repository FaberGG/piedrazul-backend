package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.TipoCita;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

@Component
public class ReglaConsultaGeneralPrevia implements ReglaCita {

    private final CitaRepository citaRepository;

    public ReglaConsultaGeneralPrevia(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    @Override
    public void validar(ContextoValidacionCita ctx) {
        if (!ctx.tipoCita().requiereConsultaPrevia()) return;
        boolean tiene = citaRepository.existsByPacienteIdAndTipoCitaAndEstado(
                ctx.pacienteId(), TipoCita.CONSULTA_GENERAL, EstadoCita.ATENDIDA);
        if (!tiene) {
            throw new BusinessRuleException(
                    "Debe tener una Consulta General atendida antes de consultar un especialista");
        }
    }
}
