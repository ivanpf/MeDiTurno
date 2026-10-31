package com.mediturno.backend.estadisticas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.PeriodoDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.estadisticas.dto.EstadisticaOcupacionDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaReservaDTO;
import com.mediturno.backend.estadisticas.dto.EstadisticaTurnoDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.usuarios.model.Medico;

/**
 * Estadísticas del administrador (CU-40, CU-41 y CU-42) con una base PostgreSQL real. Los datos están en 2031, lejos de
 * cualquier otro dato, y el período de febrero deja afuera los de marzo.
 *
 * <pre>
 * T1  Gómez  CAR  C1  marzo: A (4 cupos) 3 reservas [vigente, orden emitida, cancelada]
 *                           B (6 cupos) 2 reservas [atendida, anulada]
 *                           C (5 cupos, suspendida) 1 reserva [anulada];  febrero: D (8 cupos) 1 vigente
 * T2  Gómez  PED  C2  marzo: E (5 cupos) 2 reservas vigentes
 * T3  Rossi  CAR  C1  marzo: F (10 cupos) sin reservas
 * T4  Rossi  PED  C2  sin fechas
 * </pre>
 */
class EstadisticasIntegracionTest extends PostgresIntegracionTest {

    private static final LocalDate MARZO_DESDE = LocalDate.of(2031, 3, 1);
    private static final LocalDate MARZO_HASTA = LocalDate.of(2031, 3, 31);
    private static final LocalDate FEBRERO_DESDE = LocalDate.of(2031, 2, 1);
    private static final LocalDate FEBRERO_HASTA = LocalDate.of(2031, 2, 28);

    @Autowired JdbcTemplate jdbc;
    @Autowired EstadisticaService servicio;

    private Especialidad cardiologia;
    private Especialidad pediatria;
    private Consultorio c1;
    private Consultorio c2;
    private Medico gomez;
    private Medico rossi;
    private TurnoSemanal t1;
    private TurnoSemanal t2;
    private TurnoSemanal t3;
    private Long paciente;

