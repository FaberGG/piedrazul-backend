package com.piedrazul.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/**
 * Prueba de integración arquitectural (Spring Modulith).
 * Verifica que ningún módulo importe clases internas de otro módulo.
 * Solo los contratos {@code *Api} pueden cruzar fronteras de módulos.
 */
@Tag("integration")
class ModularityTest {

    @Test
    @DisplayName("Todos los módulos respetan sus fronteras de acceso público (*Api)")
    void debeRespetarLimitesDeModulos() {
        ApplicationModules modulos = ApplicationModules.of(PiedrazulBackendApplication.class);
        modulos.verify();
    }
}
