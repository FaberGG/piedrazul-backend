package com.piedrazul.backend.auth.internal.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para registro de paciente con autogestión.
 * Incluye datos del usuario y datos personales del paciente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterPacienteRequest {

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener mínimo 8 caracteres")
    private String password;

    @NotBlank(message = "El documento es obligatorio")
    private String documento;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "\\d{10}", message = "El celular debe tener 10 dígitos")
    private String celular;

    @NotBlank(message = "El género es obligatorio")
    private String genero;

    private LocalDate fechaNacimiento;

    @Email(message = "El correo debe tener un formato válido")
    private String correo;
}

