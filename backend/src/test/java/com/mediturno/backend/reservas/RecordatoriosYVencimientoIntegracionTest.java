package com.mediturno.backend.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.notificaciones.EmailService;
import com.mediturno.backend.notificaciones.RecordatoriosService;

/**
 * Recordatorio 24 horas antes de la consulta (CU-8) y vencimiento de reservas, con PostgreSQL real y la hora fija en
 * 21/10/2026 09:00 (Montevideo). La reserva de estos casos es el número 6 de 6 en un turno de una hora: se atiende a los 50 minutos.
 */
@SpringBootTest
class RecordatoriosYVencimientoIntegracionTest extends BasePostgresEmbebida {
    static final LocalDate HOY = LocalDate.of(2026, 10, 21);
    static final LocalDateTime AHORA = HOY.atTime(9, 0);

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "recordatorios_vencimiento");
        registro.add("spring.jpa.show-sql", () -> "false");
        // Las tareas programadas no deben correr solas en medio de la prueba (la hora está fija).
        registro.add("mediturno.recordatorios.cron", () -> "-");
        registro.add("mediturno.reservas.vencimiento.cron", () -> "-");
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired RecordatoriosService recordatorios;
    @Autowired ReservasVencidasService vencimiento;
    @MockitoBean EmailService correo;

    Long paciente;
    Long medico;
    String especialidad;
    String consultorio;

    Long id(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    Long usuario() {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) values (?,'ACTIVO',CURRENT_TIMESTAMP,?,'Ana','López','1990-01-01') returning id_usuario",
            codigo + "@prueba.com", codigo);
    }

    @BeforeEach
    void preparar() {
        reset(correo);
        // Cada prueba arranca sin reservas de las anteriores: la base se comparte dentro de la clase.
        jdbc.update("delete from notificacion");
        jdbc.update("delete from reserva");
        especialidad = "E" + UUID.randomUUID().toString().substring(0, 15);
        consultorio = "C" + UUID.randomUUID().toString().substring(0, 15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')", especialidad, especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,?, 'Piso 1','ACTIVO')", consultorio, consultorio);
        medico = usuario();
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", medico, "MAT" + medico);
        jdbc.update("insert into medico_especialidad(id_medico,codigo_especialidad) values (?,?)", medico, especialidad);
        paciente = usuario();
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", paciente);
    }

    /** Una reserva (número 6 de 6) en un turno de una hora que empieza en {@code inicio}. */
    Long reserva(LocalDateTime inicio, String estado, LocalDateTime reservadaEn) {
        Long turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) values (?,?,?,'VIERNES',?,?,6,?,'ACTIVO') returning id_turno_semanal",
            medico, especialidad, consultorio, inicio.toLocalTime(), inicio.toLocalTime().plusHours(1), HOY.minusWeeks(2));
        Long ocurrencia = id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) values (?,?,6,?,?,'HABILITADA') returning id_ocurrencia_turno",
            turno, inicio.toLocalDate(), inicio.toLocalTime(), inicio.toLocalTime().plusHours(1));
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) values (?,?,6,?,?) returning id_reserva",
            paciente, ocurrencia, estado, reservadaEn);
    }

    /** Reserva cuya hora estimada de atención cae dentro de {@code horas} horas desde ahora. */
    Long reservaAtendidaEn(long horas, String estado, LocalDateTime reservadaEn) {
        return reserva(AHORA.plusHours(horas).minusMinutes(50), estado, reservadaEn);
    }

    String estado(Long reserva) {
        return jdbc.queryForObject("select reserva_estado from reserva where id_reserva=?", String.class, reserva);
    }

    Map<String, String> avisos(Long reserva) {
        Map<String, String> resultado = new java.util.TreeMap<>();
        jdbc.query("select canal, estado from notificacion where id_reserva=? and tipo='RECORDATORIO_CONSULTA'",
            rs -> { resultado.put(rs.getString("canal"), rs.getString("estado")); }, reserva);
        return resultado;
    }

    // ------------------------------------------------------------------ recordatorios

    @Test
    void recuerdaLaConsultaQueEstaAMenosDe24HorasUnaSolaVez() {
        Long reserva = reservaAtendidaEn(20, "VIGENTE", AHORA.minusDays(5));

        assertEquals(List.of(reserva), recordatorios.reservasParaRecordar());
        assertEquals(1, recordatorios.enviarRecordatoriosPendientes());

        assertEquals(Map.of("APLICACION", "ENVIADA", "EMAIL", "ENVIADA"), avisos(reserva));
        verify(correo).enviarRecordatorioConsulta(any(), any());
        String mensaje = jdbc.queryForObject("select mensaje from notificacion where id_reserva=? and canal='APLICACION'", String.class, reserva);
        assertTrue(mensaje.contains("22/10/2026") && mensaje.contains("número 6") && mensaje.contains(consultorio), mensaje);

        // Repetir la tarea no duplica el aviso.
        assertEquals(0, recordatorios.enviarRecordatoriosPendientes());
        assertEquals(2, avisos(reserva).size());
        assertEquals(2, id("select count(*) from notificacion where id_reserva=?", reserva).intValue());
    }

    @Test
    void noRecuerdaLoQueNoCorresponde() {
        Long lejana = reservaAtendidaEn(30, "VIGENTE", AHORA.minusDays(5));
        Long pasada = reservaAtendidaEn(-1, "VIGENTE", AHORA.minusDays(5));
        Long reservadaSobreLaHora = reservaAtendidaEn(20, "VIGENTE", AHORA.minusHours(2));
        Long cancelada = reservaAtendidaEn(20, "CANCELADA", AHORA.minusDays(5));
        Long anulada = reservaAtendidaEn(20, "ANULADA", AHORA.minusDays(5));
        Long conOrden = reservaAtendidaEn(20, "ORDEN_EMITIDA", AHORA.minusDays(5));

        assertEquals(0, recordatorios.enviarRecordatoriosPendientes());

        for (Long reserva : List.of(lejana, pasada, reservadaSobreLaHora, cancelada, anulada, conOrden)) {
            assertEquals(Map.of(), avisos(reserva), "reserva " + reserva);
        }
        verify(correo, never()).enviarRecordatorioConsulta(any(), any());
    }

    @Test
    void siElCorreoFallaQuedaFallidoYElAvisoEnLaAplicacionSigue() {
        Long reserva = reservaAtendidaEn(10, "VIGENTE", AHORA.minusDays(5));
        doThrow(new RuntimeException("sin servidor de correo")).when(correo).enviarRecordatorioConsulta(any(), any());

        assertEquals(1, recordatorios.enviarRecordatoriosPendientes());

        assertEquals(Map.of("APLICACION", "ENVIADA", "EMAIL", "FALLIDA"), avisos(reserva));
        // Ya quedó registrado: no se reintenta en el ciclo siguiente.
        assertEquals(0, recordatorios.enviarRecordatoriosPendientes());
        verify(correo).enviarRecordatorioConsulta(eq(jdbc.queryForObject("select correo from usuario where id_usuario=?", String.class, paciente)), any());
    }

    // ------------------------------------------------------------------ vencimiento

    @Test
    void vencenLasVigentesCuyoTurnoYaTermino() {
        Long ayer = reserva(AHORA.minusDays(1), "VIGENTE", AHORA.minusDays(9));
        Long hoyTerminado = reserva(AHORA.minusHours(3), "VIGENTE", AHORA.minusDays(9));
        Long horaDeFin = reserva(AHORA.minusHours(1), "VIGENTE", AHORA.minusDays(9));
        Long enCurso = reserva(AHORA.minusMinutes(30), "VIGENTE", AHORA.minusDays(9));
        Long futura = reserva(AHORA.plusDays(1), "VIGENTE", AHORA.minusDays(9));

        assertEquals(3, vencimiento.marcarVencidas());

        assertEquals("VENCIDA", estado(ayer));
        assertEquals("VENCIDA", estado(hoyTerminado));
        assertEquals("VENCIDA", estado(horaDeFin));
        assertEquals("VIGENTE", estado(enCurso));
        assertEquals("VIGENTE", estado(futura));
        assertEquals(0, vencimiento.marcarVencidas());
    }

    @Test
    void laOrdenEmitidaVenceRecienAlPasarElDiaYElRestoDeLosEstadosNoSeToca() {
        Long ordenAyer = reserva(AHORA.minusDays(1), "ORDEN_EMITIDA", AHORA.minusDays(9));
        Long ordenHoyTerminado = reserva(AHORA.minusHours(3), "ORDEN_EMITIDA", AHORA.minusDays(9));
        Long atendida = reserva(AHORA.minusDays(1), "ATENDIDA", AHORA.minusDays(9));
        Long cancelada = reserva(AHORA.minusDays(1), "CANCELADA", AHORA.minusDays(9));
        Long anulada = reserva(AHORA.minusDays(1), "ANULADA", AHORA.minusDays(9));

        assertEquals(1, vencimiento.marcarVencidas());

        assertEquals("VENCIDA", estado(ordenAyer));
        assertEquals("ORDEN_EMITIDA", estado(ordenHoyTerminado));
        assertEquals("ATENDIDA", estado(atendida));
        assertEquals("CANCELADA", estado(cancelada));
        assertEquals("ANULADA", estado(anulada));
    }
}
