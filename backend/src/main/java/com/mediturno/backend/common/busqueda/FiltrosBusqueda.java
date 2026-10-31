package com.mediturno.backend.common.busqueda;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
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

    /**
     * Condición de búsqueda de una persona (paciente o usuario interno) por texto, o {@code null} si no hay nada que buscar:
     * la cédula, el nombre, el apellido y el correo, sin distinguir mayúsculas, y también «apellido nombre» y «nombre apellido».
     * La cédula se guarda como 1.234.567-8; quien escribe 1234567 también la encuentra.
     */
    public static Predicate coincidenciasDePersona(From<?, ?> persona, CriteriaBuilder cb, String texto) {
        String patron = patronContiene(texto);
        if (patron == null) {
            return null;
        }
        List<Predicate> coincidencias = new ArrayList<>(List.of(
            cb.like(cb.lower(persona.get("cedula")), patron, ESCAPE),
            cb.like(cb.lower(persona.get("nombre")), patron, ESCAPE),
            cb.like(cb.lower(persona.get("apellido")), patron, ESCAPE),
            cb.like(cb.lower(persona.get("correo")), patron, ESCAPE),
            cb.like(cb.lower(cb.concat(cb.concat(persona.get("apellido"), " "), persona.get("nombre"))), patron, ESCAPE),
            cb.like(cb.lower(cb.concat(cb.concat(persona.get("nombre"), " "), persona.get("apellido"))), patron, ESCAPE)));
        String digitos = texto.replaceAll("[.\\-\\s]", "");
        if (!digitos.isEmpty() && digitos.chars().allMatch(Character::isDigit)) {
            Expression<String> sinPuntos = cb.function("replace", String.class,
                cb.function("replace", String.class, persona.<String>get("cedula"), cb.literal("."), cb.literal("")),
                cb.literal("-"), cb.literal(""));
            coincidencias.add(cb.like(sinPuntos, "%" + digitos + "%"));
        }
        return cb.or(coincidencias.toArray(new Predicate[0]));
    }

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
     * 400 si pidió otra. {@code descendente} invierte el sentido. La propiedad se ordena con la escritura de {@code permitidas}
     * («fechaAlta»), aunque se haya pedido como «fechaalta».
     */
    public static Sort ordenar(String ordenarPor, boolean descendente, String... permitidas) {
        String propiedad = elegirOrden(ordenarPor, permitidas);
        Sort.Order orden = descendente ? Sort.Order.desc(propiedad) : Sort.Order.asc(propiedad);
        return Sort.by(orden.ignoreCase());
    }

    /**
     * La propiedad de {@code permitidas} que coincide con la pedida, sin distinguir mayúsculas (la primera si no se pidió
     * ninguna); 400 si pidió otra.
     */
    public static String elegirOrden(String ordenarPor, String... permitidas) {
        if (ordenarPor == null || ordenarPor.isBlank()) {
            return permitidas[0];
        }
        return Arrays.stream(permitidas)
            .filter(p -> p.equalsIgnoreCase(ordenarPor.trim()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El orden indicado no es válido (use " + enumerar(permitidas) + ")."));
    }

    /** El valor del enum (sin distinguir mayúsculas); {@code null} si no se indicó; 400 si no existe. */
    public static <E extends Enum<E>> E interpretarEstado(Class<E> tipo, String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "El estado indicado no es válido (use " + enumerar(Arrays.stream(tipo.getEnumConstants()).map(Enum::name).toArray(String[]::new)) + ").");
        }
    }

    private static String enumerar(String[] opciones) {
        if (opciones.length == 1) {
            return opciones[0];
        }
        return String.join(", ", Arrays.copyOf(opciones, opciones.length - 1)) + " o " + opciones[opciones.length - 1];
    }
}
