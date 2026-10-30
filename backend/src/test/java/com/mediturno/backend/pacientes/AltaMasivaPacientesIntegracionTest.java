package com.mediturno.backend.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;

/** CU-32 Alta masiva de pacientes por CSV, con una base PostgreSQL real. */
class AltaMasivaPacientesIntegracionTest extends PostgresIntegracionTest {

    private static final String ENCABEZADO = "cédula,nombre,apellido,correo,fecha de nacimiento,teléfono,domicilio\n";

    @Autowired PacienteService servicio;
    @Autowired PacienteRepository pacientes;

    @BeforeEach
    void yaRegistrados() {
        Paciente p = new Paciente();
        p.setCedula("5.400.099-9");
        p.setNombre("Existente");
        p.setApellido("Pm");
        p.setCorreo("pm.existente@ejemplo.com");
        p.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        p.setEstado(EstadoActivacion.ACTIVO);
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaAlta(LocalDateTime.now());
        pacientes.saveAndFlush(p);
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "pacientes.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private ResultadoImportacionDTO previsualizar(String contenido) {
        return servicio.previsualizarPacientesCSV(csv(contenido));
    }

    private List<String> erroresDeLaFila(ResultadoImportacionDTO r, int numero) {
        return r.filas().stream().filter(f -> f.numero() == numero).findFirst().orElseThrow().errores();
    }

    private boolean existe(String cedula) {
        return pacientes.findByCedula(cedula).isPresent();
    }

    // ------------------------------------------------------------------ previsualización

