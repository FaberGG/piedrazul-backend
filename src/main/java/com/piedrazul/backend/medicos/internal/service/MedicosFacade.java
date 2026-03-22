package com.piedrazul.backend.medicos.internal.service;

import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.domain.Medico;
import com.piedrazul.backend.medicos.repository.MedicosRepository;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementación del facade público del módulo Médicos.
 *
 * NOTA IMPORTANTE: La entidad Medico NO tiene ConfiguracionMedico en esta rama (dev).
 * Por eso se usa una configuración por defecto estándar de Piedrazul:
 *   - Horario: 07:00 a 12:00
 *   - Días: Lunes a Viernes
 *   - Intervalo: 15 minutos
 *
 * Cuando la rama `agendamiento-manual` sea mergeada a dev y ConfiguracionMedico
 * esté disponible, se debe reemplazar la configuración hardcodeada por la real.
 */
@Service
@Transactional(readOnly = true)
public class MedicosFacade implements MedicosApi {

    private final MedicosRepository medicosRepository;

    public MedicosFacade(MedicosRepository medicosRepository) {
        this.medicosRepository = medicosRepository;
    }

    @Override
    public HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId) {
        Medico medico = medicosRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", medicoId));

        boolean activo = "ACTIVO".equalsIgnoreCase(medico.getEstado());

        // Configuración estándar de Piedrazul (Lunes a Viernes, 07:00-12:00, cada 15 min)
        // REEMPLAZAR cuando se mergee la rama con ConfiguracionMedico
        return HorarioAtencionDTO.builder()
                .horaInicio(LocalTime.of(7, 0))
                .horaFin(LocalTime.of(12, 0))
                .intervaloMinutos(15)
                .diasAtencion(List.of(
                        DayOfWeek.MONDAY,
                        DayOfWeek.TUESDAY,
                        DayOfWeek.WEDNESDAY,
                        DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY
                ))
                .activo(activo)
                .build();
    }
}
