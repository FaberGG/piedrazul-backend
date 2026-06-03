package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.domain.EstadoCita;
import com.piedrazul.backend.agenda.internal.domain.HistorialCambiosCita;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.HistorialCambiosCitaResponse;
import com.piedrazul.backend.agenda.internal.dto.ReagendarCitaRequest;
import com.piedrazul.backend.agenda.internal.event.AgendaDinamicaChangedEvent;
import com.piedrazul.backend.agenda.internal.repository.CitaRepository;
import com.piedrazul.backend.agenda.internal.repository.HistorialCambiosCitaRepository;
import com.piedrazul.backend.medicos.api.MedicosApi;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.PacientesApi;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import com.piedrazul.backend.shared.exception.ResourceNotFoundException;
import org.hibernate.AssertionFailure;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class ReagendarServiceImpl implements ReagendarService {

    private final CitaRepository citaRepository;
    private final HistorialCambiosCitaRepository historialRepository;
    private final DisponibilidadService disponibilidadService;
    private final PacientesApi pacientesApi;
    private final MedicosApi medicosApi;
    private final CitaAuditoriaAdapter auditoriaAdapter;
    private final CitaServiceHelper helper;
    private final ApplicationEventPublisher eventPublisher;

    ReagendarServiceImpl(CitaRepository citaRepository,
                         HistorialCambiosCitaRepository historialRepository,
                         DisponibilidadService disponibilidadService,
                         PacientesApi pacientesApi,
                         MedicosApi medicosApi,
                         CitaAuditoriaAdapter auditoriaAdapter,
                         CitaServiceHelper helper,
                         ApplicationEventPublisher eventPublisher) {
        this.citaRepository = citaRepository;
        this.historialRepository = historialRepository;
        this.disponibilidadService = disponibilidadService;
        this.pacientesApi = pacientesApi;
        this.medicosApi = medicosApi;
        this.auditoriaAdapter = auditoriaAdapter;
        this.helper = helper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CitaResponse reagendar(Long citaId, ReagendarCitaRequest request) {
        try {
            Cita cita = citaRepository.findById(citaId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cita", citaId));

            if (cita.getEstado() != EstadoCita.ATENDIDA) {
                throw new BusinessRuleException("Solo se pueden reagendar citas que ya fueron atendidas");
            }

            Long medicoId = request.getMedicoNuevoId() != null
                    ? request.getMedicoNuevoId()
                    : cita.getMedicoId();

            helper.adquirirBloqueoOptimistaAgenda(medicoId, request.getNuevaFecha());

            LocalTime nuevaHora = helper.parseHora(request.getNuevaHora());

            if (!disponibilidadService.estaDisponible(medicoId, request.getNuevaFecha(), nuevaHora)) {
                throw new BusinessRuleException("El horario solicitado no esta disponible");
            }

            UUID usuarioId = helper.obtenerUsuarioIdAutenticado();

            // Save history before mutating — rollback keeps log consistent
            historialRepository.save(
                    HistorialCambiosCita.builder()
                            .cita(cita)
                            .fechaAnterior(cita.getFecha())
                            .horaAnterior(cita.getHora())
                            .medicoAnteriorId(cita.getMedicoId())
                            .fechaNueva(request.getNuevaFecha())
                            .horaNueva(nuevaHora)
                            .medicoNuevoId(medicoId)
                            .motivo(request.getMotivo())
                            .modificadoPor(usuarioId)
                            .build()
            );

            LocalDate fechaAnterior = cita.getFecha();
            Long medicoAnteriorId = cita.getMedicoId();

            cita.setFecha(request.getNuevaFecha());
            cita.setHora(nuevaHora);
            cita.setMedicoId(medicoId);
            cita.setEstado(EstadoCita.PROGRAMADA);

            Cita guardada = citaRepository.save(cita);

            auditoriaAdapter.registrarReagendamiento(usuarioId, guardada.getId(),
                    fechaAnterior, nuevaHora, request.getMotivo());

            // Notify both dates so the correct SSE channel receives the origin event
            // even when the doctor changes during rescheduling
            publicarCambioAgenda(medicoAnteriorId, fechaAnterior, guardada.getId(), "CITA_REAGENDADA_ORIGEN");
            publicarCambioAgenda(guardada.getMedicoId(), guardada.getFecha(), guardada.getId(), "CITA_REAGENDADA_DESTINO");

            PacienteResumenDTO paciente = pacientesApi.obtenerResumenPorId(guardada.getPacienteId());
            MedicoResumenDTO medico = medicosApi.obtenerResumenMedico(guardada.getMedicoId());

            return helper.mapToResponse(guardada, paciente, medico);

        } catch (AgendaLockConcurrencyException | ObjectOptimisticLockingFailureException | AssertionFailure ex) {
            throw helper.conflictoConcurrencia();
        } catch (DataIntegrityViolationException ex) {
            throw helper.conflictoSlotOcupado();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialCambiosCitaResponse> obtenerHistorial(Long citaId) {
        if (!citaRepository.existsById(citaId)) {
            throw new ResourceNotFoundException("Cita", citaId);
        }
        return historialRepository.findByCitaIdOrderByCreatedAtDesc(citaId)
                .stream()
                .map(h -> HistorialCambiosCitaResponse.builder()
                        .id(h.getId())
                        .fechaAnterior(h.getFechaAnterior())
                        .horaAnterior(h.getHoraAnterior())
                        .medicoAnteriorId(h.getMedicoAnteriorId())
                        .fechaNueva(h.getFechaNueva())
                        .horaNueva(h.getHoraNueva())
                        .medicoNuevoId(h.getMedicoNuevoId())
                        .motivo(h.getMotivo())
                        .modificadoPor(h.getModificadoPor())
                        .creadoEn(h.getCreatedAt())
                        .build())
                .toList();
    }

    private void publicarCambioAgenda(Long medicoId, LocalDate fecha, Long citaId, String accion) {
        eventPublisher.publishEvent(new AgendaDinamicaChangedEvent(medicoId, fecha, citaId, accion));
    }
}
