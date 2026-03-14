package com.piedrazul.backend.pacientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResponse {

    private Long id;
    private String documento;
    private String nombres;
    private String apellidos;
    private String celular;
    private String correo;
    private LocalDate fechaNacimiento;
    private String genero;

}
