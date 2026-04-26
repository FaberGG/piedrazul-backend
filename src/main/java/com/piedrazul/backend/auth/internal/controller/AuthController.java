package com.piedrazul.backend.auth.internal.controller;

import com.piedrazul.backend.auth.internal.dto.AuthResponse;
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // TODO: delegar al servicio
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register/paciente")
    public ResponseEntity<AuthResponse> registerPaciente(@Valid @RequestBody RegisterPacienteRequest request) {
        // TODO: delegar al servicio
        return ResponseEntity.status(201).body(authService.registerPaciente(request));
    }

    @PostMapping("/register/medico")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> registerMedico(@Valid @RequestBody RegisterMedicoRequest request) {
        return ResponseEntity.status(201).body(authService.registerMedico(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.status(201).body(authService.registerAdmin(request));
    }
}

