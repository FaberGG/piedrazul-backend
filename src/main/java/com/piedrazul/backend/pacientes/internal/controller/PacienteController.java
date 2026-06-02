package com.piedrazul.backend.pacientes.internal.controller;

import com.piedrazul.backend.pacientes.internal.dto.PacienteResponse;
import com.piedrazul.backend.pacientes.internal.dto.PacienteSugerenciaResponse;
import com.piedrazul.backend.pacientes.internal.service.PacienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pacientes")
@RequiredArgsConstructor
public class PacienteController {

    private final PacienteService pacienteService;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<List<PacienteResponse>> listarTodos() {
        return ResponseEntity.ok(pacienteService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MEDICO') or hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pacienteService.buscarPorId(id));
    }

    @GetMapping("/buscar")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'ADMIN')")
    public ResponseEntity<List<PacienteSugerenciaResponse>> buscarPorDocumento(
            @RequestParam String documento,
            @RequestParam(required = false, defaultValue = "5") int limit) {
        return ResponseEntity.ok(pacienteService.buscarPorDocumentoPrefijo(documento, limit));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PACIENTE')")
    public ResponseEntity<PacienteResponse> miPerfil() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder
                .getContext().getAuthentication();
        UUID usuarioId = UUID.fromString(auth.getToken().getSubject());
        return ResponseEntity.ok(pacienteService.buscarPorUsuarioId(usuarioId));
    }
}