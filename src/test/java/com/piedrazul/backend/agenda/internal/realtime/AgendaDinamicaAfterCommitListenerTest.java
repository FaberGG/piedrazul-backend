package com.piedrazul.backend.agenda.internal.realtime;

import com.piedrazul.backend.agenda.internal.event.AgendaDinamicaChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AgendaDinamicaAfterCommitListenerTest {

    @Mock
    private AgendaDinamicaSseHub sseHub;

    @InjectMocks
    private AgendaDinamicaAfterCommitListener listener;

    @Test
    void delegatesEventToHub() {
        AgendaDinamicaChangedEvent event = new AgendaDinamicaChangedEvent(
                12L,
                LocalDate.of(2026, 4, 19),
                33L,
                "CITA_MANUAL_CREADA"
        );

        listener.onAgendaChanged(event);

        verify(sseHub).broadcastAgendaChanged(event);
    }
}

