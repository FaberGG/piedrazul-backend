package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActualizarCitaRequest {

    private String nuevoEstado;
    private String nuevasObservaciones;

    private String pacienteNombres;
    private String pacienteApellidos;
    private String pacienteDocumento;
    private String pacienteCelular;
    private String pacienteCorreo;
}
