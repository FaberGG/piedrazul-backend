/**
 * Módulo NOTIFICACIONES — envío de correos de confirmación y recordatorio.
 * Escucha eventos del módulo agenda vía Spring Events.
 * Nunca llama directamente a otros módulos funcionales.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notificaciones",
        allowedDependencies = {
                "agenda::api",
                "shared::exception"
        })
package com.piedrazul.backend.Notificaciones;
