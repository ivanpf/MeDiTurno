package com.mediturno.backend.pases;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.consultas.repository.ConsultaRepository;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.pases.dto.EmitirPaseDTO;
import com.mediturno.backend.pases.model.EstadoPase;
import com.mediturno.backend.pases.model.Pase;
import com.mediturno.backend.pases.repository.PaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EmisionPaseTest {
    private final PaseRepository pases = mock(PaseRepository.class);
    private final ConsultaRepository consultas = mock(ConsultaRepository.class);
    private final EspecialidadRepository especialidades = mock(EspecialidadRepository.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-23T15:00:00Z"), ZoneId.of("America/Montevideo"));
    private final PaseService servicio = new PaseService(pases, reloj, consultas, especialidades);
    private final Consulta consulta = mock(Consulta.class, RETURNS_DEEP_STUBS);
    private final Especialidad origen = new Especialidad();
    private final Especialidad destino = new Especialidad();

    @BeforeEach
    void preparar() {
        origen.setCodigo("CARD");
        destino.setCodigo("TRAU");
        destino.setEstado(EstadoActivacion.ACTIVO);
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta));
        when(consulta.getIdConsulta()).thenReturn(10L);
        when(consulta.getMedico().getIdUsuario()).thenReturn(2L);
        when(consulta.getReserva().getOcurrenciaTurno().getTurnoSemanal().getEspecialidad()).thenReturn(origen);
        when(especialidades.findById("TRAU")).thenReturn(Optional.of(destino));
        when(pases.save(any(Pase.class))).thenAnswer(invocacion -> {
            Pase pase = invocacion.getArgument(0);
            pase.setIdPase(20L);
            return pase;
        });
    }

    @Test
    void conservaElOrigenDeLaConsultaYCalculaSesentaDiasConElRelojLocal() {
        var respuesta = servicio.emitirPase(2L, 10L, new EmitirPaseDTO("TRAU"));
        assertEquals("CARD", respuesta.especialidadOrigen());
        assertEquals("TRAU", respuesta.especialidadDestino());
        assertEquals(10L, respuesta.idConsulta());
        assertEquals(LocalDateTime.of(2026, 10, 23, 12, 0), respuesta.fechaEmision());
        assertEquals(respuesta.fechaEmision().plusDays(60), respuesta.fechaVigenciaCalculada());
        assertEquals(EstadoPase.VIGENTE, respuesta.paseEstado());
        verify(pases).save(argThat(p -> p.getEspecialidadOrigen() == origen && p.getEspecialidadDestino() == destino));
    }

    @Test
    void noEmiteSobreUnaConsultaAjena() {
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.emitirPase(3L, 10L, new EmitirPaseDTO("TRAU")));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(pases, never()).save(any());
    }

    @Test
    void noEmiteSiLaEspecialidadDejoDeEstarActiva() {
        destino.setEstado(EstadoActivacion.INACTIVO);
        var error = assertThrows(ResponseStatusException.class,
            () -> servicio.emitirPase(2L, 10L, new EmitirPaseDTO("TRAU")));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verify(pases, never()).save(any());
    }
}
