package com.mediturno.backend.consultorios;

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
import com.mediturno.backend.consultorios.model.Consultorio;

/** CU-38 Alta masiva de consultorios por CSV, con una base PostgreSQL real. */
class AltaMasivaConsultoriosIntegracionTest extends PostgresIntegracionTest {

    private static final String ENCABEZADO = "código,nombre,ubicación,descripción,estado\n";

    @Autowired ConsultorioService servicio;

    @BeforeEach
    void yaRegistrado() {
        consultorio("CM-EXI", EstadoActivacion.ACTIVO);
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "consultorios.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private ResultadoImportacionDTO previsualizar(String contenido) {
        return servicio.previsualizarConsultoriosCSV(csv(contenido));
    }

    private List<String> erroresDeLaFila(ResultadoImportacionDTO r, int numero) {
        return r.filas().stream().filter(f -> f.numero() == numero).findFirst().orElseThrow().errores();
    }

    private boolean existe(String codigo) {
        return consultorios.existsById(codigo);
    }

    // ------------------------------------------------------------------ previsualización

    @Test
    void laPrevisualizacionMuestraLasFilasDetectadasSinGuardarNada() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "CM-A,CM Alfa,Planta baja,Primero,activo\nCM-B,CM Beta,Piso 2,,inactivo\n");

        assertTrue(r.valido());
        assertEquals(List.of(), r.errores());
        assertEquals(List.of("Código", "Nombre", "Ubicación", "Descripción", "Estado"), r.columnas());
        assertEquals(2, r.totalFilas());
        assertEquals(2, r.filasValidas());
        assertEquals(0, r.filasConErrores());
        assertEquals(0, r.registrosCreados());
        assertEquals(List.of("CM-A", "CM Alfa", "Planta baja", "Primero", "activo"), r.filas().get(0).valores());
        assertEquals(2, r.filas().get(0).numero());
        assertEquals(3, r.filas().get(1).numero());
        assertFalse(existe("CM-A"));
    }

    @Test
    void aceptaPuntoYComaBomComillasYTildes() {
        String contenido = "﻿Código;Nombre;Ubicación;Descripción;Estado\r\n"
            + "CM-A;\"CM Sala; norte\";Planta baja;\"Dice \"\"hola\"\"\nen dos líneas\";Inactiva\r\n";

        ResultadoImportacionDTO r = previsualizar(contenido);

        assertTrue(r.valido(), r.filas().toString());
        assertEquals("CM Sala; norte", r.filas().get(0).valores().get(1));
        assertEquals("Dice \"hola\"\nen dos líneas", r.filas().get(0).valores().get(3));
    }

    @Test
    void reconoceLosEncabezadosEscritosDeOtraManeraEnCualquierOrden() {
        ResultadoImportacionDTO r = previsualizar("ESTADO,Descripcion,UBICACION,NOMBRE,CÓDIGO\nactiva,Desc,Piso 1,CM Alfa,CM-A\n");

        assertTrue(r.valido());
        assertEquals(List.of("CM-A", "CM Alfa", "Piso 1", "Desc", "activa"), r.filas().get(0).valores());
    }

    @Test
    void cadaFilaConErroresIndicaLaFilaYElMotivo() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "CM-A,CM Alfa,Piso 1,,activo\n"                        // fila 2: válida
            + ",,,,\n"                                               // vacía: se ignora
            + ",CM Sin código,Piso 1,,activo\n"                      // fila 4
            + "CM-B,,Piso 1,,activo\n"                               // fila 5
            + "CM-C,CM Gama,,,activo\n"                              // fila 6
            + "CM-D,CM Delta,Piso 1,,\n"                             // fila 7
            + "CM-E,CM Épsilon,Piso 1,,quizás\n"                     // fila 8
            + "CODIGO-DEMASIADO-LARGO-123,CM Zeta,Piso 1,,activo\n"  // fila 9
            + "CM-F," + "n".repeat(101) + ",Piso 1,,activo\n"        // fila 10
            + "CM-G,CM Eta," + "u".repeat(201) + ",,activo\n"        // fila 11
            + "CM-H,CM Theta,Piso 1," + "d".repeat(501) + ",activo\n");

        assertFalse(r.valido());
        assertEquals(10, r.totalFilas());
        assertEquals(1, r.filasValidas());
        assertEquals(9, r.filasConErrores());
        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("El código es obligatorio"), erroresDeLaFila(r, 4));
        assertEquals(List.of("El nombre es obligatorio"), erroresDeLaFila(r, 5));
        assertEquals(List.of("La ubicación es obligatoria"), erroresDeLaFila(r, 6));
        assertEquals(List.of("El estado es obligatorio (activo o inactivo)"), erroresDeLaFila(r, 7));
        assertEquals(List.of("El estado debe ser activo o inactivo"), erroresDeLaFila(r, 8));
        assertEquals(List.of("El código admite hasta 20 caracteres"), erroresDeLaFila(r, 9));
        assertEquals(List.of("El nombre admite hasta 100 caracteres"), erroresDeLaFila(r, 10));
        assertEquals(List.of("La ubicación admite hasta 200 caracteres"), erroresDeLaFila(r, 11));
        assertEquals(List.of("La descripción admite hasta 500 caracteres"), erroresDeLaFila(r, 12));
    }

    @Test
    void marcaLosCodigosRepetidosEnElArchivoEnLaSegundaAparicion() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "CM-A,CM Alfa,Piso 1,,activo\n"
            + "CM-A,CM Beta,Piso 1,,activo\n"
            + "CM-C,CM Alfa,Piso 1,,activo\n");

        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("El código está repetido en el archivo (también en la fila 2)"), erroresDeLaFila(r, 3));
        // El nombre no tiene que ser único: solo el código.
        assertEquals(List.of(), erroresDeLaFila(r, 4));
    }

    @Test
    void marcaLosCodigosQueYaEstanRegistrados() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "CM-EXI,CM Otro,Piso 1,,activo\n");

        assertEquals(List.of("El código ya está en uso por otro consultorio"), erroresDeLaFila(r, 2));
    }

    @Test
    void unaFilaConMasValoresQueColumnasSeMarca() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "CM-A,CM Alfa,Piso 1,,activo,sobra\n");

        assertEquals(List.of("La fila tiene más valores que columnas en el encabezado"), erroresDeLaFila(r, 2));
    }

    // ------------------------------------------------------------------ errores del archivo

    @Test
    void faltandoColumnasObligatoriasSeInformanTodas() {
        ResultadoImportacionDTO r = previsualizar("nombre,descripción\nCM Alfa,x\n");

        assertFalse(r.valido());
        assertEquals(List.of("Faltan columnas obligatorias en el encabezado: código, ubicación, estado."), r.errores());
        assertEquals(List.of(), r.filas());
    }

    @Test
    void unArchivoVacioOSoloConEncabezadoSeRechaza() {
        assertEquals(List.of("Seleccione un archivo CSV con contenido."), servicio.previsualizarConsultoriosCSV(csv("")).errores());
        assertEquals(List.of("El archivo no tiene consultorios: solo contiene el encabezado."), previsualizar(ENCABEZADO).errores());
    }

    @Test
    void unArchivoQueNoEstaEnUtf8SeRechaza() {
        MockMultipartFile latin1 = new MockMultipartFile("archivo", "c.csv", "text/csv",
            (ENCABEZADO + "CM-A,Sala,Planta baja,,activo\n").getBytes(StandardCharsets.ISO_8859_1));

        ResultadoImportacionDTO r = servicio.previsualizarConsultoriosCSV(latin1);

        assertFalse(r.valido());
        assertTrue(r.errores().get(0).startsWith("El archivo no está codificado en UTF-8"));
    }

    // ------------------------------------------------------------------ importación

    @Test
    void importaTodosLosConsultoriosConSusDatos() {
        ResultadoImportacionDTO r = servicio.altaConsultoriosCSV(csv(
            ENCABEZADO
            + "CM-A,CM Alfa,Planta baja,Primero,activo\n"
            + "CM-B,CM Beta,Piso 2,,inactivo\n"
            + "CM-C,CM Gama,Piso 3,Tercero,Activa\n"));

        assertEquals(3, r.registrosCreados());
        assertTrue(r.valido());
        Consultorio a = consultorios.findById("CM-A").orElseThrow();
        assertEquals("CM Alfa", a.getNombre());
        assertEquals("Planta baja", a.getUbicacion());
        assertEquals("Primero", a.getDescripcion());
        assertEquals(EstadoActivacion.ACTIVO, a.getEstado());
        Consultorio b = consultorios.findById("CM-B").orElseThrow();
        assertNull(b.getDescripcion());
        assertEquals(EstadoActivacion.INACTIVO, b.getEstado());
        assertEquals(EstadoActivacion.ACTIVO, consultorios.findById("CM-C").orElseThrow().getEstado());
    }

    @Test
    void siAlgunaFilaTieneErroresNoSeCreaNingunoYResponde422() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaConsultoriosCSV(csv(ENCABEZADO + "CM-A,CM Alfa,Piso 1,,activo\nCM-EXI,CM Otro,Piso 1,,activo\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
        assertFalse(existe("CM-A"));
    }

    @Test
    void unArchivoInvalidoNoSeImporta() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaConsultoriosCSV(csv("nombre\nCM Alfa\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
    }
}
