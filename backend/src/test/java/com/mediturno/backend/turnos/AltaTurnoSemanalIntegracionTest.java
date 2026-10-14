package com.mediturno.backend.turnos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.dto.AltaTurnoSemanalDTO;
import com.mediturno.backend.turnos.dto.TurnoSemanalResponseDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.usuarios.model.Medico;


/**
 * CU-12 con una base PostgreSQL real: lo importante del caso de uso (qué se considera una superposición) está en una
 * consulta, y con datos simulados no se podría comprobar. Cada prueba se deshace sola al terminar.
 */
class AltaTurnoSemanalIntegracionTest extends PostgresIntegracionTest {

    @Autowired TurnoSemanalService servicio;
    @Autowired OcurrenciaTurnoRepository ocurrencias;
    @Value("${mediturno.reservas.semanas-adelante}") int semanasDeReserva;

    // ------------------------------------------------------------------ datos de prueba

    private TurnoSemanal turnoExistente(Medico m, Especialidad e, Consultorio c, DiaSemana dia, String inicio, String fin, LocalDate hasta) {
        return turno(m, e, c, dia, inicio, fin, HOY.minusDays(30), hasta, EstadoTurnoSemanal.ACTIVO);
    }

    private AltaTurnoSemanalDTO alta(Medico m, Especialidad e, Consultorio c, List<DiaSemana> dias, String inicio, String fin) {
        return new AltaTurnoSemanalDTO(m.getIdUsuario(), e.getCodigo(), c.getCodigo(), dias,
            LocalTime.parse(inicio), LocalTime.parse(fin), 10, HOY);
    }

    private ResponseStatusException rechazo(AltaTurnoSemanalDTO datos) {
        return assertThrows(ResponseStatusException.class, () -> servicio.altaTurnoSemanal(datos));
    }

    // ------------------------------------------------------------------ flujo principal

    @Test
    void creaUnTurnoPorCadaDiaElegidoYLosDejaActivos() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        long antes = turnos.count();

        List<TurnoSemanalResponseDTO> creados = servicio.altaTurnoSemanal(
            alta(med, esp, cons, List.of(DiaSemana.MARTES, DiaSemana.LUNES, DiaSemana.MARTES), "09:00", "12:00"));

