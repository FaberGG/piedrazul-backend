package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi; // <-- IMPORTANTE: Dependemos de la API, no del Repo
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DisponibilidadServiceImpl implements DisponibilidadService {

    // Cambiamos MedicoRepository por la interfaz pública del módulo Médicos
    private final MedicosApi medicosApi;
    private final CitaRepository citaRepository;

    @Override
    public List<LocalTime> calcularHorariosDisponibles(Long medicoId, LocalDate fecha) {
        // 1. Obtener configuración desde el módulo de Médicos (Caja Negra)
        HorarioAtencionDTO config = medicosApi.obtenerHorarioAtencion(medicoId);

        if (config == null || !config.isActivo() || !config.getDiasAtencion().contains(fecha.getDayOfWeek())) {
            return List.of();
        }

        // 2. Generar todos los slots teóricos
        List<LocalTime> todosLosSlots = generarSlotsTeoricos(config);

        // 3. Obtener horas ocupadas desde nuestro propio repositorio de Agenda
        Set<LocalTime> horasOcupadas = citaRepository.findByMedicoIdAndFecha(medicoId, fecha)
                .stream()
                .filter(cita -> !"CANCELADA".equals(cita.getEstado()))
                .map(cita -> cita.getHora())
                .collect(Collectors.toSet());

        // 4. Filtrar: Slots teóricos - Horas ocupadas
        return todosLosSlots.stream()
                .filter(slot -> !horasOcupadas.contains(slot))
                .toList();
    }

    @Override
    public boolean estaDisponible(Long medicoId, LocalDate fecha, LocalTime hora) {
        // Validación optimizada: Primero ver si el slot está físicamente libre en nuestra DB
        boolean slotOcupadoEnAgenda = citaRepository.existsByMedicoIdAndFechaAndHoraAndEstadoNot(
                medicoId, fecha, hora, "CANCELADA");

        if (slotOcupadoEnAgenda) {
            return false;
        }

        // Segundo: Validar que la hora coincida con la configuración del médico
        HorarioAtencionDTO config = medicosApi.obtenerHorarioAtencion(medicoId);
        return esHoraValidaSegunConfig(hora, config, fecha);
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
        if (config == null || !config.isActivo() || !config.getDiasAtencion().contains(fecha.getDayOfWeek())) {
            return false;
        }
        if (hora.isBefore(config.getHoraInicio()) || !hora.isBefore(config.getHoraFin())) {
            return false;
        }
        // Validar que la hora caiga exactamente en un intervalo (múltiplo)
        long minutosDesdeInicio = java.time.Duration.between(config.getHoraInicio(), hora).toMinutes();
        return minutosDesdeInicio % config.getIntervaloMinutos() == 0;
    }
}