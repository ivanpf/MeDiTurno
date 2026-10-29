package com.mediturno.backend.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.CorreoYaRegistradoEnProveedorException;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.common.seguridad.ProveedorIdentidadException;
import com.mediturno.backend.pacientes.dto.ModificarPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.model.Administrador;
import com.mediturno.backend.usuarios.repository.AdministradorRepository;

/** CU-44 Modificación de datos de pacientes, con una base PostgreSQL real. */
class ModificarPacienteIntegracionTest extends PostgresIntegracionTest {

    @Autowired PacienteService servicio;
    @Autowired PacienteRepository pacientes;
    @Autowired AdministradorRepository administradores;
    @MockitoBean ProveedorIdentidad proveedor;

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    private Administrador admin;
    private Paciente sinCuenta;
    private Paciente conCuenta;
    private Paciente otro;

    private Paciente crear(String cedula, String nombre, String apellido, String correo, String uid) {
        Paciente p = new Paciente();
        p.setCedula(cedula);
        p.setNombre(nombre);
        p.setApellido(apellido);
        p.setCorreo(correo);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 17));
        p.setTelefono("099 123 456");
        p.setDomicilio("Calle 1");
        p.setEstado(EstadoActivacion.ACTIVO);
        p.setFirebaseUid(uid);
        p.setFechaCreacion(LocalDateTime.now());
        p.setFechaAlta(LocalDateTime.now());
        return pacientes.saveAndFlush(p);
    }

    @BeforeEach
    void datosBase() {
        Administrador a = new Administrador();
        a.setCorreo("9861@ejemplo.com");
        a.setEstado(EstadoActivacion.ACTIVO);
        a.setFechaCreacion(LocalDateTime.now());
        a.setCedula("9861");
        a.setNombre("Admin");
        a.setApellido("Prueba");
        a.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        admin = administradores.save(a);
        sinCuenta = crear("5.300.001-1", "Ana", "Zmrojas", "zm.ana@ejemplo.com", null);
        conCuenta = crear("5.300.002-2", "Bruno", "Zmaguirre", "zm.bruno@ejemplo.com", "uid-zm-bruno");
        otro = crear("5.300.003-3", "Carla", "Zmmendez", "zm.carla@ejemplo.com", null);
    }

    private ModificarPacienteDTO datos(String nombre, String apellido, String correo, LocalDate nacimiento, String telefono, String domicilio) {
        return new ModificarPacienteDTO(nombre, apellido, correo, nacimiento, telefono, domicilio);
    }

    /** Los mismos datos que tiene la paciente sin cuenta, para cambiarle solo lo que cada prueba necesite. */
    private ModificarPacienteDTO igualesA(Paciente p) {
        return datos(p.getNombre(), p.getApellido(), p.getCorreo(), p.getFechaNacimiento(), p.getTelefono(), p.getDomicilio());
    }

    private PacienteResponseDTO modificar(Paciente p, ModificarPacienteDTO datos) {
        return servicio.modificarPaciente(admin.getIdUsuario(), p.getIdUsuario(), datos);
    }

    private HttpStatus errorAl(Paciente p, ModificarPacienteDTO datos) {
        return (HttpStatus) assertThrows(ResponseStatusException.class, () -> modificar(p, datos)).getStatusCode();
    }

    private Paciente guardado(Paciente p) {
        pacientes.flush();
        return pacientes.findById(p.getIdUsuario()).orElseThrow();
    }

    // ------------------------------------------------------------------ modificación

    @Test
    void modificaLosSeisCamposYDevuelveElPacienteActualizado() {
        PacienteResponseDTO r = modificar(sinCuenta,
            datos("Ana María", "Zmrojas Pérez", "zm.nueva@ejemplo.com", LocalDate.of(1991, 6, 18), "098 765 432", "Calle 2"));

        assertEquals(sinCuenta.getIdUsuario(), r.idPaciente());
        assertEquals("Ana María", r.nombre());
        assertEquals("Zmrojas Pérez", r.apellido());
        assertEquals("zm.nueva@ejemplo.com", r.correo());
        assertEquals(LocalDate.of(1991, 6, 18), r.fechaNacimiento());
        assertEquals("098 765 432", r.telefono());
        assertEquals("Calle 2", r.domicilio());
        Paciente p = guardado(sinCuenta);
        assertEquals("Ana María", p.getNombre());
        assertEquals("zm.nueva@ejemplo.com", p.getCorreo());
        assertEquals(LocalDate.of(1991, 6, 18), p.getFechaNacimiento());
        assertEquals("Calle 2", p.getDomicilio());
    }

    @Test
    void laCedulaNoCambiaNiElEstadoNiLaCuenta() {
        modificar(conCuenta, datos("Otro", "Nombre", "zm.otro.correo@ejemplo.com", LocalDate.of(1991, 1, 1), null, null));

        Paciente p = guardado(conCuenta);
        assertEquals("5.300.002-2", p.getCedula());
        assertEquals(EstadoActivacion.ACTIVO, p.getEstado());
        assertEquals("uid-zm-bruno", p.getFirebaseUid());
    }

    @Test
    void registraElAdministradorYLaFechaDelCambio() {
        assertNull(sinCuenta.getAdministradorModificacion());
        assertNull(sinCuenta.getFechaModificacion());
        LocalDateTime antes = LocalDateTime.now().minusMinutes(1);

        modificar(sinCuenta, datos("Ana María", "Zmrojas", "zm.ana@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        Paciente p = guardado(sinCuenta);
        assertEquals(admin.getIdUsuario(), p.getAdministradorModificacion().getIdUsuario());
        assertTrue(p.getFechaModificacion().isAfter(antes));
    }

    @Test
    void sinCambiosNoRegistraNada() {
        modificar(sinCuenta, igualesA(sinCuenta));

        Paciente p = guardado(sinCuenta);
        assertNull(p.getAdministradorModificacion());
        assertNull(p.getFechaModificacion());
    }

    @Test
    void recortaLosEspaciosYUnTelefonoODomicilioVaciosLosBorran() {
        modificar(sinCuenta, datos("  Ana ", " Zmrojas  ", "  ZM.Ana@Ejemplo.com ", LocalDate.of(1990, 5, 17), "   ", ""));

        Paciente p = guardado(sinCuenta);
        assertEquals("Ana", p.getNombre());
        assertEquals("Zmrojas", p.getApellido());
        assertNull(p.getTelefono());
        assertNull(p.getDomicilio());
        assertEquals("zm.ana@ejemplo.com", p.getCorreo());
    }

    @Test
    void unPacienteInactivoPuedeEditarseYSigueInactivo() {
        sinCuenta.setEstado(EstadoActivacion.INACTIVO);
        pacientes.saveAndFlush(sinCuenta);

        modificar(sinCuenta, datos("Ana María", "Zmrojas", "zm.ana@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        Paciente p = guardado(sinCuenta);
        assertEquals("Ana María", p.getNombre());
        assertEquals(EstadoActivacion.INACTIVO, p.getEstado());
    }

    @Test
    void unPacienteInexistenteResponde404() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.modificarPaciente(admin.getIdUsuario(), Long.MAX_VALUE, igualesA(sinCuenta)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    @Test
    void unaPersonaQueNoEsPacienteResponde404() {
        Long idFuncionario = funcionario("9862").getIdUsuario();

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
            () -> servicio.modificarPaciente(admin.getIdUsuario(), idFuncionario, igualesA(sinCuenta)));

        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // ------------------------------------------------------------------ correo

    @Test
    void elCorreoDeOtroPacienteResponde409() {
        ModificarPacienteDTO conCorreoAjeno = datos("Ana", "Zmrojas", "ZM.Carla@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1");

        assertEquals(HttpStatus.CONFLICT, errorAl(sinCuenta, conCorreoAjeno));
        assertEquals("zm.ana@ejemplo.com", guardado(sinCuenta).getCorreo());
    }

    @Test
    void elCorreoDeCualquierOtraPersonaDelSistemaTambienResponde409() {
        String correoDelFuncionario = funcionario("9863").getCorreo();

        assertEquals(HttpStatus.CONFLICT, errorAl(sinCuenta,
            datos("Ana", "Zmrojas", correoDelFuncionario, LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1")));
    }

    @Test
    void unCorreoConFormatoInvalidoResponde400() {
        assertEquals(HttpStatus.BAD_REQUEST, errorAl(sinCuenta,
            datos("Ana", "Zmrojas", "no-es-un-correo", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1")));
    }

    @Test
    void escribirElMismoCorreoConOtrasMayusculasNoEsUnCambio() {
        modificar(conCuenta, datos("Bruno Andrés", "Zmaguirre", "ZM.Bruno@Ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        assertEquals("zm.bruno@ejemplo.com", guardado(conCuenta).getCorreo());
        verify(proveedor, never()).actualizarCorreo(any(), any());
    }

    @Test
    void siElPacienteTieneCuentaElNuevoCorreoSeCambiaTambienEnElProveedor() {
        modificar(conCuenta, datos("Bruno", "Zmaguirre", "zm.bruno.nuevo@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        assertEquals("zm.bruno.nuevo@ejemplo.com", guardado(conCuenta).getCorreo());
        verify(proveedor).actualizarCorreo("uid-zm-bruno", "zm.bruno.nuevo@ejemplo.com");
    }

    @Test
    void siElPacienteNoTieneCuentaNoSeTocaElProveedor() {
        modificar(sinCuenta, datos("Ana", "Zmrojas", "zm.ana.nueva@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        verify(proveedor, never()).actualizarCorreo(any(), any());
    }

    @Test
    void siSoloCambiaOtroDatoNoSeTocaElProveedor() {
        modificar(conCuenta, datos("Bruno Andrés", "Zmaguirre", "zm.bruno@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1"));

        verify(proveedor, never()).actualizarCorreo(any(), any());
    }

    @Test
    void siElProveedorNoPuedeCambiarElCorreoResponde503() {
        doThrow(new ProveedorIdentidadException("caído")).when(proveedor).actualizarCorreo(any(), any());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, errorAl(conCuenta,
            datos("Bruno", "Zmaguirre", "zm.bruno.nuevo@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1")));
    }

    @Test
    void siElProveedorYaTieneEseCorreoResponde409() {
        doThrow(new CorreoYaRegistradoEnProveedorException()).when(proveedor).actualizarCorreo(any(), any());

        assertEquals(HttpStatus.CONFLICT, errorAl(conCuenta,
            datos("Bruno", "Zmaguirre", "zm.bruno.nuevo@ejemplo.com", LocalDate.of(1990, 5, 17), "099 123 456", "Calle 1")));
    }

    // ------------------------------------------------------------------ validación del cuerpo

    private Set<ConstraintViolation<ModificarPacienteDTO>> validar(ModificarPacienteDTO datos) {
        return validador.validate(datos);
    }

    @Test
    void sinLosCamposObligatoriosElCuerpoNoEsValido() {
        assertEquals(4, validar(datos(" ", "", null, null, null, null)).size());
    }

    @Test
    void validaLosLargosYQueLaFechaDeNacimientoSeaPasada() {
        assertTrue(validar(datos("n".repeat(100), "a".repeat(100), "c@ejemplo.com", LocalDate.of(1990, 1, 1), "1".repeat(30), "d".repeat(255))).isEmpty());
        assertEquals(6, validar(datos("n".repeat(101), "a".repeat(101), "c".repeat(256), LocalDate.now().plusDays(1), "1".repeat(31), "d".repeat(256))).size());
    }
}
