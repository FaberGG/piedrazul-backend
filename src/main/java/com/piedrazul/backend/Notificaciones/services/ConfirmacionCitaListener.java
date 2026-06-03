package com.piedrazul.backend.Notificaciones.services;

import com.piedrazul.backend.agenda.internal.event.ConfirmacionCitaEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ConfirmacionCitaListener {

    private final EmailService emailService;

    public ConfirmacionCitaListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener
    public void onConfirmacionCita(ConfirmacionCitaEvent event) {
        emailService.enviarConfirmacionCita(
                event.destinatario(),
                event.nombrePaciente(),
                event.fecha(),
                event.hora(),
                event.medicoNombre(),
                event.tipoCita()
        );
    }
}
