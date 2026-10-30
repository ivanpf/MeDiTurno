package com.mediturno.backend.consultorios.dto;

import java.time.LocalTime;
import java.util.List;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;

/**
 * Detalle de un consultorio para el administrador (CU-39): sus datos y los turnos semanales vigentes que lo utilizan, o sea
 * los que no terminaron (activos o suspendidos).
 */
public record ConsultorioDetalleResponseDTO(
        String codigo,
        String nombre,
        String ubicacion,
        String descripcion,
        EstadoActivacion estado,
        List<TurnoAsociado> turnos) {

    public record TurnoAsociado(
            Long idTurnoSemanal,
            String nombreMedico,
            String nombreEspecialidad,
            DiaSemana dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            EstadoTurnoSemanal estado) {
    }
}
