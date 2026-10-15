package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TipoSuspension;

/**
 * Suspensión registrada. {@code estadoTurno} es el del turno en este momento: sigue ACTIVO hasta que llega
 * {@code fechaDesde}, y ese día pasa a SUSPENDIDO. {@code reservasAvisadas} incluye las anuladas y las que tienen orden emitida
 * (esas no se anulan, pero también se avisa a sus pacientes).
 */
public record SuspensionTurnoResponseDTO(
        Long idSuspensionTurno,
        Long idTurnoSemanal,
        TipoSuspension modalidad,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String motivo,
        LocalDateTime fechaRegistro,
        int fechasSuspendidas,
        int reservasAnuladas,
        int reservasAvisadas,
        EstadoTurnoSemanal estadoTurno) {
}
