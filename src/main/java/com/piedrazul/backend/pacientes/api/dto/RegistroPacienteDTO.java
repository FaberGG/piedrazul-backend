package com.piedrazul.backend.pacientes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO publico para registrar o actualizar datos basicos de paciente en flujos de agenda.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroPacienteDTO {

    private String documento;
    private String nombres;
    private String apellidos;
    private String celular;
    private String genero;
    private LocalDate fechaNacimiento;
    private String correo;
}

