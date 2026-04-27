package com.piedrazul.backend.medicos.internal.controller;

import com.piedrazul.backend.medicos.internal.dto.ConfiguracionAgendaMedicoResponse;
import com.piedrazul.backend.medicos.internal.dto.ConfigurarAgendaMedicoRequest;
import com.piedrazul.backend.medicos.internal.dto.MedicoListadoResponse;
import com.piedrazul.backend.medicos.internal.service.MedicosFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/medicos")
@RequiredArgsConstructor
public class MedicoController {

    private final MedicosFacade medicosFacade;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<List<MedicoListadoResponse>> listarMedicosActivos(
            @RequestParam(required = false) String especialidad) {
        return ResponseEntity.ok(medicosFacade.listarMedicosActivos(especialidad));
    }

    @GetMapping("/{medicoId}/configuracion")
    @PreAuthorize("hasAnyRole('AGENDADOR', 'TERAPISTA', 'MEDICO', 'PACIENTE', 'ADMIN')")
    public ResponseEntity<ConfiguracionAgendaMedicoResponse> obtenerConfiguracionAgenda(
            @PathVariable Long medicoId) {
        return ResponseEntity.ok(medicosFacade.obtenerConfiguracionAgenda(medicoId));
    }

    @PutMapping("/{medicoId}/configuracion")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfiguracionAgendaMedicoResponse> configurarAgenda(
            @PathVariable Long medicoId,
            @Valid @RequestBody ConfigurarAgendaMedicoRequest request) {
        return ResponseEntity.ok(medicosFacade.configurarAgenda(medicoId, request));
    }
}

