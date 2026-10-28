package com.mediturno.backend.reservas.dto;

import java.time.LocalDateTime;

/** CU-9: datos para elegir una reserva propia y conocer si admite cancelación. */
public record ReservaCancelacionDTO(ReservaResponseDTO reserva, boolean puedeCancelar,
        String motivoNoCancelable, LocalDateTime fechaCancelacion) {
}
