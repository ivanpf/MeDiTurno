package com.mediturno.backend.turnos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.usuarios.model.Medico;

/** Tarea programada que genera las fechas de los turnos semanales, con una base PostgreSQL real. */
class GeneradorOcurrenciasIntegracionTest extends PostgresIntegracionTest {

    @Autowired GeneradorOcurrenciasService generador;
    @Autowired OcurrenciaTurnoRepository ocurrencias;

    private Especialidad esp;
    private Consultorio cons;
    private Medico med;

    @BeforeEach
    void datosBase() {
        esp = especialidad("GEN");
        cons = consultorio("G1", EstadoActivacion.ACTIVO);
        med = medico("9101", EstadoActivacion.ACTIVO, esp);
    }

    private TurnoSemanal lunes(LocalDate desde, LocalDate hasta, EstadoTurnoSemanal estado) {
        return turno(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", desde, hasta, estado);
    }

    private List<OcurrenciaTurno> deTurno(TurnoSemanal turno) {
        return ocurrencias.findAll().stream()
            .filter(o -> o.getTurnoSemanal().getIdTurnoSemanal().equals(turno.getIdTurnoSemanal()))
            .sorted((a, b) -> a.getFecha().compareTo(b.getFecha()))
            .toList();
    }

    @Test
    void creaUnaOcurrenciaPorCadaSemanaDentroDelPeriodoYNingunaAntesDeHoy() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        LocalDate limite = HOY.plusDays(28);

        int creadas = generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        List<OcurrenciaTurno> generadas = deTurno(turno);
        assertEquals(creadas, generadas.size());
        assertEquals(HOY.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)), generadas.get(0).getFecha());
        for (int i = 0; i < generadas.size(); i++) {
            LocalDate fecha = generadas.get(i).getFecha();
            assertEquals(DayOfWeek.MONDAY, fecha.getDayOfWeek());
            assertFalse(fecha.isBefore(HOY));
            assertFalse(fecha.isAfter(limite));
            if (i > 0) {
                assertEquals(fecha.minusWeeks(1), generadas.get(i - 1).getFecha());
            }
        }
        // no falta ninguna: la siguiente ya quedaría después del límite
        assertTrue(generadas.get(generadas.size() - 1).getFecha().plusWeeks(1).isAfter(limite));
    }

    @Test
    void copiaElCupoYElHorarioDelTurnoYLaDejaHabilitada() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atStartOfDay(), 14);

        for (OcurrenciaTurno o : deTurno(turno)) {
            assertEquals(10, o.getCupos());
            assertEquals(LocalTime.of(9, 0), o.getHoraInicio());
            assertEquals(LocalTime.of(12, 0), o.getHoraFin());
            assertEquals(EstadoOcurrenciaTurno.HABILITADA, o.getOcurrenciaTurnoEstado());
        }
    }

    @Test
    void repetirLaTareaNoDuplicaNada() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atStartOfDay(), 28);
        int cantidad = deTurno(turno).size();
        int segundaVez = generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        assertEquals(0, segundaVez);
        assertEquals(cantidad, deTurno(turno).size());
    }

    @Test
    void completaSoloLasQueFaltan() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        generador.generarOcurrencias(HOY.atStartOfDay(), 28);
        OcurrenciaTurno primera = deTurno(turno).get(0);
        ocurrencias.delete(primera);
        ocurrencias.flush();

        int creadas = generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        assertEquals(1, creadas);
        assertEquals(primera.getFecha(), deTurno(turno).get(0).getFecha());
    }

    @Test
    void conUnPeriodoMasLargoSoloAgregaLasSemanasNuevas() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        generador.generarOcurrencias(HOY.atStartOfDay(), 14);
        int antes = deTurno(turno).size();

        int nuevas = generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        assertEquals(deTurno(turno).size() - antes, nuevas);
        assertTrue(nuevas >= 1);
    }

    @Test
    void noGeneraNadaAntesDeQueEmpieceLaVigencia() {
        LocalDate desde = HOY.plusDays(10);
        TurnoSemanal turno = lunes(desde, null, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        List<OcurrenciaTurno> generadas = deTurno(turno);
        assertFalse(generadas.isEmpty());
        assertEquals(desde.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)), generadas.get(0).getFecha());
    }

    @Test
    void noGeneraNadaDespuesDeQueTerminaLaVigencia() {
        LocalDate hasta = HOY.plusDays(8);
        TurnoSemanal turno = lunes(HOY.minusDays(30), hasta, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        List<OcurrenciaTurno> generadas = deTurno(turno);
        assertFalse(generadas.isEmpty());
        assertTrue(generadas.stream().noneMatch(o -> o.getFecha().isAfter(hasta)));
    }

    @Test
    void noGeneraLasFechasDeUnTurnoConSuspensionPermanente() {
        TurnoSemanal suspendido = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.SUSPENDIDO);
        suspension(suspendido, funcionario("9301"), com.mediturno.backend.turnos.model.TipoSuspension.PERMANENTE, HOY.minusDays(1), null);

        assertEquals(0, generador.generarOcurrencias(HOY.atStartOfDay(), 28));
        assertTrue(deTurno(suspendido).isEmpty());
    }

    @Test
    void unaSuspensionTemporalSoloSaltaSuPeriodoYSeguiaDespues() {
        TurnoSemanal turno = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.SUSPENDIDO);
        LocalDate desde = HOY.minusDays(1);
        LocalDate hasta = HOY.plusDays(10);
        suspension(turno, funcionario("9302"), com.mediturno.backend.turnos.model.TipoSuspension.TEMPORAL, desde, hasta);

        generador.generarOcurrencias(HOY.atStartOfDay(), 42);

        List<OcurrenciaTurno> generadas = deTurno(turno);
        assertFalse(generadas.isEmpty());
        assertTrue(generadas.stream().noneMatch(o -> !o.getFecha().isBefore(desde) && !o.getFecha().isAfter(hasta)));
        assertTrue(generadas.stream().anyMatch(o -> o.getFecha().isAfter(hasta)));
    }

    @Test
    void ignoraLosTurnosQueYaTerminaron() {
        TurnoSemanal suspendido = lunes(HOY.minusDays(30), HOY.minusDays(5), EstadoTurnoSemanal.SUSPENDIDO);
        TurnoSemanal terminado = turno(med, esp, cons, DiaSemana.MARTES, "09:00", "12:00", HOY.minusDays(60), HOY.minusDays(1),
            EstadoTurnoSemanal.ACTIVO);

        int creadas = generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        assertEquals(0, creadas);
        assertTrue(deTurno(suspendido).isEmpty());
        assertTrue(deTurno(terminado).isEmpty());
    }

    @Test
    void generaLasDeTodosLosTurnosActivos() {
        TurnoSemanal delLunes = lunes(HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        Consultorio otro = consultorio("G2", EstadoActivacion.ACTIVO);
        TurnoSemanal delViernes = turno(med, esp, otro, DiaSemana.VIERNES, "14:00", "18:00", HOY.minusDays(30), null,
            EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atStartOfDay(), 28);

        assertFalse(deTurno(delLunes).isEmpty());
        assertFalse(deTurno(delViernes).isEmpty());
        assertTrue(deTurno(delViernes).stream().allMatch(o -> o.getFecha().getDayOfWeek() == DayOfWeek.FRIDAY));
    }

    // ------------------------------------------------------------------ la fecha de hoy

    private DiaSemana diaDeHoy() {
        return Arrays.stream(DiaSemana.values()).filter(d -> d.aDayOfWeek() == HOY.getDayOfWeek()).findFirst().orElseThrow();
    }

    private boolean tieneFecha(TurnoSemanal turno, LocalDate fecha) {
        return deTurno(turno).stream().anyMatch(o -> o.getFecha().equals(fecha));
    }

    @Test
    void laFechaDeHoySeGeneraSiElTurnoTodaviaNoEmpezo() {
        TurnoSemanal turno = turno(med, esp, cons, diaDeHoy(), "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atTime(8, 59), 14);

        assertTrue(tieneFecha(turno, HOY));
        assertTrue(tieneFecha(turno, HOY.plusWeeks(1)));
    }

    /** A las 9:00 en punto ya empezó, a las 10:30 está en curso y a las 12:00 y 13:00 ya terminó: en ningún caso se ofrece hoy. */
    @ParameterizedTest
    @ValueSource(strings = {"09:00", "10:30", "12:00", "13:00"})
    void laFechaDeHoyNoSeGeneraSiElTurnoYaEmpezoOTermino(String horaActual) {
        TurnoSemanal turno = turno(med, esp, cons, diaDeHoy(), "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);

        generador.generarOcurrencias(HOY.atTime(LocalTime.parse(horaActual)), 14);

        assertFalse(tieneFecha(turno, HOY));
        assertTrue(tieneFecha(turno, HOY.plusWeeks(1))); // las fechas de días posteriores se generan siempre
    }
}
