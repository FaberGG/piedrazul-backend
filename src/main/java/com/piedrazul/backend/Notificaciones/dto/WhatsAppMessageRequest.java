package com.piedrazul.backend.Notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class WhatsAppMessageRequest {
    
    @NotBlank
    @Pattern(regexp = "^\\+?[1-9][0-9]{7,14}$")
    private String to;
    
    @NotBlank
    private String message;
    
    private String mediaUrl;
    
    // Getters y Setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMediaUrl() {
        return mediaUrl;
    }
    
    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}