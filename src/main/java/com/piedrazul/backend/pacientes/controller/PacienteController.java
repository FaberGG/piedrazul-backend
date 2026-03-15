package com.piedrazul.backend.pacientes.controller;

import com.piedrazul.backend.pacientes.dto.PacienteResponse;
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO')")
    public ResponseEntity<List<PacienteResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO') or hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }
}