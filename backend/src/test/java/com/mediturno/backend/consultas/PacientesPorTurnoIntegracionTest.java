package com.mediturno.backend.consultas;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
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
import org.springframework.web.server.ResponseStatusException;
import com.mediturno.backend.BasePostgresEmbebida;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.reservas.model.EstadoReserva;

/** CU-21 con PostgreSQL real: aislamiento por médico, fecha, estados y datos del listado. */
@SpringBootTest
class PacientesPorTurnoIntegracionTest extends BasePostgresEmbebida {
    static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @DynamicPropertySource
    static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "pacientes_por_turno");
        registro.add("spring.jpa.show-sql", () -> "false");
    }

    @TestConfiguration
    static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            // En UTC ya es el día siguiente; para el médico de Montevideo todavía es HOY.
            return Clock.fixed(Instant.parse("2026-10-15T01:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired ConsultaService consultas;
    Long medico;
    Long otroMedico;
    Long paciente;
    Long turno;
    Long ocurrencia;
    String especialidad;
    String consultorio;

    Long id(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }

    Long usuario(String nombre) {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) values (?,'ACTIVO',CURRENT_TIMESTAMP,?,?, 'Apellido','1990-01-01') returning id_usuario", codigo + "@prueba.com", codigo, nombre);
    }

    Long medico() {
        Long usuario = usuario("Médico");
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", usuario, "MAT" + usuario);
        return usuario;
    }

    @BeforeEach void preparar() {
        especialidad = "E" + UUID.randomUUID().toString().substring(0, 15);
        consultorio = "C" + UUID.randomUUID().toString().substring(0, 15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')", especialidad, "Cardiología " + especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,'Consultorio 1','Piso 1','ACTIVO')", consultorio);
        medico = medico();
        otroMedico = medico();
        paciente = usuario("Ana");
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", paciente);
        turno = id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) values (?,?,?,'MIERCOLES','09:00','10:00',6,?,'ACTIVO') returning id_turno_semanal", medico, especialidad, consultorio, HOY);
        ocurrencia = ocurrencia(HOY, "HABILITADA");
    }

    Long ocurrencia(LocalDate fecha, String estado) {
        return id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) values (?,?,6,'09:00','10:00',?) returning id_ocurrencia_turno", turno, fecha, estado);
    }

    Long reserva(int numero, String estado) {
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) values (?,?,?,?,CURRENT_TIMESTAMP) returning id_reserva", paciente, ocurrencia, numero, estado);
    }

    @Test void listaSoloLosTurnosPropiosDeLaFechaYUsaHoyDeMontevideo() {
        Long manana = ocurrencia(HOY.plusDays(1), "SUSPENDIDA");
        assertEquals(List.of(ocurrencia), consultas.consultarTurnosDeAtencion(medico, null).stream().map(t -> t.idOcurrenciaTurno()).toList());
        var lista = consultas.consultarTurnosDeAtencion(medico, HOY.plusDays(1));
        assertEquals(manana, lista.get(0).idOcurrenciaTurno());
        assertEquals("Cardiología " + especialidad, lista.get(0).nombreEspecialidad());
        assertEquals("Consultorio 1", lista.get(0).nombreConsultorio());
        assertTrue(consultas.consultarTurnosDeAtencion(otroMedico, HOY).isEmpty());
        assertTrue(consultas.consultarTurnosDeAtencion(medico, HOY.minusDays(1)).isEmpty());
    }

    @Test void listaVigentesYOrdenesPorNumeroConNombreHoraYEstado() {
        Long conOrden = reserva(3, "ORDEN_EMITIDA");
        Long sinOrden = reserva(1, "VIGENTE");
        reserva(2, "CANCELADA");
        reserva(4, "ANULADA");
        reserva(5, "ATENDIDA");
        reserva(6, "VENCIDA");
        Long funcionario = id("select id_usuario from funcionario limit 1");
        Long orden = id("insert into orden(id_paciente,id_funcionario,id_reserva,fecha_emision,orden_estado) values (?,?,?,CURRENT_TIMESTAMP,'EMITIDA') returning numero_orden", paciente, funcionario, conOrden);
        var lista = consultas.consultarPacientesPorTurno(medico, ocurrencia);
        assertEquals(List.of(sinOrden, conOrden), lista.stream().map(r -> r.idReserva()).toList());
        assertEquals(paciente, lista.get(0).idPaciente());
        assertEquals("Ana Apellido", lista.get(0).nombrePaciente());
        assertEquals(LocalTime.of(9, 0), lista.get(0).horaEstimada());
        assertEquals(EstadoReserva.VIGENTE, lista.get(0).estado());
        assertNull(lista.get(0).estadoOrden());
        assertNull(lista.get(0).numeroOrden());
        assertEquals(LocalTime.of(9, 20), lista.get(1).horaEstimada());
        assertEquals(EstadoReserva.ORDEN_EMITIDA, lista.get(1).estado());
        assertEquals(EstadoOrden.EMITIDA, lista.get(1).estadoOrden());
        assertEquals(orden, lista.get(1).numeroOrden());
    }

    @Test void turnoSinPacientesDevuelveListaVacia() {
        assertTrue(consultas.consultarPacientesPorTurno(medico, ocurrencia).isEmpty());
    }

    @Test void noRevelaPacientesDeOtroMedicoNiExistenciaDelTurno() {
        reserva(1, "VIGENTE");
        var ajeno = assertThrows(ResponseStatusException.class, () -> consultas.consultarPacientesPorTurno(otroMedico, ocurrencia));
        var inexistente = assertThrows(ResponseStatusException.class, () -> consultas.consultarPacientesPorTurno(medico, Long.MAX_VALUE));
        assertEquals(404, ajeno.getStatusCode().value());
        assertEquals(ajeno.getStatusCode(), inexistente.getStatusCode());
        assertEquals(ajeno.getReason(), inexistente.getReason());
    }
}
