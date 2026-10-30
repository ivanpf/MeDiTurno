package com.mediturno.backend.common.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.model.EstadoActivacion;

/**
 * Esqueleto de las cargas masivas por CSV (CU-29, CU-32, CU-35 y CU-38). Cada caso de uso declara sus columnas y le da la regla
 * de validación de una fila; acá se resuelve lo común: leer el archivo, reconocer los encabezados (sin importar tildes,
 * mayúsculas ni orden), avisar de las columnas obligatorias que faltan o de un archivo sin filas, marcar las filas que traen
 * más valores que columnas y armar el {@link ResultadoImportacionDTO} que se muestra en la previsualización.
 * <p>
 * Uso, en el servicio de cada entidad:
 * <pre>
 * Analisis&lt;Consultorio&gt; analisis = AnalizadorCsv.analizar(archivo, MAXIMO, "consultorios", COLUMNAS, (fila, errores) -&gt; { ... });
 * </pre>
 * La previsualización devuelve {@code analisis.resultado()}; la importación vuelve a analizar el archivo y llama a
 * {@link Analisis#importar}, que guarda todo o nada.
 */
public final class AnalizadorCsv {

    /** Una columna del archivo. {@code titulo} es el que se muestra; se reconoce sin tildes, mayúsculas ni espacios. */
    public record Columna(String titulo, boolean obligatoria) {

        public static Columna obligatoria(String titulo) {
            return new Columna(titulo, true);
        }

        public static Columna opcional(String titulo) {
            return new Columna(titulo, false);
        }

        String clave() {
            return LectorCsv.normalizar(titulo);
        }
    }

    /** Una fila de datos del archivo, para validar. */
    public static final class Fila {

        private final LectorCsv.FilaCsv fila;
        private final Map<String, Integer> indices;

        private Fila(LectorCsv.FilaCsv fila, Map<String, Integer> indices) {
            this.fila = fila;
            this.indices = indices;
        }

        /** Número de la fila en el archivo (el encabezado es la fila 1). */
        public int numero() {
            return fila.numero();
        }

        /** El valor de esa columna (la que se declaró con ese título), o vacío si el archivo no la trae. */
        public String valor(String titulo) {
            Integer indice = indices.get(LectorCsv.normalizar(titulo));
            return indice == null || indice >= fila.valores().size() ? "" : fila.valores().get(indice);
        }
    }

    /** Regla de validación de una fila: agrega a {@code errores} lo que esté mal y, si no hay errores, devuelve lo que se crearía. */
    @FunctionalInterface
    public interface Validador<T> {
        T validar(Fila fila, List<String> errores);
    }

    /**
     * Para detectar un valor repetido dentro del archivo: se marca la segunda aparición e informa la fila de la primera, aunque
     * la primera tuviera otros errores.
     */
    public static final class Repetidos {

        private final Map<String, Integer> vistos = new HashMap<>();

        /** Fila de la primera aparición del valor, o {@code null} si es la primera (la registra). Los valores vacíos no cuentan. */
        public Integer vistoEn(String valor, int numeroDeFila) {
            if (valor.isEmpty()) {
                return null;
            }
            Integer anterior = vistos.putIfAbsent(valor, numeroDeFila);
            return anterior;
        }
    }

    /** Lo que se sabe de un archivo analizado: lo que se le muestra a la persona y los registros que se crearían. */
    public record Analisis<T>(ResultadoImportacionDTO resultado, List<T> nuevos) {

        /**
         * Importa: si alguna fila tiene errores no se crea nada (422); si no, {@code guardar} recibe todos los registros y debe
         * guardarlos de una vez dentro de la transacción del servicio. Si la base rechaza alguno porque otra persona lo
         * registró en ese instante, responde 409 y no se crea nada.
         */
        public ResultadoImportacionDTO importar(Consumer<List<T>> guardar, String mensajeConErrores, String mensajeConflicto) {
            if (!resultado.valido()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, mensajeConErrores);
            }
            try {
                guardar.accept(nuevos);
            } catch (DataIntegrityViolationException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, mensajeConflicto);
            }
            return resultado.conRegistrosCreados(nuevos.size());
        }
    }

    private AnalizadorCsv() {
    }

    /**
     * Lee el archivo y valida cada fila con {@code validador}, sin guardar nada.
     *
     * @param registros cómo se llaman los registros del archivo, en plural y minúscula («consultorios»), para el aviso de
     *                  que el archivo solo trae el encabezado
     */
    public static <T> Analisis<T> analizar(MultipartFile archivo, int maximoDeFilas, String registros, List<Columna> columnas,
            Validador<T> validador) {
        LectorCsv.ContenidoCsv contenido;
        try {
            contenido = LectorCsv.leer(archivo, maximoDeFilas);
        } catch (LectorCsv.CsvInvalidoException e) {
            return conError(e.getMessage());
        }

        Map<String, Integer> indices = new HashMap<>();
        for (int i = 0; i < contenido.encabezados().size(); i++) {
            indices.putIfAbsent(LectorCsv.normalizar(contenido.encabezados().get(i)), i);
        }
        List<String> faltan = columnas.stream()
            .filter(c -> c.obligatoria() && !indices.containsKey(c.clave()))
            .map(c -> c.titulo().toLowerCase())
            .toList();
        if (!faltan.isEmpty()) {
            return conError("Faltan columnas obligatorias en el encabezado: " + String.join(", ", faltan) + ".");
        }
        if (contenido.filas().isEmpty()) {
            return conError("El archivo no tiene " + registros + ": solo contiene el encabezado.");
        }

        int cantidadDeEncabezados = contenido.encabezados().size();
        List<ResultadoImportacionDTO.Fila> filas = new ArrayList<>();
        List<T> nuevos = new ArrayList<>();
        for (LectorCsv.FilaCsv filaCsv : contenido.filas()) {
            Fila fila = new Fila(filaCsv, indices);
            List<String> errores = new ArrayList<>();
            if (filaCsv.valores().size() > cantidadDeEncabezados
                    && filaCsv.valores().subList(cantidadDeEncabezados, filaCsv.valores().size()).stream().anyMatch(v -> !v.isEmpty())) {
                errores.add("La fila tiene más valores que columnas en el encabezado");
            }
            T creado = validador.validar(fila, errores);
            if (errores.isEmpty() && creado != null) {
                nuevos.add(creado);
            }
            filas.add(new ResultadoImportacionDTO.Fila(fila.numero(), columnas.stream().map(c -> fila.valor(c.titulo())).toList(), errores));
        }
        return new Analisis<>(ResultadoImportacionDTO.deFilas(columnas.stream().map(Columna::titulo).toList(), filas), nuevos);
    }

    private static <T> Analisis<T> conError(String motivo) {
        return new Analisis<>(ResultadoImportacionDTO.conErrorDeArchivo(motivo), List.of());
    }

    /** «sí», «si», «s», «true», «1», «verdadero» y sus contrarios; nulo si no es ninguno. */
    public static Boolean interpretarSiNo(String texto) {
        return switch (LectorCsv.normalizar(texto)) {
            case "si", "s", "true", "verdadero", "1" -> Boolean.TRUE;
            case "no", "n", "false", "falso", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    /** «activo» o «activa», «inactivo» o «inactiva»; nulo si no es ninguno. */
    public static EstadoActivacion interpretarEstado(String texto) {
        return switch (LectorCsv.normalizar(texto)) {
            case "activo", "activa" -> EstadoActivacion.ACTIVO;
            case "inactivo", "inactiva" -> EstadoActivacion.INACTIVO;
            default -> null;
        };
    }
}
