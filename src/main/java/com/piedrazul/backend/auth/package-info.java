/**
 * Módulo AUTH — autenticación y gestión de usuarios.
 *
 * INTERFAZ PÚBLICA:  {@link com.piedrazul.backend.auth.api.AuthApi}
 * IMPLEMENTACIÓN:    {@link com.piedrazul.backend.auth.internal.service.AuthFacade}
 *
 * Depende de: SHARED, PACIENTES (api/api-dto), MEDICOS (api/api-dto).
 * Gestiona: Usuario, Login, Registro, Roles.
 *
 * REGLA: Las clases en auth.internal son PRIVADAS al módulo.
 *        Solo auth.api y auth.api.dto son accesibles desde otros módulos.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Auth",
        allowedDependencies = {
                "pacientes::api",
                "pacientes::api-dto",
                "medicos::api",
                "medicos::api-dto",
                "shared::exception",
                "shared::audit",
                "shared::security"

        })
package com.piedrazul.backend.auth;

