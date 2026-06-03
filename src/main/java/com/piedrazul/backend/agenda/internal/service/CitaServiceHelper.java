package com.piedrazul.backend.agenda.internal.service;

import com.piedrazul.backend.agenda.internal.domain.AgendaDiaLock;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.repository.AgendaDiaLockRepository;
import com.piedrazul.backend.auth.api.AuthApi;
import com.piedrazul.backend.medicos.api.dto.MedicoResumenDTO;
import com.piedrazul.backend.pacientes.api.dto.PacienteResumenDTO;
import com.piedrazul.backend.shared.exception.BusinessRuleException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
public class CitaServiceHelper {

    private final AgendaDiaLockRepository agendaDiaLockRepository;
    private final AuthApi authApi;

    public CitaServiceHelper(AgendaDiaLockRepository agendaDiaLockRepository, AuthApi authApi) {
        this.agendaDiaLockRepository = agendaDiaLockRepository;
        this.authApi = authApi;
    }

    public LocalTime parseHora(String hora) {
        try {
            return LocalTime.parse(hora);
        } catch (DateTimeParseException ex) {
            throw new BusinessRuleException("La hora debe tener formato HH:mm:ss");
        }
    }

    public UUID obtenerUsuarioIdAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String keycloakUserId = jwtAuth.getToken().getSubject();
            // Prefer internal DB UUID; fall back to Keycloak subject UUID directly
            // (covers admin/bootstrap users not registered through the app)
            return authApi.findByKeycloakId(keycloakUserId)
                    .map(com.piedrazul.backend.auth.api.dto.UsuarioInfoDto::getId)
                    .orElseGet(() -> {
                        try { return UUID.fromString(keycloakUserId); }
                        catch (IllegalArgumentException e) { return null; }
                    });
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalStr) {
            return authApi.findByKeycloakId(principalStr)
                    .map(com.piedrazul.backend.auth.api.dto.UsuarioInfoDto::getId)
                    .orElseGet(() -> {
                        try { return UUID.fromString(principalStr); }
                        catch (IllegalArgumentException e) { return null; }
                    });
        }

        return null;
    }

    public void adquirirBloqueoOptimistaAgenda(Long medicoId, LocalDate fecha) {
        AgendaDiaLock lock = agendaDiaLockRepository.findByMedicoIdAndFecha(medicoId, fecha)
                .orElseGet(() -> crearLockAgendaDia(medicoId, fecha));

        lock.touch();
        agendaDiaLockRepository.saveAndFlush(lock);
    }

    private AgendaDiaLock crearLockAgendaDia(Long medicoId, LocalDate fecha) {
        AgendaDiaLock lock = new AgendaDiaLock();
        lock.setMedicoId(medicoId);
        lock.setFecha(fecha);
        lock.touch();

        try {
            return agendaDiaLockRepository.saveAndFlush(lock);
        } catch (DataIntegrityViolationException ex) {
            // Another transaction created the same (medico, fecha) row first
            throw new AgendaLockConcurrencyException(ex);
        }
    }

    public CitaResponse mapToResponse(Cita cita, PacienteResumenDTO paciente, MedicoResumenDTO medico) {
        return CitaResponse.builder()
                .id(cita.getId())
                .pacienteNombre(paciente.getNombres() + " " + paciente.getApellidos())
                .pacienteDocumento(paciente.getDocumento())
                .medicoNombre(medico.getNombresCompletos())
                .especialidad(medico.getEspecialidad())
                .fecha(cita.getFecha())
                .hora(cita.getHora())
                .estado(cita.getEstado().name())
                .tipoCita(cita.getTipoCita() != null ? cita.getTipoCita().name() : null)
                .observaciones(cita.getObservaciones())
                .build();
    }

    public BusinessRuleException conflictoConcurrencia() {
        return new BusinessRuleException("La agenda fue modificada concurrentemente. Intente nuevamente");
    }

    public BusinessRuleException conflictoSlotOcupado() {
        return new BusinessRuleException("El horario seleccionado ya esta ocupado");
    }
}
