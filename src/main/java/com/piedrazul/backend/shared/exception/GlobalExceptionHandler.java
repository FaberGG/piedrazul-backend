package com.piedrazul.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.piedrazul.backend.shared.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para respuestas de error estandarizadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erroresCampo = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                erroresCampo.putIfAbsent(error.getField(), error.getDefaultMessage()));

        String mensaje = erroresCampo.isEmpty()
                ? "Solicitud invalida"
                : "Solicitud invalida: " + erroresCampo.values().iterator().next();

        return buildResponse(HttpStatus.BAD_REQUEST, mensaje, erroresCampo);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String message,
                                                        Map<String, String> errors) {
        String safeMessage = (message == null || message.isBlank())
                ? status.getReasonPhrase()
                : message;

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .message(safeMessage)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}

