package com.piedrazul.backend.medicos.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO publico para compartir datos basicos del medico con otros modulos.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicoResumenDTO {

    private Long id;
    private String nombresCompletos;
    private String especialidad;
    private boolean activo;
}

