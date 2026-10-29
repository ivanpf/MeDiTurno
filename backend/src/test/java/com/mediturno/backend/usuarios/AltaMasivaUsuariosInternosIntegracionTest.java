package com.mediturno.backend.usuarios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.common.dto.ResultadoImportacionDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.CorreoYaRegistradoEnProveedorException;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.notificaciones.EmailService;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.model.Usuario;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** CU-35 Alta masiva de usuarios internos por CSV, con una base PostgreSQL real. */
class AltaMasivaUsuariosInternosIntegracionTest extends BaseUsuariosInternos {

    private static final String ENCABEZADO = "cédula,nombre,apellido,correo,fecha de nacimiento,rol,matrícula,especialidades\n";

    @Autowired UsuarioInternoService servicio;
    @MockitoBean ProveedorIdentidad proveedor;
    @MockitoBean EmailService correo;

    @BeforeEach
    void datosBase() {
        especialidad("MS1");
        especialidad("MS2");
        Especialidad inactiva = especialidad("MS3");
        inactiva.setEstado(EstadoActivacion.INACTIVO);
        especialidades.saveAndFlush(inactiva);
        medicoInterno("5.640.099-9", "Existente", "Msexistente", "ms.existente@ejemplo.com", "MS-099", EstadoActivacion.ACTIVO,
            java.time.LocalDateTime.now());
        when(proveedor.crearCuenta(anyString(), anyString())).thenAnswer(i -> "uid-" + i.getArgument(0));
    }

    private MockMultipartFile csv(String contenido) {
        return new MockMultipartFile("archivo", "usuarios.csv", "text/csv", contenido.getBytes(StandardCharsets.UTF_8));
    }

    private ResultadoImportacionDTO previsualizar(String contenido) {
        return servicio.previsualizarUsuariosInternosCSV(csv(contenido));
    }

    /**
     * La prueba corre dentro de una transacción que se deshace sola y nunca llega al «commit», así que lo que el servicio deja
     * para después de confirmar (los correos) se ejecuta a mano. Si el servicio lanza una excepción no se ejecuta, igual que en
     * una transacción deshecha.
     */
    private ResultadoImportacionDTO importar(String contenido) {
        ResultadoImportacionDTO resultado = servicio.altaUsuariosInternosCSV(csv(contenido));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        return resultado;
    }

    private List<String> erroresDeLaFila(ResultadoImportacionDTO r, int numero) {
        return r.filas().stream().filter(f -> f.numero() == numero).findFirst().orElseThrow().errores();
    }

    private boolean existe(String correo) {
        return usuarios.existsByCorreoIgnoreCase(correo);
    }

    private static final String TRES_VALIDOS = ENCABEZADO
        + "5.640.001-1,Ana,Msrojas,ms.ana@ejemplo.com,1980-05-17,administrador,,\n"
        + "5.640.002-2,Bruno,Msaguirre,ms.bruno@ejemplo.com,27/03/1985,funcionario,,\n"
        + "5.640.003-3,Carla,Msmendez,ms.carla@ejemplo.com,1990-01-02,médico,MS-001,MS1|MS2\n";

    // ------------------------------------------------------------------ previsualización

    @Test
    void laPrevisualizacionMuestraLasFilasDetectadasSinGuardarNiCrearNada() {
        ResultadoImportacionDTO r = previsualizar(TRES_VALIDOS);

        assertTrue(r.valido(), r.filas().toString());
        assertEquals(List.of("Cédula", "Nombre", "Apellido", "Correo", "Fecha de nacimiento", "Rol", "Matrícula", "Especialidades"), r.columnas());
        assertEquals(3, r.totalFilas());
        assertEquals(3, r.filasValidas());
        assertEquals(0, r.registrosCreados());
        assertFalse(existe("ms.ana@ejemplo.com"));
        verify(proveedor, never()).crearCuenta(anyString(), anyString());
        verify(correo, never()).enviarCredencialesTemporales(anyString(), anyString());
    }

