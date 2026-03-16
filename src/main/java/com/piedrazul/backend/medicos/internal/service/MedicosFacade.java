package com.piedrazul.backend.medicos.internal.service;

import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.medicos.domain.Medico;
import com.piedrazul.backend.medicos.repository.MedicosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementacion minima del facade de medicos para habilitar inyeccion de MedicosApi.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MedicosFacade implements MedicosApi {

    private final MedicosRepository medicosRepository;

    @Override
    public HorarioAtencionDTO obtenerHorarioAtencion(Long medicoId) {
        return medicosRepository.findById(medicoId)
                .map(this::toHorarioAtencion)
                .orElse(null);
    }

    @Override
    public MedicoResumenDTO obtenerResumenMedico(Long medicoId) {
        return medicosRepository.findById(medicoId)
                .map(this::toResumen)
                .orElse(null);
    }

    @Override
    public List<MedicoResumenDTO> listarMedicosActivos() {
        return medicosRepository.findByEstadoIgnoreCase("ACTIVO")
                .stream()
                .map(this::toResumen)
                .toList();
    }

    private HorarioAtencionDTO toHorarioAtencion(Medico medico) {
        // Configuracion por defecto para sprint 1 mientras se agrega configuracion detallada por medico.
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
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .build();
    }

    private MedicoResumenDTO toResumen(Medico medico) {
        return MedicoResumenDTO.builder()
                .id(medico.getId())
                .nombresCompletos((medico.getNombres() + " " + medico.getApellidos()).trim())
                .especialidad(medico.getEspecialidad())
                .activo("ACTIVO".equalsIgnoreCase(medico.getEstado()))
                .build();
    }
}

