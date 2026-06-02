package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para creación manual de cita (RF2).
 * Incluye datos del paciente y de la cita.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearCitaManualRequest {

    // --- Datos del paciente ---
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

    // --- Datos de la cita ---
    @NotNull(message = "El médico es obligatorio")
    private Long medicoId;

    @NotBlank(message = "La hora es obligatoria")
    private String hora;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    private String tipoCita;

    private String observaciones;
}

