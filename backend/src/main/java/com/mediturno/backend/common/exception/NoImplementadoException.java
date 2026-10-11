package com.mediturno.backend.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Se lanza desde los métodos que todavía no se implementaron. Responde 501.
 * Al implementar un método, se elimina el {@code throw}.
 */
public class NoImplementadoException extends ResponseStatusException {

    public NoImplementadoException() {
        super(HttpStatus.NOT_IMPLEMENTED, "Funcionalidad pendiente de implementar");
    }
}
