package com.piedrazul.backend.shared.audit.service;

import com.piedrazul.backend.shared.audit.domain.Auditoria;
import com.piedrazul.backend.shared.audit.dto.AuditoriaResponse;
import com.piedrazul.backend.shared.audit.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public void registrar(UUID usuarioId, String accion, String entidad,
                          Long entidadId, String detalles) {
        Auditoria registro = Auditoria.builder()
                .usuarioId(usuarioId)
                .accion(accion)
                .entidad(entidad)
                .entidadId(entidadId)
                .detalles(detalles)
                .ipAddress(resolverIp())
                .build();
        auditoriaRepository.save(registro);
    }

    public Page<AuditoriaResponse> listar(String accion, String entidad,
                                          LocalDate desde, LocalDate hasta,
                                          Pageable pageable) {
        // cb.conjunction() = always-true predicate (1=1), used as neutral starting point
        Specification<Auditoria> spec = (root, query, cb) -> cb.conjunction();

        if (accion != null && !accion.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("accion"), accion.trim().toUpperCase()));
        }
        if (entidad != null && !entidad.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("entidad"), entidad.trim().toUpperCase()));
        }
        if (desde != null) {
            spec = spec.and((root, q, cb) ->
                    cb.greaterThanOrEqualTo(root.get("timestamp"), desde.atStartOfDay()));
        }
        if (hasta != null) {
            spec = spec.and((root, q, cb) ->
                    cb.lessThan(root.get("timestamp"), hasta.plusDays(1).atStartOfDay()));
        }

        return auditoriaRepository.findAll(spec, pageable).map(AuditoriaResponse::from);
    }

    private String resolverIp() {
        try {
            HttpServletRequest req = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes()).getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "N/A";
        }
    }
}
