package com.piedrazul.backend.agenda.internal.controller;

import com.piedrazul.backend.agenda.internal.dto.AgendarAutonomoRequest;
import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.dto.AgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.CitaResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaManualRequest;
import com.piedrazul.backend.agenda.internal.dto.ActualizarCitaRequest;
import com.piedrazul.backend.agenda.internal.dto.CitaDetalleResponse;
import com.piedrazul.backend.agenda.internal.dto.CrearCitaPrioritariaRequest;
import com.piedrazul.backend.agenda.internal.dto.HistorialCambiosCitaResponse;
import com.piedrazul.backend.agenda.internal.dto.PrimerHorarioDisponibleResponse;
import com.piedrazul.backend.agenda.internal.dto.ReagendarCitaRequest;
import com.piedrazul.backend.agenda.internal.realtime.AgendaDinamicaSseHub;
import com.piedrazul.backend.agenda.internal.service.CitaService;
import com.piedrazul.backend.agenda.internal.service.DisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/citas")
public class CitaController {

    private final CitaService citaService;
    private final DisponibilidadService disponibilidadService;
    private final AgendaDinamicaSseHub agendaDinamicaSseHub;

    public CitaController(CitaService citaService,
                          DisponibilidadService disponibilidadService,
                          AgendaDinamicaSseHub agendaDinamicaSseHub) {
        this.citaService = citaService;
        this.disponibilidadService = disponibilidadService;
        this.agendaDinamicaSseHub = agendaDinamicaSseHub;
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

    @GetMapping(value = "/agenda-dinamica/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'ADMIN')")
    public SseEmitter agendaDinamicaStream(
            @RequestParam Long medicoId,
            @RequestParam LocalDate fecha) {
        SseEmitter emitter = agendaDinamicaSseHub.subscribe(medicoId, fecha);
        AgendaDinamicaResponse snapshot = citaService.obtenerAgendaDinamica(medicoId, fecha);
        agendaDinamicaSseHub.sendInitialSnapshot(emitter, snapshot);
        return emitter;
    }

    @PostMapping("/prioridad")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<CitaResponse> crearCitaPrioritaria(
            @Valid @RequestBody CrearCitaPrioritariaRequest request) {
        return ResponseEntity.status(201).body(citaService.crearCitaPrioritaria(request));
    }

    @PatchMapping("/{id}/reagendar")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<CitaResponse> reagendarCita(
            @PathVariable Long id,
            @Valid @RequestBody ReagendarCitaRequest request) {
        return ResponseEntity.ok(citaService.reagendarCita(id, request));
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<List<HistorialCambiosCitaResponse>> obtenerHistorial(
            @PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerHistorialCambios(id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<CitaDetalleResponse> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(citaService.obtenerDetalleCita(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDICO', 'ADMIN')")
    public ResponseEntity<CitaResponse> actualizarCita(
            @PathVariable Long id,
            @RequestBody ActualizarCitaRequest request) {
        return ResponseEntity.ok(citaService.actualizarCita(id, request));
    }
}