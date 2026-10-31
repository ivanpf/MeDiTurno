package com.mediturno.backend.estadisticas.dto;

import java.time.LocalTime;

import com.mediturno.backend.turnos.model.DiaSemana;

/** CU-41. Un turno semanal del ranking de demanda, con las reservas que recibió en el período (de cualquier estado). */
public record EstadisticaTurnoDTO(
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
        Integer cupoMaximo,
        long cantidadReservas) {
}
