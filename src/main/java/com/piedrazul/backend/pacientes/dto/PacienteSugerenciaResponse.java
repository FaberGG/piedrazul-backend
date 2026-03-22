package com.piedrazul.backend.pacientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteSugerenciaResponse {

    private Long id;
    private String documento;
    private String nombresCompletos;
}

