/**
 * Módulo AUTH — autenticación y gestión de usuarios.
 *
 * INTERFAZ PÚBLICA:  {@link com.piedrazul.backend.auth.api.AuthApi}
 * IMPLEMENTACIÓN:    {@link com.piedrazul.backend.auth.internal.service.AuthFacade}
 *
 * Depende de: SHARED.
 * Gestiona: Usuario, Login, Registro, Roles.
 *
 * REGLA: Las clases en auth.domain y auth.repository son INTERNAS.
 *        Solo AuthApi y los DTOs en auth.dto son públicos.
 */

/**
 * Paquete expuesto del módulo AUTH.
 *
 * Tipos públicos de este paquete (usables desde otros módulos):
 *  - UsuarioInfoDto → consumido por medicos, pacientes y agenda via AuthApi
 *
 */


@org.springframework.modulith.NamedInterface("api")
package com.piedrazul.backend.auth.api.dto;

