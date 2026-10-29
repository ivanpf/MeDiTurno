package com.mediturno.backend.common.busqueda;

import java.util.Arrays;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * Piezas comunes de los listados con búsqueda del administrador (CU-30, CU-33, CU-36 y CU-39): el texto a buscar, el estado
 * ACTIVO o INACTIVO y el orden. Cada servicio arma con ellas su {@code Specification}.
 */
public final class FiltrosBusqueda {

    private FiltrosBusqueda() {
    }

    /**
     * Patrón de LIKE para buscar el texto en cualquier parte (en minúsculas: se compara contra {@code lower(columna)}), o
     * {@code null} si no hay nada que buscar. Los «%» y «_» escritos se buscan como texto y no como comodines.
     */
    public static String patronContiene(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String escapado = texto.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escapado + "%";
    }

    /** El carácter de escape del patrón de {@link #patronContiene}, para pasárselo a {@code cb.like}. */
    public static final char ESCAPE = '\\';

    /** ACTIVO o INACTIVO (sin distinguir mayúsculas); {@code null} si no se indicó; 400 si es otra cosa. */
    public static EstadoActivacion interpretarEstado(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return EstadoActivacion.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado indicado no es válido (use ACTIVO o INACTIVO).");
        }
    }

    /**
     * Orden por una de las propiedades permitidas, sin distinguir mayúsculas (la primera es la que rige si no se pidió ninguna);
     * 400 si pidió otra. {@code descendente} invierte el sentido.
     */
    public static Sort ordenar(String ordenarPor, boolean descendente, String... permitidas) {
        String propiedad = ordenarPor == null || ordenarPor.isBlank() ? permitidas[0] : ordenarPor.trim().toLowerCase();
        if (!Arrays.asList(permitidas).contains(propiedad)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden indicado no es válido (use " + enumerar(permitidas) + ").");
        }
        Sort.Order orden = descendente ? Sort.Order.desc(propiedad) : Sort.Order.asc(propiedad);
        return Sort.by(orden.ignoreCase());
    }

    private static String enumerar(String[] opciones) {
        if (opciones.length == 1) {
            return opciones[0];
        }
        return String.join(", ", Arrays.copyOf(opciones, opciones.length - 1)) + " o " + opciones[opciones.length - 1];
    }
}
