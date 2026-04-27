package com.piedrazul.backend.medicos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO publico para registrar medico vinculado a un usuario existente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroMedicoDTO {

    private UUID usuarioId;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String tipo;
}

