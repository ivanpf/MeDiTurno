package com.mediturno.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.exception.GlobalExceptionHandler;

class GlobalExceptionHandlerTest {

    @Test
    void devuelveElEstadoYElMensajeDeLaExcepcion() {
        var respuesta = new GlobalExceptionHandler()
            .error(new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"));

        assertEquals(404, respuesta.getStatusCode().value());
        assertEquals("No existe", respuesta.getBody().mensaje());
    }

    @Test
    void unCuerpoQueNoSePuedeLeerDevuelve400() {
        var respuesta = new GlobalExceptionHandler().cuerpoIlegible(
            new HttpMessageNotReadableException("JSON mal formado", (HttpInputMessage) null));

        assertEquals(400, respuesta.getStatusCode().value());
        assertEquals("Hay datos inválidos", respuesta.getBody().mensaje());
    }

    @Test
    void unErrorInesperadoDevuelve500() {
        var respuesta = new GlobalExceptionHandler().error(new IllegalStateException("falla"));

        assertEquals(500, respuesta.getStatusCode().value());
    }
}
