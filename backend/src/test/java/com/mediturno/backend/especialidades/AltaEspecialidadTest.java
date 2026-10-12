package com.mediturno.backend.especialidades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.especialidades.dto.AltaEspecialidadDTO;
import com.mediturno.backend.especialidades.dto.EspecialidadResponseDTO;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;

/** CU-28 Alta individual de especialidades médicas. */
class AltaEspecialidadTest {

    private EspecialidadRepository repositorio;
    private EspecialidadService servicio;
    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @BeforeEach
    void preparar() {
        repositorio = mock(EspecialidadRepository.class);
        servicio = new EspecialidadService(repositorio);
    }

    private AltaEspecialidadDTO datos(String codigo, String nombre) {
        return new AltaEspecialidadDTO(codigo, nombre, "  Atención del corazón ", true, EstadoActivacion.ACTIVO);
    }

    @Test
    void registraLaEspecialidadConLosDatosIngresados() {
        EspecialidadResponseDTO respuesta = servicio.altaEspecialidad(datos(" CARD ", " Cardiología "));

        ArgumentCaptor<Especialidad> guardada = ArgumentCaptor.forClass(Especialidad.class);
        verify(repositorio).saveAndFlush(guardada.capture());
        assertEquals("CARD", guardada.getValue().getCodigo());
        assertEquals("Cardiología", guardada.getValue().getNombre());
        assertEquals("Atención del corazón", guardada.getValue().getDescripcion());
        assertTrue(guardada.getValue().isRequierePase());
        assertEquals(EstadoActivacion.ACTIVO, guardada.getValue().getEstado());
        assertEquals(new EspecialidadResponseDTO("CARD", "Cardiología", "Atención del corazón", true, EstadoActivacion.ACTIVO), respuesta);
    }

    @Test
    void sinDescripcionLaGuardaVacia() {
        servicio.altaEspecialidad(new AltaEspecialidadDTO("PED", "Pediatría", "   ", false, EstadoActivacion.INACTIVO));

        ArgumentCaptor<Especialidad> guardada = ArgumentCaptor.forClass(Especialidad.class);
        verify(repositorio).saveAndFlush(guardada.capture());
        assertEquals(null, guardada.getValue().getDescripcion());
        assertEquals(EstadoActivacion.INACTIVO, guardada.getValue().getEstado());
    }

    @Test
    void rechazaUnCodigoDuplicadoIndicandoCualEs() {
        when(repositorio.existsById("CARD")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaEspecialidad(datos("CARD", "Cardiología")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("El código ya está en uso por otra especialidad", error.getReason());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void rechazaUnNombreDuplicadoSinImportarMayusculas() {
        when(repositorio.existsByNombreIgnoreCase("cardiología")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaEspecialidad(datos("CARD2", "cardiología")));

        assertEquals("El nombre ya está en uso por otra especialidad", error.getReason());
        verify(repositorio, never()).saveAndFlush(any());
    }

    @Test
    void siSeRepitenElCodigoYElNombreLosInformaJuntos() {
        when(repositorio.existsById("CARD")).thenReturn(true);
        when(repositorio.existsByNombreIgnoreCase("Cardiología")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaEspecialidad(datos("CARD", "Cardiología")));

        assertEquals("El código y el nombre ya están en uso por otras especialidades", error.getReason());
    }

    @Test
    void siLaBaseDetectaUnDuplicadoConcurrenteResponde409() {
        when(repositorio.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uq_especialidad_nombre"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> servicio.altaEspecialidad(datos("CARD", "Cardiología")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void informaTodosLosCamposObligatoriosQueFaltan() {
        Set<ConstraintViolation<AltaEspecialidadDTO>> faltas = validador.validate(new AltaEspecialidadDTO(" ", "", null, null, null));

        Set<String> campos = faltas.stream().map(f -> f.getPropertyPath().toString()).collect(Collectors.toSet());
        assertEquals(Set.of("codigo", "nombre", "requierePase", "estado"), campos);
    }

    @Test
    void laDescripcionEsOpcionalPeroTieneLimite() {
        assertTrue(validador.validate(new AltaEspecialidadDTO("A", "B", null, false, EstadoActivacion.ACTIVO)).isEmpty());
        assertFalse(validador.validate(new AltaEspecialidadDTO("A", "B", "x".repeat(501), false, EstadoActivacion.ACTIVO)).isEmpty());
    }
}
