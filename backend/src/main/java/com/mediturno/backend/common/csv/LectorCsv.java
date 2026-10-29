package com.mediturno.backend.common.csv;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

/**
 * Lee un archivo CSV en UTF-8 para las cargas masivas (CU-29, CU-32, CU-35 y CU-38). El separador (coma, punto y coma o
 * tabulación) se detecta en el encabezado, porque Excel en español guarda con punto y coma. Admite campos entre comillas
 * (con saltos de línea y comillas dobles escapadas) y un BOM al principio. Las filas totalmente vacías se ignoran, pero
 * cuentan para el número de fila, que es el que vería la persona en una planilla (el encabezado es la fila 1).
 */
public final class LectorCsv {

    /** Una fila de datos: su número en el archivo y sus valores (sin espacios en los extremos). */
    public record FilaCsv(int numero, List<String> valores) {
    }

    public record ContenidoCsv(List<String> encabezados, List<FilaCsv> filas) {
    }

    /** El archivo no se puede leer como CSV: el mensaje es para mostrar a quien lo cargó. */
    public static class CsvInvalidoException extends RuntimeException {
        public CsvInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    private LectorCsv() {
    }

    public static ContenidoCsv leer(MultipartFile archivo, int maximoDeFilas) {
        if (archivo == null || archivo.isEmpty()) {
            throw new CsvInvalidoException("Seleccione un archivo CSV con contenido.");
        }
        String texto = decodificar(archivo);
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        List<List<String>> registros = separarRegistros(texto, detectarSeparador(texto));

        int indiceEncabezado = -1;
        for (int i = 0; i < registros.size(); i++) {
            if (!estaVacio(registros.get(i))) {
                indiceEncabezado = i;
                break;
            }
        }
        if (indiceEncabezado < 0) {
            throw new CsvInvalidoException("El archivo está vacío.");
        }
        List<String> encabezados = registros.get(indiceEncabezado);
        List<FilaCsv> filas = new ArrayList<>();
        for (int i = indiceEncabezado + 1; i < registros.size(); i++) {
            if (!estaVacio(registros.get(i))) {
                filas.add(new FilaCsv(i + 1, registros.get(i)));
            }
        }
        if (filas.size() > maximoDeFilas) {
            throw new CsvInvalidoException("El archivo supera el máximo de " + maximoDeFilas + " filas por carga.");
        }
        return new ContenidoCsv(encabezados, filas);
    }

    /** Texto sin tildes ni mayúsculas ni separadores, para reconocer encabezados escritos de distintas maneras. */
    public static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private static String decodificar(MultipartFile archivo) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(archivo.getBytes()))
                .toString();
        } catch (CharacterCodingException e) {
            throw new CsvInvalidoException("El archivo no está codificado en UTF-8. Guárdelo como «CSV UTF-8» y vuelva a cargarlo.");
        } catch (IOException e) {
            throw new CsvInvalidoException("No fue posible leer el archivo.");
        }
    }

    /** El separador que más aparece en la primera línea con contenido (por defecto, la coma). */
    private static char detectarSeparador(String texto) {
        int fin = texto.indexOf('\n');
        String primera = texto.substring(texto.startsWith("﻿") ? 1 : 0, fin < 0 ? texto.length() : fin);
        long comas = primera.chars().filter(c -> c == ',').count();
        long puntosYComas = primera.chars().filter(c -> c == ';').count();
        long tabulaciones = primera.chars().filter(c -> c == '\t').count();
        if (puntosYComas > comas && puntosYComas >= tabulaciones) {
            return ';';
        }
        if (tabulaciones > comas) {
            return '\t';
        }
        return ',';
    }

    private static List<List<String>> separarRegistros(String texto, char separador) {
        List<List<String>> registros = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean entreComillas = false;
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (entreComillas) {
                if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
                    campo.append('"');
                    i++;
                } else if (c == '"') {
                    entreComillas = false;
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                entreComillas = true;
            } else if (c == separador) {
                actual.add(campo.toString().trim());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
                    i++;
                }
                actual.add(campo.toString().trim());
                campo.setLength(0);
                registros.add(actual);
                actual = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (entreComillas) {
            throw new CsvInvalidoException("El archivo tiene comillas sin cerrar.");
        }
        if (campo.length() > 0 || !actual.isEmpty()) {
            actual.add(campo.toString().trim());
            registros.add(actual);
        }
        return registros;
    }

    private static boolean estaVacio(List<String> registro) {
        return registro.stream().allMatch(String::isEmpty);
    }
}
