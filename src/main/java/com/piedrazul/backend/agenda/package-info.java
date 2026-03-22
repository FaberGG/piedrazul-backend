/**
 * Módulo AGENDA — módulo principal del sistema.
 *
 * INTERFAZ PÚBLICA:  AgendaApi (paquete raíz)
 * SUB-PAQUETE EXPUESTO: agenda.dto (anotado con @NamedInterface)
 *
 * Depende de: SHARED, AUTH (solo vía contexto de seguridad Spring o AuthApi).
 * Gestiona: Paciente, Médico, ConfiguracionMedico, Cita,
 *           HistorialCambiosCita, HistoriaClinica, ConfiguracionGlobal.
 *
 * PRIVADOS (nunca importar desde fuera):
 *  agenda.domain, agenda.repository, agenda.service, agenda.controller
 */
@org.springframework.modulith.ApplicationModule(displayName = "Agenda",
        allowedDependencies = {
                "medicos::api",
                "medicos::api-dto",
                "pacientes::api",
                "pacientes::api-dto",
                "shared::exception",
                "shared::audit"
        })
package com.piedrazul.backend.agenda;

