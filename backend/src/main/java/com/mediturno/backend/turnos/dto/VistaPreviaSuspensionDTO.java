package com.mediturno.backend.turnos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.mediturno.backend.turnos.model.TipoSuspension;

/**
 * Qué pasaría si se suspende el turno con los datos indicados (pasos 4, 6 y 7 de CU-13): cuántas fechas dejarían de atenderse
 * y qué reservas se anularían. No modifica nada. No figura en el Documento de diseño: se agregó para mostrar el resumen
 * antes de pedir la confirmación.
 */
public record VistaPreviaSuspensionDTO(
        Long idTurnoSemanal,
        TipoSuspension modalidad,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        int cantidadFechas,
        int cantidadReservasAfectadas,
        List<ReservaAfectada> reservas) {

    public record ReservaAfectada(Long idReserva, LocalDate fecha, LocalTime horaInicio, Integer numero,
            String nombrePaciente, String cedulaPaciente) {
    }
}
