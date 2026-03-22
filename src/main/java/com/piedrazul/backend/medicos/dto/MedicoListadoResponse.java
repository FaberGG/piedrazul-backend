package com.piedrazul.backend.medicos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoListadoResponse {

    private Long id;
    private String nombresCompletos;
    private String especialidad;
    private String tipo;
    private boolean activo;
    private Integer intervaloMinutos;
}

