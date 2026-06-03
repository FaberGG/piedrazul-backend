package com.piedrazul.backend.pacientes.internal.migration;

import com.piedrazul.backend.pacientes.internal.repository.PacientesRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PacientesKeycloakIdMigration {

    private static final Logger log = LoggerFactory.getLogger(PacientesKeycloakIdMigration.class);

    private final PacientesRepository pacientesRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfillKeycloakIds() {
        int updated = pacientesRepository.backfillKeycloakIds();
        if (updated > 0) {
            log.info("Backfilled keycloak_id for {} existing patient(s)", updated);
        }
    }
}
