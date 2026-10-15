package com.mediturno.backend.turnos;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.OcurrenciaTurno;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;

/**
 * Reglas de disponibilidad de los turnos que comparten varios casos de uso: CU-8 (consultar y confirmar una reserva) y
 * CU-12 (generar las fechas de un turno nuevo). Es un servicio interno: no figura en el Documento de diseño.
 * <p>
 * Reglas del día de hoy, que no definen los documentos y son distintas según el momento:
 * <ul>
 *   <li><b>Al generar la fecha</b> ({@link #turnoTodaviaNoEmpezo}): la de hoy se crea solo si el turno todavía no empezó.</li>
 *   <li><b>Al reservar</b> ({@link #estaHabilitada}, {@link #esNumeroFuturo}): una fecha de hoy se ofrece hasta que el turno termina,
 *       y solo con los números cuya hora estimada todavía no pasó.</li>
 * </ul>
 */
@Service
public class DisponibilidadService {

    private final Clock reloj;
    private final SuspensionTurnoRepository suspensionTurnoRepository;
    private final int semanasDeReserva;

    public DisponibilidadService(Clock reloj, SuspensionTurnoRepository suspensionTurnoRepository,
            @Value("${mediturno.reservas.semanas-adelante}") int semanasDeReserva) {
        this.reloj = reloj;
        this.suspensionTurnoRepository = suspensionTurnoRepository;
        this.semanasDeReserva = semanasDeReserva;
    }

    // ------------------------------------------------------------------ la ventana de reserva

    /** Última fecha en la que se puede reservar si hoy es {@code hoy} (CU-8: las próximas ocho semanas, configurable). */
    public LocalDate ultimaFechaReservable(LocalDate hoy) {
        return hoy.plusWeeks(semanasDeReserva);
    }

    /** Cuántos días hacia adelante se puede reservar: hasta donde hay que tener generadas las fechas de los turnos. */
    public int diasDeReserva() {
        return semanasDeReserva * 7;
    }

    // ------------------------------------------------------------------ generar las fechas de un turno

    /** La fecha de un día posterior a hoy siempre empieza en el futuro; la de hoy, solo si la hora de inicio todavía no llegó. */
    public boolean turnoTodaviaNoEmpezo(TurnoSemanal turno, LocalDate fecha, LocalDateTime ahora) {
        return LocalDateTime.of(fecha, turno.getHoraInicio()).isAfter(ahora);
    }

    // ------------------------------------------------------------------ reservar (CU-8)

    /** Si la ocurrencia se puede ofrecer y reservar ahora. */
    public boolean estaHabilitada(OcurrenciaTurno ocurrencia) {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate fecha = ocurrencia.getFecha();
        TurnoSemanal turno = ocurrencia.getTurnoSemanal();
        return dentroDeLaVentanaDeReserva(fecha, hoy)
                && ocurrencia.getOcurrenciaTurnoEstado() == EstadoOcurrenciaTurno.HABILITADA
                && turnoActivoConTodosSusDatosActivos(turno)
                && dentroDeLaVigencia(turno, fecha)
                && !suspendido(turno, fecha)
                && !yaTermino(ocurrencia, hoy);
    }

    /** Hora estimada de atención del número: el horario del turno se reparte en partes iguales entre los números. */
    public LocalTime horaEstimada(OcurrenciaTurno ocurrencia, int numero) {
        long duracion = ChronoUnit.MINUTES.between(ocurrencia.getHoraInicio(), ocurrencia.getHoraFin());
        return ocurrencia.getHoraInicio().plusMinutes((numero - 1L) * duracion / ocurrencia.getCupos()).truncatedTo(ChronoUnit.MINUTES);
    }

    /** Un número de una fecha posterior a hoy siempre es futuro; uno de hoy, solo si su hora estimada todavía no pasó. */
    public boolean esNumeroFuturo(OcurrenciaTurno ocurrencia, int numero) {
        return !ocurrencia.getFecha().equals(LocalDate.now(reloj)) || horaEstimada(ocurrencia, numero).isAfter(LocalTime.now(reloj));
    }

    private boolean dentroDeLaVentanaDeReserva(LocalDate fecha, LocalDate hoy) {
        return !fecha.isBefore(hoy) && !fecha.isAfter(ultimaFechaReservable(hoy));
    }

    /** El turno, su médico, su especialidad y su consultorio están activos, y el médico atiende esa especialidad. */
    private static boolean turnoActivoConTodosSusDatosActivos(TurnoSemanal turno) {
        return turno.getEstado() == EstadoTurnoSemanal.ACTIVO
                && turno.getMedico().getEstado() == EstadoActivacion.ACTIVO
                && turno.getEspecialidad().getEstado() == EstadoActivacion.ACTIVO
                && turno.getConsultorio().getEstado() == EstadoActivacion.ACTIVO
                && turno.getMedico().getEspecialidades().stream()
                    .anyMatch(e -> e.getCodigo().equals(turno.getEspecialidad().getCodigo()));
    }

    private static boolean dentroDeLaVigencia(TurnoSemanal turno, LocalDate fecha) {
        return !fecha.isBefore(turno.getVigenteDesde())
                && (turno.getVigenteHasta() == null || !fecha.isAfter(turno.getVigenteHasta()));
    }

    /** La fecha cae dentro de una suspensión del turno que todavía no se rehabilitó. */
    private boolean suspendido(TurnoSemanal turno, LocalDate fecha) {
        return suspensionTurnoRepository.buscarSinRehabilitarDelTurno(turno.getIdTurnoSemanal()).stream()
                .anyMatch(suspension -> cubre(suspension, fecha));
    }

    private static boolean cubre(SuspensionTurno suspension, LocalDate fecha) {
        return !fecha.isBefore(suspension.getFechaInicio())
                && (suspension.getFechaFin() == null || !fecha.isAfter(suspension.getFechaFin()));
    }

    /** La fecha es hoy y el turno ya terminó. */
    private boolean yaTermino(OcurrenciaTurno ocurrencia, LocalDate hoy) {
        return ocurrencia.getFecha().equals(hoy) && !ocurrencia.getHoraFin().isAfter(LocalTime.now(reloj));
    }
}
