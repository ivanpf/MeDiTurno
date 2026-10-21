package com.mediturno.backend.ordenes.dto;

import java.time.LocalDateTime;

import com.mediturno.backend.ordenes.model.EstadoOrden;

/** CU-16. La orden emitida se identifica mediante {@code numeroOrden}. */
public record OrdenResponseDTO(Long numeroOrden, Long idReserva, EstadoOrden estado, LocalDateTime fechaEmision) {
}