    @BeforeEach
    void datosBase() {
        String s = UUID.randomUUID().toString().substring(0, 8);
        cardiologia = especialidad("CAR" + s);
        pediatria = especialidad("PED" + s);
        c1 = consultorio("E1" + s, EstadoActivacion.ACTIVO);
        c2 = consultorio("E2" + s, EstadoActivacion.ACTIVO);
        gomez = medico("G" + s, EstadoActivacion.ACTIVO, cardiologia, pediatria);
        rossi = medico("R" + s, EstadoActivacion.ACTIVO, cardiologia, pediatria);
        LocalDate desde = LocalDate.of(2030, 1, 1);
        t1 = turno(gomez, cardiologia, c1, DiaSemana.LUNES, "09:00", "12:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        t2 = turno(gomez, pediatria, c2, DiaSemana.MARTES, "09:00", "12:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        t3 = turno(rossi, cardiologia, c1, DiaSemana.MIERCOLES, "09:00", "12:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        turno(rossi, pediatria, c2, DiaSemana.JUEVES, "09:00", "12:00", desde, null, EstadoTurnoSemanal.ACTIVO);
        paciente = paciente(s);

        long a = ocurrencia(t1, LocalDate.of(2031, 3, 3), 4, "HABILITADA");
        reserva(a, 1, "VIGENTE");
        reserva(a, 2, "ORDEN_EMITIDA");
        reserva(a, 3, "CANCELADA");
        long b = ocurrencia(t1, LocalDate.of(2031, 3, 10), 6, "HABILITADA");
        reserva(b, 1, "ATENDIDA");
        reserva(b, 2, "ANULADA");
        long c = ocurrencia(t1, LocalDate.of(2031, 3, 17), 5, "SUSPENDIDA");
        reserva(c, 1, "ANULADA");
        long d = ocurrencia(t1, LocalDate.of(2031, 2, 10), 8, "FINALIZADA");
        reserva(d, 1, "VIGENTE");
        long e = ocurrencia(t2, LocalDate.of(2031, 3, 4), 5, "HABILITADA");
        reserva(e, 1, "VIGENTE");
        reserva(e, 2, "VIGENTE");
        ocurrencia(t3, LocalDate.of(2031, 3, 5), 10, "HABILITADA");
    }

    private PeriodoDTO marzo() {
        return new PeriodoDTO(MARZO_DESDE, MARZO_HASTA, null, null, null, null, null, null);
    }

    private PeriodoDTO marzo(String codigoEspecialidad, Long idMedico, String codigoConsultorio, Boolean descendente) {
        return new PeriodoDTO(MARZO_DESDE, MARZO_HASTA, codigoEspecialidad, idMedico, codigoConsultorio, null, null, descendente);
    }

    private PeriodoDTO conEstadoYOrden(String estado, String ordenarPor, Boolean descendente) {
        return new PeriodoDTO(MARZO_DESDE, MARZO_HASTA, null, null, null, estado, ordenarPor, descendente);
    }

    private static void assert400(Runnable accion) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
    }

    private List<Long> idsTurnos(List<EstadisticaTurnoDTO> lista) {
        return lista.stream().map(EstadisticaTurnoDTO::idTurnoSemanal).toList();
    }

    private List<Long> idsOcupacion(List<EstadisticaOcupacionDTO> lista) {
        return lista.stream().map(EstadisticaOcupacionDTO::idTurnoSemanal).toList();
    }

    // ---------------------------------------------------------------- CU-40

    @Test
    void reservasPorEspecialidadCuentaTodosLosEstadosYCalculaElPorcentaje() {
        List<EstadisticaReservaDTO> lista = servicio.estadisticaReservasPorEspecialidad(marzo());

        assertEquals(2, lista.size()); // las especialidades sin reservas no salen
        assertEquals(cardiologia.getCodigo(), lista.get(0).codigoEspecialidad());
        assertEquals("Especialidad " + cardiologia.getCodigo(), lista.get(0).nombreEspecialidad());
        assertEquals(6, lista.get(0).cantidadReservas());
        assertEquals(75.0, lista.get(0).porcentaje());
        assertEquals(pediatria.getCodigo(), lista.get(1).codigoEspecialidad());
        assertEquals(2, lista.get(1).cantidadReservas());
        assertEquals(25.0, lista.get(1).porcentaje());
    }

    @Test
    void reservasPorEspecialidadFiltraPorEstadoYElPorcentajeEsDelTotalFiltrado() {
        List<EstadisticaReservaDTO> vigentes = servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden("vigente", null, null));
        assertEquals(List.of(pediatria.getCodigo(), cardiologia.getCodigo()), vigentes.stream().map(EstadisticaReservaDTO::codigoEspecialidad).toList());
        assertEquals(66.67, vigentes.get(0).porcentaje());
        assertEquals(33.33, vigentes.get(1).porcentaje());

        List<EstadisticaReservaDTO> atendidas = servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden("ATENDIDA", null, null));
        assertEquals(1, atendidas.size());
        assertEquals(100.0, atendidas.get(0).porcentaje());
    }

    @Test
    void reservasPorEspecialidadOrdenaPorNombreOCantidadEnAmbosSentidos() {
        assertEquals(List.of(cardiologia.getCodigo(), pediatria.getCodigo()), codigos(servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden(null, "nombre", null))));
        assertEquals(List.of(pediatria.getCodigo(), cardiologia.getCodigo()), codigos(servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden(null, "nombre", true))));
        assertEquals(List.of(pediatria.getCodigo(), cardiologia.getCodigo()), codigos(servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden(null, "cantidad", false))));
    }

    private List<String> codigos(List<EstadisticaReservaDTO> lista) {
        return lista.stream().map(EstadisticaReservaDTO::codigoEspecialidad).toList();
    }

    @Test
    void reservasPorEspecialidadRespetaElPeriodo() {
        List<EstadisticaReservaDTO> febrero = servicio.estadisticaReservasPorEspecialidad(new PeriodoDTO(FEBRERO_DESDE, FEBRERO_HASTA, null, null, null, null, null, null));
        assertEquals(1, febrero.size());
        assertEquals(1, febrero.get(0).cantidadReservas());
        assertEquals(100.0, febrero.get(0).porcentaje());

        // Sin límite de fechas suma todo el historial.
        long total = servicio.estadisticaReservasPorEspecialidad(new PeriodoDTO(LocalDate.of(2031, 1, 1), LocalDate.of(2031, 12, 31), null, null, null, null, null, null))
            .stream().mapToLong(EstadisticaReservaDTO::cantidadReservas).sum();
        assertEquals(9, total);
    }

    @Test
    void reservasPorEspecialidadSinDatosDevuelveUnaListaVacia() {
        assertTrue(servicio.estadisticaReservasPorEspecialidad(new PeriodoDTO(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 1, 31), null, null, null, null, null, null)).isEmpty());
    }

    @Test
    void reservasPorEspecialidadRechazaCriteriosInvalidos() {
        assert400(() -> servicio.estadisticaReservasPorEspecialidad(new PeriodoDTO(MARZO_HASTA, MARZO_DESDE, null, null, null, null, null, null)));
        assert400(() -> servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden("INEXISTENTE", null, null)));
        assert400(() -> servicio.estadisticaReservasPorEspecialidad(conEstadoYOrden(null, "color", null)));
    }

    // ---------------------------------------------------------------- CU-41

    @Test
    void turnosMasDemandadosOrdenaDeMayorAMenorYMuestraLosDatosDelTurno() {
        List<EstadisticaTurnoDTO> lista = servicio.estadisticaTurnosMasDemandados(marzo());

        assertEquals(List.of(t1.getIdTurnoSemanal(), t2.getIdTurnoSemanal(), t3.getIdTurnoSemanal()), idsTurnos(lista)); // T4 no tiene fechas
        EstadisticaTurnoDTO primero = lista.get(0);
        assertEquals(6, primero.cantidadReservas());
        assertEquals(cardiologia.getCodigo(), primero.codigoEspecialidad());
        assertEquals("Especialidad " + cardiologia.getCodigo(), primero.nombreEspecialidad());
        assertEquals(gomez.getIdUsuario(), primero.idMedico());
        assertEquals(gomez.nombreCompleto(), primero.nombreMedico());
        assertEquals(DiaSemana.LUNES, primero.dia());
        assertEquals("09:00", primero.horaInicio().toString());
        assertEquals("12:00", primero.horaFin().toString());
        assertEquals(c1.getCodigo(), primero.codigoConsultorio());
        assertEquals(c1.getNombre(), primero.nombreConsultorio());
        assertEquals(10, primero.cupoMaximo());
        assertEquals(0, lista.get(2).cantidadReservas()); // sin reservas, pero con fechas en el período
    }

    @Test
    void turnosMasDemandadosPuedeOrdenarDeMenorAMayor() {
        assertEquals(List.of(t3.getIdTurnoSemanal(), t2.getIdTurnoSemanal(), t1.getIdTurnoSemanal()),
            idsTurnos(servicio.estadisticaTurnosMasDemandados(marzo(null, null, null, false))));
    }

    @Test
    void turnosMasDemandadosFiltraPorEspecialidadYMedico() {
        assertEquals(List.of(t1.getIdTurnoSemanal(), t3.getIdTurnoSemanal()),
            idsTurnos(servicio.estadisticaTurnosMasDemandados(marzo(cardiologia.getCodigo(), null, null, null))));
        assertEquals(List.of(t1.getIdTurnoSemanal(), t2.getIdTurnoSemanal()),
            idsTurnos(servicio.estadisticaTurnosMasDemandados(marzo(null, gomez.getIdUsuario(), null, null))));
        assertEquals(List.of(t3.getIdTurnoSemanal()),
            idsTurnos(servicio.estadisticaTurnosMasDemandados(marzo(cardiologia.getCodigo(), rossi.getIdUsuario(), null, null))));
    }

    @Test
    void turnosMasDemandadosRespetaElPeriodoYRechazaCriteriosInvalidos() {
        List<EstadisticaTurnoDTO> febrero = servicio.estadisticaTurnosMasDemandados(new PeriodoDTO(FEBRERO_DESDE, FEBRERO_HASTA, null, null, null, null, null, null));
        assertEquals(List.of(t1.getIdTurnoSemanal()), idsTurnos(febrero));
        assertEquals(1, febrero.get(0).cantidadReservas());

        assertTrue(servicio.estadisticaTurnosMasDemandados(new PeriodoDTO(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 1, 31), null, null, null, null, null, null)).isEmpty());
        assert400(() -> servicio.estadisticaTurnosMasDemandados(new PeriodoDTO(MARZO_HASTA, MARZO_DESDE, null, null, null, null, null, null)));
        assert400(() -> servicio.estadisticaTurnosMasDemandados(conEstadoYOrden(null, "medico", null)));
    }

    // ---------------------------------------------------------------- CU-42

    @Test
    void ocupacionCalculaCuposReservasYPorcentaje() {
        List<EstadisticaOcupacionDTO> lista = servicio.estadisticaOcupacionTurnos(marzo());

        assertEquals(List.of(t2.getIdTurnoSemanal(), t1.getIdTurnoSemanal(), t3.getIdTurnoSemanal()), idsOcupacion(lista));

        EstadisticaOcupacionDTO uno = lista.get(1);
        assertEquals(10, uno.cuposOfrecidos()); // 4 + 6; la fecha suspendida no ofrece cupos
        assertEquals(3, uno.cuposReservados()); // vigente, orden emitida y atendida; las canceladas y anuladas no cuentan
        assertEquals(2, uno.reservasVigentes());
        assertEquals(7, uno.cuposNoReservados());
        assertEquals(30.0, uno.porcentajeOcupacion());
        assertEquals(gomez.nombreCompleto(), uno.nombreMedico());
        assertEquals(cardiologia.getCodigo(), uno.codigoEspecialidad());
        assertEquals(c1.getCodigo(), uno.codigoConsultorio());
        assertEquals(DiaSemana.LUNES, uno.dia());

        EstadisticaOcupacionDTO dos = lista.get(0);
        assertEquals(5, dos.cuposOfrecidos());
        assertEquals(2, dos.cuposReservados());
        assertEquals(2, dos.reservasVigentes());
        assertEquals(3, dos.cuposNoReservados());
        assertEquals(40.0, dos.porcentajeOcupacion());

        EstadisticaOcupacionDTO tres = lista.get(2);
        assertEquals(10, tres.cuposOfrecidos());
        assertEquals(0, tres.cuposReservados());
        assertEquals(10, tres.cuposNoReservados());
        assertEquals(0.0, tres.porcentajeOcupacion());
    }

    @Test
    void ocupacionOrdenaPorPorcentajeEnAmbosSentidos() {
        assertEquals(List.of(t3.getIdTurnoSemanal(), t1.getIdTurnoSemanal(), t2.getIdTurnoSemanal()),
            idsOcupacion(servicio.estadisticaOcupacionTurnos(marzo(null, null, null, false))));
    }

    @Test
    void ocupacionFiltraPorEspecialidadMedicoYConsultorio() {
        assertEquals(List.of(t2.getIdTurnoSemanal()), idsOcupacion(servicio.estadisticaOcupacionTurnos(marzo(pediatria.getCodigo(), null, null, null))));
        assertEquals(List.of(t1.getIdTurnoSemanal(), t2.getIdTurnoSemanal()), idsOcupacion(servicio.estadisticaOcupacionTurnos(marzo(null, gomez.getIdUsuario(), null, false))));
        assertEquals(List.of(t1.getIdTurnoSemanal(), t3.getIdTurnoSemanal()), idsOcupacion(servicio.estadisticaOcupacionTurnos(marzo(null, null, c1.getCodigo(), null))));
        assertEquals(List.of(t3.getIdTurnoSemanal()), idsOcupacion(servicio.estadisticaOcupacionTurnos(marzo(cardiologia.getCodigo(), rossi.getIdUsuario(), c1.getCodigo(), null))));
        assertTrue(servicio.estadisticaOcupacionTurnos(marzo(pediatria.getCodigo(), null, c1.getCodigo(), null)).isEmpty());
    }

    @Test
    void ocupacionRespetaElPeriodoYRechazaCriteriosInvalidos() {
        List<EstadisticaOcupacionDTO> febrero = servicio.estadisticaOcupacionTurnos(new PeriodoDTO(FEBRERO_DESDE, FEBRERO_HASTA, null, null, null, null, null, null));
        assertEquals(1, febrero.size());
        assertEquals(8, febrero.get(0).cuposOfrecidos());
        assertEquals(12.5, febrero.get(0).porcentajeOcupacion());

        assertTrue(servicio.estadisticaOcupacionTurnos(new PeriodoDTO(LocalDate.of(2040, 1, 1), LocalDate.of(2040, 1, 31), null, null, null, null, null, null)).isEmpty());
        assert400(() -> servicio.estadisticaOcupacionTurnos(new PeriodoDTO(MARZO_HASTA, MARZO_DESDE, null, null, null, null, null, null)));
        assert400(() -> servicio.estadisticaOcupacionTurnos(conEstadoYOrden(null, "medico", null)));
    }

    // ---------------------------------------------------------------- datos

    private Long paciente(String sufijo) {
        Long id = jdbc.queryForObject("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Ana', ?, '1990-01-01') returning id_usuario",
            Long.class, "est" + sufijo + "@prueba.com", "est" + sufijo, sufijo);
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", id);
        return id;
    }

    private long ocurrencia(TurnoSemanal turno, LocalDate fecha, int cupos, String estado) {
        return jdbc.queryForObject("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) "
            + "values (?, ?, ?, '09:00', '12:00', ?) returning id_ocurrencia_turno",
            Long.class, turno.getIdTurnoSemanal(), fecha, cupos, estado);
    }

    private void reserva(long idOcurrencia, int numero, String estado) {
        jdbc.update("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) "
            + "values (?, ?, ?, ?, CURRENT_TIMESTAMP)", paciente, idOcurrencia, numero, estado);
    }
}
