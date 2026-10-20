package com.mediturno.backend.consultas.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import com.mediturno.backend.turnos.model.EstadoOcurrenciaTurno;

/** CU-21: una fecha concreta de atención del médico autenticado, incluso si no tiene pacientes. */
public record TurnoAtencionDTO(Long idOcurrenciaTurno, LocalDate fecha, LocalTime horaInicio,
        LocalTime horaFin, String nombreEspecialidad, String nombreConsultorio, EstadoOcurrenciaTurno estado) {
}
