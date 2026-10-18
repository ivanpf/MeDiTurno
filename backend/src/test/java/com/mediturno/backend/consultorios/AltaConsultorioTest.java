package com.mediturno.backend.consultorios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import com.mediturno.backend.common.exception.GlobalExceptionHandler;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.dto.AltaConsultorioDTO;
import com.mediturno.backend.consultorios.repository.ConsultorioRepository;

class AltaConsultorioTest {
    private final ConsultorioRepository repositorio = mock(ConsultorioRepository.class);
    private final ConsultorioService servicio = new ConsultorioService(repositorio);

    @Test
    void camposObligatoriosInvalidosDevuelven400ConSuMensajeSinGuardar() {
        var casos = List.of(
                new AltaConsultorioDTO(null, "Piso 1", "Consultorio", null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("   ", "Piso 1", "Consultorio", null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("C1", null, "Consultorio", null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("C1", "   ", "Consultorio", null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("C1", "Piso 1", null, null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("C1", "Piso 1", "   ", null, EstadoActivacion.ACTIVO),
                new AltaConsultorioDTO("C1", "Piso 1", "Consultorio", null, null));
        for (var datos : casos) {
            var error = assertThrows(ResponseStatusException.class, () -> servicio.altaConsultorio(datos));
            var respuesta = new GlobalExceptionHandler().error(error);
            assertEquals(400, respuesta.getStatusCode().value());
            assertNotNull(error.getReason());
            assertEquals(error.getReason(), respuesta.getBody().mensaje());
        }
        verify(repositorio, never()).save(any());
    }

    @Test
    void codigoDuplicadoDevuelve409ConElMensajeSinGuardar() {
        when(repositorio.existsById("C1")).thenReturn(true);
        var datos = new AltaConsultorioDTO("C1", "Piso 1", "Consultorio", null, EstadoActivacion.ACTIVO);
        var error = assertThrows(ResponseStatusException.class, () -> servicio.altaConsultorio(datos));
        var respuesta = new GlobalExceptionHandler().error(error);
        assertEquals(409, respuesta.getStatusCode().value());
        assertEquals("El código ingresado ya corresponde a un consultorio existente.", respuesta.getBody().mensaje());
        verify(repositorio, never()).save(any());
    }

    @Test
    void datosValidosSiguenPermitiendoElAltaSinDescripcion() {
        when(repositorio.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        var respuesta = servicio.altaConsultorio(new AltaConsultorioDTO("C1", "Piso 1", "Consultorio", null, EstadoActivacion.ACTIVO));
        assertEquals("C1", respuesta.codigo());
        assertEquals("Consultorio", respuesta.nombre());
        assertEquals("Piso 1", respuesta.ubicacion());
        assertEquals(EstadoActivacion.ACTIVO, respuesta.estado());
        verify(repositorio).save(any());
    }
}
