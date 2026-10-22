package com.mediturno.backend.pases.dto;

import jakarta.validation.constraints.NotBlank;

public record EmitirPaseDTO(
    @NotBlank(message = "La especialidad de destino es obligatoria")
    String especialidadDestino
) {}
