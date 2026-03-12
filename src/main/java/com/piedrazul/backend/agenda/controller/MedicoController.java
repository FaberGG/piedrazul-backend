package com.piedrazul.backend.agenda.controller;

import com.piedrazul.backend.agenda.dto.MedicoResponse;
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
    public ResponseEntity<List<MedicoResponse>> listarMedicos(
            @RequestParam(required = false) String especialidad) {
        if (especialidad != null && !especialidad.isBlank()) {
            return ResponseEntity.ok(medicoService.listarPorEspecialidad(especialidad));
        }
        return ResponseEntity.ok(medicoService.listarMedicosActivos());
    }

    /**
     * Obtiene los datos de un médico por ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MedicoResponse> obtenerMedico(@PathVariable Long id) {
        return ResponseEntity.ok(medicoService.obtenerPorId(id));
    }

    /**
     * Actualiza la configuración de horarios de un médico (solo admin).
     * TODO: crear ConfiguracionMedicoRequest DTO para tipar el body.
     */
    @PutMapping("/{id}/configuracion")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<?> actualizarConfiguracion(
            @PathVariable Long id,
            @RequestBody Object configuracion) {
        // TODO: implementar actualización de configuración de horario
        return ResponseEntity.ok().build();
    }
}