    @Test
    void aceptaPuntoYComaBomEncabezadosEnOtroOrdenYElRolEscritoDeOtraManera() {
        String contenido = "﻿ROL;CEDULA;Nombre;Apellido;Correo;Fecha de nacimiento;Matricula;Especialidades\r\n"
            + "Admin;5.640.001-1;Ana;Msrojas;ms.ana@ejemplo.com;17/05/1980;;\r\n"
            + "MEDICO;5.640.003-3;Carla;\"Ms; mendez\";ms.carla@ejemplo.com;1990-01-02;MS-001;\"MS1;MS2\"\r\n";

        ResultadoImportacionDTO r = previsualizar(contenido);

        assertTrue(r.valido(), r.filas().toString());
        assertEquals("Ms; mendez", r.filas().get(1).valores().get(2));
    }

    @Test
    void lasColumnasDeMatriculaYEspecialidadesSonOpcionalesSiNoHayMedicos() {
        ResultadoImportacionDTO r = previsualizar("cédula,nombre,apellido,correo,fecha de nacimiento,rol\n"
            + "5.640.001-1,Ana,Msrojas,ms.ana@ejemplo.com,1980-05-17,funcionario\n");

        assertTrue(r.valido(), r.filas().toString());
    }

    @Test
    void faltandoUnaColumnaObligatoriaSoloSeInformaElMotivo() {
        ResultadoImportacionDTO r = previsualizar("cédula,nombre,apellido,correo,fecha de nacimiento\n5.640.001-1,Ana,Msrojas,ms.ana@ejemplo.com,1980-05-17\n");

        assertFalse(r.valido());
        assertEquals(1, r.errores().size());
        assertTrue(r.errores().get(0).contains("rol"));
    }

