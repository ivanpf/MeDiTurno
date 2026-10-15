package com.mediturno.backend.common.validation;

import java.util.regex.Pattern;

public final class FormatoUsuario {

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

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
}
