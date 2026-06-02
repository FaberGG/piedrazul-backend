package com.piedrazul.backend.agenda.internal.starup;

import com.piedrazul.backend.agenda.internal.service.ConfiguracionAgendaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Carga (idempotente) los días festivos para el año actual al arrancar la aplicación.
 * Evita duplicados porque `importarFestivos` sólo inserta fechas no existentes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FestivosStartupLoader implements ApplicationListener<ApplicationReadyEvent> {

    private final ConfiguracionAgendaService configuracionAgendaService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        int year = LocalDate.now().getYear();
        try {
            log.info("Importando festivos para el año {} al iniciar la aplicación", year);
            configuracionAgendaService.importarFestivos(year);

            // Importar también el siguiente año cuando estemos cerca de fin de año
            int month = LocalDate.now().getMonthValue();
            if (month >= 11) {
                int siguiente = year + 1;
                log.info("Importando festivos para el año siguiente {}", siguiente);
                configuracionAgendaService.importarFestivos(siguiente);
            }
        } catch (Exception ex) {
            log.error("Error al importar festivos en startup: {}", ex.getMessage(), ex);
        }
    }
}
