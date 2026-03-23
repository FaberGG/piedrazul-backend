package com.piedrazul.backend.agenda.internal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgendaDinamicaSlotResponse {

    private String hora;
    private String estado;
    private Long citaId;
    private String pacienteDocumento;
    private String pacienteNombres;
    private String pacienteApellidos;
    private String pacienteCelular;
    private boolean permiteAbrirPrioridadPosterior;
}

