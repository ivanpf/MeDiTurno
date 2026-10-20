package com.mediturno.backend.reservas.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import com.mediturno.backend.ordenes.model.EstadoOrden;
import com.mediturno.backend.reservas.model.EstadoReserva;

/**
 * Datos de una reserva: confirmación (CU-8), consulta por identificador (CU-15) y pacientes de un turno (CU-21).
 * {@code horaEstimada} es la hora estimada de atención del número. {@code numeroOrden} y {@code estadoOrden} son los de la
 * orden emitida para la reserva (CU-16); son nulos mientras no se emitió. Cada caso de uso completa los campos que le
 * corresponden y deja el resto en nulo (por ejemplo, CU-21 no informa médico, especialidad ni consultorio).
 */
public record ReservaResponseDTO(
        Long idReserva,
        Integer numero,
        LocalTime horaEstimada,
        EstadoReserva estado,
        Long idPaciente,
        String nombrePaciente,
        String cedulaPaciente,
        String nombreMedico,
        String especialidad,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        String consultorio,
        Long numeroOrden,
        EstadoOrden estadoOrden) {

    /** CU-8 y CU-15: datos completos de la reserva. */
    public ReservaResponseDTO(Long idReserva, Integer numero, LocalTime horaEstimada, EstadoReserva estado,
            String nombrePaciente, String cedulaPaciente, String nombreMedico, String especialidad, LocalDate fecha,
            LocalTime horaInicio, LocalTime horaFin, String consultorio, Long numeroOrden) {
        this(idReserva, numero, horaEstimada, estado, null, nombrePaciente, cedulaPaciente, nombreMedico, especialidad,
            fecha, horaInicio, horaFin, consultorio, numeroOrden, null);
    }

    /** CU-21: fila de un paciente dentro de un turno. */
    public ReservaResponseDTO(Long idReserva, Integer numero, LocalTime horaEstimada, Long idPaciente,
            String nombrePaciente, EstadoReserva estado, Long numeroOrden, EstadoOrden estadoOrden) {
        this(idReserva, numero, horaEstimada, estado, idPaciente, nombrePaciente, null, null, null, null, null, null, null,
            numeroOrden, estadoOrden);
    }
}
