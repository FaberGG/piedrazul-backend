package com.piedrazul.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de cruce de módulos: lo que el módulo AUTH expone
 * a otros módulos a través de {@link com.piedrazul.backend.auth.AuthApi}.
 *
 * Contiene únicamente los datos de identidad necesarios;
 * nunca expone la contraseña ni datos sensibles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioInfoDto {

    private Long id;
    private String username;
    /** Rol del usuario: PACIENTE | AGENDADOR | MEDICO_TERAPISTA | ADMINISTRADOR */
    private String rol;
    private String estado;
}

