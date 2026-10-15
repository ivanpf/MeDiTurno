package com.mediturno.backend.reservas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** CU-8: el paciente elige una ocurrencia y un número libre. */
public record ReservaRequestDTO(
        @NotNull(message = "Seleccione el turno")
        @Positive(message = "El turno seleccionado no es válido")
        Long idOcurrenciaTurno,

        @NotNull(message = "Seleccione un número")
        @Positive(message = "El número seleccionado no es válido")
        Integer numero) {
}
