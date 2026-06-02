package com.piedrazul.backend.Notificaciones.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${spring.mail.username}")
    private String fromEmail;

    private final MailSender mailSender;

    public EmailService(MailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarConfirmacionCita(String destinatario,
                                       String nombrePaciente,
                                       String fecha,
                                       String hora,
                                       String medico,
                                       String tipoCita) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(fromEmail);
        mensaje.setTo(destinatario);
        mensaje.setSubject("PIEDRAZUL - Confirmacion de Cita");
        mensaje.setText(
            "Estimado/a " + nombrePaciente + ",\n\n" +
            "Su cita ha sido confirmada:\n\n" +
            "Fecha:  " + fecha + "\n" +
            "Hora:   " + hora + "\n" +
            "Medico: Dr. " + medico + "\n" +
            "Tipo:   " + tipoCita + "\n\n" +
            "Presentese 15 minutos antes.\n" +
            "Consultas: (01) 123-4567\n\n" +
            "Gracias por confiar en PIEDRAZUL."
        );
        mailSender.send(mensaje);
    }
}