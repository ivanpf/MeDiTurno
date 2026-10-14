package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.mediturno.backend.turnos.model.DiaSemana;

/**
 * CU-12: médico, especialidad, consultorio, días de la semana, hora de inicio y fin, cantidad máxima de números y fecha
 * de vigencia. Se crea un turno semanal por cada día elegido, todos en la misma operación.
 * (El diagrama de secuencia original tiene un solo día; se amplió a una lista, como pide el caso de uso.)
 */
public record AltaTurnoSemanalDTO(
        @NotNull(message = "Seleccione el médico")
        Long idMedico,

        @NotBlank(message = "Seleccione la especialidad")
        String codigoEspecialidad,

        @NotBlank(message = "Seleccione el consultorio")
        String codigoConsultorio,

        @NotEmpty(message = "Seleccione al menos un día de la semana")
        List<DiaSemana> dias,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime horaInicio,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime horaFin,

        @NotNull(message = "El máximo de pacientes es obligatorio")
        @Min(value = 1, message = "El máximo de pacientes debe ser al menos 1")
        Integer cupoMaximo,

        @NotNull(message = "La fecha de vigencia es obligatoria")
        @FutureOrPresent(message = "La fecha de vigencia no puede ser anterior a hoy")
        LocalDate vigenteDesde) {
}