        assertEquals(2, creados.size());
        assertEquals(List.of(DiaSemana.LUNES, DiaSemana.MARTES), creados.stream().map(TurnoSemanalResponseDTO::dia).toList());
        assertEquals(antes + 2, turnos.count());
        for (TurnoSemanalResponseDTO t : creados) {
            assertEquals(EstadoTurnoSemanal.ACTIVO, t.estado());
            assertNull(t.vigenteHasta());
            assertEquals(HOY, t.vigenteDesde());
            assertEquals(10, t.cupoMaximo());
            assertEquals("CARD", t.codigoEspecialidad());
            assertEquals("C1", t.codigoConsultorio());
        }
    }

    // ------------------------------------------------------------------ superposiciones (paso 4)

    @Test
    void rechazaUnaSuperposicionDelMismoMedicoAunqueSeaEnOtroConsultorio() {
        Especialidad esp = especialidad("CARD");
        Consultorio c1 = consultorio("C1", EstadoActivacion.ACTIVO);
        Consultorio c2 = consultorio("C2", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        TurnoSemanal existente = turnoExistente(med, esp, c1, DiaSemana.LUNES, "09:00", "12:00", null);
        long antes = turnos.count();

        ResponseStatusException error = rechazo(alta(med, esp, c2, List.of(DiaSemana.LUNES), "11:00", "13:00"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().startsWith("Superposición con: "));
        assertTrue(error.getReason().contains("Lunes 09:00-12:00"));
        assertTrue(error.getReason().contains("turno #" + existente.getIdTurnoSemanal()));
        assertEquals(antes, turnos.count());
    }

    @Test
    void rechazaUnaSuperposicionDelMismoConsultorioAunqueSeaOtroMedico() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico uno = medico("9001", EstadoActivacion.ACTIVO, esp);
        Medico otro = medico("9002", EstadoActivacion.ACTIVO, esp);
        turnoExistente(uno, esp, cons, DiaSemana.LUNES, "09:00", "12:00", null);

        ResponseStatusException error = rechazo(alta(otro, esp, cons, List.of(DiaSemana.LUNES), "08:00", "10:00"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    @Test
    void unaFranjaQueEmpiezaCuandoTerminaLaOtraNoSeSuperpone() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        turnoExistente(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", null);

        assertEquals(1, servicio.altaTurnoSemanal(alta(med, esp, cons, List.of(DiaSemana.LUNES), "12:00", "14:00")).size());
        assertEquals(1, servicio.altaTurnoSemanal(alta(med, esp, cons, List.of(DiaSemana.LUNES), "07:00", "09:00")).size());
    }

    @Test
    void elMismoHorarioEnOtroDiaNoSeSuperpone() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        turnoExistente(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", null);

        assertEquals(1, servicio.altaTurnoSemanal(alta(med, esp, cons, List.of(DiaSemana.MARTES), "09:00", "12:00")).size());
    }

    @Test
    void unTurnoCuyaVigenciaYaTerminoNoGeneraConflicto() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        turnoExistente(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(1));

        assertEquals(1, servicio.altaTurnoSemanal(alta(med, esp, cons, List.of(DiaSemana.LUNES), "09:00", "12:00")).size());
    }

    @Test
    void unTurnoSuspendidoSigueOcupandoSuFranjaPorqueSePuedeRehabilitar() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        TurnoSemanal suspendido = turnoExistente(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", null);
        suspendido.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        turnos.saveAndFlush(suspendido);

        assertEquals(HttpStatus.CONFLICT, rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES), "10:00", "11:00")).getStatusCode());
    }

    @Test
    void siUnSoloDiaSeSuperponeNoSeCreaNingunoDeLosDemas() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        turnoExistente(med, esp, cons, DiaSemana.MARTES, "09:00", "12:00", null);
        long antes = turnos.count();
        long ocurrenciasAntes = ocurrencias.count();

        ResponseStatusException error = rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES, DiaSemana.MARTES), "10:00", "11:00"));

        assertTrue(error.getReason().contains("Martes"));
        assertEquals(antes, turnos.count());
        assertEquals(ocurrenciasAntes, ocurrencias.count());
    }

    // ------------------------------------------------------------------ médico y consultorio (paso 3)

    @Test
    void rechazaUnMedicoInactivo() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.INACTIVO, esp);

        ResponseStatusException error = rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES), "09:00", "12:00"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("El médico seleccionado está inactivo.", error.getReason());
    }

    @Test
    void rechazaUnMedicoQueNoTieneLaEspecialidadIndicada() {
        Especialidad cardio = especialidad("CARD");
        Especialidad pediatria = especialidad("PED");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, cardio);

        ResponseStatusException error = rechazo(alta(med, pediatria, cons, List.of(DiaSemana.LUNES), "09:00", "12:00"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("El médico seleccionado no tiene asignada la especialidad indicada.", error.getReason());
    }

    @Test
    void rechazaUnConsultorioInactivo() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.INACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);

        ResponseStatusException error = rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES), "09:00", "12:00"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("El consultorio seleccionado está inactivo.", error.getReason());
    }

    @Test
    void respondeNoEncontradoSiElMedicoOElConsultorioNoExisten() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);

        AltaTurnoSemanalDTO medicoInexistente = new AltaTurnoSemanalDTO(999_999L, "CARD", "C1", List.of(DiaSemana.LUNES),
            LocalTime.of(9, 0), LocalTime.of(12, 0), 10, HOY);
        AltaTurnoSemanalDTO consultorioInexistente = new AltaTurnoSemanalDTO(med.getIdUsuario(), "CARD", "NO-EXISTE", List.of(DiaSemana.LUNES),
            LocalTime.of(9, 0), LocalTime.of(12, 0), 10, HOY);

        assertEquals(HttpStatus.NOT_FOUND, rechazo(medicoInexistente).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, rechazo(consultorioInexistente).getStatusCode());
        assertEquals("C1", cons.getCodigo());
    }

    // ------------------------------------------------------------------ datos inválidos (paso 2)

    @Test
    void rechazaUnaHoraDeFinQueNoEsPosteriorALaDeInicio() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);

        assertEquals(HttpStatus.BAD_REQUEST, rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES), "12:00", "12:00")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, rechazo(alta(med, esp, cons, List.of(DiaSemana.LUNES), "12:00", "09:00")).getStatusCode());
    }

    // ------------------------------------------------------------------ las fechas reservables

    private List<LocalDate> fechasDe(TurnoSemanalResponseDTO turno) {
        return ocurrencias.findAll().stream()
            .filter(o -> o.getTurnoSemanal().getIdTurnoSemanal().equals(turno.idTurnoSemanal()))
            .map(OcurrenciaTurno::getFecha)
            .sorted()
            .toList();
    }

    @Test
    void alCrearElTurnoYaQuedanGeneradasSusFechasHastaDondeSePuedeReservar() {
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        LocalDate desde = HOY.plusDays(1); // mañana: no depende de la hora a la que corra la prueba
        LocalDate limite = HOY.plusWeeks(semanasDeReserva);

        List<TurnoSemanalResponseDTO> creados = servicio.altaTurnoSemanal(new AltaTurnoSemanalDTO(med.getIdUsuario(), "CARD", "C1",
            List.of(DiaSemana.LUNES, DiaSemana.JUEVES), LocalTime.of(9, 0), LocalTime.of(12, 0), 10, desde));

        assertEquals(2, creados.size());
        for (TurnoSemanalResponseDTO turno : creados) {
            DayOfWeek dia = turno.dia().aDayOfWeek();
            List<LocalDate> fechas = fechasDe(turno);
            assertFalse(fechas.isEmpty());
            assertEquals(desde.with(TemporalAdjusters.nextOrSame(dia)), fechas.get(0));
            assertTrue(fechas.stream().allMatch(f -> f.getDayOfWeek() == dia && !f.isBefore(desde) && !f.isAfter(limite)));
            assertTrue(fechas.get(fechas.size() - 1).plusWeeks(1).isAfter(limite)); // no falta ninguna
        }
    }

    @Test
    void unTurnoDeHoyQueTodaviaNoEmpezoQuedaDisponibleHoy() {
        assumeTrue(LocalTime.now().isBefore(LocalTime.of(22, 0)), "hace falta que falte tiempo para las 23:00");
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        DiaSemana hoy = DiaSemana.values()[HOY.getDayOfWeek().getValue() - 1];

        List<TurnoSemanalResponseDTO> creados = servicio.altaTurnoSemanal(new AltaTurnoSemanalDTO(med.getIdUsuario(), "CARD", "C1",
            List.of(hoy), LocalTime.of(23, 0), LocalTime.of(23, 30), 10, HOY));

        assertEquals(HOY, fechasDe(creados.get(0)).get(0));
    }

    @Test
    void unTurnoDeHoyQueYaEmpezoNoSeOfreceHoyPeroSiLasSemanasSiguientes() {
        assumeTrue(LocalTime.now().isAfter(LocalTime.of(0, 5)), "hace falta que ya haya pasado la medianoche");
        Especialidad esp = especialidad("CARD");
        Consultorio cons = consultorio("C1", EstadoActivacion.ACTIVO);
        Medico med = medico("9001", EstadoActivacion.ACTIVO, esp);
        DiaSemana hoy = DiaSemana.values()[HOY.getDayOfWeek().getValue() - 1];

        List<TurnoSemanalResponseDTO> creados = servicio.altaTurnoSemanal(new AltaTurnoSemanalDTO(med.getIdUsuario(), "CARD", "C1",
            List.of(hoy), LocalTime.of(0, 0), LocalTime.of(0, 4), 10, HOY));

        List<LocalDate> fechas = fechasDe(creados.get(0));
        assertFalse(fechas.contains(HOY));
        assertEquals(HOY.plusWeeks(1), fechas.get(0));
    }
}
