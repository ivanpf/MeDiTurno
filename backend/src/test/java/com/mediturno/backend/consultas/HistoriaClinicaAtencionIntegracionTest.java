package com.mediturno.backend.consultas;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

/** CU-22 con PostgreSQL: vínculo, fecha, estados, antecedentes y revalidación de cada detalle. */
@SpringBootTest
class HistoriaClinicaAtencionIntegracionTest extends BasePostgresEmbebida {
    static final LocalDate HOY = LocalDate.of(2026, 10, 22);
    @DynamicPropertySource static void base(DynamicPropertyRegistry registro) {
        registrarBaseDeDatos(registro, "historia_durante_atencion");
        registro.add("spring.jpa.show-sql", () -> "false");
    }
    @TestConfiguration static class Configuracion {
        @Bean @Primary Clock relojPrueba() {
            // En UTC ya es el 23; en Montevideo la fecha autorizada sigue siendo el 22.
            return Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"), ZoneId.of("America/Montevideo"));
        }
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired ConsultaService consultas;
    Long medico, otroMedico, paciente, otroPaciente, funcionario, contexto;
    String especialidad, consultorio;
    Long id(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }
    Long usuario(String nombre) {
        String codigo = UUID.randomUUID().toString().substring(0, 15);
        return id("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) values (?,'ACTIVO',CURRENT_TIMESTAMP,?,?, 'Apellido','1990-01-01') returning id_usuario", codigo+"@prueba.com", codigo, nombre);
    }
    Long medico() {
        Long id = usuario("Médico");
        jdbc.update("insert into medico(id_usuario,matricula) values (?,?)", id,"MAT"+id);
        return id;
    }
    Long paciente() {
        Long id = usuario("Paciente");
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)",id);
        return id;
    }
    @BeforeEach void preparar() {
        especialidad="E"+UUID.randomUUID().toString().substring(0,15);
        consultorio="C"+UUID.randomUUID().toString().substring(0,15);
        jdbc.update("insert into especialidad(codigo,nombre,requiere_pase,estado) values (?,?,false,'ACTIVO')",especialidad,especialidad);
        jdbc.update("insert into consultorio(codigo,nombre,ubicacion,estado) values (?,'Consultorio de prueba','Piso 1','ACTIVO')",consultorio);
        medico=medico();otroMedico=medico();paciente=paciente();otroPaciente=paciente();
        funcionario=id("select id_usuario from funcionario limit 1");
        contexto=reserva(medico,paciente,HOY,"VIGENTE");
    }
    Long reserva(Long idMedico,Long idPaciente,LocalDate fecha,String estado) {
        Long turno=id("insert into turno_semanal(id_medico,codigo_especialidad,codigo_consultorio,dia,hora_inicio,hora_fin,cupo_maximo,vigente_desde,estado) values (?,?,?,'JUEVES','09:00','12:00',6,?,'ACTIVO') returning id_turno_semanal",idMedico,especialidad,consultorio,HOY.minusYears(1));
        Long ocurrencia=id("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) values (?,?,6,'09:00','12:00','HABILITADA') returning id_ocurrencia_turno",turno,fecha);
        return id("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) values (?,?,1,?,CURRENT_TIMESTAMP) returning id_reserva",idPaciente,ocurrencia,estado);
    }
    Long orden(Long reserva,String estado) {
        Long paciente=id("select id_paciente from reserva where id_reserva=?",reserva);
        return id("insert into orden(id_paciente,id_funcionario,id_reserva,fecha_emision,orden_estado) values (?,?,?,CURRENT_TIMESTAMP,?) returning numero_orden",paciente,funcionario,reserva,estado);
    }
    Long consulta(Long paciente,Long medico,LocalDateTime fecha) {
        Long reserva=reserva(medico,paciente,fecha.toLocalDate(),"ATENDIDA");
        Long orden=orden(reserva,"UTILIZADA");
        Long historia=id("insert into historia_clinica(id_paciente,fecha_apertura) values (?,?) returning id_historia_clinica",paciente,fecha);
        return id("insert into consulta(id_paciente,id_medico,id_historia_clinica,id_reserva,numero_orden,fecha,motivo,observaciones,indicaciones,fecha_registro) values (?,?,?,?,?,?,'Motivo anterior','Observaciones clínicas','Indicaciones anteriores',?) returning id_consulta",paciente,medico,historia,reserva,orden,fecha,fecha);
    }
    void error(int codigo,Runnable accion) {
        assertEquals(codigo,assertThrows(ResponseStatusException.class,accion::run).getStatusCode().value());
    }
    @Test void muestraAntecedentesDeOtrosMedicosOrdenadosSinModificarDatos() {
        Long antigua=consulta(paciente,otroMedico,HOY.minusMonths(2).atTime(10,0));
        Long reciente=consulta(paciente,medico,HOY.minusDays(1).atTime(9,0));
        consulta(otroPaciente,otroMedico,HOY.minusDays(2).atTime(9,0));
        Long cantidad=id("select count(*) from consulta");
        var lista=consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente);
        assertEquals(List.of(reciente,antigua),lista.stream().map(c->c.idConsulta()).toList());
        assertEquals(otroMedico,lista.get(1).idMedico());
        assertEquals("Motivo anterior",lista.get(1).motivo());
        assertEquals("Observaciones clínicas",lista.get(1).observaciones());
        assertEquals("Indicaciones anteriores",lista.get(1).indicaciones());
        assertEquals(cantidad,id("select count(*) from consulta"));
        assertEquals("VIGENTE",jdbc.queryForObject("select reserva_estado from reserva where id_reserva=?",String.class,contexto));
    }
    @Test void sinAntecedentesDevuelveListaVacia() {
        assertTrue(consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente).isEmpty());
    }
    @Test void exigeOrdenEmitidaRealYDelPaciente() {
        jdbc.update("update reserva set reserva_estado='ORDEN_EMITIDA' where id_reserva=?",contexto);
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
        Long orden=orden(contexto,"EMITIDA");
        assertTrue(consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente).isEmpty());
        jdbc.update("update orden set orden_estado='UTILIZADA' where numero_orden=?",orden);
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
    }
    @Test void rechazaMedicoAjenoPacienteSinVinculoEInexistente() {
        consulta(paciente,otroMedico,HOY.minusDays(1).atTime(10,0));
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(otroMedico,paciente));
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,otroPaciente));
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,Long.MAX_VALUE));
    }
    @Test void reservasNoVigentesNoHabilitanAcceso() {
        for(String estado:List.of("CANCELADA","ANULADA","ATENDIDA","VENCIDA")) {
            jdbc.update("update reserva set reserva_estado=? where id_reserva=?",estado,contexto);
            error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
        }
    }
    @Test void soloLaFechaLocalDelTurnoHabilitaElAcceso() {
        Long ocurrencia=id("select id_ocurrencia_turno from reserva where id_reserva=?",contexto);
        for(LocalDate fecha:List.of(HOY.minusDays(1),HOY.plusDays(1))) {
            jdbc.update("update ocurrencia_turno set fecha=? where id_ocurrencia_turno=?",fecha,ocurrencia);
            error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
        }
        jdbc.update("update ocurrencia_turno set fecha=? where id_ocurrencia_turno=?",HOY,ocurrencia);
        assertTrue(consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente).isEmpty());
    }
    @Test void turnoSuspendidoOConSuspensionVigenteNoHabilitaAcceso() {
        Long ocurrencia=id("select id_ocurrencia_turno from reserva where id_reserva=?",contexto);
        jdbc.update("update ocurrencia_turno set ocurrencia_turno_estado='SUSPENDIDA' where id_ocurrencia_turno=?",ocurrencia);
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
        jdbc.update("update ocurrencia_turno set ocurrencia_turno_estado='HABILITADA' where id_ocurrencia_turno=?",ocurrencia);
        Long turno=id("select id_turno_semanal from ocurrencia_turno where id_ocurrencia_turno=?",ocurrencia);
        jdbc.update("insert into suspension_turno(id_turno_semanal,id_funcionario,suspension_tipo,fecha_inicio,fecha_fin,fecha_registro) values (?,?,'TEMPORAL',?,?,CURRENT_TIMESTAMP)",turno,funcionario,HOY,HOY.plusDays(1));
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
    }
    @Test void detalleNoExponeOtraHistoriaYRevalidaElVinculo() {
        Long propia=consulta(paciente,otroMedico,HOY.minusDays(1).atTime(10,0));
        Long ajena=consulta(otroPaciente,otroMedico,HOY.minusDays(1).atTime(10,0));
        assertEquals(propia,consultas.consultarDetalleDuranteAtencion(medico,paciente,propia).idConsulta());
        error(404,()->consultas.consultarDetalleDuranteAtencion(medico,paciente,ajena));
        error(404,()->consultas.consultarDetalleDuranteAtencion(medico,paciente,Long.MAX_VALUE));
        jdbc.update("update reserva set reserva_estado='CANCELADA' where id_reserva=?",contexto);
        error(403,()->consultas.consultarDetalleDuranteAtencion(medico,paciente,propia));
        error(403,()->consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente));
    }
    @Test void noIncluyeConsultasFuturasNiAtencionesQueNoEstanFinalizadas() {
        Long futura=consulta(paciente,otroMedico,HOY.plusDays(1).atTime(10,0));
        Long invalida=consulta(paciente,otroMedico,HOY.minusDays(1).atTime(10,0));
        jdbc.update("update reserva set reserva_estado='ANULADA' where id_reserva=(select id_reserva from consulta where id_consulta=?)",invalida);
        assertTrue(consultas.consultarHistoriaClinicaDuranteAtencion(medico,paciente).isEmpty());
        error(404,()->consultas.consultarDetalleDuranteAtencion(medico,paciente,futura));
        error(404,()->consultas.consultarDetalleDuranteAtencion(medico,paciente,invalida));
    }
}
