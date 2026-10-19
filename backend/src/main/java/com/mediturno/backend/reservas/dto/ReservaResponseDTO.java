package com.mediturno.backend.reservas.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.mediturno.backend.reservas.model.EstadoReserva;

/**
 * Datos de una reserva (CU-8, confirmación; CU-15, consulta por identificador): paciente, médico, especialidad, fecha, horario,
 * consultorio, número y estado. {@code horaEstimada} es la hora estimada de atención del número. {@code numeroOrden} es el de la
 * orden emitida para la reserva (CU-16); es nulo mientras no se emitió.
 */
public record ReservaResponseDTO(
        Long idReserva,
        Integer numero,
        LocalTime horaEstimada,
        EstadoReserva estado,
        String nombrePaciente,
        String cedulaPaciente,
        String nombreMedico,
        String especialidad,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String consultorio,
        Long numeroOrden) {
}
