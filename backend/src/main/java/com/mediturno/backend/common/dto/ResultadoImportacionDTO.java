package com.mediturno.backend.common.dto;

import java.util.List;

/**
 * Resultado de una carga masiva por CSV (CU-29, CU-32, CU-35 y CU-38), tanto de la previsualización como de la importación.
 * <ul>
 *   <li>{@code errores}: problemas del archivo en sí (vacío, mal codificado, faltan columnas). Si hay alguno, no hay filas.</li>
 *   <li>{@code columnas} y {@code filas}: lo detectado en el archivo, para mostrar en una tabla. Cada fila trae su número en
 *       el archivo (el encabezado es la fila 1), sus valores en el orden de las columnas y los errores que tiene.</li>
 *   <li>{@code valido}: se puede importar, es decir, hay filas y ninguna tiene errores.</li>
 *   <li>{@code registrosCreados}: cuántos registros se crearon; siempre 0 en la previsualización.</li>
 * </ul>
 */
public record ResultadoImportacionDTO(
        boolean valido,
        List<String> errores,
        List<String> columnas,
        List<Fila> filas,
        int totalFilas,
        int filasValidas,
        int filasConErrores,
        int registrosCreados) {

    public record Fila(int numero, List<String> valores, List<String> errores) {
    }

    /** Falló la lectura del archivo: solo se informa el motivo. */
    public static ResultadoImportacionDTO conErrorDeArchivo(String motivo) {
        return new ResultadoImportacionDTO(false, List.of(motivo), List.of(), List.of(), 0, 0, 0, 0);
    }

    /** Resultado de analizar las filas de un archivo que se pudo leer (no crea nada). */
    public static ResultadoImportacionDTO deFilas(List<String> columnas, List<Fila> filas) {
        int conErrores = (int) filas.stream().filter(f -> !f.errores().isEmpty()).count();
        return new ResultadoImportacionDTO(!filas.isEmpty() && conErrores == 0, List.of(), columnas, filas,
            filas.size(), filas.size() - conErrores, conErrores, 0);
    }

    /** El mismo resultado, ya importado. */
    public ResultadoImportacionDTO conRegistrosCreados(int cantidad) {
        return new ResultadoImportacionDTO(valido, errores, columnas, filas, totalFilas, filasValidas, filasConErrores, cantidad);
    }
}
