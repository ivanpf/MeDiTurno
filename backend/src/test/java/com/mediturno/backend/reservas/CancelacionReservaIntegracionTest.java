package com.mediturno.backend.reservas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.turnos.TurnoSemanalService;

/** CU-9 con PostgreSQL real: plazo desde el inicio, propiedad, historial, reutilización y concurrencia. */
@SpringBootTest
class CancelacionReservaIntegracionTest extends BasePostgresEmbebida {
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    static final LocalDateTime AHORA = HOY.atTime(9, 0);
    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "cancelacion_reservas");
        registro.add("spring.jpa.show-sql", () -> "false");
    }
    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservaService reservas;
    @Autowired TurnoSemanalService turnos;
    Long paciente;
    Long otroPaciente;
    Long medico;
    String especialidad;
    String consultorio;

    Long id(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }
    Long usuario() {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) values (?,'ACTIVO',CURRENT_TIMESTAMP,?,'Ana','López','1990-01-01') returning id_usuario", codigo + "@prueba.com", codigo);
    }
    Long paciente() {
        Long usuario = usuario();
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", usuario);
        return usuario;
    }
    @BeforeEach void preparar() {
        especialidad = "E" + UUID.randomUUID().toString().substring(0, 15);
        consultorio = "C" + UUID.randomUUID().toString().substring(0, 15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')", especialidad, especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,?, 'Piso 1','ACTIVO')", consultorio, consultorio);
        medico = usuario();
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", medico, "MAT" + medico);
        jdbc.update("insert into medico_especialidad(id_medico,codigo_especialidad) values (?,?)", medico, especialidad);
        paciente = paciente();
        otroPaciente = paciente();
    }
    Long reserva(LocalDateTime inicio, String estado) {
        Long turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) values (?,?,?,'VIERNES',?,?,6,?,'ACTIVO') returning id_turno_semanal", medico, especialidad, consultorio, inicio.toLocalTime(), inicio.toLocalTime().plusHours(1), HOY.minusWeeks(1));
        Long ocurrencia = id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) values (?,?,6,?,?,'HABILITADA') returning id_ocurrencia_turno", turno, inicio.toLocalDate(), inicio.toLocalTime(), inicio.toLocalTime().plusHours(1));
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) values (?,?,6,?,CURRENT_TIMESTAMP) returning id_reserva", paciente, ocurrencia, estado);
    }
    String estado(Long reserva) {
        return jdbc.queryForObject("select reserva_estado from reserva where id_reserva=?", String.class, reserva);
    }
    void error(int codigo, Runnable accion) {
        assertEquals(codigo, assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value());
    }
    @Test void cancelaExactamente48HorasAntesConservaHistorialYLiberaElNumero() {
        Long reserva = reserva(AHORA.plusHours(48), "VIGENTE");
        var previa = reservas.vistaPreviaCancelacion(reserva, paciente);
        assertTrue(previa.puedeCancelar());
        assertEquals("VIGENTE", estado(reserva), "La vista previa no modifica nada");
        assertEquals(LocalTime.of(9, 50), previa.reserva().horaEstimada());
        reservas.cancelarReserva(reserva, paciente);
        assertEquals("CANCELADA", estado(reserva));
        assertEquals(AHORA, jdbc.queryForObject("select fecha_cancelacion from reserva where id_reserva=?", LocalDateTime.class, reserva));
        assertEquals("Cancelada por el paciente", jdbc.queryForObject("select motivo_cancelacion from reserva where id_reserva=?", String.class, reserva));
        var lista = reservas.listarProximasPropias(paciente);
        assertEquals(EstadoReserva.CANCELADA, lista.get(0).reserva().estado());
        assertEquals(AHORA, lista.get(0).fechaCancelacion());
        assertFalse(lista.get(0).puedeCancelar());
        Long ocurrencia = id("select id_ocurrencia_turno from reserva where id_reserva=?", reserva);
        assertFalse(turnos.listarOcurrenciasDisponibles(especialidad, otroPaciente).get(0).numerosNoDisponibles().contains(6));
        var nueva = reservas.reservarOrden(otroPaciente, new ReservaRequestDTO(ocurrencia, 6));
        assertNotEquals(reserva, nueva.idReserva());
        assertEquals("CANCELADA", estado(reserva));
        assertEquals("VIGENTE", estado(nueva.idReserva()));
    }
    @Test void rechazaUnSegundoMenosDe48HorasAunqueElNumeroSeAtiendaMasTarde() {
        Long reserva = reserva(AHORA.plusHours(48).minusSeconds(1), "VIGENTE");
        assertFalse(reservas.listarProximasPropias(paciente).get(0).puedeCancelar());
        error(409, () -> reservas.vistaPreviaCancelacion(reserva, paciente));
        error(409, () -> reservas.cancelarReserva(reserva, paciente));
        assertEquals("VIGENTE", estado(reserva));
        assertNull(jdbc.queryForObject("select fecha_cancelacion from reserva where id_reserva=?", LocalDateTime.class, reserva));
    }
    @Test void permiteMasDe48HorasYRechazaTodasLasReservasNoVigentes() {
        Long vigente = reserva(AHORA.plusHours(49), "VIGENTE");
        reservas.cancelarReserva(vigente, paciente);
        for (String estado : List.of("CANCELADA", "ANULADA", "ORDEN_EMITIDA", "ATENDIDA", "VENCIDA")) {
            Long reserva = reserva(AHORA.plusHours(72), estado);
            error(409, () -> reservas.vistaPreviaCancelacion(reserva, paciente));
            error(409, () -> reservas.cancelarReserva(reserva, paciente));
            assertEquals(estado, estado(reserva));
        }
    }
    @Test void noPermiteConsultarNiCancelarReservasAjenasOInexistentes() {
        Long reserva = reserva(AHORA.plusHours(72), "VIGENTE");
        for (Long id : List.of(reserva, Long.MAX_VALUE)) {
            error(404, () -> reservas.vistaPreviaCancelacion(id, otroPaciente));
            error(404, () -> reservas.cancelarReserva(id, otroPaciente));
        }
        assertTrue(reservas.listarProximasPropias(otroPaciente).isEmpty());
        assertEquals("VIGENTE", estado(reserva));
    }
    @Test void listadoSoloPropioDesdeHoyOrdenadoYConDatosParaConfirmar() {
        Long posterior = reserva(AHORA.plusHours(96), "VIGENTE");
        Long anterior = reserva(AHORA.plusHours(72), "VIGENTE");
        reserva(AHORA.minusDays(1), "VIGENTE");
        var lista = reservas.listarProximasPropias(paciente);
        assertEquals(List.of(anterior, posterior), lista.stream().map(r -> r.reserva().idReserva()).toList());
        assertEquals(consultorio, lista.get(0).reserva().consultorio());
        assertEquals(especialidad, lista.get(0).reserva().especialidad());
        assertNotNull(lista.get(0).reserva().nombreMedico());
    }
    @Test void confirmarRevalidaElEstadoAunqueLaVistaPreviaFueraValida() {
        Long reserva = reserva(AHORA.plusHours(72), "VIGENTE");
        assertTrue(reservas.vistaPreviaCancelacion(reserva, paciente).puedeCancelar());
        jdbc.update("update reserva set reserva_estado='ANULADA' where id_reserva=?", reserva);
        error(409, () -> reservas.cancelarReserva(reserva, paciente));
        assertEquals("ANULADA", estado(reserva));
    }
    @Test void soloUnaCancelacionConcurrenteModificaLaReserva() throws Exception {
        Long reserva = reserva(AHORA.plusHours(72), "VIGENTE");
        var ejecutor = Executors.newFixedThreadPool(2);
        var salida = new CountDownLatch(1);
        try {
            var tareas = java.util.stream.IntStream.range(0, 2).mapToObj(i -> ejecutor.submit(() -> {
                salida.await();
                try { reservas.cancelarReserva(reserva, paciente); return 204; }
                catch (ResponseStatusException e) { return e.getStatusCode().value(); }
            })).toList();
            salida.countDown();
            var codigos = List.of(tareas.get(0).get(10, TimeUnit.SECONDS), tareas.get(1).get(10, TimeUnit.SECONDS));
            assertTrue(codigos.contains(204));
            assertTrue(codigos.contains(409));
            assertEquals("CANCELADA", estado(reserva));
        } finally { ejecutor.shutdownNow(); }
    }
}
