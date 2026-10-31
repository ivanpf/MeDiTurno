package com.mediturno.backend.estadisticas.dto;

/**
 * CU-40. Reservas de una especialidad en el período: la cantidad y qué porcentaje es del total de reservas del período
 * (con los mismos filtros), con dos decimales.
 */
public record EstadisticaReservaDTO(
        String codigoEspecialidad,
        String nombreEspecialidad,
        long cantidadReservas,
        double porcentaje) {
}
