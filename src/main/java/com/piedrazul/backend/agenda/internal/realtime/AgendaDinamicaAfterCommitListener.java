package com.piedrazul.backend.agenda.internal.realtime;

import com.piedrazul.backend.agenda.internal.event.AgendaDinamicaChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends SSE updates only after DB commit to avoid stale notifications.
 */
@Component
public class AgendaDinamicaAfterCommitListener {

    private final AgendaDinamicaSseHub sseHub;

    public AgendaDinamicaAfterCommitListener(AgendaDinamicaSseHub sseHub) {
        this.sseHub = sseHub;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAgendaChanged(AgendaDinamicaChangedEvent event) {
        sseHub.broadcastAgendaChanged(event);
    }
}

