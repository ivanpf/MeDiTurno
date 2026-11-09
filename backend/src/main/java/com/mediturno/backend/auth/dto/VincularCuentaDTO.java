package com.mediturno.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** Cédula del perfil local que se vincula a la cuenta Firebase ya autenticada. */
public record VincularCuentaDTO(
    @NotBlank(message = "La cédula es obligatoria")
    String cedula
) {
}
