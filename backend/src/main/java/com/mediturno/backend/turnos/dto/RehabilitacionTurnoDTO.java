package com.mediturno.backend.turnos.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Min;

import com.mediturno.backend.turnos.model.DiaSemana;

/**
 * CU-14: cambios opcionales al rehabilitar. Los campos nulos conservan la configuración vigente del turno.
 */
public record RehabilitacionTurnoDTO(
        DiaSemana dia,
        LocalTime horaInicio,
        LocalTime horaFin,
        String codigoConsultorio,
        @Min(value = 1, message = "El máximo de pacientes debe ser al menos 1")
        Integer cupoMaximo) {
}
