/**
 * Paquete expuesto del módulo SHARED — excepciones de dominio.
 *
 * {@code @NamedInterface} declara este sub-paquete como parte de la
 * API pública del módulo. Spring Modulith permite a otros módulos
 * importar tipos de {@code shared.exception} sin violar los límites del módulo.
 *
 * Tipos públicos:
 *  - ResourceNotFoundException  → HTTP 404, usada en todos los módulos funcionales
 *  - BusinessRuleException      → HTTP 422, usada en todos los módulos funcionales
 */
@org.springframework.modulith.NamedInterface("exception")
package com.piedrazul.backend.shared.exception;

