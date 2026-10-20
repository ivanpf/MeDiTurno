package com.mediturno.backend.consultas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.validation.Validation;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultas.dto.RegistrarConsultaDTO;

/** CU-23 con PostgreSQL real: registro de la consulta, estados, historia clínica, aviso al paciente y errores. «Ahora» es el 14/10/2026 a las 09:30. */
@SpringBootTest
class RegistroConsultaIntegracionTest extends BasePostgresEmbebida {

    static final LocalDate HOY = LocalDate.of(2026, 10, 14);
    static final RegistrarConsultaDTO DATOS = new RegistrarConsultaDTO("Dolor de cabeza", "Presión normal", "Reposo");

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "registro_consulta");
        registro.add("spring.jpa.show-sql", () -> "false");
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-14T12:30:00Z"), ZoneId.of("America/Montevideo")); // 09:30 en Montevideo
        }
    }

    @MockitoBean JavaMailSender correo;

    @Autowired JdbcTemplate jdbc;
    @Autowired ConsultaService consultas;

    Long medico;
    Long otroMedico;
    Long paciente;
    Long funcionario;
    Long turno;
    int dias;

    Long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    Long usuario(String nombre, String apellido) {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?,'ACTIVO',CURRENT_TIMESTAMP,?,?,?,'1990-01-01') returning id_usuario", codigo + "@prueba.com", codigo, nombre, apellido);
    }

    Long medico() {
        Long usuario = usuario("Laura", "Gómez");
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", usuario, "MAT" + usuario);
        return usuario;
    }

    @BeforeEach
    void preparar() {
        String especialidad = "E" + UUID.randomUUID().toString().substring(0, 15);
        String consultorio = "C" + UUID.randomUUID().toString().substring(0, 15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')", especialidad, "Cardiología " + especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,'Consultorio 1','Piso 1','ACTIVO')", consultorio);
        medico = medico();
        otroMedico = medico();
        paciente = usuario("Ana", "Pérez");
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", paciente);
        funcionario = usuario("Fede", "Ruiz");
        jdbc.update("insert into funcionario(id_usuario) values (?)", funcionario);
        turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) "
            + "values (?,?,?,'MIERCOLES','09:00','12:00',6,?,'ACTIVO') returning id_turno_semanal", medico, especialidad, consultorio, HOY.minusDays(30));
        dias = 0;
    }

    /** Una reserva con su orden, en una fecha propia del turno (hay una sola ocurrencia por turno y fecha). */
    Long orden(String estadoReserva, String estadoOrden) {
        Long ocurrencia = id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) "
            + "values (?,?,6,'09:00','12:00','HABILITADA') returning id_ocurrencia_turno", turno, HOY.minusDays(dias++));
        Long reserva = id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) "
            + "values (?,?,1,?,CURRENT_TIMESTAMP) returning id_reserva", paciente, ocurrencia, estadoReserva);
        return id("insert into orden(id_paciente,id_funcionario,id_reserva,fecha_emision,orden_estado) "
            + "values (?,?,?,CURRENT_TIMESTAMP,?) returning numero_orden", paciente, funcionario, reserva, estadoOrden);
    }

    Long ordenEmitida() {
        return orden("ORDEN_EMITIDA", "EMITIDA");
    }

    String texto(String sql, Object... args) {
        return jdbc.queryForObject(sql, String.class, args);
    }

    long cantidad(String sql, Object... args) {
        return id(sql, args);
    }

    void rechazo(int estado, Long numeroOrden, Long idMedico) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> consultas.registrarConsulta(idMedico, numeroOrden, DATOS));
        assertEquals(estado, e.getStatusCode().value());
    }

    @Test
    void registraLaConsultaYActualizaLaReservaYLaOrden() {
        Long numeroOrden = ordenEmitida();

        ConsultaResponseDTO consulta = consultas.registrarConsulta(medico, numeroOrden, DATOS);

        assertNotNull(consulta.idConsulta());
        assertEquals(numeroOrden, consulta.numeroOrden());
        assertEquals("Pérez, Ana", consulta.nombrePaciente());
        assertEquals("Gómez, Laura", consulta.nombreMedico());
        assertTrue(consulta.especialidad().startsWith("Cardiología"));
        assertEquals(HOY.atTime(9, 30), consulta.fecha());
        assertEquals("Dolor de cabeza", consulta.motivo());
        assertEquals("Presión normal", consulta.observaciones());
        assertEquals("Reposo", consulta.indicaciones());
        assertEquals("ATENDIDA", texto("select r.reserva_estado from reserva r join orden o on o.id_reserva=r.id_reserva where o.numero_orden=?", numeroOrden));
        assertEquals("UTILIZADA", texto("select orden_estado from orden where numero_orden=?", numeroOrden));
        assertEquals(medico, id("select id_medico from consulta where numero_orden=?", numeroOrden));
    }

    @Test
    void abreLaHistoriaClinicaDelPacienteSiTodaviaNoTenia() {
        consultas.registrarConsulta(medico, ordenEmitida(), DATOS);

        assertEquals(1, cantidad("select count(*) from historia_clinica where id_paciente=?", paciente));
        assertEquals(1, cantidad("select count(*) from consulta c join historia_clinica h on h.id_historia_clinica=c.id_historia_clinica where h.id_paciente=?", paciente));
        assertNotNull(jdbc.queryForObject("select fecha_actualizacion from historia_clinica where id_paciente=?", java.sql.Timestamp.class, paciente));
    }

    @Test
    void usaLaHistoriaClinicaQueYaExiste() {
        Long historia = id("insert into historia_clinica(id_paciente,antecedentes,fecha_apertura) values (?,'Alergia a la penicilina',CURRENT_TIMESTAMP) returning id_historia_clinica", paciente);

        consultas.registrarConsulta(medico, ordenEmitida(), DATOS);
        consultas.registrarConsulta(medico, ordenEmitida(), DATOS);

        assertEquals(1, cantidad("select count(*) from historia_clinica where id_paciente=?", paciente));
        assertEquals(2, cantidad("select count(*) from consulta where id_historia_clinica=?", historia));
    }

    @Test
    void laOrdenInexistenteOLaDeOtroMedicoNoSeAceptanYNoSeDiferencian() {
        Long numeroOrden = ordenEmitida();

        ResponseStatusException inexistente = assertThrows(ResponseStatusException.class, () -> consultas.registrarConsulta(medico, 987654321L, DATOS));
        ResponseStatusException ajena = assertThrows(ResponseStatusException.class, () -> consultas.registrarConsulta(otroMedico, numeroOrden, DATOS));

        assertEquals(422, inexistente.getStatusCode().value());
        assertEquals(422, ajena.getStatusCode().value());
        assertEquals(inexistente.getReason(), ajena.getReason());
        assertEquals(0, cantidad("select count(*) from consulta where id_paciente=?", paciente));
        assertEquals("ORDEN_EMITIDA", texto("select r.reserva_estado from reserva r join orden o on o.id_reserva=r.id_reserva where o.numero_orden=?", numeroOrden));
    }

    @Test
    void unaOrdenQueNoEstaEmitidaNoSeAcepta() {
        rechazo(422, orden("ORDEN_EMITIDA", "UTILIZADA"), medico);
        rechazo(422, orden("CANCELADA", "EMITIDA"), medico);
        rechazo(422, orden("VIGENTE", "EMITIDA"), medico);
        assertEquals(0, cantidad("select count(*) from consulta where id_paciente=?", paciente));
    }

    @Test
    void laMismaConsultaNoSePuedeRegistrarDosVeces() {
        Long numeroOrden = ordenEmitida();
        consultas.registrarConsulta(medico, numeroOrden, DATOS);

        rechazo(409, numeroOrden, medico);

        assertEquals(1, cantidad("select count(*) from consulta where numero_orden=?", numeroOrden));
    }

    @Test
    void avisaAlPacienteEnLaAplicacionYPorCorreoSinContenidoClinico() {
        Long numeroOrden = ordenEmitida();

        ConsultaResponseDTO consulta = consultas.registrarConsulta(medico, numeroOrden, DATOS);

        List<String> canales = jdbc.queryForList("select canal || ':' || estado from notificacion where id_consulta=? and tipo='CONSULTA_REGISTRADA' and id_usuario=? order by canal",
            String.class, consulta.idConsulta(), paciente);
        assertEquals(List.of("APLICACION:ENVIADA", "EMAIL:ENVIADA"), canales);
        String mensaje = texto("select mensaje from notificacion where id_consulta=? and canal='APLICACION'", consulta.idConsulta());
        assertTrue(mensaje.contains("historia clínica"));
        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo).send(enviado.capture());
        String correoDelPaciente = texto("select correo from usuario where id_usuario=?", paciente);
        assertEquals(correoDelPaciente, enviado.getValue().getTo()[0]);
        for (String contenido : List.of(mensaje, enviado.getValue().getText(), enviado.getValue().getSubject())) {
            assertFalse(contenido.contains("Dolor de cabeza") || contenido.contains("Presión normal") || contenido.contains("Reposo"));
        }
    }

    @Test
    void siElCorreoFallaLaConsultaQuedaRegistradaYElAvisoFallido() {
        doThrow(new MailSendException("sin servidor")).when(correo).send(any(SimpleMailMessage.class));
        Long numeroOrden = ordenEmitida();

        ConsultaResponseDTO consulta = consultas.registrarConsulta(medico, numeroOrden, DATOS);

        assertEquals("ATENDIDA", texto("select r.reserva_estado from reserva r join orden o on o.id_reserva=r.id_reserva where o.numero_orden=?", numeroOrden));
        assertEquals("FALLIDA", texto("select estado from notificacion where id_consulta=? and canal='EMAIL'", consulta.idConsulta()));
        assertEquals("ENVIADA", texto("select estado from notificacion where id_consulta=? and canal='APLICACION'", consulta.idConsulta()));
    }

    @Test
    void siElRegistroSeRechazaNoSeAvisaAlPaciente() {
        rechazo(422, 987654321L, medico);

        verify(correo, never()).send(any(SimpleMailMessage.class));
        assertEquals(0, cantidad("select count(*) from notificacion where tipo='CONSULTA_REGISTRADA' and id_usuario=?", paciente));
    }

    @Test
    void laIndicacionesVaciasSeGuardanComoNuloYLosTextosSeRecortan() {
        Long numeroOrden = ordenEmitida();

        ConsultaResponseDTO consulta = consultas.registrarConsulta(medico, numeroOrden, new RegistrarConsultaDTO("  Control  ", " Todo bien ", "   "));

        assertEquals("Control", consulta.motivo());
        assertEquals("Todo bien", consulta.observaciones());
        assertNull(consulta.indicaciones());
    }

    @Test
    void elMotivoYLasObservacionesSonObligatoriosYLasIndicacionesNo() {
        var validador = Validation.buildDefaultValidatorFactory().getValidator();

        assertTrue(validador.validate(new RegistrarConsultaDTO("Control", "Todo bien", null)).isEmpty());
        var errores = validador.validate(new RegistrarConsultaDTO(" ", null, "x".repeat(5001))).stream()
            .map(v -> v.getPropertyPath().toString()).collect(Collectors.toSet());
        assertEquals(java.util.Set.of("motivo", "observaciones", "indicaciones"), errores);
    }
}
