package com.piedrazul.backend.auth.controller;

import com.piedrazul.backend.auth.dto.AuthResponse;
import com.piedrazul.backend.auth.dto.LoginRequest;
import com.piedrazul.backend.auth.dto.RegisterPacienteRequest;
import com.piedrazul.backend.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
}

