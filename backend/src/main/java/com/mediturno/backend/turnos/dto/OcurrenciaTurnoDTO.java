package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * CU-8: una fecha concreta con médico, horario, cupos y números no disponibles.
 */
public record OcurrenciaTurnoDTO(Long idOcurrenciaTurno, Long idMedico, String nombreMedico,
        LocalDate fecha, LocalTime horaInicio, LocalTime horaFin,
        String consultorio, Integer cupos, List<Integer> numerosOcupados) {
}
