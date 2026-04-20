package com.piedrazul.backend.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.AssertionFailure;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String MENSAJE_ERROR_INTERNO = "Error interno del servidor";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", null);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                "La agenda fue modificada concurrentemente. Intente nuevamente",
                null);
    }

    @ExceptionHandler(AssertionFailure.class)
    public ResponseEntity<ErrorResponse> handleHibernateAssertion(AssertionFailure ex) {
        log.warn("Conflicto de concurrencia detectado por Hibernate", ex);
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY,
                "Conflicto de concurrencia al agendar. Intente nuevamente",
                null);
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

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Error de ejecución no controlado", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Error inesperado no controlado", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_ERROR_INTERNO, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status,
                                                        String message,
                                                        Map<String, String> errors) {
        String safeMessage = (message == null || message.isBlank())
                ? status.getReasonPhrase()
                : message;

        ErrorResponse body = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(safeMessage)
                .timestamp(LocalDateTime.now())
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(body);
    }
}