    @Test
    void laPrevisualizacionMuestraLasFilasDetectadasSinGuardarNada() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,099 123 456,Calle 1\n"
            + "5.400.002-2,Bruno,Pm Aguirre,pm.bruno@ejemplo.com,1985-03-27,,\n");

        assertTrue(r.valido());
        assertEquals(List.of(), r.errores());
        assertEquals(List.of("Cédula", "Nombre", "Apellido", "Correo", "Fecha de nacimiento", "Teléfono", "Domicilio"), r.columnas());
        assertEquals(2, r.totalFilas());
        assertEquals(2, r.filasValidas());
        assertEquals(0, r.filasConErrores());
        assertEquals(0, r.registrosCreados());
        assertEquals(List.of("5.400.001-1", "Ana", "Pm Rojas", "pm.ana@ejemplo.com", "1990-05-17", "099 123 456", "Calle 1"),
            r.filas().get(0).valores());
        assertEquals(2, r.filas().get(0).numero());
        assertEquals(3, r.filas().get(1).numero());
        assertFalse(existe("5.400.001-1"));
    }

    @Test
    void aceptaPuntoYComaBomComillasYTildes() {
        String contenido = "﻿Cédula;Nombre;Apellido;Correo;Fecha de nacimiento;Teléfono;Domicilio\r\n"
            + "5.400.001-1;Ana María;\"Pm; Rojas\";pm.ana@ejemplo.com;17/05/1990;;\"Av. Italia 1234\nApto 5\"\r\n";

        ResultadoImportacionDTO r = previsualizar(contenido);

        assertTrue(r.valido(), r.filas().toString());
        assertEquals("Pm; Rojas", r.filas().get(0).valores().get(2));
        assertEquals("Av. Italia 1234\nApto 5", r.filas().get(0).valores().get(6));
    }

    @Test
    void reconoceLosEncabezadosEscritosDeOtraManeraEnCualquierOrdenYSinLasColumnasOpcionales() {
        ResultadoImportacionDTO r = previsualizar("FECHA DE NACIMIENTO,CORREO,Apellido,NOMBRE,CEDULA\n1990-05-17,pm.ana@ejemplo.com,Pm Rojas,Ana,5.400.001-1\n");

        assertTrue(r.valido(), r.filas().toString());
        assertEquals(List.of("5.400.001-1", "Ana", "Pm Rojas", "pm.ana@ejemplo.com", "1990-05-17", "", ""), r.filas().get(0).valores());
    }

    @Test
    void aceptaLasFechasEnAmbosFormatos() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,\n"
            + "5.400.002-2,Bruno,Pm Aguirre,pm.bruno@ejemplo.com,27/03/1985,,\n"
            + "5.400.003-3,Carla,Pm Mendez,pm.carla@ejemplo.com,1/2/1990,,\n");

        assertTrue(r.valido(), r.filas().toString());
    }

    @Test
    void cadaFilaConErroresIndicaLaFilaYElMotivo() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,\n"            // fila 2: válida
            + ",,,,,,\n"                                                              // vacía: se ignora
            + ",Sin,Cédula,pm.sin@ejemplo.com,1990-05-17,,\n"                         // fila 4
            + "abc,Mala,Cédula,pm.mala@ejemplo.com,1990-05-17,,\n"                    // fila 5
            + "5.400.006-6,,Pm Sin nombre,pm.nn@ejemplo.com,1990-05-17,,\n"           // fila 6
            + "5.400.007-7,Sin,,pm.sa@ejemplo.com,1990-05-17,,\n"                     // fila 7
            + "5.400.008-8,Sin,Correo,,1990-05-17,,\n"                                // fila 8
            + "5.400.009-9,Mal,Correo,sin-arroba,1990-05-17,,\n"                      // fila 9
            + "5.400.010-0,Sin,Fecha,pm.sf@ejemplo.com,,,\n"                          // fila 10
            + "5.400.011-1,Mala,Fecha,pm.mf@ejemplo.com,31/02/1990,,\n"               // fila 11
            + "5.400.012-2,Otra,Fecha,pm.of@ejemplo.com,1990/05/17,,\n"               // fila 12
            + "5.400.013-3,Futura,Fecha,pm.ff@ejemplo.com," + HOY.plusDays(1) + ",,\n" // fila 13
            + "5.400.014-4," + "n".repeat(101) + ",Largo,pm.nl@ejemplo.com,1990-05-17,,\n"           // fila 14
            + "5.400.015-5,Largo," + "a".repeat(101) + ",pm.al@ejemplo.com,1990-05-17,,\n"          // fila 15
            + "5.400.016-6,Largo,Correo,p@" + "c".repeat(250) + ".com,1990-05-17,,\n"               // fila 16
            + "5.400.017-7,Largo,Telefono,pm.tl@ejemplo.com,1990-05-17," + "1".repeat(31) + ",\n"   // fila 17
            + "5.400.018-8,Largo,Domicilio,pm.dl@ejemplo.com,1990-05-17,," + "d".repeat(256) + "\n"); // fila 18

        assertFalse(r.valido());
        assertEquals(16, r.totalFilas());
        assertEquals(1, r.filasValidas());
        assertEquals(15, r.filasConErrores());
        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("La cédula es obligatoria"), erroresDeLaFila(r, 4));
        assertEquals(List.of("La cédula no tiene un formato válido (ejemplo: 1.234.567-8)"), erroresDeLaFila(r, 5));
        assertEquals(List.of("El nombre es obligatorio"), erroresDeLaFila(r, 6));
        assertEquals(List.of("El apellido es obligatorio"), erroresDeLaFila(r, 7));
        assertEquals(List.of("El correo electrónico es obligatorio"), erroresDeLaFila(r, 8));
        assertEquals(List.of("El correo electrónico no tiene un formato válido"), erroresDeLaFila(r, 9));
        assertEquals(List.of("La fecha de nacimiento es obligatoria"), erroresDeLaFila(r, 10));
        assertEquals(List.of("La fecha de nacimiento no tiene un formato válido (use AAAA-MM-DD o DD/MM/AAAA)"), erroresDeLaFila(r, 11));
        assertEquals(List.of("La fecha de nacimiento no tiene un formato válido (use AAAA-MM-DD o DD/MM/AAAA)"), erroresDeLaFila(r, 12));
        assertEquals(List.of("La fecha de nacimiento debe ser anterior a hoy"), erroresDeLaFila(r, 13));
        assertEquals(List.of("El nombre admite hasta 100 caracteres"), erroresDeLaFila(r, 14));
        assertEquals(List.of("El apellido admite hasta 100 caracteres"), erroresDeLaFila(r, 15));
        assertEquals(List.of("El correo electrónico admite hasta 255 caracteres"), erroresDeLaFila(r, 16));
        assertEquals(List.of("El teléfono admite hasta 30 caracteres"), erroresDeLaFila(r, 17));
        assertEquals(List.of("El domicilio admite hasta 255 caracteres"), erroresDeLaFila(r, 18));
    }

    @Test
    void unaFilaConVariosErroresLosInformaTodos() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "abc,,,sin-arroba,,,\n");

        assertEquals(List.of(
            "La cédula no tiene un formato válido (ejemplo: 1.234.567-8)",
            "El nombre es obligatorio",
            "El apellido es obligatorio",
            "El correo electrónico no tiene un formato válido",
            "La fecha de nacimiento es obligatoria"), erroresDeLaFila(r, 2));
    }

    @Test
    void marcaLasCedulasRepetidasEnElArchivoEnLaSegundaAparicionAunqueSeEscribanDistinto() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,\n"
            + "54000011,Bruno,Pm Aguirre,pm.bruno@ejemplo.com,1990-05-17,,\n"
            + "5.400.003-3,Carla,Pm Mendez,pm.carla@ejemplo.com,1990-05-17,,\n");

        assertEquals(List.of(), erroresDeLaFila(r, 2));
        assertEquals(List.of("La cédula está repetida en el archivo (también en la fila 2)"), erroresDeLaFila(r, 3));
        assertEquals(List.of(), erroresDeLaFila(r, 4));
    }

    @Test
    void marcaLosCorreosRepetidosEnElArchivoSinDistinguirMayusculas() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,\n"
            + "5.400.002-2,Bruno,Pm Aguirre,PM.Ana@Ejemplo.com,1990-05-17,,\n");

        assertEquals(List.of("El correo electrónico está repetido en el archivo (también en la fila 2)"), erroresDeLaFila(r, 3));
    }

    @Test
    void marcaLasCedulasYCorreosQueYaEstanRegistrados() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.099-9,Otro,Pm,pm.otro@ejemplo.com,1990-05-17,,\n"
            + "5.400.002-2,Otro,Pm,PM.Existente@ejemplo.com,1990-05-17,,\n");

        assertEquals(List.of("La cédula ya está registrada"), erroresDeLaFila(r, 2));
        assertEquals(List.of("El correo electrónico ya está registrado"), erroresDeLaFila(r, 3));
    }

    @Test
    void laCedulaYElCorreoDeCualquierRolCuentanComoRegistrados() {
        String correoDelMedico = medico("9891", EstadoActivacion.ACTIVO).getCorreo();
        funcionario("5.400.098-8");

        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.400.001-1,Ana,Pm Rojas," + correoDelMedico + ",1990-05-17,,\n"
            + "5.400.098-8,Bruno,Pm Aguirre,pm.bruno@ejemplo.com,1990-05-17,,\n");

        assertEquals(List.of("El correo electrónico ya está registrado"), erroresDeLaFila(r, 2));
        assertEquals(List.of("La cédula ya está registrada"), erroresDeLaFila(r, 3));
    }

    @Test
    void unaFilaConMasValoresQueColumnasSeMarca() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,,sobra\n");

        assertEquals(List.of("La fila tiene más valores que columnas en el encabezado"), erroresDeLaFila(r, 2));
    }

    // ------------------------------------------------------------------ errores del archivo

    @Test
    void faltandoColumnasObligatoriasSeInformanTodas() {
        ResultadoImportacionDTO r = previsualizar("nombre,apellido,teléfono\nAna,Pm,099\n");

        assertFalse(r.valido());
        assertEquals(List.of("Faltan columnas obligatorias en el encabezado: cédula, correo, fecha de nacimiento."), r.errores());
        assertEquals(List.of(), r.filas());
    }

    @Test
    void unArchivoVacioOSoloConEncabezadoSeRechaza() {
        assertEquals(List.of("Seleccione un archivo CSV con contenido."), servicio.previsualizarPacientesCSV(csv("")).errores());
        assertEquals(List.of("El archivo no tiene pacientes: solo contiene el encabezado."), previsualizar(ENCABEZADO).errores());
    }

    @Test
    void unArchivoQueNoEstaEnUtf8SeRechaza() {
        MockMultipartFile latin1 = new MockMultipartFile("archivo", "p.csv", "text/csv",
            (ENCABEZADO + "5.400.001-1,José,Pérez,pm.jose@ejemplo.com,1990-05-17,,\n").getBytes(StandardCharsets.ISO_8859_1));

        ResultadoImportacionDTO r = servicio.previsualizarPacientesCSV(latin1);

        assertFalse(r.valido());
        assertTrue(r.errores().get(0).startsWith("El archivo no está codificado en UTF-8"));
    }

    // ------------------------------------------------------------------ importación

    @Test
    void importaTodosLosPacientesConSusDatosYSinCuenta() {
        ResultadoImportacionDTO r = servicio.altaPacientesCSV(csv(ENCABEZADO
            + "5.400.001-1,  Ana María ,Pm Rojas,  PM.Ana@Ejemplo.com ,17/05/1990,099 123 456,Calle 1\n"
            + "54000022,Bruno,Pm Aguirre,pm.bruno@ejemplo.com,1985-03-27,,\n"));

        assertEquals(2, r.registrosCreados());
        assertTrue(r.valido());

        Paciente ana = pacientes.findByCedula("5.400.001-1").orElseThrow();
        assertEquals("Ana María", ana.getNombre());
        assertEquals("Pm Rojas", ana.getApellido());
        assertEquals("pm.ana@ejemplo.com", ana.getCorreo());
        assertEquals(LocalDate.of(1990, 5, 17), ana.getFechaNacimiento());
        assertEquals("099 123 456", ana.getTelefono());
        assertEquals("Calle 1", ana.getDomicilio());
        assertEquals(EstadoActivacion.ACTIVO, ana.getEstado());
        assertNull(ana.getFirebaseUid());
        assertNotNull(ana.getFechaAlta());
        assertNotNull(ana.getFechaCreacion());

        Paciente bruno = pacientes.findByCedula("5.400.002-2").orElseThrow(); // la cédula se guarda siempre con el mismo formato
        assertEquals(LocalDate.of(1985, 3, 27), bruno.getFechaNacimiento());
        assertNull(bruno.getTelefono());
        assertNull(bruno.getDomicilio());
        assertNull(bruno.getFirebaseUid());
    }

    @Test
    void siAlgunaFilaTieneErroresNoSeCreaNingunoYResponde422() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaPacientesCSV(csv(ENCABEZADO
                + "5.400.001-1,Ana,Pm Rojas,pm.ana@ejemplo.com,1990-05-17,,\n"
                + "5.400.099-9,Otro,Pm,pm.otro@ejemplo.com,1990-05-17,,\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
        assertFalse(existe("5.400.001-1"));
    }

    @Test
    void unArchivoInvalidoNoSeImporta() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.altaPacientesCSV(csv("nombre\nAna\n")));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode());
    }
}
