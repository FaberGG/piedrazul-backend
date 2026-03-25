package com.piedrazul.backend.shared.exception;

import com.piedrazul.backend.shared.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
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
        assertEquals("Medico no encontrado(a) con id: 99", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    void retorna422ConCuerpoLegible() {
        ResponseEntity<ErrorResponse> response =
                handler.handleBusinessRule(new BusinessRuleException("La fecha debe ser futura"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("La fecha debe ser futura", response.getBody().getMessage());
        assertNotNull(response.getBody().getTimestamp());
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
        assertTrue(response.getBody().getMessage().startsWith("Solicitud invalida"));
        assertEquals("La hora es obligatoria", response.getBody().getErrors().get("hora"));
        assertEquals("La fecha es obligatoria", response.getBody().getErrors().get("fecha"));
    }
}

