package com.mediturno.backend.reservas;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import com.mediturno.backend.reservas.dto.ReservaRequestDTO;
import com.mediturno.backend.turnos.TurnoSemanalService;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/** CU-8 contra PostgreSQL aislado: transacciones, bloqueo, filtros y pases reales. */
@SpringBootTest
class ReservaCU8Test {
    static final EmbeddedPostgres POSTGRES = iniciarPostgres();
    static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    static EmbeddedPostgres iniciarPostgres() {
        try { return EmbeddedPostgres.start(); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registro.add("spring.datasource.username", () -> "postgres");
        registro.add("spring.datasource.password", () -> "postgres");
        registro.add("spring.jpa.show-sql", () -> "false");
    }

    @AfterAll static void cerrar() throws IOException { POSTGRES.close(); }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ReservaService reservas;
    @Autowired TurnoSemanalService turnos;
    String especialidad;
    String consultorio;
    Long medico;
    Long paciente;
    Long otroPaciente;
    Long turno;
    Long ocurrencia;

    Long id(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    Long usuario() {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Nombre', 'Apellido', '1990-01-01') returning id_usuario", codigo + "@prueba.com", codigo);
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
        turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) values (?,?,?,'JUEVES','09:00','10:00',3,?,'ACTIVO') returning id_turno_semanal", medico, especialidad, consultorio, HOY);
        ocurrencia = ocurrencia(HOY.plusDays(1));
    }

    Long ocurrencia(LocalDate fecha) {
        return id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) values (?, ?,3,'09:00','10:00','HABILITADA') returning id_ocurrencia_turno", turno, fecha);
    }

