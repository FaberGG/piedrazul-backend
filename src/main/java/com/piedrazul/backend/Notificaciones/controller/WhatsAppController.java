package com.piedrazul.backend.Notificaciones.controller;

import com.piedrazul.backend.Notificaciones.dto.WhatsAppMessageRequest;
import com.piedrazul.backend.Notificaciones.services.WhatsAppMessageService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {
    
    private final WhatsAppMessageService whatsAppService;
    
    public WhatsAppController(WhatsAppMessageService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }
    
    @PostMapping("/send")
    public ResponseEntity<?> sendWhatsAppMessage(@Valid @RequestBody WhatsAppMessageRequest request) {
        try {
            var message = whatsAppService.sendTextMessage(request);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messageSid", message.getSid(),
                "to", request.getTo()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}