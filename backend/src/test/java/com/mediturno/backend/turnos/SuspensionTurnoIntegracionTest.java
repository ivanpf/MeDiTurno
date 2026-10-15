package com.mediturno.backend.turnos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.mediturno.backend.PostgresIntegracionTest;
import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.consultorios.model.Consultorio;
import com.mediturno.backend.especialidades.model.Especialidad;
import com.mediturno.backend.notificaciones.model.CanalNotificacion;
import com.mediturno.backend.notificaciones.model.EstadoNotificacion;
import com.mediturno.backend.notificaciones.model.Notificacion;
import com.mediturno.backend.notificaciones.repository.NotificacionRepository;
import com.mediturno.backend.pacientes.model.Paciente;
import com.mediturno.backend.pacientes.repository.PacienteRepository;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.reservas.model.Reserva;
import com.mediturno.backend.reservas.repository.ReservaRepository;
import com.mediturno.backend.turnos.dto.SuspensionTurnoDTO;
import com.mediturno.backend.turnos.dto.SuspensionTurnoResponseDTO;
import com.mediturno.backend.turnos.dto.VistaPreviaSuspensionDTO;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.model.TipoSuspension;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.OcurrenciaTurnoRepository;
import com.mediturno.backend.usuarios.model.Funcionario;
import com.mediturno.backend.usuarios.model.Medico;

/** CU-13 Suspensión de un turno semanal, con una base PostgreSQL real. El servidor de correo es un doble. */
class SuspensionTurnoIntegracionTest extends PostgresIntegracionTest {

    @MockitoBean JavaMailSender correo;

    @Autowired TurnoSemanalService servicio;
    @Autowired ActualizadorEstadosPorSuspensionService actualizador;
    @Autowired DisponibilidadService disponibilidad;
    @Autowired OcurrenciaTurnoRepository ocurrencias;
    @Autowired ReservaRepository reservas;
    @Autowired PacienteRepository pacientes;
    @Autowired NotificacionRepository notificaciones;

    private TurnoSemanal turno;
    private Funcionario funcionario;
    private Paciente ana;
    private Paciente beto;
    /** Cuatro lunes seguidos a partir del próximo, con una ocurrencia y una reserva en cada uno. */
    private LocalDate lunes1;
    private LocalDate lunes2;
    private LocalDate lunes3;
    private LocalDate lunes4;
    private Reserva reserva1;
    private Reserva reserva2;
    private Reserva reserva3;
    private Reserva reserva4;

    @BeforeEach
    void datosBase() {
        Especialidad esp = especialidad("SUS");
        Consultorio cons = consultorio("S1", EstadoActivacion.ACTIVO);
        Medico med = medico("9501", EstadoActivacion.ACTIVO, esp);
        turno = turno(med, esp, cons, DiaSemana.LUNES, "09:00", "12:00", HOY.minusDays(30), null, EstadoTurnoSemanal.ACTIVO);
        funcionario = funcionario("9502");
        ana = paciente("9503");
        beto = paciente("9504");

        lunes1 = HOY.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        lunes2 = lunes1.plusWeeks(1);
        lunes3 = lunes1.plusWeeks(2);
        lunes4 = lunes1.plusWeeks(3);
        reserva1 = reserva(ana, ocurrencia(lunes1), 1, EstadoReserva.VIGENTE);
        reserva2 = reserva(ana, ocurrencia(lunes2), 1, EstadoReserva.VIGENTE);
        reserva3 = reserva(beto, ocurrencia(lunes3), 2, EstadoReserva.VIGENTE);
        reserva4 = reserva(beto, ocurrencia(lunes4), 3, EstadoReserva.VIGENTE);
    }

    // ------------------------------------------------------------------ datos de prueba

    private Paciente paciente(String id) {
        Paciente p = new Paciente();
        p.setCorreo(id + "@ejemplo.com");
        p.setEstado(EstadoActivacion.ACTIVO);
        p.setFechaCreacion(LocalDateTime.now());
        p.setCedula(id);
        p.setNombre("Paciente" + id);
        p.setApellido("Apellido" + id);
        p.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        p.setFechaAlta(LocalDateTime.now());
        return pacientes.save(p);
    }

