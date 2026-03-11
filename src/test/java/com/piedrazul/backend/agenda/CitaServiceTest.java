package com.piedrazul.backend.agenda;

import com.piedrazul.backend.agenda.service.CitaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests unitarios para CitaService.
 */
@SpringBootTest
class CitaServiceTest {

    @Autowired
    private CitaService citaService;

    @Test
    void contextLoads() {
        assertNotNull(citaService);
    }

    // TODO: agregar tests para listarAgendaMedico, crearCitaManual, agendarAutonomo
}

