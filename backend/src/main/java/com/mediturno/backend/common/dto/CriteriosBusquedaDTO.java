package com.mediturno.backend.common.dto;

import java.time.LocalDate;

/**
 * Filtros y ordenamiento de las búsquedas y listados. Todos los campos son opcionales: los que vienen vacíos no filtran.
 * Es un DTO compartido: cada caso de uso agrega los criterios que necesita al final y usa solo los suyos.
 * <ul>
 *   <li>CU-11 (historia clínica): {@code fechaDesde}, {@code fechaHasta}, {@code codigoEspecialidad} e {@code idMedico};
 *       {@code ordenarPor} puede ser fecha.</li>
 *   <li>CU-20 (turnos semanales): {@code idMedico}, {@code codigoEspecialidad}, {@code dia} (LUNES...DOMINGO),
 *       {@code codigoConsultorio} y {@code estado} (ACTIVO o SUSPENDIDO); {@code ordenarPor} puede ser medico,
 *       especialidad, dia, horario, consultorio o estado.</li>
 * </ul>
 * {@code dia} y {@code estado} son texto porque cada caso de uso tiene sus propios valores.
 */
public record CriteriosBusquedaDTO(
        Long idMedico,
        String codigoEspecialidad,
        String dia,
        String codigoConsultorio,
        String estado,
        String ordenarPor,
        Boolean descendente,
        LocalDate fechaDesde,
        LocalDate fechaHasta) {

    /** Conserva la forma usada por los casos de uso que no utilizan un rango de fechas. */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, null, null);
    }

    /** Sin ningún criterio: devuelve todo, en el orden por defecto. */
    public static CriteriosBusquedaDTO vacio() {
        return new CriteriosBusquedaDTO(null, null, null, null, null, null, null, null, null);
    }
}
