package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitaDetalleResponse {

    private Long id;
    private String pacienteNombre;
    private String pacienteDocumento;
    private String pacienteCelular;
    private String pacienteCorreo;
    private String medicoNombre;
    private String especialidad;
    private LocalDate fecha;
    private LocalTime hora;
    private String estado;
    private String observaciones;
    private boolean esPrimeraCita;
}
