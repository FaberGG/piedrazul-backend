package com.piedrazul.backend.agenda.internal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.piedrazul.backend.agenda.internal.domain.Cita;
import com.piedrazul.backend.shared.audit.service.AuditService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/**
 * Typed facade over AuditService that uses ObjectMapper for JSON serialization.
 * Replaces hand-built JSON strings which break on quotes, backslashes, or newlines.
 */
@Component
public class CitaAuditoriaAdapter {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CitaAuditoriaAdapter(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public void registrarCreacion(UUID actor, String accion, Cita cita) {
        String json = toJson(Map.of(
                "medicoId",   cita.getMedicoId(),
                "pacienteId", cita.getPacienteId()
        ));
        auditService.registrar(actor, accion, "CITA", cita.getId(), json);
    }

    public void registrarReagendamiento(UUID actor, Long citaId,
                                        LocalDate fechaAnterior, LocalTime nuevaHora,
                                        String motivo) {
        String json = toJson(Map.of(
                "fechaAnterior", fechaAnterior.toString(),
                "horaNueva",     nuevaHora.toString(),
                "motivo",        motivo != null ? motivo : ""
        ));
        auditService.registrar(actor, "REPROGRAMAR", "CITA", citaId, json);
    }

    public void registrarActualizacion(UUID actor, Long citaId,
                                       String nuevoEstado,
                                       String obsAnterior, String obsNueva) {
        Map<String, Object> campos;
        if (obsNueva != null && !obsNueva.equals(obsAnterior)) {
            campos = Map.of(
                    "nuevoEstado",           nuevoEstado != null ? nuevoEstado : "",
                    "observacionesAnterior", obsAnterior != null ? obsAnterior : "",
                    "observacionesNueva",    obsNueva
            );
        } else {
            campos = Map.of("nuevoEstado", nuevoEstado != null ? nuevoEstado : "");
        }
        auditService.registrar(actor, "ACTUALIZAR", "CITA", citaId, toJson(campos));
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\":\"audit-serialization-failed\"}";
        }
    }
}
