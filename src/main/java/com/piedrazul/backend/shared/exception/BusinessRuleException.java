package com.piedrazul.backend.shared.exception;

/**
 * Excepción para violaciones de reglas de negocio (HTTP 422).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}

