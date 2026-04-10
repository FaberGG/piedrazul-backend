package com.piedrazul.backend.medicos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO publico para registrar medico vinculado a un usuario existente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroMedicoDTO {

    private Long usuarioId;
    private String nombres;
    private String apellidos;
    private String especialidad;
    private String tipo;
}

