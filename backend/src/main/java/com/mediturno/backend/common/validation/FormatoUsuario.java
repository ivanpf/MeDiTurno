package com.mediturno.backend.common.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

public final class FormatoUsuario {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // La fecha puede venir como 1985-03-27 (ISO) o como 27/03/1985 (la forma habitual en Uruguay).
    private static final DateTimeFormatter FECHA_CON_BARRAS = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private FormatoUsuario() {
    }

    public static String normalizarCedula(String texto) {
        if (texto == null) {
            return null;
        }
        String limpia = texto.trim();
        if (!limpia.matches("[0-9.\\-\\s]+")) {
            return null;
        }
        String digitos = limpia.replaceAll("[^0-9]", "");
        if (digitos.length() < 7 || digitos.length() > 8) {
            return null;
        }
        String cuerpo = digitos.substring(0, digitos.length() - 1);
        String cuerpoAlReves = new StringBuilder(cuerpo).reverse().toString().replaceAll("(\\d{3})(?=\\d)", "$1.");
        String conPuntos = new StringBuilder(cuerpoAlReves).reverse().toString();
        return conPuntos + "-" + digitos.charAt(digitos.length() - 1);
    }

    public static boolean correoValido(String correo) {
        return correo != null && CORREO.matcher(correo).matches();
    }

    /** Fecha en formato AAAA-MM-DD o DD/MM/AAAA (la que traen los CSV); nula si está vacía o no es una fecha real. */
    public static LocalDate interpretarFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpia = texto.trim();
        try {
            return limpia.contains("/") ? LocalDate.parse(limpia, FECHA_CON_BARRAS) : LocalDate.parse(limpia);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
