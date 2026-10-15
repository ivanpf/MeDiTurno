package com.mediturno.backend.notificaciones.dto;

import java.time.LocalDateTime;

import com.mediturno.backend.notificaciones.model.TipoNotificacion;

/** Una notificación de la aplicación (la campanita). {@code leida} es falso hasta que la persona la abre. */
public record NotificacionResponseDTO(
        Long idNotificacion,
        TipoNotificacion tipo,
        String mensaje,
        LocalDateTime fecha,
        boolean leida,
        Long idReserva) {
}
