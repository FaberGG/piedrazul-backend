package com.piedrazul.backend.pacientes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO publico resumido del paciente para respuestas de agenda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResumenDTO {

    private Long id;
    private String documento;
    private String nombres;
    private String apellidos;
    private String celular;
}