    private OcurrenciaTurno ocurrencia(LocalDate fecha) {
        OcurrenciaTurno o = new OcurrenciaTurno();
        o.setTurnoSemanal(turno);
        o.setFecha(fecha);
        o.setCupos(10);
        o.setHoraInicio(LocalTime.of(9, 0));
        o.setHoraFin(LocalTime.of(12, 0));
        o.setOcurrenciaTurnoEstado(EstadoOcurrenciaTurno.HABILITADA);
        return ocurrencias.saveAndFlush(o);
    }

    private OcurrenciaTurno ocurrenciaDe(LocalDate fecha) {
        return ocurrencias.findAll().stream()
            .filter(o -> o.getTurnoSemanal().getIdTurnoSemanal().equals(turno.getIdTurnoSemanal()) && o.getFecha().equals(fecha))
            .findFirst().orElseThrow();
    }

    private Reserva reserva(Paciente paciente, OcurrenciaTurno ocurrencia, int numero, EstadoReserva estado) {
        Reserva r = new Reserva();
        r.setPaciente(paciente);
        r.setOcurrenciaTurno(ocurrencia);
        r.setNumero(numero);
        r.setReservaEstado(estado);
        r.setFechaReserva(LocalDateTime.now());
        return reservas.saveAndFlush(r);
    }

    private SuspensionTurnoResponseDTO suspender(TipoSuspension tipo, LocalDate desde, LocalDate hasta) {
        return servicio.suspenderTurno(turno.getIdTurnoSemanal(), new SuspensionTurnoDTO(tipo, desde, hasta, null), funcionario.getIdUsuario());
    }

    private EstadoOcurrenciaTurno estadoDe(LocalDate fecha) {
        return ocurrencias.findAll().stream()
            .filter(o -> o.getTurnoSemanal().getIdTurnoSemanal().equals(turno.getIdTurnoSemanal()) && o.getFecha().equals(fecha))
            .findFirst().orElseThrow().getOcurrenciaTurnoEstado();
    }

    private TurnoSemanal turnoActual() {
        return turnos.findById(turno.getIdTurnoSemanal()).orElseThrow();
    }

    private EstadoReserva estadoDe(Reserva reserva) {
        return reservas.findById(reserva.getIdReserva()).orElseThrow().getReservaEstado();
    }

    private ResponseStatusException rechazo(TipoSuspension tipo, LocalDate desde, LocalDate hasta) {
        return assertThrows(ResponseStatusException.class, () -> suspender(tipo, desde, hasta));
    }

    // ------------------------------------------------------------------ flujo principal

    @Test
    void unaSuspensionTemporalFuturaSuspendeSoloLasFechasDelPeriodoYAnulaSusReservas() {
        SuspensionTurnoResponseDTO respuesta = suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        // hasta la fecha desde, todo sigue igual; el turno sigue activo
        assertEquals(EstadoOcurrenciaTurno.HABILITADA, estadoDe(lunes1));
        assertEquals(EstadoReserva.VIGENTE, estadoDe(reserva1));
        assertEquals(EstadoTurnoSemanal.ACTIVO, turnos.findById(turno.getIdTurnoSemanal()).orElseThrow().getEstado());
        // las fechas del período (incluidos los dos extremos) quedan suspendidas y sus reservas, anuladas
        assertEquals(EstadoOcurrenciaTurno.SUSPENDIDA, estadoDe(lunes2));
        assertEquals(EstadoOcurrenciaTurno.SUSPENDIDA, estadoDe(lunes3));
        assertEquals(EstadoReserva.ANULADA, estadoDe(reserva2));
        assertEquals(EstadoReserva.ANULADA, estadoDe(reserva3));
        // las posteriores al fin siguen igual
        assertEquals(EstadoOcurrenciaTurno.HABILITADA, estadoDe(lunes4));
        assertEquals(EstadoReserva.VIGENTE, estadoDe(reserva4));

        assertEquals(2, respuesta.fechasSuspendidas());
        assertEquals(2, respuesta.reservasAnuladas());
        assertEquals(EstadoTurnoSemanal.ACTIVO, respuesta.estadoTurno());
        SuspensionTurno guardada = suspensiones.findById(respuesta.idSuspensionTurno()).orElseThrow();
        assertEquals(TipoSuspension.TEMPORAL, guardada.getSuspensionTipo());
        assertEquals(lunes2, guardada.getFechaInicio());
        assertEquals(lunes3, guardada.getFechaFin());
        assertEquals(funcionario.getIdUsuario(), guardada.getFuncionario().getIdUsuario());
    }

