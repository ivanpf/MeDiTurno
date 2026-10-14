package com.mediturno.backend.reservas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** CU-8: el paciente elige una ocurrencia y un número libre. */
public record ReservaRequestDTO(@NotNull @Positive Long idOcurrenciaTurno, @NotNull @Positive Integer numero) {
}
