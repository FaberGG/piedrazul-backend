package com.piedrazul.backend;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Test de arquitectura modular.
 *
 * Verifica en tiempo de build que:
 *  - No existen dependencias circulares entre módulos.
 *  - Los módulos no acceden a clases internas de otros módulos
 *    (solo a sus interfaces públicas: AgendaApi, AuthApi).
 *  - El grafo de dependencias cumple: SHARED ← AUTH ← AGENDA ← REPORTES
 *
 * Ejecutar con: ./mvnw test -Dtest=ModularityTest
 */
class ModularityTest {

    @Test
    void applicationModulesAreCompliant() {
        ApplicationModules.of(PiedrazulBackendApplication.class).verify();
    }
}
