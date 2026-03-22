package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.HorarioAtencionDTO;
import com.piedrazul.backend.medicos.repository.MedicosRepository;
import com.piedrazul.backend.medicos.domain.Medico;
import com.piedrazul.backend.shared.audit.AuditService;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Implementación del servicio de citas (módulo AGENDA).
 * RF1: Listar agenda de un médico por fecha.
 * RF2: Crear cita manual.
 */
@Service
@Transactional
public class CitaServiceImpl implements CitaService {

    private final CitaRepository citaRepository;
    private final DisponibilidadService disponibilidadService;
    private final AuditService auditService;
    private final MedicosApi medicosApi;
    private final MedicosRepository medicosRepository;

    public CitaServiceImpl(CitaRepository citaRepository,
                           DisponibilidadService disponibilidadService,
                           AuditService auditService,
                           MedicosApi medicosApi,
                           MedicosRepository medicosRepository) {
        this.citaRepository       = citaRepository;
        this.disponibilidadService = disponibilidadService;
        this.auditService          = auditService;
        this.medicosApi            = medicosApi;
        this.medicosRepository     = medicosRepository;
    }

    // ─────────────────────────────────────────────────────────────
    // RF1 — Listar agenda de un médico por fecha
    // ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AgendaResponse listarAgendaMedico(Long medicoId, LocalDate fecha) {

        // 1. Obtener datos del médico
        Medico medico = medicosRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", medicoId));

        // 2. Obtener configuración de horario del médico
        HorarioAtencionDTO config = medicosApi.obtenerHorarioAtencion(medicoId);

        if (config == null) {
            throw new ResourceNotFoundException("Configuración de horario para médico", medicoId);
        }

        // 3. Obtener citas del día desde el repositorio
        List<Cita> citasDelDia = citaRepository.findByMedicoIdAndFecha(medicoId, fecha);

        // 4. Calcular horarios disponibles
        List<LocalTime> disponibles = disponibilidadService.calcularHorariosDisponibles(medicoId, fecha);

        // 5. Calcular capacidad total (slots teóricos)
        int totalSlots = calcularTotalSlots(config);
        int slotsOcupados = (int) citasDelDia.stream()
                .filter(c -> !"CANCELADA".equals(c.getEstado()))
                .count();
        double porcentajeOcupacion = totalSlots > 0
                ? (slotsOcupados * 100.0) / totalSlots
                : 0.0;

        // 6. Mapear citas a DTOs de respuesta
        List<CitaResponse> citasDto = citasDelDia.stream()
                .map(this::mapToCitaResponse)
                .toList();

        // 7. Construir y retornar respuesta
        return AgendaResponse.builder()
                .medicoId(medicoId)
                .medicoNombre(medico.getNombres() + " " + medico.getApellidos())
                .especialidad(medico.getEspecialidad())
                .fecha(fecha)
                .citas(citasDto)
                .horariosDisponibles(disponibles.stream().map(LocalTime::toString).toList())
                .totalSlots(totalSlots)
                .slotsOcupados(slotsOcupados)
                .porcentajeOcupacion(porcentajeOcupacion)
                .build();
    }

    // ─────────────────────────────────────────────────────────────
    // RF2 — Crear cita manual
    // ─────────────────────────────────────────────────────────────

    @Override
    public CitaResponse crearCitaManual(CrearCitaManualRequest request) {
        throw new UnsupportedOperationException("TODO RF2: implementar crearCitaManual");
    }

    // ─────────────────────────────────────────────────────────────
    // RF3 — Agendamiento autónomo (paciente)
    // ─────────────────────────────────────────────────────────────

    @Override
    public CitaResponse agendarAutonomo(AgendarAutonomoRequest request) {
        throw new UnsupportedOperationException("TODO RF3: implementar agendarAutonomo");
    }

    // ─────────────────────────────────────────────────────────────
    // Métodos auxiliares privados
    // ─────────────────────────────────────────────────────────────

    /**
     * Calcula la cantidad total de slots posibles en un día,
     * usando la configuración del médico.
     * Fórmula: (horaFin - horaInicio en minutos) / intervaloMinutos
     */
    private int calcularTotalSlots(HorarioAtencionDTO config) {
        if (config.getHoraInicio() == null || config.getHoraFin() == null
                || config.getIntervaloMinutos() <= 0) {
            return 0;
        }
        long minutosTotales = java.time.Duration
                .between(config.getHoraInicio(), config.getHoraFin())
                .toMinutes();
        return (int) (minutosTotales / config.getIntervaloMinutos());
    }

    /**
     * Mapea una entidad Cita a su DTO de respuesta.
     * Nota: En esta versión, los campos del paciente y médico se obtienen
     * desde los IDs almacenados en la cita. Si necesitas nombres, inyecta
     * los repositorios/APIs de pacientes y médicos aquí.
     */
    private CitaResponse mapToCitaResponse(Cita cita) {
        return CitaResponse.builder()
                .id(cita.getId())
                // Por ahora mostramos el documento/id del paciente
                // Cuando tengas el módulo de pacientes integrado,
                // reemplaza con pacientesApi.obtenerNombre(cita.getPacienteId())
                .pacienteDocumento("ID: " + cita.getPacienteId())
                .pacienteNombre("Paciente #" + cita.getPacienteId())
                .fecha(cita.getFecha())
                .hora(cita.getHora())
                .estado(cita.getEstado())
                .observaciones(cita.getObservaciones())
                .build();
    }
}