    @Test
    void laReservaAnuladaConservaSuAntecedente() {
        suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        Reserva anulada = reservas.findById(reserva2.getIdReserva()).orElseThrow();
        assertEquals(EstadoReserva.ANULADA, anulada.getReservaEstado());
        assertEquals(ana.getIdUsuario(), anulada.getPaciente().getIdUsuario());
        assertEquals(1, anulada.getNumero());
        assertTrue(anulada.getMotivoCancelacion().startsWith("Turno suspendido"));
        assertFalse(anulada.getFechaCancelacion() == null);
    }

    @Test
    void unaSuspensionPermanenteAlcanzaTodasLasFechasPosterioresYSiRigeHoyElTurnoPasaASuspendido() {
        SuspensionTurnoResponseDTO respuesta = suspender(TipoSuspension.PERMANENTE, HOY, null);

        for (LocalDate fecha : List.of(lunes1, lunes2, lunes3, lunes4)) {
            assertEquals(EstadoOcurrenciaTurno.SUSPENDIDA, estadoDe(fecha));
        }
        assertEquals(4, respuesta.reservasAnuladas());
        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, respuesta.estadoTurno());
        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, turnos.findById(turno.getIdTurnoSemanal()).orElseThrow().getEstado());
        assertEquals(null, suspensiones.findById(respuesta.idSuspensionTurno()).orElseThrow().getFechaFin());
    }

    @Test
    void soloSeAnulanLasReservasVigentes() {
        Reserva conOrden = reserva(beto, ocurrenciaDe(lunes2), 5, EstadoReserva.ORDEN_EMITIDA);

        suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertEquals(EstadoReserva.ORDEN_EMITIDA, estadoDe(conOrden));
    }

    // ------------------------------------------------------------------ avisos (paso 11)

    @Test
    void avisaACadaPacienteAfectadoEnLaAplicacionYPorCorreo() {
        SuspensionTurnoResponseDTO respuesta = servicio.suspenderTurno(turno.getIdTurnoSemanal(),
            new SuspensionTurnoDTO(TipoSuspension.TEMPORAL, lunes2, lunes3, "Congreso médico"), funcionario.getIdUsuario());

        List<Notificacion> avisos = notificaciones.findAll();
        assertEquals(4, avisos.size()); // dos reservas anuladas, cada una con su aviso en la aplicación y su correo
        long enLaAplicacion = avisos.stream().filter(n -> n.getCanal() == CanalNotificacion.APLICACION).count();
        long porCorreo = avisos.stream().filter(n -> n.getCanal() == CanalNotificacion.EMAIL).count();
        assertEquals(2, enLaAplicacion);
        assertEquals(2, porCorreo);
        assertTrue(avisos.stream().allMatch(n -> n.getEstado() == EstadoNotificacion.ENVIADA));
        Notificacion deAna = avisos.stream().filter(n -> n.getUsuario().getIdUsuario().equals(ana.getIdUsuario())).findFirst().orElseThrow();
        assertTrue(deAna.getMensaje().contains("fue anulada porque el turno fue suspendido"));
        assertTrue(deAna.getMensaje().contains("Motivo: Congreso médico"));
        assertEquals(2, respuesta.reservasAnuladas());

        ArgumentCaptor<SimpleMailMessage> enviado = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(correo, times(2)).send(enviado.capture());
        assertTrue(enviado.getAllValues().stream().anyMatch(m -> m.getTo()[0].equals("9503@ejemplo.com")));
        assertTrue(enviado.getAllValues().stream().anyMatch(m -> m.getTo()[0].equals("9504@ejemplo.com")));
    }

    @Test
    void siElCorreoFallaLaSuspensionQuedaRegistradaYElAvisoQuedaFallido() {
        doThrow(new MailSendException("servidor caído")).when(correo).send(any(SimpleMailMessage.class));

        SuspensionTurnoResponseDTO respuesta = suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertEquals(2, respuesta.reservasAnuladas());
        assertEquals(EstadoReserva.ANULADA, estadoDe(reserva2));
        List<Notificacion> correos = notificaciones.findAll().stream().filter(n -> n.getCanal() == CanalNotificacion.EMAIL).toList();
        assertEquals(2, correos.size());
        assertTrue(correos.stream().allMatch(n -> n.getEstado() == EstadoNotificacion.FALLIDA));
        assertTrue(notificaciones.findAll().stream().filter(n -> n.getCanal() == CanalNotificacion.APLICACION)
            .allMatch(n -> n.getEstado() == EstadoNotificacion.ENVIADA));
    }

    @Test
    void sinReservasAfectadasNoSeAvisaANadie() {
        reservas.deleteAll(List.of(reserva1, reserva2, reserva3, reserva4));
        reservas.flush();

        SuspensionTurnoResponseDTO respuesta = suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertEquals(0, respuesta.reservasAnuladas());
        assertTrue(notificaciones.findAll().isEmpty());
        verify(correo, never()).send(any(SimpleMailMessage.class));
    }

    // ------------------------------------------------------------------ errores

    @Test
    void rechazaUnPeriodoInvalidoYNoModificaNada() {
        long antes = suspensiones.count();

        assertEquals(HttpStatus.BAD_REQUEST, rechazo(TipoSuspension.TEMPORAL, HOY.minusDays(1), HOY.plusDays(5)).getStatusCode());
        assertEquals("Indique hasta qué fecha dura la suspensión temporal.", rechazo(TipoSuspension.TEMPORAL, lunes2, null).getReason());
        assertEquals("La fecha hasta debe ser posterior a la fecha desde.", rechazo(TipoSuspension.TEMPORAL, lunes2, lunes2).getReason());
        assertEquals("La fecha hasta debe ser posterior a la fecha desde.", rechazo(TipoSuspension.TEMPORAL, lunes3, lunes2).getReason());

        assertEquals(antes, suspensiones.count());
        assertEquals(EstadoReserva.VIGENTE, estadoDe(reserva2));
    }

    @Test
    void unaSuspensionPermanenteIgnoraLaFechaHastaQueSeMande() {
        SuspensionTurnoResponseDTO respuesta = suspender(TipoSuspension.PERMANENTE, lunes2, lunes3);

        assertEquals(null, respuesta.fechaHasta());
        assertEquals(EstadoOcurrenciaTurno.SUSPENDIDA, estadoDe(lunes4));
    }

    @Test
    void rechazaSuspenderUnTurnoQueYaNoEstaActivo() {
        TurnoSemanal t = turnos.findById(turno.getIdTurnoSemanal()).orElseThrow();
        t.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        turnos.saveAndFlush(t);

        ResponseStatusException error = rechazo(TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().startsWith("El turno ya no está activo"));
    }

    @Test
    void rechazaUnaSegundaSuspensionMientrasLaPrimeraSigueVigenteOProgramada() {
        suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        ResponseStatusException error = rechazo(TipoSuspension.PERMANENTE, lunes1, null);

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().startsWith("El turno ya tiene una suspensión registrada"));
    }

    @Test
    void respondeNoEncontradoSiElTurnoNoExiste() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> servicio.suspenderTurno(999_999L, new SuspensionTurnoDTO(TipoSuspension.PERMANENTE, lunes2, null, null), funcionario.getIdUsuario()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    // ------------------------------------------------------------------ vista previa (pasos 4, 6 y 7)

    @Test
    void laVistaPreviaInformaLasReservasQueSeAnularianYNoModificaNada() {
        long suspensionesAntes = suspensiones.count();

        VistaPreviaSuspensionDTO vista = servicio.vistaPreviaSuspension(turno.getIdTurnoSemanal(),
            new SuspensionTurnoDTO(TipoSuspension.TEMPORAL, lunes2, lunes3, null));

        assertEquals(2, vista.cantidadFechas());
        assertEquals(2, vista.cantidadReservasAfectadas());
        assertEquals(List.of(lunes2, lunes3), vista.reservas().stream().map(VistaPreviaSuspensionDTO.ReservaAfectada::fecha).toList());
        assertEquals("Apellido9503, Paciente9503", vista.reservas().get(0).nombrePaciente());
        assertEquals("9503", vista.reservas().get(0).cedulaPaciente());
        assertEquals(lunes3, vista.fechaHasta());

        assertEquals(suspensionesAntes, suspensiones.count());
        assertEquals(EstadoReserva.VIGENTE, estadoDe(reserva2));
        assertEquals(EstadoOcurrenciaTurno.HABILITADA, estadoDe(lunes2));
    }

    @Test
    void laVistaPreviaDeUnaPermanenteCuentaTodasLasPosteriores() {
        VistaPreviaSuspensionDTO vista = servicio.vistaPreviaSuspension(turno.getIdTurnoSemanal(),
            new SuspensionTurnoDTO(TipoSuspension.PERMANENTE, lunes3, null, null));

        assertEquals(2, vista.cantidadReservasAfectadas());
        assertEquals(null, vista.fechaHasta());
    }

    @Test
    void laVistaPreviaValidaLoMismoQueLaSuspension() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class, () -> servicio.vistaPreviaSuspension(
            turno.getIdTurnoSemanal(), new SuspensionTurnoDTO(TipoSuspension.TEMPORAL, lunes2, null, null))).getStatusCode());
    }

    // ------------------------------------------------------------------ el estado del turno sigue a las fechas

    @Test
    void elTurnoPasaASuspendidoReciénCuandoLlegaLaFechaDesde() {
        suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertEquals(0, actualizador.actualizarEstados(lunes2.minusDays(1).atStartOfDay()));
        assertEquals(EstadoTurnoSemanal.ACTIVO, turnoActual().getEstado());

        assertEquals(1, actualizador.actualizarEstados(lunes2.atStartOfDay()));
        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, turnoActual().getEstado());
        assertEquals(0, actualizador.actualizarEstados(lunes2.plusDays(1).atStartOfDay())); // repetirla no cambia nada
    }

    @Test
    void unaSuspensionTemporalSeCierraSolaAlTerminarYElTurnoVuelveAActivo() {
        suspender(TipoSuspension.TEMPORAL, lunes2, lunes3);
        actualizador.actualizarEstados(lunes2.atStartOfDay());

        assertEquals(1, actualizador.actualizarEstados(lunes3.plusDays(1).atStartOfDay()));

        assertEquals(EstadoTurnoSemanal.ACTIVO, turnos.findById(turno.getIdTurnoSemanal()).orElseThrow().getEstado());
        assertTrue(suspensiones.buscarSinRehabilitarDelTurno(turno.getIdTurnoSemanal()).isEmpty());
        assertEquals(0, actualizador.actualizarEstados(lunes3.plusDays(2).atStartOfDay()));
        // y se puede volver a suspender
        assertEquals(1, suspender(TipoSuspension.TEMPORAL, lunes4, lunes4.plusDays(1)).fechasSuspendidas());
    }

    @Test
    void unaSuspensionPermanenteNoVuelveAActivoSola() {
        suspender(TipoSuspension.PERMANENTE, lunes2, null);
        actualizador.actualizarEstados(lunes2.atStartOfDay());

        actualizador.actualizarEstados(lunes2.plusYears(1).atStartOfDay());

        assertEquals(EstadoTurnoSemanal.SUSPENDIDO, turnos.findById(turno.getIdTurnoSemanal()).orElseThrow().getEstado());
    }

    // ------------------------------------------------------------------ la reserva se decide por las fechas que cubre la suspensión

    @Test
    void unTurnoSuspendidoPorUnPeriodoSigueReservableDespuesDeQueTermina() {
        TurnoSemanal t = turnoActual();
        t.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        turnos.saveAndFlush(t);
        suspension(t, funcionario, TipoSuspension.TEMPORAL, lunes2, lunes3);

        assertTrue(disponibilidad.estaHabilitada(ocurrenciaDe(lunes1)));
        assertFalse(disponibilidad.estaHabilitada(ocurrenciaDe(lunes2)));
        assertFalse(disponibilidad.estaHabilitada(ocurrenciaDe(lunes3)));
        assertTrue(disponibilidad.estaHabilitada(ocurrenciaDe(lunes4)));
    }

    @Test
    void unTurnoSuspendidoConSuspensionPermanenteNoSeReservaEnNingunaFecha() {
        TurnoSemanal t = turnoActual();
        t.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        turnos.saveAndFlush(t);
        suspension(t, funcionario, TipoSuspension.PERMANENTE, HOY, null);

        for (LocalDate fecha : List.of(lunes1, lunes2, lunes3, lunes4)) {
            assertFalse(disponibilidad.estaHabilitada(ocurrenciaDe(fecha)));
        }
    }

    @Test
    void unTurnoSuspendidoSinNingunaSuspensionRegistradaNoSeReserva() {
        TurnoSemanal t = turnoActual();
        t.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
        turnos.saveAndFlush(t);

        assertFalse(disponibilidad.estaHabilitada(ocurrenciaDe(lunes1)));
    }
}
