package com.piedrazul.backend.Notificaciones.services;

import com.piedrazul.backend.Notificaciones.dto.WhatsAppMessageRequest;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import com.twilio.Twilio;

@Service
public class WhatsAppMessageService {
    
    @Value("${twilio.account.sid}")
    private String accountSid;
    
    @Value("${twilio.auth.token}")
    private String authToken;
    
    @Value("${twilio.whatsapp.number}")
    private String fromWhatsappNumber;
    
    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }
    
    public Message sendTextMessage(WhatsAppMessageRequest request) {
        PhoneNumber to = new PhoneNumber("whatsapp:" + request.getTo());
        PhoneNumber from = new PhoneNumber("whatsapp:" + fromWhatsappNumber);
        
        Message message = Message.creator(to, from, request.getMessage()).create();
        
        System.out.println("📤 Mensaje enviado a: " + request.getTo());
        System.out.println("   SID: " + message.getSid());
        
        return message;
    }
}