package com.mediturno.backend.especialidades.dto;

import java.time.LocalTime;
import java.util.List;

import com.mediturno.backend.common.model.EstadoActivacion;
import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;

/**
 * Detalle de una especialidad para el administrador (CU-30): sus datos y las asociaciones vigentes, que son los médicos
 * activos que la atienden y los turnos semanales cuya vigencia no terminó (activos o suspendidos).
 */
public record EspecialidadDetalleResponseDTO(
        String codigo,
        String nombre,
        String descripcion,
        boolean requierePase,
        EstadoActivacion estado,
        List<MedicoAsociado> medicos,
        List<TurnoAsociado> turnos) {

    public record MedicoAsociado(Long idMedico, String nombreCompleto, String matricula) {
    }

    public record TurnoAsociado(
            Long idTurnoSemanal,
            String nombreMedico,
            DiaSemana dia,
            LocalTime horaInicio,
            LocalTime horaFin,
            String codigoConsultorio,
            String nombreConsultorio,
            EstadoTurnoSemanal estado) {
    }
}
