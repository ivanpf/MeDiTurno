package com.mediturno.backend.notificaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token del dispositivo para notificaciones push.
 */
public record RegistrarPushDTO(@NotBlank @Size(max = 512) String token) {
}
