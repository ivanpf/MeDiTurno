package com.mediturno.backend.common.dto;

import java.time.LocalDate;

/**
 * Período y filtros de las estadísticas del administrador. Todos los campos son opcionales: los que vienen vacíos no filtran.
 * Cada estadística usa solo los suyos:
 * <ul>
 *   <li>CU-40 (reservas por especialidad): {@code fechaDesde}, {@code fechaHasta} y {@code estado} (de la reserva);
 *       {@code ordenarPor} puede ser cantidad (por defecto, de mayor a menor) o nombre (por defecto, de la A a la Z).</li>
 *   <li>CU-41 (turnos más demandados): {@code fechaDesde}, {@code fechaHasta}, {@code codigoEspecialidad} e {@code idMedico};
 *       siempre se ordena por reservas, de mayor a menor salvo que {@code descendente} sea {@code false}.</li>
 *   <li>CU-42 (ocupación de turnos): {@code fechaDesde}, {@code fechaHasta}, {@code codigoEspecialidad}, {@code idMedico} y
 *       {@code codigoConsultorio}; siempre se ordena por porcentaje de ocupación, de mayor a menor salvo que
 *       {@code descendente} sea {@code false}.</li>
 * </ul>
 * El período se mide por la fecha de atención del turno (no por la fecha en que se reservó). Sin {@code fechaDesde} o sin
 * {@code fechaHasta}, el período no tiene límite de ese lado. Ambas fechas son inclusive.
 */
public record PeriodoDTO(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        String codigoEspecialidad,
        Long idMedico,
        String codigoConsultorio,
        String estado,
        String ordenarPor,
        Boolean descendente) {

    /** Sin ningún filtro: todo el historial, en el orden por defecto. */
    public static PeriodoDTO vacio() {
        return new PeriodoDTO(null, null, null, null, null, null, null, null);
    }
}
