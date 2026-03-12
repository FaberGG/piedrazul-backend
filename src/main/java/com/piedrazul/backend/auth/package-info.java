/**
 * Módulo AUTH — autenticación y gestión de usuarios.
 *
 * INTERFAZ PÚBLICA:  {@link com.piedrazul.backend.auth.AuthApi}
 * IMPLEMENTACIÓN:    {@link com.piedrazul.backend.auth.service.AuthFacade}
 *
 * Depende de: SHARED.
 * Gestiona: Usuario, Login, Registro, Roles.
 *
 * REGLA: Las clases en auth.domain y auth.repository son INTERNAS.
 *        Solo AuthApi y los DTOs en auth.dto son públicos.
 */
package com.piedrazul.backend.auth;

