package com.mediturno.backend.turnos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TipoSuspension;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.usuarios.model.Medico;

/** CU-20 Búsqueda y listado de turnos semanales, con una base PostgreSQL real. */
class BusquedaTurnosIntegracionTest extends PostgresIntegracionTest {

    @Autowired TurnoSemanalService servicio;

    private Medico gomez;
    private Medico rossi;
    private Especialidad cardiologia;
    private Especialidad pediatria;
    private Consultorio c1;
    private Consultorio c2;
    private TurnoSemanal gomezLunesManana;
    private TurnoSemanal gomezMartesTarde;
    private TurnoSemanal rossiLunesTarde;
    private TurnoSemanal rossiViernes;

    @BeforeEach
    void datosBase() {
        cardiologia = especialidad("CAR");
        pediatria = especialidad("PED");
        c1 = consultorio("B1", EstadoActivacion.ACTIVO);
        c2 = consultorio("B2", EstadoActivacion.ACTIVO);
        gomez = medico("9601", EstadoActivacion.ACTIVO, cardiologia, pediatria);
        rossi = medico("9602", EstadoActivacion.ACTIVO, pediatria);
        LocalDate desde = HOY.minusDays(30);
        gomezLunesManana = turno(gomez, cardiologia, c1, DiaSemana.LUNES, "09:00", "12:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        gomezMartesTarde = turno(gomez, pediatria, c2, DiaSemana.MARTES, "14:00", "18:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        rossiLunesTarde = turno(rossi, pediatria, c2, DiaSemana.LUNES, "14:00", "18:00", desde, null, EstadoTurnoSemanal.SUSPENDIDO);
        rossiViernes = turno(rossi, pediatria, c1, DiaSemana.VIERNES, "08:00", "10:00", desde, null, EstadoTurnoSemanal.ACTIVO);
    }

    private CriteriosBusquedaDTO criterios(Long idMedico, String especialidad, String dia, String consultorio, String estado,
            String ordenarPor, Boolean descendente) {
        return new CriteriosBusquedaDTO(idMedico, especialidad, dia, consultorio, estado, ordenarPor, descendente);
    }

    private List<Long> ids(List<TurnoSemanalResponseDTO> lista) {
        return lista.stream().map(TurnoSemanalResponseDTO::idTurnoSemanal).toList();
    }

    @Test
    void sinCriteriosDevuelveTodosPorDiaHorarioYMedico() {
        List<TurnoSemanalResponseDTO> lista = servicio.buscarTurnosSemanales(CriteriosBusquedaDTO.vacio());

        assertEquals(List.of(gomezLunesManana, rossiLunesTarde, gomezMartesTarde, rossiViernes).stream().map(TurnoSemanal::getIdTurnoSemanal).toList(), ids(lista));
    }

    @Test
    void muestraLosDatosQuePideElCasoDeUso() {
        TurnoSemanalResponseDTO t = servicio.buscarTurnosSemanales(criterios(gomez.getIdUsuario(), "CAR", null, null, null, null, null)).get(0);

        assertEquals("Apellido9601, Nombre9601", t.nombreMedico());
        assertEquals("Especialidad CAR", t.nombreEspecialidad());
        assertEquals(DiaSemana.LUNES, t.dia());
        assertEquals("09:00", t.horaInicio().toString());
        assertEquals("12:00", t.horaFin().toString());
        assertEquals("B1", t.codigoConsultorio());
        assertEquals(10, t.cupoMaximo());
        assertEquals(EstadoTurnoSemanal.ACTIVO, t.estado());
        assertNull(t.suspension());
    }

    @Test
    void filtraPorCadaCriterio() {
        assertEquals(List.of(gomezLunesManana.getIdTurnoSemanal(), gomezMartesTarde.getIdTurnoSemanal()),
            ids(servicio.buscarTurnosSemanales(criterios(gomez.getIdUsuario(), null, null, null, null, null, null))));
        assertEquals(List.of(gomezLunesManana.getIdTurnoSemanal()),
            ids(servicio.buscarTurnosSemanales(criterios(null, "CAR", null, null, null, null, null))));
        assertEquals(List.of(gomezLunesManana.getIdTurnoSemanal(), rossiLunesTarde.getIdTurnoSemanal()),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, "lunes", null, null, null, null))));
        assertEquals(List.of(gomezLunesManana.getIdTurnoSemanal(), rossiViernes.getIdTurnoSemanal()),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, null, "B1", null, null, null))));
        assertEquals(List.of(rossiLunesTarde.getIdTurnoSemanal()),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, null, null, "SUSPENDIDO", null, null))));
    }

    @Test
    void combinaLosCriterios() {
        List<TurnoSemanalResponseDTO> lista = servicio.buscarTurnosSemanales(criterios(rossi.getIdUsuario(), "PED", "LUNES", "B2", "SUSPENDIDO", null, null));

        assertEquals(List.of(rossiLunesTarde.getIdTurnoSemanal()), ids(lista));
    }

    @Test
    void sinCoincidenciasDevuelveUnaListaVacia() {
        assertTrue(servicio.buscarTurnosSemanales(criterios(gomez.getIdUsuario(), "CAR", "VIERNES", null, null, null, null)).isEmpty());
    }

    @Test
    void ordenaPorElCriterioElegidoYEnAmbosSentidos() {
        assertEquals(List.of(gomezLunesManana, gomezMartesTarde, rossiLunesTarde, rossiViernes).stream().map(TurnoSemanal::getIdTurnoSemanal).toList(),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, null, null, null, "medico", null))));
        assertEquals(List.of(rossiViernes, gomezLunesManana, rossiLunesTarde, gomezMartesTarde).stream().map(TurnoSemanal::getIdTurnoSemanal).toList(),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, null, null, null, "horario", null))));
        assertEquals(List.of(rossiViernes, gomezMartesTarde, rossiLunesTarde, gomezLunesManana).stream().map(TurnoSemanal::getIdTurnoSemanal).toList(),
            ids(servicio.buscarTurnosSemanales(criterios(null, null, null, null, null, "dia", true))));
    }

    @Test
    void indicaLaSuspensionRegistradaDeCadaTurno() {
        LocalDate desde = HOY.plusDays(3);
        suspension(gomezLunesManana, funcionario("9603"), TipoSuspension.TEMPORAL, desde, desde.plusDays(10));

        List<TurnoSemanalResponseDTO> lista = servicio.buscarTurnosSemanales(CriteriosBusquedaDTO.vacio());

        TurnoSemanalResponseDTO conSuspension = lista.stream().filter(t -> t.idTurnoSemanal().equals(gomezLunesManana.getIdTurnoSemanal())).findFirst().orElseThrow();
        assertEquals(TipoSuspension.TEMPORAL, conSuspension.suspension().modalidad());
        assertEquals(desde, conSuspension.suspension().fechaDesde());
        assertEquals(desde.plusDays(10), conSuspension.suspension().fechaHasta());
        assertEquals(EstadoTurnoSemanal.ACTIVO, conSuspension.estado()); // todavía no llegó la fecha desde
        assertTrue(lista.stream().filter(t -> !t.idTurnoSemanal().equals(gomezLunesManana.getIdTurnoSemanal())).allMatch(t -> t.suspension() == null));
    }

    @Test
    void rechazaCriteriosInvalidos() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.buscarTurnosSemanales(criterios(null, null, "FERIADO", null, null, null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.buscarTurnosSemanales(criterios(null, null, null, null, "CERRADO", null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> servicio.buscarTurnosSemanales(criterios(null, null, null, null, null, "color", null))).getStatusCode());
    }
}
