package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;
import com.mediturno.backend.turnos.model.TipoSuspension;

/**
 * Turno semanal con su médico, especialidad, consultorio, día, horario, vigencia y estado. {@code suspension} es la
 * suspensión registrada que todavía no terminó ni se rehabilitó (programada o vigente), o nulo si no tiene.
 */
public record TurnoSemanalResponseDTO(
        Long idTurnoSemanal,
        Long idMedico,
        String nombreMedico,
        String codigoEspecialidad,
        String nombreEspecialidad,
        String codigoConsultorio,
        String nombreConsultorio,
        DiaSemana dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        Integer cupoMaximo,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        EstadoTurnoSemanal estado,
        SuspensionResumen suspension) {

    /** Modalidad y fechas de una suspensión (hasta es nula si es permanente). */
    public record SuspensionResumen(TipoSuspension modalidad, LocalDate fechaDesde, LocalDate fechaHasta) {
    }
}
