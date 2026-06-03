/**
 * Módulo REPORTES — consultas analíticas y exportaciones.
 *
 * Depende de: SHARED, AGENDA (solo vía AgendaApi y los DTOs de agenda.api.dto).
 * Solo lectura, nunca modifica estado del sistema.
 *
 * REGLA: Este módulo NUNCA importa clases de agenda.domain, agenda.repository
 *        ni agenda.service. Toda comunicación es a través de AgendaApi.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reportes",
        allowedDependencies = {
                "agenda::api",
                "agenda::dto",
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.reportes;

