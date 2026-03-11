package com.piedrazul.backend.agenda.controller;

import com.piedrazul.backend.agenda.service.MedicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gestión y consulta de médicos.
 */
@RestController
@RequestMapping("/api/v1/medicos")
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    /**
     * Lista médicos activos, opcionalmente filtrados por especialidad.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<?>> listarMedicos(
            @RequestParam(required = false) String especialidad) {
        // TODO: delegar al servicio
        return ResponseEntity.ok(List.of());
    }

    /**
     * Actualiza la configuración de horarios de un médico (solo admin).
     */
    @PutMapping("/{id}/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarConfiguracion(
            @PathVariable Long id,
            @RequestBody Object configuracion) {
        // TODO: implementar actualización de configuración
        return ResponseEntity.ok().build();
    }
}

