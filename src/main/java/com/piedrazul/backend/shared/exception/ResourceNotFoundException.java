package com.piedrazul.backend.shared.exception;

/**
 * Excepción para recursos no encontrados (HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " no encontrado(a) con id: " + id);
    }
}

