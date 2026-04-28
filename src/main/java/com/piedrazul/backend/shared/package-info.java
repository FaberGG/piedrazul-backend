/**
 * Módulo SHARED — componentes transversales.
 * Contiene: seguridad (OAuth2/Keycloak), auditoría, excepciones y DTOs comunes.
 * No depende de ningún módulo funcional.
 *
 * Sub-paquetes expuestos (vía @NamedInterface):
 *  - shared.exception → ResourceNotFoundException, BusinessRuleException
 *  - shared.security  → SecurityConfig
 *  - shared.audit     → AuditService
 */
@org.springframework.modulith.ApplicationModule(displayName = "Shared")
package com.piedrazul.backend.shared;

