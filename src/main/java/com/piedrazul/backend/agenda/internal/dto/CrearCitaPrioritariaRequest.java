package com.piedrazul.backend.agenda.internal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrearCitaPrioritariaRequest {

    @NotBlank(message = "El documento es obligatorio")
    private String documento;

    @NotBlank(message = "Los nombres son obligatorios")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    private String apellidos;

    @NotBlank(message = "El celular es obligatorio")
    @Pattern(regexp = "\\d{10}", message = "El celular debe tener 10 digitos")
    private String celular;

    @NotBlank(message = "El genero es obligatorio")
    private String genero;

    private LocalDate fechaNacimiento;

    @Email(message = "El correo debe tener un formato valido")
    private String correo;

    @NotNull(message = "El medico es obligatorio")
    private Long medicoId;

    @NotNull(message = "La fecha es obligatoria")
    private LocalDate fecha;

    @NotBlank(message = "La hora de referencia es obligatoria")
    private String horaReferencia;

    private String observaciones;
}

