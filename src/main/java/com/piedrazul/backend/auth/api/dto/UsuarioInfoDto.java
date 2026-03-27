package com.piedrazul.backend.auth.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de cruce de módulos: lo que el módulo AUTH expone
 * a otros módulos a través de {@link com.piedrazul.backend.auth.api.AuthApi}.
 *
 * Este objeto NO es una entidad de dominio. Es un contrato de datos
 * intencionalmente simple para no acoplar los módulos.
 *
 * Consumido por: medicos, pacientes y agenda via AuthApi.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioInfoDto {
    private Long id;
    private String username;
    private String rol;
    private String estado;
}