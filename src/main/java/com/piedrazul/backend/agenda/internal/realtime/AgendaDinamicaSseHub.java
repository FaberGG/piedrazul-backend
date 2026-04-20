package com.piedrazul.backend.agenda.internal.realtime;

import com.piedrazul.backend.agenda.internal.dto.AgendaDinamicaResponse;
import com.piedrazul.backend.agenda.internal.event.AgendaDinamicaChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory hub that manages SSE subscribers for agenda updates.
 */
@Component
public class AgendaDinamicaSseHub {

    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;
    private static final String EVENT_CONNECTED = "connected";
    private static final String EVENT_SNAPSHOT = "agenda-snapshot";
    private static final String EVENT_UPDATED = "agenda-updated";

    private final AtomicLong eventSequence = new AtomicLong(0L);
    private final Map<AgendaKey, CopyOnWriteArrayList<SseEmitter>> subscribersByAgenda = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long medicoId, LocalDate fecha) {
        AgendaKey key = new AgendaKey(medicoId, fecha);
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        subscribersByAgenda
                .computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        Runnable removeCallback = () -> removeSubscriber(key, emitter);
        emitter.onCompletion(removeCallback);
        emitter.onTimeout(removeCallback);
        emitter.onError(error -> removeCallback.run());

        sendConnectedEvent(emitter, medicoId, fecha);
        return emitter;
    }

    public void sendInitialSnapshot(SseEmitter emitter, AgendaDinamicaResponse snapshot) {
        try {
            emitter.send(SseEmitter.event()
                    .id(nextEventId())
                    .name(EVENT_SNAPSHOT)
                    .data(snapshot));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    public void broadcastAgendaChanged(AgendaDinamicaChangedEvent changedEvent) {
        AgendaKey key = new AgendaKey(changedEvent.medicoId(), changedEvent.fecha());
        CopyOnWriteArrayList<SseEmitter> subscribers = subscribersByAgenda.get(key);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        AgendaChangeNotification payload = new AgendaChangeNotification(
                changedEvent.medicoId(),
                changedEvent.fecha(),
                changedEvent.citaId(),
                changedEvent.accion(),
                Instant.now()
        );

        for (SseEmitter emitter : subscribers) {
            try {
                emitter.send(SseEmitter.event()
                        .id(nextEventId())
                        .name(EVENT_UPDATED)
                        .data(payload));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    private void sendConnectedEvent(SseEmitter emitter, Long medicoId, LocalDate fecha) {
        try {
            emitter.send(SseEmitter.event()
                    .id(nextEventId())
                    .name(EVENT_CONNECTED)
                    .data(Map.of(
                            "medicoId", medicoId,
                            "fecha", fecha,
                            "connectedAt", Instant.now()
                    )));
        } catch (IOException ex) {
            emitter.completeWithError(ex);
        }
    }

    private void removeSubscriber(AgendaKey key, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> subscribers = subscribersByAgenda.get(key);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(emitter);
        if (subscribers.isEmpty()) {
            subscribersByAgenda.remove(key);
        }
    }

    private String nextEventId() {
        return Long.toString(eventSequence.incrementAndGet());
    }

    private record AgendaKey(Long medicoId, LocalDate fecha) {
    }

    private record AgendaChangeNotification(
            Long medicoId,
            LocalDate fecha,
            Long citaId,
            String accion,
            Instant changedAt
    ) {
    }
}

