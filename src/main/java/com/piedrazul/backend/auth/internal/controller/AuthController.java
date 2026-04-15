package com.piedrazul.backend.auth.internal.controller;

import com.piedrazul.backend.auth.internal.dto.LoginRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterMedicoRequest;
import com.piedrazul.backend.auth.internal.dto.RegisterPacienteRequest;
import com.piedrazul.backend.auth.internal.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticación.
 * Endpoints públicos: login y registro de paciente.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // ← /login eliminado, Keycloak lo maneja

    @PostMapping("/register/paciente")
    public ResponseEntity<Void> registerPaciente(@Valid @RequestBody RegisterPacienteRequest request) {
        authService.registerPaciente(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/register/medico")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> registerMedico(@Valid @RequestBody RegisterMedicoRequest request) {
        authService.registerMedico(request);
        return ResponseEntity.status(201).build();
    }

    @PostMapping("/register/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> registerAdmin(@Valid @RequestBody LoginRequest request) {
        authService.registerAdmin(request);
        return ResponseEntity.status(201).build();
    }
}