    @Test
    void marcaLosCamposObligatoriosYLosFormatosInvalidos() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + ",Ana,,,,,,\n"
            + "no-es-cedula,Ana,Msrojas,sin-arroba,1980-13-45,jefe,,\n"
            + "5.640.001-1,Ana,Msrojas,ms.ana@ejemplo.com,2999-01-01,funcionario,,\n");

        assertFalse(r.valido());
        assertEquals(3, r.filasConErrores());
        assertTrue(erroresDeLaFila(r, 2).contains("La cédula es obligatoria"));
        assertTrue(erroresDeLaFila(r, 2).contains("El rol es obligatorio"));
        assertTrue(erroresDeLaFila(r, 3).stream().anyMatch(e -> e.contains("La cédula no tiene un formato válido")));
        assertTrue(erroresDeLaFila(r, 3).stream().anyMatch(e -> e.contains("correo electrónico no tiene un formato válido")));
        assertTrue(erroresDeLaFila(r, 3).stream().anyMatch(e -> e.contains("fecha de nacimiento no tiene un formato válido")));
        assertTrue(erroresDeLaFila(r, 3).stream().anyMatch(e -> e.contains("El rol no es válido")));
        assertTrue(erroresDeLaFila(r, 4).contains("La fecha de nacimiento debe ser anterior a hoy"));
    }

    @Test
    void marcaLosDuplicadosYaRegistradosYLosRepetidosEnElArchivo() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.640.099-9,Ana,Msrojas,ms.ana@ejemplo.com,1980-05-17,funcionario,,\n"
            + "5.640.002-2,Bruno,Msaguirre,ms.existente@ejemplo.com,1980-05-17,funcionario,,\n"
            + "5.640.003-3,Carla,Msmendez,ms.carla@ejemplo.com,1980-05-17,funcionario,,\n"
            + "5.640.003-3,Carlos,Msmendez,MS.CARLA@ejemplo.com,1980-05-17,funcionario,,\n");

        assertTrue(erroresDeLaFila(r, 2).contains("La cédula ya está registrada"));
        assertTrue(erroresDeLaFila(r, 3).contains("El correo electrónico ya está registrado"));
        assertEquals(List.of(), erroresDeLaFila(r, 4));
        assertTrue(erroresDeLaFila(r, 5).stream().anyMatch(e -> e.contains("cédula está repetida en el archivo (también en la fila 4)")));
        assertTrue(erroresDeLaFila(r, 5).stream().anyMatch(e -> e.contains("correo electrónico está repetido en el archivo (también en la fila 4)")));
    }

    @Test
    void validaLaMatriculaYLasEspecialidadesDeLosMedicos() {
        ResultadoImportacionDTO r = previsualizar(ENCABEZADO
            + "5.640.001-1,Ana,Msrojas,ms.ana@ejemplo.com,1980-05-17,medico,,MS1\n"
            + "5.640.002-2,Bruno,Msaguirre,ms.bruno@ejemplo.com,1980-05-17,medico,MS-002,\n"
            + "5.640.003-3,Carla,Msmendez,ms.carla@ejemplo.com,1980-05-17,medico,MS-099,MS1\n"
            + "5.640.004-4,Diego,Msdiaz,ms.diego@ejemplo.com,1980-05-17,medico,MS-003,MS1|NOEXISTE|MS3\n"
            + "5.640.005-5,Elena,Msestevez,ms.elena@ejemplo.com,1980-05-17,medico,ms-003,MS2\n"
            + "5.640.006-6,Fede,Msfuentes,ms.fede@ejemplo.com,1980-05-17,funcionario,MS-004,MS1\n");

        assertTrue(erroresDeLaFila(r, 2).contains("La matrícula es obligatoria para los médicos"));
        assertTrue(erroresDeLaFila(r, 3).contains("Los médicos necesitan al menos una especialidad"));
        assertTrue(erroresDeLaFila(r, 4).contains("La matrícula ya está registrada"));
        assertTrue(erroresDeLaFila(r, 5).contains("No existen las especialidades: NOEXISTE"));
        assertTrue(erroresDeLaFila(r, 5).contains("No están activas las especialidades: MS3"));
        assertTrue(erroresDeLaFila(r, 6).stream().anyMatch(e -> e.contains("matrícula está repetida en el archivo (también en la fila 5)")));
        assertTrue(erroresDeLaFila(r, 7).stream().anyMatch(e -> e.contains("Solo los médicos tienen matrícula y especialidades")));
    }

    @Test
    void unArchivoSinFilasOMalCodificadoSoloInformaElMotivo() {
        assertFalse(previsualizar(ENCABEZADO).valido());
        ResultadoImportacionDTO r = servicio.previsualizarUsuariosInternosCSV(
            new MockMultipartFile("archivo", "u.csv", "text/csv", new byte[] {(byte) 0xE9, (byte) 0xE9}));
        assertFalse(r.valido());
        assertTrue(r.errores().get(0).contains("UTF-8"));
    }

    // ------------------------------------------------------------------ importación

    @Test
    void importaCreaLasCuentasActivasConContrasenaTemporalYMandaLasCredenciales() {
        ResultadoImportacionDTO r = importar(TRES_VALIDOS);

        assertEquals(3, r.registrosCreados());
        Usuario ana = usuarios.findAll().stream().filter(u -> u.getCorreo().equals("ms.ana@ejemplo.com")).findFirst().orElseThrow();
        assertInstanceOf(Administrador.class, ana);
        assertEquals("5.640.001-1", ana.getCedula());
        assertEquals(EstadoActivacion.ACTIVO, ana.getEstado());
        assertEquals(ProveedorAcceso.PASSWORD, ana.getProveedorAcceso());
        assertEquals("uid-ms.ana@ejemplo.com", ana.getFirebaseUid());
        assertNotNull(ana.getFechaCreacion());
        assertInstanceOf(Funcionario.class, usuarios.findAll().stream().filter(u -> u.getCorreo().equals("ms.bruno@ejemplo.com")).findFirst().orElseThrow());
        Medico carla = assertInstanceOf(Medico.class, usuarios.findAll().stream().filter(u -> u.getCorreo().equals("ms.carla@ejemplo.com")).findFirst().orElseThrow());
        assertEquals("MS-001", carla.getMatricula());
        assertEquals(2, carla.getEspecialidades().size());
        verify(proveedor, times(3)).crearCuenta(anyString(), anyString());
        verify(correo).enviarCredencialesTemporales(eq("ms.ana@ejemplo.com"), anyString());
        verify(correo).enviarCredencialesTemporales(eq("ms.bruno@ejemplo.com"), anyString());
        verify(correo).enviarCredencialesTemporales(eq("ms.carla@ejemplo.com"), anyString());
    }

    @Test
    void cadaCuentaSeCreaConUnaContrasenaTemporalDistinta() {
        importar(TRES_VALIDOS);

        org.mockito.ArgumentCaptor<String> contrasenas = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(proveedor, times(3)).crearCuenta(anyString(), contrasenas.capture());
        assertEquals(3, contrasenas.getAllValues().stream().distinct().count());
        assertTrue(contrasenas.getAllValues().stream().allMatch(c -> c.length() >= 12));
    }

    @Test
    void conErroresEnAlgunaFilaNoSeCreaNadaYSeResponde422() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> importar(TRES_VALIDOS
            + "5.640.004-4,Diego,Msdiaz,sin-arroba,1980-05-17,funcionario,,\n"));

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, error.getStatusCode());
        assertFalse(existe("ms.ana@ejemplo.com"));
        verify(proveedor, never()).crearCuenta(anyString(), anyString());
        verify(correo, never()).enviarCredencialesTemporales(anyString(), anyString());
    }

    @Test
    void siElProveedorFallaEnUnaFilaNoSeCreaNadaYSeBorranLasCuentasYaCreadas() {
        when(proveedor.crearCuenta(eq("ms.bruno@ejemplo.com"), anyString())).thenThrow(new CorreoYaRegistradoEnProveedorException());

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> importar(TRES_VALIDOS));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("ms.bruno@ejemplo.com")); // dice en cuál cuenta se frenó
        verify(proveedor).eliminarCuenta("uid-ms.ana@ejemplo.com");
        verify(correo, never()).enviarCredencialesTemporales(anyString(), anyString());
    }

    @Test
    void siFallaUnCorreoLasCuentasQuedanCreadasYLosDemasCorreosSalen() {
        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "correo caído"))
            .when(correo).enviarCredencialesTemporales(eq("ms.bruno@ejemplo.com"), anyString());

        ResultadoImportacionDTO r = importar(TRES_VALIDOS);

        assertEquals(3, r.registrosCreados());
        assertTrue(existe("ms.bruno@ejemplo.com"));
        verify(proveedor, never()).eliminarCuenta(anyString());
        verify(correo).enviarCredencialesTemporales(eq("ms.ana@ejemplo.com"), anyString());
        verify(correo).enviarCredencialesTemporales(eq("ms.carla@ejemplo.com"), anyString());
    }

    @Test
    void ningunCorreoSaleAntesDeConfirmarLaImportacion() {
        servicio.altaUsuariosInternosCSV(csv(TRES_VALIDOS));

        verify(correo, never()).enviarCredencialesTemporales(anyString(), anyString());
    }

    @Test
    void superandoElMaximoDeFilasElArchivoSeRechaza() {
        StringBuilder grande = new StringBuilder(ENCABEZADO);
        for (int i = 0; i < 101; i++) {
            grande.append("5.641.").append(String.format("%03d", i)).append("-1,Nombre,Apellido,ms").append(i).append("@ejemplo.com,1980-01-01,funcionario,,\n");
        }

        ResultadoImportacionDTO r = previsualizar(grande.toString());

        assertFalse(r.valido());
        assertTrue(r.errores().get(0).contains("100"));
    }
}
