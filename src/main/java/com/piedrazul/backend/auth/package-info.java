/**
 * Módulo AUTH — autenticación y gestión de usuarios.
 *
 * INTERFAZ PÚBLICA:  {@link com.piedrazul.backend.auth.api.AuthApi}
 * IMPLEMENTACIÓN:    {@link com.piedrazul.backend.auth.internal.service.AuthFacade}
 *
 * Depende de: SHARED.
 * Gestiona: Usuario, Login, Registro, Roles.
 *
 * REGLA: Las clases en auth.internal son PRIVADAS al módulo.
 *        Solo auth.api y auth.api.dto son accesibles desde otros módulos.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Auth",
        allowedDependencies = {
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.auth;

