package com.mediturno.backend.especialidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.model.Especialidad;

/** CU-29 Alta masiva de especialidades médicas por CSV, con una base PostgreSQL real. */
class AltaMasivaEspecialidadesIntegracionTest extends PostgresIntegracionTest {

    private static final String ENCABEZADO = "código,nombre,descripción,requiere pase\n";

    @Autowired EspecialidadService servicio;

    @BeforeEach
    void yaRegistrada() {
        Especialidad e = new Especialidad();
        e.setCodigo("CM-EXI");
        e.setNombre("CM Existente");
        e.setRequierePase(false);
        e.setEstado(EstadoActivacion.ACTIVO);
        especialidades.saveAndFlush(e);
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "especialidades.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private ResultadoImportacionDTO previsualizar(String contenido) {
        return servicio.previsualizarEspecialidadesCSV(csv(contenido));
    }

    private List<String> erroresDeLaFila(ResultadoImportacionDTO r, int numero) {
        return r.filas().stream().filter(f -> f.numero() == numero).findFirst().orElseThrow().errores();
    }

    private boolean existe(String codigo) {
        return especialidades.existsById(codigo);
    }

    // ------------------------------------------------------------------ previsualización

    @Test
    void laPrevisualizacionMuestraLasFilasDetectadasSinGuardarNada() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "CM-A,CM Alfa,Primera,sí\nCM-B,CM Beta,,no\n");

        assertTrue(r.valido());
        assertEquals(List.of(), r.errores());
        assertEquals(List.of("Código", "Nombre", "Descripción", "Requiere pase", "Estado"), r.columnas());
        assertEquals(2, r.totalFilas());
        assertEquals(2, r.filasValidas());
        assertEquals(0, r.filasConErrores());
        assertEquals(0, r.registrosCreados());
        assertEquals(List.of("CM-A", "CM Alfa", "Primera", "sí", ""), r.filas().get(0).valores());
        assertEquals(2, r.filas().get(0).numero());
        assertEquals(3, r.filas().get(1).numero());
        assertFalse(existe("CM-A"));
    }

    @Test
    void aceptaPuntoYComaBomComillasYTildes() {
        String contenido = "﻿Código;Nombre;Descripción;Requiere pase;Estado\r\n"
            + "CM-A;\"CM Ginecología; obstetricia\";\"Dice \"\"hola\"\"\nen dos líneas\";Sí;Inactiva\r\n";

        ResultadoImportacionDTO r = previsualizar(contenido);

        assertTrue(r.valido(), r.filas().toString());
        assertEquals("CM Ginecología; obstetricia", r.filas().get(0).valores().get(1));
        assertEquals("Dice \"hola\"\nen dos líneas", r.filas().get(0).valores().get(2));
    }

    @Test
    void reconoceLosEncabezadosEscritosDeOtraManeraEnCualquierOrden() {
        ResultadoImportacionDTO r = previsualizar("REQUIERE_PASE,Descripcion,NOMBRE,CÓDIGO\nsi,Desc,CM Alfa,CM-A\n");

        assertTrue(r.valido());
        assertEquals(List.of("CM-A", "CM Alfa", "Desc", "si", ""), r.filas().get(0).valores());
    }

    @Test
    void ignoraLasFilasVaciasPeroCuentanParaElNumeroDeFila() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "\nCM-A,CM Alfa,,no\n\n,,,\nCM-B,,,no\n");

        assertEquals(2, r.totalFilas());
        assertEquals(List.of(3, 6), r.filas().stream().map(ResultadoImportacionDTO.Fila::numero).toList());
        assertEquals(List.of("El nombre es obligatorio"), erroresDeLaFila(r, 6));
    }

    @Test
    void cadaFilaConErroresIndicaLaFilaYElMotivo() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "CM-A,CM Alfa,,no\n"                       // fila 2: válida
            + ",,,\n"                                     // vacía: se ignora
            + ",CM Sin código,,no\n"                      // fila 4
            + "CM-B,,,no\n"                               // fila 5
            + "CM-C,CM Gama,,quizás\n"                    // fila 6
            + "CM-D,CM Delta,,\n"                         // fila 7
            + "CODIGO-DEMASIADO-LARGO-123,CM Épsilon,,no\n"
            + "CM-F," + "n".repeat(101) + ",,no\n"
            + "CM-G,CM Eta," + "d".repeat(501) + ",no\n");

        assertFalse(r.valido());
        assertEquals(8, r.totalFilas());
        assertEquals(1, r.filasValidas());
        assertEquals(7, r.filasConErrores());
        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("El código es obligatorio"), erroresDeLaFila(r, 4));
        assertEquals(List.of("El nombre es obligatorio"), erroresDeLaFila(r, 5));
        assertEquals(List.of("«Requiere pase» debe ser sí o no"), erroresDeLaFila(r, 6));
        assertEquals(List.of("Indique si requiere pase previo (sí o no)"), erroresDeLaFila(r, 7));
        assertEquals(List.of("El código admite hasta 20 caracteres"), erroresDeLaFila(r, 8));
        assertEquals(List.of("El nombre admite hasta 100 caracteres"), erroresDeLaFila(r, 9));
        assertEquals(List.of("La descripción admite hasta 500 caracteres"), erroresDeLaFila(r, 10));
    }

    @Test
    void unaFilaPuedeTenerVariosErrores() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + ",,,\n,,,x\n");

        assertEquals(List.of("El código es obligatorio", "El nombre es obligatorio", "«Requiere pase» debe ser sí o no"),
            erroresDeLaFila(r, 3));
    }

    @Test
    void marcaLosCodigosYNombresRepetidosEnElArchivoEnLaSegundaAparicion() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "CM-A,CM Alfa,,no\n"
            + "CM-A,CM Beta,,no\n"
            + "CM-C,cm alfa,,no\n");

        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("El código está repetido en el archivo (también en la fila 2)"), erroresDeLaFila(r, 3));
        assertEquals(List.of("El nombre está repetido en el archivo (también en la fila 2)"), erroresDeLaFila(r, 4));
    }

    @Test
    void marcaLosCodigosYNombresQueYaEstanRegistrados() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "CM-EXI,CM Otro,,no\n"
            + "CM-NUEVO,cm existente,,no\n");

        assertEquals(List.of("El código ya está en uso por otra especialidad"), erroresDeLaFila(r, 2));
        assertEquals(List.of("El nombre ya está en uso por otra especialidad"), erroresDeLaFila(r, 3));
    }

    @Test
    void unaFilaConMasValoresQueColumnasSeMarca() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "CM-A,CM Alfa,,no,sobra\n");

        assertEquals(List.of("La fila tiene más valores que columnas en el encabezado"), erroresDeLaFila(r, 2));
    }

    // ------------------------------------------------------------------ errores del archivo

    @Test
    void faltandoColumnasObligatoriasSeInformanTodas() {
        ResultadoImportacionDTO r = previsualizar("nombre,descripción\nCM Alfa,x\n");

        assertFalse(r.valido());
        assertEquals(List.of("Faltan columnas obligatorias en el encabezado: código, requiere pase."), r.errores());
        assertEquals(List.of(), r.filas());
    }

    @Test
    void unArchivoVacioOSoloConEncabezadoSeRechaza() {
        assertEquals(List.of("Seleccione un archivo CSV con contenido."), servicio.previsualizarEspecialidadesCSV(csv("")).errores());
        assertEquals(List.of("El archivo está vacío."), previsualizar("\n  \n").errores());
        assertEquals(List.of("El archivo no tiene especialidades: solo contiene el encabezado."), previsualizar(ENCABEZADO).errores());
    }

    @Test
    void unArchivoQueNoEstaEnUtf8SeRechaza() {
        MockMultipartFile latin1 = new MockMultipartFile("archivo", "e.csv", "text/csv",
            (ENCABEZADO + "CM-A,Cardiología,,no\n").getBytes(StandardCharsets.ISO_8859_1));

        ResultadoImportacionDTO r = servicio.previsualizarEspecialidadesCSV(latin1);

        assertFalse(r.valido());
        assertTrue(r.errores().get(0).startsWith("El archivo no está codificado en UTF-8"));
    }

    @Test
    void comillasSinCerrarSeRechazan() {
        assertEquals(List.of("El archivo tiene comillas sin cerrar."), previsualizar(ENCABEZADO + "CM-A,\"CM Alfa,,no\n").errores());
    }

    @Test
    void unArchivoConDemasiadasFilasSeRechaza() {
        StringBuilder grande = new StringBuilder(ENCABEZADO);
        for (int i = 0; i < 1001; i++) {
            grande.append("C").append(i).append(",Nombre ").append(i).append(",,no\n");
        }

        assertEquals(List.of("El archivo supera el máximo de 1000 filas por carga."), previsualizar(grande.toString()).errores());
    }

    // ------------------------------------------------------------------ importación

    @Test
    void importaTodasLasEspecialidadesConSusDatos() {
        ResultadoImportacionDTO r = servicio.altaEspecialidadesCSV(csv(
            "código,nombre,descripción,requiere pase,estado\n"
            + "CM-A,CM Alfa,Primera,sí,\n"
            + "CM-B,CM Beta,,no,inactiva\n"
            + "CM-C,CM Gama,Tercera,NO,Activo\n"));

        assertEquals(3, r.registrosCreados());
        assertTrue(r.valido());
        Especialidad a = especialidades.findById("CM-A").orElseThrow();
        assertEquals("CM Alfa", a.getNombre());
        assertEquals("Primera", a.getDescripcion());
        assertTrue(a.isRequierePase());
        assertEquals(EstadoActivacion.ACTIVO, a.getEstado()); // sin estado en el archivo: activa
        Especialidad b = especialidades.findById("CM-B").orElseThrow();
        assertNull(b.getDescripcion());
        assertFalse(b.isRequierePase());
        assertEquals(EstadoActivacion.INACTIVO, b.getEstado());
        assertFalse(especialidades.findById("CM-C").orElseThrow().isRequierePase());
    }

    @Test
    void siAlgunaFilaTieneErroresNoSeCreaNingunaYResponde422() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaEspecialidadesCSV(csv(ENCABEZADO + "CM-A,CM Alfa,,no\nCM-EXI,CM Otra,,no\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
        assertFalse(existe("CM-A"));
    }

    @Test
    void unArchivoInvalidoNoSeImporta() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaEspecialidadesCSV(csv("nombre\nCM Alfa\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
    }
}
