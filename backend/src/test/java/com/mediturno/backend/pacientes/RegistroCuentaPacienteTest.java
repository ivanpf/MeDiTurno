package com.mediturno.backend.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.common.seguridad.CorreoYaRegistradoEnProveedorException;
import com.mediturno.backend.common.seguridad.IdentidadVerificada;
import com.mediturno.backend.common.seguridad.ProveedorIdentidad;
import com.mediturno.backend.common.seguridad.ProveedorIdentidadException;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.dto.RegistroPacienteDTO;
import com.mediturno.backend.pacientes.dto.VerificacionRegistroDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.model.ProveedorAcceso;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/** CU-7 Registro de cuenta de paciente. El proveedor de identidad (Firebase) es un doble. */
class RegistroCuentaPacienteTest {

    private static final String CORREO_DEL_PADRON = "padron@correo.com";

    private PacienteRepository pacientes;
    private UsuarioRepository usuarios;
    private ProveedorIdentidad proveedor;
    private PacienteService servicio;
    private Paciente delPadron;

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteRepository.class);
        usuarios = mock(UsuarioRepository.class);
        proveedor = mock(ProveedorIdentidad.class);
        servicio = new PacienteService(pacientes, usuarios, new AuthService(usuarios, proveedor));

        delPadron = new Paciente();
        delPadron.setIdUsuario(7L);
        delPadron.setCedula("1.234.567-8");
        delPadron.setCorreo(CORREO_DEL_PADRON);
        delPadron.setTelefono("099000000");
        delPadron.setDomicilio("Domicilio del padrón");
        delPadron.setEstado(EstadoActivacion.ACTIVO);
        when(pacientes.findByCedula("1.234.567-8")).thenReturn(Optional.of(delPadron));
    }

    private RegistroPacienteDTO conContrasena(String cedula, String correo, String contrasena) {
        return new RegistroPacienteDTO(cedula, ProveedorAcceso.PASSWORD, correo, contrasena, null, "098111222", " Av. Italia 1234 ");
    }

    private RegistroPacienteDTO conGoogle(String correo, String token) {
        return new RegistroPacienteDTO("1.234.567-8", ProveedorAcceso.GOOGLE, correo, null, token, null, null);
    }

    private ResponseStatusException rechazo(Runnable accion) {
        return assertThrows(ResponseStatusException.class, accion::run);
    }

    @Test
    void conCorreoYContrasenaCreaLaCuentaConElCorreoDelPadronYLaVincula() {
        when(proveedor.crearCuenta(CORREO_DEL_PADRON, "clave1234")).thenReturn("uid-nuevo");

        PacienteResponseDTO respuesta = servicio.registrarCuentaPaciente(conContrasena("12345678", " PADRON@correo.com ", "clave1234"));

        ArgumentCaptor<Paciente> guardado = ArgumentCaptor.forClass(Paciente.class);
        verify(pacientes).saveAndFlush(guardado.capture());
        assertEquals("uid-nuevo", guardado.getValue().getFirebaseUid());
        assertEquals(ProveedorAcceso.PASSWORD, guardado.getValue().getProveedorAcceso());
        assertEquals(CORREO_DEL_PADRON, guardado.getValue().getCorreo());
        assertEquals("098111222", guardado.getValue().getTelefono());
        assertEquals("Av. Italia 1234", guardado.getValue().getDomicilio());
        assertTrue(respuesta.tieneCuenta());
    }

    @Test
    void sinTelefonoNiDomicilioConservaLosDelPadron() {
        when(proveedor.crearCuenta(any(), any())).thenReturn("uid-nuevo");

        servicio.registrarCuentaPaciente(new RegistroPacienteDTO("1.234.567-8", ProveedorAcceso.PASSWORD, CORREO_DEL_PADRON, "clave1234", null, " ", null));

        assertEquals("099000000", delPadron.getTelefono());
        assertEquals("Domicilio del padrón", delPadron.getDomicilio());
    }

    @Test
    void conGoogleVinculaLaCuentaSiUsaElCorreoDelPadron() {
        when(proveedor.verificarIdentidad("token-google")).thenReturn(Optional.of(new IdentidadVerificada("uid-google", "Padron@Correo.com")));

        PacienteResponseDTO respuesta = servicio.registrarCuentaPaciente(conGoogle(CORREO_DEL_PADRON, "token-google"));

        assertEquals("uid-google", delPadron.getFirebaseUid());
        assertEquals(ProveedorAcceso.GOOGLE, delPadron.getProveedorAcceso());
        assertEquals(CORREO_DEL_PADRON, delPadron.getCorreo());
        assertTrue(respuesta.tieneCuenta());
        verify(proveedor, never()).crearCuenta(any(), any());
    }

    @Test
    void siLaCedulaNoEstaEnElPadronResponde422() {
        when(pacientes.findByCedula("9.999.999-9")).thenReturn(Optional.empty());

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("9.999.999-9", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(422, error.getStatusCode().value());
        verify(proveedor, never()).crearCuenta(any(), any());
    }

    @Test
    void siElCorreoNoCoincideConElDelPadronResponde422Igual() {
        ResponseStatusException cedulaAjena = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("9.999.999-9", CORREO_DEL_PADRON, "clave1234")));
        ResponseStatusException correoAjeno = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", "otro@correo.com", "clave1234")));

        assertEquals(422, correoAjeno.getStatusCode().value());
        // el mismo mensaje en los dos casos: no se revela cuál de los dos datos falló
        assertEquals(cedulaAjena.getReason(), correoAjeno.getReason());
        verify(proveedor, never()).crearCuenta(any(), any());
    }

    @Test
    void siYaTieneCuentaResponde409SinCrearOtra() {
        delPadron.setFirebaseUid("uid-existente");

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("Ya existe una cuenta"));
        verify(proveedor, never()).crearCuenta(any(), any());
    }

    @Test
    void siElProveedorNoPuedeCrearLaCuentaResponde503YNoVinculaNada() {
        when(proveedor.crearCuenta(any(), any())).thenThrow(new ProveedorIdentidadException("caído"));

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(pacientes, never()).saveAndFlush(any());
        assertNull(delPadron.getFirebaseUid());
    }

    @Test
    void siElCorreoYaTieneCuentaEnElProveedorResponde409() {
        when(proveedor.crearCuenta(any(), any())).thenThrow(new CorreoYaRegistradoEnProveedorException());

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void siNoSePuedeVincularBorraLaCuentaQueSeCreoEnElProveedor() {
        when(proveedor.crearCuenta(any(), any())).thenReturn("uid-huerfano");
        when(pacientes.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_usuario_firebase_uid"));

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(proveedor).eliminarCuenta("uid-huerfano");
    }

    @Test
    void siLaCuentaDeGoogleUsaOtroCorreoResponde422YBorraLaCuentaQueSeCreoAlAbrirGoogle() {
        when(proveedor.verificarIdentidad("token")).thenReturn(Optional.of(new IdentidadVerificada("uid-sin-dueño", "otra@gmail.com")));

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conGoogle(CORREO_DEL_PADRON, "token")));

        assertEquals(422, error.getStatusCode().value());
        assertTrue(error.getReason().contains("mismo correo"));
        verify(proveedor).eliminarCuenta("uid-sin-dueño");
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void siElTokenDeGoogleNoSePuedeVerificarResponde503() {
        when(proveedor.verificarIdentidad("malo")).thenReturn(Optional.empty());

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conGoogle(CORREO_DEL_PADRON, "malo")));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void unaCuentaDeGoogleYaVinculadaAOtraPersonaResponde409YNoLaBorra() {
        when(proveedor.verificarIdentidad("token")).thenReturn(Optional.of(new IdentidadVerificada("uid-ajeno", CORREO_DEL_PADRON)));
        when(usuarios.findByFirebaseUid("uid-ajeno")).thenReturn(Optional.of(new Paciente()));

        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conGoogle(CORREO_DEL_PADRON, "token")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(proveedor, never()).eliminarCuenta(any());
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void exigeLoQueCadaMetodoNecesita() {
        ResponseStatusException claveCorta = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "corta")));
        assertEquals("La contraseña debe tener al menos 6 caracteres.", claveCorta.getReason());

        ResponseStatusException sinToken = rechazo(() -> servicio.registrarCuentaPaciente(conGoogle(CORREO_DEL_PADRON, null)));
        assertEquals(HttpStatus.BAD_REQUEST, sinToken.getStatusCode());

        verify(proveedor, never()).crearCuenta(any(), any());
    }

    @Test
    void aceptaUnaContrasenaDeSeisCaracteres() {
        when(proveedor.crearCuenta(any(), any())).thenReturn("uid-nuevo");

        servicio.registrarCuentaPaciente(conContrasena("1.234.567-8", CORREO_DEL_PADRON, "abc123"));

        assertEquals("uid-nuevo", delPadron.getFirebaseUid());
    }

    @Test
    void laCedulaConFormatoInvalidoResponde400() {
        ResponseStatusException error = rechazo(() -> servicio.registrarCuentaPaciente(conContrasena("abc", CORREO_DEL_PADRON, "clave1234")));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void laVerificacionPreviaNoHaceNadaSiLaPersonaPuedeRegistrarse() {
        servicio.verificarPacienteEnPadron(new VerificacionRegistroDTO("12345678", CORREO_DEL_PADRON));

        verify(proveedor, never()).crearCuenta(any(), any());
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void laVerificacionPreviaRechazaLoMismoQueElRegistro() {
        assertEquals(422, rechazo(() -> servicio.verificarPacienteEnPadron(new VerificacionRegistroDTO("1.234.567-8", "otro@correo.com"))).getStatusCode().value());

        delPadron.setFirebaseUid("uid-existente");
        assertEquals(HttpStatus.CONFLICT, rechazo(() -> servicio.verificarPacienteEnPadron(new VerificacionRegistroDTO("1.234.567-8", CORREO_DEL_PADRON))).getStatusCode());
    }
}
