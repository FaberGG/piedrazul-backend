package com.piedrazul.backend.shared.audit;

import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servicio transversal de auditoría.
 * Registra operaciones críticas con contexto de usuario e IP.
 */
@Service
public class AuditService {

    /**
     * Registra una acción de auditoría.
     *
     * @param usuarioId ID del usuario que realizó la acción
     * @param accion    tipo de acción (CREAR, MODIFICAR, CANCELAR, etc.)
     * @param entidad   nombre de la entidad afectada
     * @param entidadId ID de la entidad afectada
     * @param detalles  información adicional en formato JSON
     * @param ip        dirección IP del cliente
     */
    public void registrar(UUID usuarioId, String accion, String entidad,
                           Long entidadId, String detalles, String ip) {
        // TODO: persistir registro de auditoría
    }
}

