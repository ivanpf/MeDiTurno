package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.mediturno.backend.turnos.model.TipoSuspension;

/**
 * CU-13: modalidad (TEMPORAL o PERMANENTE), fechaDesde y, solo para la temporal, fechaHasta. Además, un motivo opcional.
 * La suspensión rige desde fechaDesde (inclusive): hasta ese día el turno sigue funcionando igual. En la temporal, rige
 * hasta fechaHasta (inclusive); en la permanente, no termina. Se usa también para la vista previa (por eso el motivo no es
 * obligatorio ni se valida acá).
 */
public record SuspensionTurnoDTO(
        @NotNull(message = "Indique si la suspensión es temporal o permanente")
        TipoSuspension modalidad,

        @NotNull(message = "La fecha desde la que rige la suspensión es obligatoria")
        LocalDate fechaDesde,

        LocalDate fechaHasta,

        @Size(max = 500, message = "El motivo admite hasta 500 caracteres")
        String motivo) {
}
