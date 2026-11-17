package com.mediturno.backend.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.dto.CriteriosBusquedaDTO;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultas.ConsultaService;
import com.mediturno.backend.consultas.dto.ConsultaResponseDTO;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.reservas.dto.ReservaResponseDTO;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.usuarios.model.Medico;

/** Historiales CU-10 y CU-26: filtros, orden, y restricción a la persona autenticada. */
class HistorialesIntegracionTest extends PostgresIntegracionTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired ReservaService reservas;
    @Autowired ConsultaService consultas;

    private Especialidad cardiologia;
    private Especialidad pediatria;
    private Consultorio consultorio;
    private Medico medico;
    private Medico otroMedico;
    private Long paciente;
    private Long otroPaciente;
    private Long idConsultaAntigua;
    private Long idConsultaReciente;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString().substring(0, 12);
        cardiologia = especialidad("CAR" + sufijo);
        pediatria = especialidad("PED" + sufijo);
        consultorio = consultorio("C" + sufijo, EstadoActivacion.ACTIVO);
        medico = medico("M" + sufijo, EstadoActivacion.ACTIVO, cardiologia, pediatria);
        otroMedico = medico("O" + sufijo, EstadoActivacion.ACTIVO, cardiologia);
        paciente = paciente("P" + sufijo);
        otroPaciente = paciente("X" + sufijo);

        TurnoSemanal turnoCardiologia = turno(medico, cardiologia, consultorio, DiaSemana.LUNES,
            "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        TurnoSemanal turnoPediatria = turno(medico, pediatria, consultorio, DiaSemana.MARTES,
            "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        TurnoSemanal turnoOtroMedico = turno(otroMedico, cardiologia, consultorio, DiaSemana.MIERCOLES,
            "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);

        reserva(turnoCardiologia, paciente, HOY.minusDays(3), "CANCELADA", false);
        long reservaAntigua = reserva(turnoCardiologia, paciente, HOY.minusDays(2), "ATENDIDA", true);
        long reservaReciente = reserva(turnoPediatria, paciente, HOY.minusDays(1), "ATENDIDA", true);
        long reservaAjena = reserva(turnoCardiologia, otroPaciente, HOY.minusDays(1), "ATENDIDA", true);
        long reservaOtroMedico = reserva(turnoOtroMedico, paciente, HOY.minusDays(1), "ATENDIDA", true);

        idConsultaAntigua = consulta(reservaAntigua, paciente, medico, HOY.minusDays(2));
        idConsultaReciente = consulta(reservaReciente, paciente, medico, HOY.minusDays(1));
        consulta(reservaAjena, otroPaciente, medico, HOY.minusDays(1));
        consulta(reservaOtroMedico, paciente, otroMedico, HOY.minusDays(1));
    }

    @Test
    void cu10FiltraPorPacienteEstadoEspecialidadYFechasIncluyendoOrden() {
        List<ReservaResponseDTO> historial = reservas.consultarHistorialReservas(paciente,
            new CriteriosBusquedaDTO(medico.getIdUsuario(), cardiologia.getCodigo(), null, null, "ATENDIDA", "fecha", false,
                HOY.minusDays(4), HOY));

        assertEquals(1, historial.size());
        assertEquals(EstadoReserva.ATENDIDA, historial.get(0).estado());
        assertEquals(cardiologia.getNombre(), historial.get(0).especialidad());
        assertEquals(paciente, historial.get(0).idPaciente());
        assertNotNull(historial.get(0).numeroOrden());
        assertEquals(EstadoOrden.UTILIZADA, historial.get(0).estadoOrden());
        assertEquals(EstadoReserva.CANCELADA,
            reservas.consultarHistorialReservas(paciente,
                new CriteriosBusquedaDTO(null, null, null, null, "CANCELADA", null, null)).get(0).estado());
    }

    @Test
    void cu10ValidaEstadoYRangoDeFechas() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> reservas.consultarHistorialReservas(paciente,
                new CriteriosBusquedaDTO(null, null, null, null, "INVALIDO", null, null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> reservas.consultarHistorialReservas(paciente,
                new CriteriosBusquedaDTO(null, null, null, null, null, null, null, HOY, HOY.minusDays(1)))).getStatusCode());
    }

    @Test
    void cu26DevuelveSoloConsultasDelMedicoAutenticadoConFiltrosYOrden() {
        List<ConsultaResponseDTO> historial = consultas.consultarHistorialAtencionesPropias(medico.getIdUsuario(),
            new CriteriosBusquedaDTO(null, pediatria.getCodigo(), null, null, null, "fecha", null,
                HOY.minusDays(3), HOY));

        assertEquals(1, historial.size());
        assertEquals(idConsultaReciente, historial.get(0).idConsulta());
        assertEquals("Especialidad " + pediatria.getCodigo(), historial.get(0).especialidad());
        assertEquals(idConsultaAntigua, consultas.consultarHistorialAtencionesPropias(medico.getIdUsuario(),
            new CriteriosBusquedaDTO(null, cardiologia.getCodigo(), null, null, null, "fecha", false,
                HOY.minusDays(3), HOY)).get(0).idConsulta());
    }

    @Test
    void cu10DetalleSoloDeLaPersonaAutenticada() {
        Long id = reservas.consultarHistorialReservas(paciente, CriteriosBusquedaDTO.vacio()).get(0).idReserva();
        assertEquals(paciente, reservas.consultarDetalleReservaPropia(paciente, id).idPaciente());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> reservas.consultarDetalleReservaPropia(otroPaciente, id)).getStatusCode());
    }

    @Test
    void cu19IncluyeEstadosYOrdenesYRespetaFiltrosDeTurnoFechaYMedico() {
        var criterios = new CriteriosBusquedaDTO(medico.getIdUsuario(), cardiologia.getCodigo(), null, null,
            "ATENDIDA", "paciente", false, HOY.minusDays(2), HOY.minusDays(1));
        var lista = reservas.listarReservasParaAtencion(criterios);
        assertEquals(2, lista.size());
        assertNotNull(lista.get(0).numeroOrden());
        assertEquals(EstadoOrden.UTILIZADA, lista.get(0).estadoOrden());
        Long turno = lista.get(0).idTurnoSemanal();
        var porTurno = reservas.listarReservasParaAtencion(new CriteriosBusquedaDTO(null, null, null, null,
            null, "numero", true, null, null, null, null, null, null, null, turno));
        assertEquals(3, porTurno.size());
        assertEquals(true, porTurno.stream().allMatch(r -> turno.equals(r.idTurnoSemanal())));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> reservas.listarReservasParaAtencion(new CriteriosBusquedaDTO(null, null, null, null, null, "invalido", null))).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
            () -> reservas.listarReservasParaAtencion(new CriteriosBusquedaDTO(null, null, null, null, "invalido", null, null))).getStatusCode());
    }

    @Test
    void cu26BuscaPorNombreYCedulaYProtegeElDetallePropio() {
        var filtro = new CriteriosBusquedaDTO(null, null, null, null, null, "fecha", null,
            null, null, "Ana", null);
        assertEquals(3, consultas.consultarHistorialAtencionesPropias(medico.getIdUsuario(), filtro).size());
        String cedula = jdbc.queryForObject("select cedula from usuario where id_usuario = ?", String.class, paciente);
        var porCedula = new CriteriosBusquedaDTO(null, null, null, null, null, null, null,
            null, null, cedula, null);
        assertEquals(2, consultas.consultarHistorialAtencionesPropias(medico.getIdUsuario(), porCedula).size());
        assertEquals(idConsultaAntigua, consultas.consultarDetalleAtencionPropia(medico.getIdUsuario(), idConsultaAntigua).idConsulta());
        assertEquals(HttpStatus.NOT_FOUND, assertThrows(ResponseStatusException.class,
            () -> consultas.consultarDetalleAtencionPropia(otroMedico.getIdUsuario(), idConsultaAntigua)).getStatusCode());
    }

    private Long paciente(String sufijo) {
        Long id = jdbc.queryForObject("insert into usuario(correo,estado,fecha_creacion,cedula,nombre,apellido,fecha_nacimiento) "
            + "values (?, 'ACTIVO', CURRENT_TIMESTAMP, ?, 'Ana', ?, '1990-01-01') returning id_usuario",
            Long.class, sufijo + "@prueba.com", sufijo, sufijo);
        jdbc.update("insert into paciente(id_usuario,fecha_alta) values (?,CURRENT_TIMESTAMP)", id);
        return id;
    }

    private Long reserva(TurnoSemanal turno, Long idPaciente, LocalDate fecha, String estado, boolean conOrden) {
        Long idOcurrencia = jdbc.queryForObject("insert into ocurrencia_turno(id_turno_semanal,fecha,cupos,hora_inicio,hora_fin,ocurrencia_turno_estado) "
            + "values (?, ?, 10, '09:00', '12:00', 'FINALIZADA') returning id_ocurrencia_turno",
            Long.class, turno.getIdTurnoSemanal(), fecha);
        Long id = jdbc.queryForObject("insert into reserva(id_paciente,id_ocurrencia_turno,numero,reserva_estado,fecha_reserva) "
            + "values (?, ?, 1, ?, CURRENT_TIMESTAMP) returning id_reserva", Long.class, idPaciente, idOcurrencia, estado);
        if (conOrden) {
            Long idFuncionario = funcionario("F" + UUID.randomUUID().toString().substring(0, 10)).getIdUsuario();
            jdbc.update("insert into orden(id_paciente,id_funcionario,id_reserva,fecha_emision,orden_estado) "
                + "values (?, ?, ?, CURRENT_TIMESTAMP, 'UTILIZADA')", idPaciente, idFuncionario, id);
        }
        return id;
    }

    private Long consulta(Long idReserva, Long idPaciente, Medico medico, LocalDate fecha) {
        Long idOrden = jdbc.queryForObject("select numero_orden from orden where id_reserva = ?", Long.class, idReserva);
        Long idHistoria = jdbc.queryForObject("insert into historia_clinica(id_paciente,fecha_apertura) "
            + "values (?, CURRENT_TIMESTAMP) returning id_historia_clinica", Long.class, idPaciente);
        return jdbc.queryForObject("insert into consulta(id_paciente,id_medico,id_historia_clinica,id_reserva,numero_orden,fecha,motivo,fecha_registro) "
            + "values (?, ?, ?, ?, ?, ?, 'Control', CURRENT_TIMESTAMP) returning id_consulta",
            Long.class, idPaciente, medico.getIdUsuario(), idHistoria, idReserva, idOrden, fecha.atTime(10, 0));
    }
}
