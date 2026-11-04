package com.mediturno.backend.pases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.consultas.model.Consulta;
import com.mediturno.backend.consultas.repository.ConsultaRepository;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.especialidades.repository.EspecialidadRepository;
import com.mediturno.backend.pases.dto.PaseResponseDTO;
import com.mediturno.backend.pases.model.EstadoPase;
import com.mediturno.backend.pases.model.Pase;
import com.mediturno.backend.pases.model.VigenciaPase;
import com.mediturno.backend.pases.repository.PaseRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class BusquedaPasesEmitidosTest {
    private final PaseRepository pases = mock(PaseRepository.class);
    private final PaseService servicio = new PaseService(pases, Clock.system(ZoneId.of("America/Montevideo")),
        mock(ConsultaRepository.class), mock(EspecialidadRepository.class));

    @Test
    void listaPasesDelMedicoConVigenciaCalculadaYOrdenMasRecientePrimero() {
        LocalDateTime fechaEmision = LocalDateTime.of(2026, 10, 23, 12, 0);
        Pase pase = paseDePrueba(fechaEmision);
        when(pases.findAll(ArgumentMatchers.<Specification<Pase>>any(), ArgumentMatchers.any(Sort.class)))
            .thenReturn(List.of(pase));

        List<PaseResponseDTO> respuesta = servicio.buscarPasesEmitidos(2L, CriteriosBusquedaDTO.vacio());

        assertEquals(1, respuesta.size());
        assertEquals(20L, respuesta.get(0).idPase());
        assertEquals(10L, respuesta.get(0).idConsulta());
        assertEquals("CARD", respuesta.get(0).especialidadOrigen());
        assertEquals("TRAU", respuesta.get(0).especialidadDestino());
        assertEquals(fechaEmision, respuesta.get(0).fechaEmision());
        assertEquals(VigenciaPase.MAXIMA.vencimiento(fechaEmision), respuesta.get(0).fechaVigenciaCalculada());
        assertEquals(EstadoPase.VIGENTE, respuesta.get(0).paseEstado());
        verify(pases).findAll(ArgumentMatchers.<Specification<Pase>>any(), eq(Sort.by(Sort.Direction.DESC, "fechaEmision")
            .and(Sort.by(Sort.Direction.DESC, "idPase"))));
    }

    @Test
    void rechazaUnRangoDeFechasInvertido() {
        CriteriosBusquedaDTO criterios = new CriteriosBusquedaDTO(null, null, null, null, null, null, null,
            LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 23));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> servicio.buscarPasesEmitidos(2L, criterios));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rechazaUnEstadoNoValido() {
        CriteriosBusquedaDTO criterios = new CriteriosBusquedaDTO(null, null, null, null, "PENDIENTE", null, null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> servicio.buscarPasesEmitidos(2L, criterios));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void soloPermiteOrdenarPorFecha() {
        CriteriosBusquedaDTO criterios = new CriteriosBusquedaDTO(null, null, null, null, null, "paciente", null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> servicio.buscarPasesEmitidos(2L, criterios));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    private static Pase paseDePrueba(LocalDateTime fechaEmision) {
        Especialidad origen = new Especialidad();
        origen.setCodigo("CARD");
        Especialidad destino = new Especialidad();
        destino.setCodigo("TRAU");
        Consulta consulta = mock(Consulta.class);
        when(consulta.getIdConsulta()).thenReturn(10L);

        Pase pase = new Pase();
        pase.setIdPase(20L);
        pase.setConsulta(consulta);
        pase.setEspecialidadOrigen(origen);
        pase.setEspecialidadDestino(destino);
        pase.setFechaEmision(fechaEmision);
        pase.setPaseEstado(EstadoPase.VIGENTE);
        return pase;
    }
}
