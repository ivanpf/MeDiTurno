package com.mediturno.backend.estadisticas.dto;

import java.time.LocalTime;

import com.mediturno.backend.turnos.model.DiaSemana;

/**
 * CU-42. Ocupación de un horario de atención en el período. {@code cuposOfrecidos} suma los cupos de sus fechas (sin las
 * suspendidas); {@code cuposReservados} son los números que una reserva conserva (todas menos las canceladas y anuladas);
 * {@code cuposNoReservados} es la diferencia; {@code reservasVigentes} son las que todavía no se atendieron (vigentes o con
 * orden emitida). {@code porcentajeOcupacion} es reservados sobre ofrecidos, con dos decimales.
 */
public record EstadisticaOcupacionDTO(
        Long idTurnoSemanal,
        String codigoEspecialidad,
        String nombreEspecialidad,
        Long idMedico,
        String nombreMedico,
        DiaSemana dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        String codigoConsultorio,
        String nombreConsultorio,
        long cuposOfrecidos,
        long cuposReservados,
        long reservasVigentes,
        long cuposNoReservados,
        double porcentajeOcupacion) {
}
