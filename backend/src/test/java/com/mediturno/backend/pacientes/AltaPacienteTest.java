package com.mediturno.backend.pacientes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.auth.AuthService;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.pacientes.dto.AltaPacienteDTO;
import com.mediturno.backend.pacientes.dto.PacienteResponseDTO;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.usuarios.repository.UsuarioRepository;

/** CU-31 Alta individual de pacientes en el padrón. */
class AltaPacienteTest {

    private PacienteRepository pacientes;
    private UsuarioRepository usuarios;
    private PacienteService servicio;
    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void preparar() {
        pacientes = mock(PacienteRepository.class);
        usuarios = mock(UsuarioRepository.class);
        servicio = new PacienteService(pacientes, usuarios, mock(AuthService.class));
    }

    private AltaPacienteDTO datos(String cedula, String correo) {
        return new AltaPacienteDTO(cedula, " Ana ", " López ", correo, LocalDate.of(1988, 4, 12), "099123456", " Av. Italia 1234 ");
    }

    private Paciente guardado() {
        ArgumentCaptor<Paciente> captura = ArgumentCaptor.forClass(Paciente.class);
        verify(pacientes).saveAndFlush(captura.capture());
        return captura.getValue();
    }

    @Test
    void incorporaAlPacienteAlPadronSinCuenta() {
        PacienteResponseDTO respuesta = servicio.altaPaciente(datos("1.234.567-8", " Ana.Lopez@Correo.com "));

        Paciente paciente = guardado();
        assertEquals("1.234.567-8", paciente.getCedula());
        assertEquals("ana.lopez@correo.com", paciente.getCorreo());
        assertEquals("Ana", paciente.getNombre());
        assertEquals("López", paciente.getApellido());
        assertEquals("Av. Italia 1234", paciente.getDomicilio());
        assertEquals(EstadoActivacion.ACTIVO, paciente.getEstado());
        assertNotNull(paciente.getFechaCreacion());
        assertNotNull(paciente.getFechaAlta());
        assertNull(paciente.getFirebaseUid());
        assertNull(paciente.getProveedorAcceso());
        assertFalse(respuesta.tieneCuenta());
    }

    @Test
    void guardaLaCedulaSiempreConElMismoFormato() {
        servicio.altaPaciente(datos("12345678", "a@b.com"));
        assertEquals("1.234.567-8", guardado().getCedula());
    }

    @Test
    void aceptaCedulasDeSieteDigitos() {
        servicio.altaPaciente(datos("123456-7", "a@b.com"));
        assertEquals("123.456-7", guardado().getCedula());
    }

    @Test
    void sinTelefonoNiDomicilioLosGuardaVacios() {
        servicio.altaPaciente(new AltaPacienteDTO("1.234.567-8", "Ana", "López", "a@b.com", LocalDate.of(1988, 4, 12), " ", null));

        Paciente paciente = guardado();
        assertNull(paciente.getTelefono());
        assertNull(paciente.getDomicilio());
    }

    @Test
    void rechazaUnaCedulaConFormatoInvalido() {
        for (String cedula : new String[] {"abc", "123", "1234567890123"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos(cedula, "a@b.com")));
            assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
            assertTrue(error.getReason().startsWith("La cédula no tiene un formato válido"), cedula);
        }
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnCorreoConFormatoInvalidoEInformaAmbosSiHaceFalta() {
        ResponseStatusException soloCorreo = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("1.234.567-8", "sin-arroba")));
        assertEquals("El correo electrónico no tiene un formato válido.", soloCorreo.getReason());

        ResponseStatusException ambos = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("abc", "sin-arroba")));
        assertTrue(ambos.getReason().contains("La cédula") && ambos.getReason().contains("El correo"));
    }

    @Test
    void rechazaUnaCedulaYaRegistradaYAunqueSeEscribaDistinto() {
        when(usuarios.existsByCedula("1.234.567-8")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("12345678", "a@b.com")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("La cédula ya está registrada", error.getReason());
        verify(pacientes, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnCorreoYaRegistradoSinImportarMayusculas() {
        when(usuarios.existsByCorreoIgnoreCase("ana@correo.com")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("1.234.567-8", "ANA@correo.com")));

        assertEquals("El correo electrónico ya está registrado", error.getReason());
    }

    @Test
    void siSeRepitenLaCedulaYElCorreoLosInformaJuntos() {
        when(usuarios.existsByCedula("1.234.567-8")).thenReturn(true);
        when(usuarios.existsByCorreoIgnoreCase("a@b.com")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("1.234.567-8", "a@b.com")));

        assertEquals("La cédula y el correo electrónico ya están registrados", error.getReason());
    }

    @Test
    void siLaBaseDetectaUnDuplicadoConcurrenteResponde409() {
        when(pacientes.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_usuario_cedula"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaPaciente(datos("1.234.567-8", "a@b.com")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void informaTodosLosCamposObligatoriosQueFaltan() {
        Set<ConstraintViolation<AltaPacienteDTO>> faltas = validador.validate(new AltaPacienteDTO(" ", "", null, "", null, null, null));

        Set<String> campos = faltas.stream().map(f -> f.getPropertyPath().toString()).collect(Collectors.toSet());
        assertEquals(Set.of("cedula", "nombre", "apellido", "correo", "fechaNacimiento"), campos);
    }

    @Test
    void laFechaDeNacimientoNoPuedeSerFutura() {
        Set<ConstraintViolation<AltaPacienteDTO>> faltas = validador.validate(
            new AltaPacienteDTO("1.234.567-8", "Ana", "López", "a@b.com", LocalDate.now().plusDays(1), null, null));

        assertEquals(1, faltas.size());
    }
}
