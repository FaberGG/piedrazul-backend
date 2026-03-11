package com.piedrazul.backend.agenda.controller;

import com.piedrazul.backend.agenda.dto.AgendaResponse;
import com.piedrazul.backend.agenda.dto.CitaResponse;
import com.piedrazul.backend.agenda.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Controller para gestión de citas médicas.
 */
@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /**
     * RF1 — Listar agenda de un médico por fecha.
     */
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'ADMINISTRADOR')")
    public ResponseEntity<AgendaResponse> listarAgenda(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        // TODO: delegar al servicio
        return ResponseEntity.ok(citaService.listarAgendaMedico(medicoId, fecha));
    }

    /**
     * RF2 — Crear cita manual (agendador/médico).
     */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA')")
    public ResponseEntity<CitaResponse> crearCitaManual(@Valid @RequestBody CrearCitaManualRequest request) {
        // TODO: delegar al servicio
        return ResponseEntity.status(201).body(citaService.crearCitaManual(request));
    }

    /**
     * RF3 — Agendamiento autónomo (paciente).
     */
    @PostMapping("/autonomo")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponse> agendarAutonomo(@RequestBody Object request) {
        // TODO: implementar
        return ResponseEntity.status(201).build();
    }
}

