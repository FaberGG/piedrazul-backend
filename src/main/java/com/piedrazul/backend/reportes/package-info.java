/**
 * Módulo REPORTES — consultas analíticas y exportaciones.
 *
 * Depende de: SHARED, AGENDA (solo vía {@link com.piedrazul.backend.agenda.api.AgendaApi}).
 * Solo lectura, nunca modifica estado del sistema.
 *
 * REGLA: Este módulo NUNCA importa clases de agenda.domain, agenda.repository
 *        ni agenda.service. Toda comunicación es a través de AgendaApi.
 */
package com.piedrazul.backend.reportes;

