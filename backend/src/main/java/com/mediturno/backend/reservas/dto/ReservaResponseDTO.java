package com.mediturno.backend.reservas.dto;

import java.time.LocalTime;
import com.mediturno.backend.reservas.model.EstadoReserva;
import com.mediturno.backend.ordenes.model.EstadoOrden;

/**
 * Confirmación CU-8 y pacientes por turno CU-21. Sin orden emitida, numeroOrden y estadoOrden son nulos.
 */
public record ReservaResponseDTO(Long idReserva, Integer numero, LocalTime horaEstimada,
        Long idPaciente, String nombrePaciente, EstadoReserva estadoReserva, Long numeroOrden, EstadoOrden estadoOrden) {
    public ReservaResponseDTO(Long idReserva, Integer numero, LocalTime horaEstimada) {
        this(idReserva, numero, horaEstimada, null, null, null, null, null);
    }
}
