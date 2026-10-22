package com.mediturno.backend.ordenes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import com.mediturno.backend.ordenes.dto.EnvioOrdenResponseDTO;
import com.mediturno.backend.ordenes.dto.OrdenResponseDTO;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.reservas.ReservaService;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;

/** CU-15 (consulta de una reserva) y CU-16 (emisión de la orden) contra PostgreSQL. «Ahora» es el 14/10/2026 a las 09:00. */
@SpringBootTest
class OrdenIntegracionTest extends BasePostgresEmbebida {

    static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "ordenes");
        registro.add("spring.jpa.show-sql", () -> "false");
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            return Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneId.of("America/Montevideo")); // 09:00 en Montevideo
        }
    }

    @MockitoBean JavaMailSender correo;

    @Autowired JdbcTemplate jdbc;
    @Autowired ReservaService reservas;
    @Autowired OrdenService ordenes;

    String especialidad;
    String consultorio;
    Long medico;
    Long paciente;
    Long funcionario;
    Long turno;

    Long id(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    Long usuario() {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Ana', 'López', '1990-01-01') returning id_usuario", codigo + "@prueba.com", codigo);
    }

    @BeforeEach
    void preparar() {
        when(correo.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
        especialidad = "E" + UUID.randomUUID().toString().substring(0, 15);
        consultorio = "C" + UUID.randomUUID().toString().substring(0, 15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')", especialidad, "Cardiología " + especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,?, 'Piso 1','ACTIVO')", consultorio, "Consultorio 3");
        medico = usuario();
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", medico, "MAT" + medico);
        jdbc.update("insert into medico_especialidad(id_medico,codigo_especialidad) values (?,?)", medico, especialidad);
        paciente = usuario();
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", paciente);
        funcionario = usuario();
        jdbc.update("insert into funcionario(id_usuario) values (?)", funcionario);
        turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,"
            + "vigente_desde,estado) values (?,?,?,'MIERCOLES','09:00','10:00',3,?,'ACTIVO') returning id_turno_semanal",
            medico, especialidad, consultorio, HOY.minusDays(30));
    }

    /** Una reserva en un turno de 3 números que va de {@code inicio} a {@code fin} (cada número dura un tercio del horario). */
    Long reserva(LocalDate fecha, String inicio, String fin, int numero, String estado) {
        Long ocurrencia = id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) "
            + "values (?,?,3,?::time,?::time,'HABILITADA') returning id_ocurrencia_turno", turno, fecha, inicio, fin);
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) "
            + "values (?,?,?,?,CURRENT_TIMESTAMP) returning id_reserva", paciente, ocurrencia, numero, estado);
    }

    void conflicto(Runnable accion) {
        assertEquals(409, assertThrows(ResponseStatusException.class, accion::run).getStatusCode().value());
    }

    String estadoDeReserva(Long idReserva) {
        return jdbc.queryForObject("select reserva_estado from reserva where id_reserva=?", String.class, idReserva);
    }

    long cantidadDeOrdenes(Long idReserva) {
        return id("select count(*) from orden where id_reserva=?", idReserva);
    }

    // ------------------------------------------------------------------ CU-15

    @Test
    void consultaMuestraLosDatosDeLaReserva() {
        Long idReserva = reserva(HOY, "14:00", "17:00", 2, "VIGENTE");

        ReservaResponseDTO datos = reservas.buscarReservaPorId(idReserva);

        assertEquals(idReserva, datos.idReserva());
        assertEquals("López, Ana", datos.nombrePaciente());
        assertEquals("López, Ana", datos.nombreMedico());
        assertEquals("Cardiología " + especialidad, datos.especialidad());
        assertEquals(HOY, datos.fecha());
        assertEquals(LocalTime.of(14, 0), datos.horaInicio());
        assertEquals(LocalTime.of(17, 0), datos.horaFin());
        assertEquals("Consultorio 3", datos.consultorio());
        assertEquals(2, datos.numero());
        assertEquals(LocalTime.of(15, 0), datos.horaEstimada());
        assertEquals(EstadoReserva.VIGENTE, datos.estado());
        assertNull(datos.numeroOrden());
    }

    @Test
    void consultaMuestraUnaReservaCanceladaConSuEstado() {
        Long idReserva = reserva(HOY, "14:00", "17:00", 1, "CANCELADA");

        assertEquals(EstadoReserva.CANCELADA, reservas.buscarReservaPorId(idReserva).estado());
    }

    @Test
    void consultaRechazaUnIdentificadorInvalidoOInexistente() {
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> reservas.buscarReservaPorId(0L)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> reservas.buscarReservaPorId(-5L)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> reservas.buscarReservaPorId(null)).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> reservas.buscarReservaPorId(987654321L)).getStatusCode().value());
    }

    @Test
    void consultaIncluyeElNumeroDeOrdenSiYaSeEmitio() {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE");
        OrdenResponseDTO orden = ordenes.emitirOrden(idReserva, funcionario);

        assertEquals(orden.numeroOrden(), reservas.buscarReservaPorId(idReserva).numeroOrden());
        assertEquals(EstadoReserva.ORDEN_EMITIDA, reservas.buscarReservaPorId(idReserva).estado());
    }

    // ------------------------------------------------------------------ CU-16

    @Test
    void emiteLaOrdenYCambiaElEstadoDeLaReserva() {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE"); // turno 09:30-12:30: se puede emitir de 08:30 a 12:30

        OrdenResponseDTO orden = ordenes.emitirOrden(idReserva, funcionario);

        assertNotNull(orden.numeroOrden());
        assertEquals(idReserva, orden.idReserva());
        assertEquals(EstadoOrden.EMITIDA, orden.estado());
        assertEquals(HOY.atTime(9, 0), orden.fechaEmision());
        assertEquals("ORDEN_EMITIDA", estadoDeReserva(idReserva));
        assertEquals(funcionario, id("select id_funcionario from orden where numero_orden=?", orden.numeroOrden()));
        assertEquals(paciente, id("select id_paciente from orden where numero_orden=?", orden.numeroOrden()));
    }

    @Test
    void puedeEmitirseJustoUnaHoraAntesDeQueEmpieceElTurno() {
        Long idReserva = reserva(HOY, "10:00", "13:00", 1, "VIGENTE"); // la ventana abre a las 09:00 y «ahora» es 09:00

        assertNotNull(ordenes.emitirOrden(idReserva, funcionario).numeroOrden());
    }

    @Test
    void puedeEmitirseJustoCuandoTerminaElTurno() {
        Long idReserva = reserva(HOY, "06:00", "09:00", 1, "VIGENTE"); // termina a las 09:00 y «ahora» es 09:00

        assertNotNull(ordenes.emitirOrden(idReserva, funcionario).numeroOrden());
    }

    @Test
    void puedeEmitirseEnCualquierNumeroDelTurnoAunqueSuHoraEstimadaYaPaso() {
        Long idReserva = reserva(HOY, "08:00", "11:00", 1, "VIGENTE"); // el número 1 era para las 08:00, pero el turno sigue

        assertNotNull(ordenes.emitirOrden(idReserva, funcionario).numeroOrden());
    }

    @Test
    void antesDeLaVentanaNoGuardaLaOrdenNiCambiaLaReserva() {
        Long idReserva = reserva(HOY, "10:10", "13:10", 1, "VIGENTE"); // la ventana abre a las 09:10

        conflicto(() -> ordenes.emitirOrden(idReserva, funcionario));

        assertEquals("VIGENTE", estadoDeReserva(idReserva));
        assertEquals(0, cantidadDeOrdenes(idReserva));
    }

    @Test
    void despuesDeQueTerminoElTurnoNoSeEmite() {
        Long idReserva = reserva(HOY, "07:00", "08:59", 1, "VIGENTE");

        conflicto(() -> ordenes.emitirOrden(idReserva, funcionario));

        assertEquals("VIGENTE", estadoDeReserva(idReserva));
        assertEquals(0, cantidadDeOrdenes(idReserva));
    }

    @Test
    void noSeEmiteAntesDelDiaDeLaConsulta() {
        Long idReserva = reserva(HOY.plusDays(1), "14:00", "17:00", 1, "VIGENTE");

        conflicto(() -> ordenes.emitirOrden(idReserva, funcionario));

        assertEquals(0, cantidadDeOrdenes(idReserva));
    }

    @Test
    void noSeEmiteSiLaReservaNoEstaVigente() {
        for (String estado : new String[] {"CANCELADA", "ANULADA", "VENCIDA", "ATENDIDA", "ORDEN_EMITIDA"}) {
            Long idReserva = reserva(HOY, "09:30", "12:30", 1, estado);
            conflicto(() -> ordenes.emitirOrden(idReserva, funcionario));
            assertEquals(0, cantidadDeOrdenes(idReserva));
            jdbc.update("delete from reserva where id_reserva=?", idReserva);
            jdbc.update("delete from ocurrencia_turno where id_turno_semanal=?", turno);
        }
    }

    @Test
    void noSeEmiteDosVecesLaMismaOrden() {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE");
        ordenes.emitirOrden(idReserva, funcionario);

        conflicto(() -> ordenes.emitirOrden(idReserva, funcionario));

        assertEquals(1, cantidadDeOrdenes(idReserva));
    }

    @Test
    void unaReservaInexistenteNoSePuedeEmitir() {
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> ordenes.emitirOrden(987654321L, funcionario)).getStatusCode().value());
    }

    @Test
    void generaElPdfDeLaOrdenEmitida() {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE");
        OrdenResponseDTO orden = ordenes.emitirOrden(idReserva, funcionario);

        byte[] pdf = ordenes.generarOrdenPDF(orden.numeroOrden());

        assertTrue(pdf.length > 500);
        assertArrayEquals("%PDF".getBytes(), Arrays.copyOf(pdf, 4));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> ordenes.generarOrdenPDF(987654321L)).getStatusCode().value());
    }

    @Test
    void enviaElPdfDeLaOrdenPorCorreoAlPaciente() throws Exception {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE");
        OrdenResponseDTO orden = ordenes.emitirOrden(idReserva, funcionario);

        EnvioOrdenResponseDTO envio = ordenes.enviarOrdenPorCorreo(orden.numeroOrden());

        String correoDelPaciente = jdbc.queryForObject("select correo from usuario where id_usuario=?", String.class, paciente);
        assertEquals(correoDelPaciente, envio.destino());
        ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
        verify(correo).send(enviado.capture());
        assertEquals(correoDelPaciente, enviado.getValue().getAllRecipients()[0].toString());
        assertEquals("MeDiTurno: su orden de consulta", enviado.getValue().getSubject());
    }

    @Test
    void siElCorreoFallaResponde503() {
        Long idReserva = reserva(HOY, "09:30", "12:30", 1, "VIGENTE");
        OrdenResponseDTO orden = ordenes.emitirOrden(idReserva, funcionario);
        doThrow(new MailSendException("sin servidor")).when(correo).send(any(MimeMessage.class));

        assertEquals(503, assertThrows(ResponseStatusException.class, () -> ordenes.enviarOrdenPorCorreo(orden.numeroOrden()))
            .getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> ordenes.enviarOrdenPorCorreo(987654321L))
            .getStatusCode().value());
    }
}
