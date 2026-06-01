package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.repository.DiaNoLaboralRepository;
import com.piedrazul.backend.medicos.api.MedicosApi; // <-- IMPORTANTE: Dependemos de la API, no del Repo
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisponibilidadServiceImpl implements DisponibilidadService {

    // Cambiamos MedicoRepository por la interfaz pública del módulo Médicos
    private final MedicosApi medicosApi;
    private final CitaRepository citaRepository;
    private final DiaNoLaboralRepository diaNoLaboralRepository;

    @Override
    public List<LocalTime> calcularHorariosDisponibles(Long medicoId, LocalDate fecha) {
        // Si la fecha es un día no laboral, no hay slots disponibles
        if (fecha != null && diaNoLaboralRepository.existsByFecha(fecha)) {
            return List.of();
        }

        HorarioAtencionDTO config = medicosApi.obtenerHorarioAtencion(medicoId);
        if (!esConfiguracionValidaParaFecha(config, fecha)) {
            return List.of();
        }

        int duracionEstandar = config.getIntervaloMinutos();
        List<LocalTime> todosLosSlots = generarSlotsTeoricos(config);
        List<Cita> citasActivas = obtenerCitasActivasOrdenadas(medicoId, fecha);

        return todosLosSlots.stream()
                .filter(slot -> cabeCitaEnSlot(slot, config, citasActivas, duracionEstandar))
                .toList();
    }

    @Override
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        // No disponible en día no laboral
        if (fecha != null && diaNoLaboralRepository.existsByFecha(fecha)) {
            return false;
        }

        HorarioAtencionDTO config = medicosApi.obtenerHorarioAtencion(medicoId);
        if (!esHoraValidaSegunConfig(hora, config, fecha)) {
            return false;
        }

        List<Cita> citasActivas = obtenerCitasActivasOrdenadas(medicoId, fecha);
        return cabeCitaEnSlot(hora, config, citasActivas, config.getIntervaloMinutos());
    }

    private List<LocalTime> generarSlotsTeoricos(HorarioAtencionDTO config) {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime actual = config.getHoraInicio();
        while (actual.isBefore(config.getHoraFin())) {
            slots.add(actual);
            actual = actual.plusMinutes(config.getIntervaloMinutos());
        }
        return slots;
    }

    private boolean esHoraValidaSegunConfig(LocalTime hora, HorarioAtencionDTO config, LocalDate fecha) {
        if (!esConfiguracionValidaParaFecha(config, fecha)) {
            return false;
        }
        if (hora.isBefore(config.getHoraInicio()) || !hora.isBefore(config.getHoraFin())) {
            return false;
        }

        long minutosDesdeInicio = java.time.Duration.between(config.getHoraInicio(), hora).toMinutes();
        return minutosDesdeInicio % config.getIntervaloMinutos() == 0;
    }

    private boolean esConfiguracionValidaParaFecha(HorarioAtencionDTO config, LocalDate fecha) {
        return config != null
                && config.isActivo()
                && config.getHoraInicio() != null
                && config.getHoraFin() != null
                && config.getIntervaloMinutos() > 0
                && config.getDiasAtencion() != null
                && config.getDiasAtencion().contains(fecha.getDayOfWeek())
                && config.getHoraInicio().isBefore(config.getHoraFin());
    }

    private List<Cita> obtenerCitasActivasOrdenadas(Long medicoId, LocalDate fecha) {
        return citaRepository.findByMedicoIdAndFecha(medicoId, fecha)
                .stream()
                .filter(cita -> !"CANCELADA".equalsIgnoreCase(cita.getEstado()))
                .sorted(Comparator.comparing(Cita::getHora))
                .toList();
    }

    private boolean cabeCitaEnSlot(LocalTime inicioSlot,
                                   HorarioAtencionDTO config,
                                   List<Cita> citasActivas,
                                   int duracionEstandar) {
        LocalTime finSlot = inicioSlot.plusMinutes(duracionEstandar);
        if (finSlot.isAfter(config.getHoraFin())) {
            return false;
        }

        return citasActivas.stream().noneMatch(cita -> seSolapa(inicioSlot, finSlot, cita, duracionEstandar));
    }

    private boolean seSolapa(LocalTime inicioNuevo, LocalTime finNuevo, Cita existente, int duracionEstandar) {
        LocalTime inicioExistente = existente.getHora();
        LocalTime finExistente = inicioExistente.plusMinutes(duracionCita(existente, duracionEstandar));
        return inicioNuevo.isBefore(finExistente) && finNuevo.isAfter(inicioExistente);
    }

    private int duracionCita(Cita cita, int duracionEstandar) {
        Integer duracion = cita.getDuracionMinutos();
        return duracion != null && duracion > 0 ? duracion : duracionEstandar;
    }
}