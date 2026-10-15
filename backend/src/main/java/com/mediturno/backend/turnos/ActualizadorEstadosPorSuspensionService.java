package com.mediturno.backend.turnos;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.SuspensionTurno;
import com.mediturno.backend.turnos.model.TurnoSemanal;
import com.mediturno.backend.turnos.repository.SuspensionTurnoRepository;

/**
 * Mantiene al día el estado de los turnos que tienen una suspensión registrada. La ejecuta la tarea programada (ver
 * {@code TareasProgramadas}), todos los días antes de generar las fechas.
 * <ul>
 *   <li>Un turno con una suspensión que ya rige (fecha desde alcanzada) pasa de ACTIVO a SUSPENDIDO.</li>
 *   <li>Una suspensión temporal que ya terminó se cierra sola (queda con fecha de rehabilitación) y el turno vuelve a ACTIVO.</li>
 * </ul>
 * La reserva de fechas no depende de este estado: se decide por las fechas que cubre cada suspensión (ver
 * {@link DisponibilidadService}). Por eso una suspensión temporal no impide reservar las fechas posteriores a su fin.
 */
@Service
public class ActualizadorEstadosPorSuspensionService {

    private final SuspensionTurnoRepository suspensionTurnoRepository;

    public ActualizadorEstadosPorSuspensionService(SuspensionTurnoRepository suspensionTurnoRepository) {
        this.suspensionTurnoRepository = suspensionTurnoRepository;
    }

    /** Devuelve cuántos turnos o suspensiones cambiaron. Es seguro repetirla. */
    @Transactional
    public int actualizarEstados(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        int cambios = 0;
        for (SuspensionTurno suspension : suspensionTurnoRepository.buscarTodasSinRehabilitar()) {
            TurnoSemanal turno = suspension.getTurnoSemanal();
            boolean terminada = suspension.getFechaFin() != null && suspension.getFechaFin().isBefore(hoy);
            if (terminada) {
                suspension.setFechaRehabilitacion(ahora);
                turno.setEstado(EstadoTurnoSemanal.ACTIVO);
                cambios++;
            } else if (!suspension.getFechaInicio().isAfter(hoy) && turno.getEstado() == EstadoTurnoSemanal.ACTIVO) {
                turno.setEstado(EstadoTurnoSemanal.SUSPENDIDO);
                cambios++;
            }
        }
        return cambios;
    }
}
