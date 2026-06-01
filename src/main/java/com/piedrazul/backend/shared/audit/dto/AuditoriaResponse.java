package com.piedrazul.backend.shared.audit.dto;

import com.piedrazul.backend.shared.audit.domain.Auditoria;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuditoriaResponse(
        Long id,
        UUID usuarioId,
        String accion,
        String entidad,
        Long entidadId,
        String detalles,
        String ipAddress,
        LocalDateTime timestamp
) {
    public static AuditoriaResponse from(Auditoria a) {
        return new AuditoriaResponse(
                a.getId(),
                a.getUsuarioId(),
                a.getAccion(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getDetalles(),
                a.getIpAddress(),
                a.getTimestamp()
        );
    }
}
