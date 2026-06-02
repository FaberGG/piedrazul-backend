package com.piedrazul.backend.Notificaciones;

import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Configuration

public class NotificationConfig {  
    
    @Value("${twilio.account.sid}")
    private String accountSid;
    
    @Value("${twilio.auth.token}")
    private String authToken;
    
    @Value("${twilio.whatsapp.number}")
    private String whatsappNumber;
    
    @PostConstruct
    public void initTwilio() {
        Twilio.init(accountSid, authToken);
        System.out.println("✅ Twilio WhatsApp inicializado - Número: " + whatsappNumber);
    }
}