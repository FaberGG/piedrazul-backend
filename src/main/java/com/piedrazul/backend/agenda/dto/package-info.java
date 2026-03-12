/**
 * Paquete expuesto del módulo AGENDA.
 *
 * {@code @NamedInterface} declara este sub-paquete como parte de la
 * API pública del módulo. Spring Modulith permite a otros módulos
 * importar tipos de {@code agenda.dto} sin violar los límites del módulo.
 *
 * Tipos públicos de este paquete (usables desde otros módulos):
 *  - ResumenCitasDto  → consumido por el módulo reportes via AgendaApi
 *
 * Tipos HTTP-only (no importar desde otros módulos, solo para controllers):
 *  - AgendaResponse, CitaResponse, CrearCitaManualRequest
 */
@org.springframework.modulith.NamedInterface("dto")
package com.piedrazul.backend.agenda.dto;

