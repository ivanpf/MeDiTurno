package com.mediturno.backend.consultas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CU-23: información clínica de la atención. {@code motivo} y {@code observaciones} son obligatorios; {@code indicaciones}
 * es opcional.
 */
public record RegistrarConsultaDTO(
        @NotBlank(message = "El motivo de la consulta es obligatorio")
        @Size(max = 5000, message = "El motivo no puede superar los 5000 caracteres")
        String motivo,
        @NotBlank(message = "Las observaciones son obligatorias")
        @Size(max = 5000, message = "Las observaciones no pueden superar los 5000 caracteres")
        String observaciones,
        @Size(max = 5000, message = "Las indicaciones no pueden superar los 5000 caracteres")
        String indicaciones) {
}
