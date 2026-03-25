package com.piedrazul.backend.shared.exception;

import com.piedrazul.backend.shared.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void retorna404ConCuerpoLegible() {
        ResponseEntity<ErrorResponse> response =
                handler.handleNotFound(new ResourceNotFoundException("Medico no encontrado(a) con id: 99"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Medico no encontrado(a) con id: 99", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void retorna404ParaUsernameNoEncontrado() {
        ResponseEntity<ErrorResponse> response =
                handler.handleUsernameNotFound(new UsernameNotFoundException("Usuario no encontrado: demo"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Not Found", response.getBody().getError());
        assertEquals("Usuario no encontrado: demo", response.getBody().getMessage());
    }

    @Test
    void retorna422ConCuerpoLegible() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessRule(new BusinessRuleException("La fecha debe ser futura"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("Unprocessable Entity", response.getBody().getError());
        assertEquals("La fecha debe ser futura", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void retorna401ParaCredencialesInvalidas() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBadCredentials(new BadCredentialsException("mensaje interno"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Unauthorized", response.getBody().getError());
        assertEquals("Credenciales inválidas", response.getBody().getMessage());
    }

    @Test
    void retorna400ConErroresDeCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("crearCitaManualRequest", "hora", "La hora es obligatoria"),
                new FieldError("crearCitaManualRequest", "fecha", "La fecha es obligatoria")
        ));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Bad Request", response.getBody().getError());
        assertTrue(response.getBody().getMessage().startsWith("Solicitud invalida"));
        assertEquals("La hora es obligatoria", response.getBody().getErrors().get("hora"));
        assertEquals("La fecha es obligatoria", response.getBody().getErrors().get("fecha"));
    }

    @Test
    void retorna500ParaRuntimeGenerica() {
        ResponseEntity<ErrorResponse> response = handler.handleRuntime(new RuntimeException("detalle interno"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Internal Server Error", response.getBody().getError());
        assertEquals("Error interno del servidor", response.getBody().getMessage());
    }
}

