package com.piedrazul.backend.pacientes.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarPacienteDTO {

    private String nombres;
    private String apellidos;
    private String documento;
    private String celular;
    private String correo;
}
