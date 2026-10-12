package com.mediturno.backend.pacientes.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * CU-7, paso previo al registro con Google: la cédula y el correo tienen que coincidir con los del padrón.
 * No es un método del Documento de diseño: se agregó para no abrir la ventana de Google a quien no puede registrarse.
 */
public record VerificacionRegistroDTO(
        @NotBlank(message = "La cédula es obligatoria")
        String cedula,

        @NotBlank(message = "El correo electrónico es obligatorio")
        String correo) {
}
