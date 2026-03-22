package com.piedrazul.backend.agenda.internal.controller;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.service.CitaService;
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

    /** RF1 — Listar agenda de un médico por fecha. */
    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'ADMIN')")
    public ResponseEntity<AgendaResponse> listarAgenda(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(citaService.listarAgendaMedico(medicoId, fecha));
    }

    /** RF2 — Crear cita manual (agendador/médico). */
    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO')")
    public ResponseEntity<CitaResponse> crearCitaManual(
            @Valid @RequestBody CrearCitaManualRequest request) {
        return ResponseEntity.status(201).body(citaService.crearCitaManual(request));
    }

    /** RF3 — Agendamiento autónomo (paciente autenticado). */
    @PostMapping("/autonomo")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponse> agendarAutonomo(
            @Valid @RequestBody AgendarAutonomoRequest request) {
        return ResponseEntity.status(201).body(citaService.agendarAutonomo(request));
    }

    /** Recomendación de primer hueco disponible para un medico especifico. */
    @GetMapping("/disponibilidad/primera")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<PrimerHorarioDisponibleResponse> primerHorarioMedico(
            @RequestParam Long medicoId,
            @RequestParam(required = false) LocalDate desde) {
        return ResponseEntity.ok(citaService.obtenerPrimerHorarioDisponibleMedico(medicoId, desde));
    }

    /** Recomendación de primer hueco disponible global entre medicos activos. */
    @GetMapping("/disponibilidad/primera/global")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<PrimerHorarioDisponibleResponse> primerHorarioGlobal(
            @RequestParam(required = false) LocalDate desde) {
        return ResponseEntity.ok(citaService.obtenerPrimerHorarioDisponibleGlobal(desde));
    }

    /**
     * Endpoint agregado para renderizar el panel de agendamiento del dia.
     */
    @GetMapping("/agenda-dinamica")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<AgendaDinamicaResponse> agendaDinamica(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerAgendaDinamica(medicoId, fecha));
    }

    /**
     * Inserta una cita prioritaria de 5 minutos despues de una cita de referencia.
     */
    @PostMapping("/prioridad")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<CitaResponse> crearCitaPrioritaria(
            @Valid @RequestBody CrearCitaPrioritariaRequest request) {
        return ResponseEntity.status(201).body(citaService.crearCitaPrioritaria(request));
    }
}



