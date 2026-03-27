package com.piedrazul.backend.agenda.internal.controller;

import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaRequest;
import com.piedrazul.backend.agenda.internal.dto.ConfiguracionAgendaResponse;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralRequest;
import com.piedrazul.backend.agenda.internal.dto.DiaNoLaboralResponse;
import com.piedrazul.backend.agenda.internal.service.ConfiguracionAgendaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/configuracion/agenda")
@RequiredArgsConstructor
public class ConfiguracionAgendaController {

    private final ConfiguracionAgendaService configuracionAgendaService;

    /** Obtener configuración actual. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionAgendaResponse> obtener() {
        return ResponseEntity.ok(configuracionAgendaService.obtenerConfiguracion());
    }

    /** Actualizar ventana de agendamiento. */
    @PutMapping("/ventana")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionAgendaResponse> actualizarVentana(
            @Valid @RequestBody ConfiguracionAgendaRequest request) {
        return ResponseEntity.ok(configuracionAgendaService.actualizarVentana(request));
    }

    /** Listar días no laborales. */
    @GetMapping("/dias-no-laborales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DiaNoLaboralResponse>> listar() {
        return ResponseEntity.ok(configuracionAgendaService.listarDiasNoLaborales());
    }

    /** Agregar día no laboral. */
    @PostMapping("/dias-no-laborales")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DiaNoLaboralResponse> agregar(
            @Valid @RequestBody DiaNoLaboralRequest request) {
        return ResponseEntity.status(201).body(configuracionAgendaService.agregarDiaNoLaboral(request));
    }

    /** Eliminar día no laboral. */
    @DeleteMapping("/dias-no-laborales/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        configuracionAgendaService.eliminarDiaNoLaboral(id);
        return ResponseEntity.noContent().build();
    }
}