package com.mediturno.backend.common.dto;

/**
 * Filtros y ordenamiento de las búsquedas y listados. Todos los campos son opcionales: los que vienen vacíos no filtran.
 * Es un DTO compartido: cada caso de uso agrega los criterios que necesita al final y usa solo los suyos.
 * <ul>
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
        Boolean descendente) {

    /** Sin ningún criterio: devuelve todo, en el orden por defecto. */
    public static CriteriosBusquedaDTO vacio() {
        return new CriteriosBusquedaDTO(null, null, null, null, null, null, null);
    }
}
