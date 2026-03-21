package com.piedrazul.backend.pacientes.controller;

import com.piedrazul.backend.pacientes.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.dto.PacienteSugerenciaResponse;
import com.piedrazul.backend.pacientes.port.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<List<PacienteResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO') or hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'MEDICO_TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<List<PacienteSugerenciaResponse>> buscarPorDocumento(
            @RequestParam String documento,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(pacienteService.buscarPorDocumentoPrefijo(documento, limit));
    }
}