package com.mediturno.backend.pases.model;

import java.time.LocalDateTime;

/**
 * Vigencia de un pase, en días. Todos los pases valen lo mismo (60 días desde que se emiten);
 * si la regla cambia, se cambia acá.
 */
public enum VigenciaPase {

    MAXIMA(60);

    private final int dias;

    VigenciaPase(int dias) {
        this.dias = dias;
    }

    public int getDias() {
        return dias;
    }

    /** Último momento en que un pase emitido en {@code fechaEmision} sigue siendo válido. */
    public LocalDateTime vencimiento(LocalDateTime fechaEmision) {
        return fechaEmision.plusDays(dias);
    }
}
