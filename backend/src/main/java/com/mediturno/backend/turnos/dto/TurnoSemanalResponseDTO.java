package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.mediturno.backend.turnos.model.DiaSemana;
import com.mediturno.backend.turnos.model.EstadoTurnoSemanal;

/** Turno semanal con su médico, especialidad, consultorio, día, horario, vigencia y estado. */
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
        EstadoTurnoSemanal estado) {
}
