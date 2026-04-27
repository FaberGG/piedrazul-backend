package com.piedrazul.backend.agenda.internal.controller;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.service.CitaService;
import com.piedrazul.backend.agenda.internal.service.DisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    private final CitaService citaService;
    private final DisponibilidadService disponibilidadService;

    public CitaController(CitaService citaService, DisponibilidadService disponibilidadService) {
        this.citaService = citaService;
        this.disponibilidadService = disponibilidadService;
    }

    @GetMapping("/agenda")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'ADMIN', 'MEDICO')")
    public ResponseEntity<AgendaResponse> listarAgenda(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(citaService.listarAgendaMedico(medicoId, fecha));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO')")
    public ResponseEntity<CitaResponse> crearCitaManual(
            @Valid @RequestBody CrearCitaManualRequest request) {
        return ResponseEntity.status(201).body(citaService.crearCitaManual(request));
    }

    @PostMapping("/autonomo")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<CitaResponse> agendarAutonomo(
            @Valid @RequestBody AgendarAutonomoRequest request) {
        return ResponseEntity.status(201).body(citaService.agendarAutonomo(request));
    }

    @GetMapping("/disponibilidad/primera")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<PrimerHorarioDisponibleResponse> primerHorarioMedico(
            @RequestParam Long medicoId,
            @RequestParam(required = false) LocalDate desde) {
        return ResponseEntity.ok(citaService.obtenerPrimerHorarioDisponibleMedico(medicoId, desde));
    }

    @GetMapping("/disponibilidad/primera/global")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<PrimerHorarioDisponibleResponse> primerHorarioGlobal(
            @RequestParam(required = false) LocalDate desde) {
        return ResponseEntity.ok(citaService.obtenerPrimerHorarioDisponibleGlobal(desde));
    }

    @GetMapping("/disponibilidad/franjas")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<List<LocalTime>> obtenerFranjasDisponibles(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(disponibilidadService.calcularHorariosDisponibles(medicoId, fecha));
    }

    @GetMapping("/agenda-dinamica")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<AgendaDinamicaResponse> agendaDinamica(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(citaService.obtenerAgendaDinamica(medicoId, fecha));
    }

    @PostMapping("/prioridad")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<CitaResponse> crearCitaPrioritaria(
            @Valid @RequestBody CrearCitaPrioritariaRequest request) {
        return ResponseEntity.status(201).body(citaService.crearCitaPrioritaria(request));
    }
}