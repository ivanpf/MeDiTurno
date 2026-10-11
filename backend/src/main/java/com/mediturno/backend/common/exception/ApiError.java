package com.mediturno.backend.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato único de error que devuelve la API.
 *
 * @param estado   código HTTP
 * @param mensaje  mensaje para mostrar al usuario
 * @param detalles errores por campo (solo en validaciones), puede ser vacío
 * @param fecha    momento del error
 */
public record ApiError(int estado, String mensaje, List<String> detalles, Instant fecha) {

    public static ApiError of(int estado, String mensaje) {
        return new ApiError(estado, mensaje, List.of(), Instant.now());
    }
}