    Long reservaDirecta(int numero, String estado) {
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) values (?,?,?,?,CURRENT_TIMESTAMP) returning id_reserva", paciente, ocurrencia, numero, estado);
    }

    void error(int estado, Runnable accion) {
        assertEquals(estado, assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value());
    }

    @Test void registraNumeroElegidoYHoraEstimada() {
        var respuesta = reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 2));
        assertNotNull(respuesta.idReserva());
        assertEquals(2, respuesta.numero());
        assertEquals(LocalTime.of(9, 20), respuesta.horaEstimada());
        assertEquals(paciente, id("select id_paciente from reserva where id_reserva=?", respuesta.idReserva()));
        assertEquals("VIGENTE", jdbc.queryForObject("select reserva_estado from reserva where id_reserva=?", String.class, respuesta.idReserva()));
    }

    @Test void listaOcupadosYLiberaCanceladosYAnulados() {
        reservaDirecta(1, "CANCELADA");
        reservaDirecta(2, "ANULADA");
        reservaDirecta(3, "ORDEN_EMITIDA");
        var disponibles = turnos.listarOcurrenciasDisponibles(especialidad, paciente);
        assertEquals(1, disponibles.size());
        assertEquals(List.of(3), disponibles.get(0).numerosOcupados());
        reservas.reservarOrden(otroPaciente, new ReservaRequestDTO(ocurrencia, 1));
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 3)));
    }

    @Test void ocultaTurnosCompletos() {
        for (int n = 1; n <= 3; n++) reservaDirecta(n, "VIGENTE");
        assertTrue(turnos.listarOcurrenciasDisponibles(especialidad, paciente).isEmpty());
    }

    @Test void limitaFechasYOcultaNumerosPasadosDeHoy() {
        Long pasado = ocurrencia(HOY.minusDays(1));
        Long lejano = ocurrencia(HOY.plusWeeks(8).plusDays(1));
        Long hoy = ocurrencia(HOY);
        var lista = turnos.listarOcurrenciasDisponibles(especialidad, paciente);
        assertEquals(2, lista.size());
        assertEquals(List.of(1), lista.get(0).numerosOcupados());
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(pasado, 1)));
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(lejano, 1)));
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(hoy, 1)));
    }

    @Test void rechazaNumeroFueraDeCupoYTurnoInexistente() {
        error(400, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 4)));
        error(404, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(Long.MAX_VALUE, 1)));
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new ReservaRequestDTO(null, 0)).isEmpty());
        }
    }

    @Test void rechazaMedicoConsultorioYEspecialidadInactivos() {
        jdbc.update("update usuario set estado='INACTIVO' where id_usuario=?", medico);
        assertTrue(turnos.listarOcurrenciasDisponibles(especialidad, paciente).isEmpty());
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
        jdbc.update("update usuario set estado='ACTIVO' where id_usuario=?", medico);
        jdbc.update("update consultorio set estado='INACTIVO' where codigo=?", consultorio);
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
        jdbc.update("update consultorio set estado='ACTIVO' where codigo=?", consultorio);
        jdbc.update("update especialidad set estado='INACTIVO' where codigo=?", especialidad);
        error(409, () -> turnos.listarOcurrenciasDisponibles(especialidad, paciente));
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
    }

    @Test void suspensionesYVigenciaImpidenReservar() {
        Long funcionario = id("select id_usuario from funcionario limit 1");
        jdbc.update("insert into suspension_turno(id_turno_semanal,id_funcionario,suspension_tipo,fecha_inicio,fecha_fin,fecha_registro) values (?,?,'TEMPORAL',?,?,CURRENT_TIMESTAMP)", turno, funcionario, HOY, HOY.plusDays(2));
        assertTrue(turnos.listarOcurrenciasDisponibles(especialidad, paciente).isEmpty());
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
        jdbc.update("update suspension_turno set fecha_rehabilitacion=CURRENT_TIMESTAMP where id_turno_semanal=?", turno);
        jdbc.update("update turno_semanal set vigente_hasta=? where id_turno_semanal=?", HOY, turno);
        error(409, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
    }

    @Test void exigePaseAlConsultarYAlConfirmar() {
        jdbc.update("update especialidad set requiere_pase=true where codigo=?", especialidad);
        error(403, () -> turnos.listarOcurrenciasDisponibles(especialidad, paciente));
        error(403, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
        assertEquals(0L, id("select count(*) from reserva where id_ocurrencia_turno=?", ocurrencia));
    }

    Long pase(LocalDateTime emision) {
        Long historia = id("insert into historia_clinica(id_paciente,fecha_apertura) values (?,CURRENT_TIMESTAMP) returning id_historia_clinica", paciente);
        Long anterior = reservaDirecta(3, "ATENDIDA");
        Long funcionario = id("select id_usuario from funcionario limit 1");
        Long orden = id("insert into orden(id_paciente,id_funcionario,id_reserva,fecha_emision,orden_estado) values (?,?,?,CURRENT_TIMESTAMP,'UTILIZADA') returning numero_orden", paciente, funcionario, anterior);
        Long consulta = id("insert into consulta(id_paciente,id_medico,id_historia_clinica,id_reserva,numero_orden,fecha,motivo,fecha_registro) values (?,?,?,?,?,CURRENT_TIMESTAMP,'Prueba',CURRENT_TIMESTAMP) returning id_consulta", paciente, medico, historia, anterior, orden);
        return id("insert into pase(id_paciente,id_consulta,especialidad_origen,especialidad_destino,fecha_emision,pase_estado) values (?,?,?,?,?,'VIGENTE') returning id_pase", paciente, consulta, especialidad, especialidad, emision);
    }

    @Test void vinculaPaseVigenteYNoAceptaPaseAjeno() {
        Long pase = pase(HOY.atTime(9, 0).minusDays(2));
        jdbc.update("update especialidad set requiere_pase=true where codigo=?", especialidad);
        assertFalse(turnos.listarOcurrenciasDisponibles(especialidad, paciente).isEmpty());
        error(403, () -> reservas.reservarOrden(otroPaciente, new ReservaRequestDTO(ocurrencia, 1)));
        var reserva = reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1));
        assertEquals(pase, id("select id_pase from reserva where id_reserva=?", reserva.idReserva()));
    }

    @Test void paseVencidoOUsadoNoHabilitaReserva() {
        Long pase = pase(HOY.atTime(9, 0).minusDays(60).minusSeconds(1));
        jdbc.update("update especialidad set requiere_pase=true where codigo=?", especialidad);
        error(403, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
        jdbc.update("update pase set fecha_emision=?, pase_estado='USADO' where id_pase=?", HOY.atTime(8, 0), pase);
        error(403, () -> reservas.reservarOrden(paciente, new ReservaRequestDTO(ocurrencia, 1)));
    }

    @Test void soloUnaReservaConcurrenteObtieneElNumero() throws Exception {
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        try {
            var tareas = List.of(paciente, otroPaciente).stream().map(id -> ejecutor.submit(() -> {
                salida.await();
                try { reservas.reservarOrden(id, new ReservaRequestDTO(ocurrencia, 2)); return 201; }
                catch (ResponseStatusException error) { return error.getStatusCode().value(); }
            })).toList();
            salida.countDown();
            var estados = List.of(tareas.get(0).get(15, TimeUnit.SECONDS), tareas.get(1).get(15, TimeUnit.SECONDS));
            assertTrue(estados.contains(201));
            assertTrue(estados.contains(409));
            assertEquals(1L, id("select count(*) from reserva where id_ocurrencia_turno=? and numero=2", ocurrencia));
        } finally { ejecutor.shutdownNow(); }
    }
}
