package com.mediturno.backend.common.dto;

import java.time.LocalDate;

/**
 * Filtros y ordenamiento de las búsquedas y listados. Todos los campos son opcionales: los que vienen vacíos no filtran.
 * Es un DTO compartido: cada caso de uso agrega los criterios que necesita al final y usa solo los suyos.
 * <ul>
 *   <li>CU-10 (historial de reservas): {@code idMedico}, {@code codigoEspecialidad}, {@code estado},
 *       {@code fechaDesde} y {@code fechaHasta}; {@code ordenarPor} puede ser fecha, medico,
 *       especialidad, estado o numero.</li>
 *   <li>CU-11 (historia clínica): {@code fechaDesde}, {@code fechaHasta}, {@code codigoEspecialidad} e {@code idMedico};
 *       {@code ordenarPor} puede ser fecha.</li>
 *   <li>CU-26 (atenciones propias del médico): {@code fechaDesde}, {@code fechaHasta}, {@code texto} (nombre o cédula del paciente) y {@code codigoEspecialidad};
 *       {@code ordenarPor} puede ser fecha.</li>
 *   <li>CU-19 (ventanilla): período, médico, especialidad, estado e {@code idTurnoSemanal};
 *       orden por numero o paciente. Sin fechas ni turno consulta hoy en Montevideo.</li>
 *   <li>CU-27 (pases emitidos por el médico): {@code texto} (busca datos del paciente), {@code fechaDesde},
 *       {@code fechaHasta}, {@code codigoEspecialidad} (destino) y {@code estado}; {@code ordenarPor} puede ser fecha.</li>
 *   <li>CU-20 (turnos semanales): {@code idMedico}, {@code codigoEspecialidad}, {@code dia} (LUNES...DOMINGO),
 *       {@code codigoConsultorio} y {@code estado} (ACTIVO o SUSPENDIDO); {@code ordenarPor} puede ser medico,
 *       especialidad, dia, horario, consultorio o estado.</li>
 *   <li>CU-30 (especialidades): {@code texto} (busca en el código y en el nombre), {@code estado} (ACTIVO o INACTIVO) y
 *       {@code requierePase}; {@code ordenarPor} puede ser codigo o nombre (por defecto, nombre).</li>
 *   <li>CU-39 (consultorios): {@code texto} (busca en el código y en el nombre), {@code ubicacion} (exacta, sin distinguir
 *       mayúsculas) y {@code estado} (ACTIVO o INACTIVO); {@code ordenarPor} puede ser codigo, nombre o ubicacion (por defecto,
 *       nombre).</li>
 *   <li>CU-33 (pacientes): {@code texto} (busca en la cédula, el nombre, el apellido y el correo), {@code tieneCuenta}
 *       (si ya creó su cuenta o no) y {@code estado} (ACTIVO o INACTIVO); {@code ordenarPor} puede ser apellido, nombre o
 *       fechaAlta (por defecto, apellido).</li>
 *   <li>CU-36 (usuarios internos): {@code texto} (busca en la cédula, el nombre, el apellido y el correo), {@code rol}
 *       (ADMINISTRADOR, FUNCIONARIO o MEDICO), {@code estado} (ACTIVO o INACTIVO) y, para los médicos,
 *       {@code codigoEspecialidad}; {@code ordenarPor} puede ser apellido, nombre, rol o fechaCreacion (por defecto,
 *       apellido).</li>
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
        LocalDate fechaHasta,
        String texto,
        Boolean requierePase,
        String ubicacion,
        Boolean tieneCuenta,
        String rol,
        Long idTurnoSemanal) {

    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente, LocalDate fechaDesde, LocalDate fechaHasta,
            String texto, Boolean requierePase, String ubicacion, Boolean tieneCuenta, String rol) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, fechaDesde,
            fechaHasta, texto, requierePase, ubicacion, tieneCuenta, rol, null);
    }

    /** Conserva la forma usada por los casos de uso que no filtran por rol (CU-33). */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente, LocalDate fechaDesde, LocalDate fechaHasta,
            String texto, Boolean requierePase, String ubicacion, Boolean tieneCuenta) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, fechaDesde, fechaHasta,
            texto, requierePase, ubicacion, tieneCuenta, null);
    }

    /** Conserva la forma usada por los casos de uso que no filtran por si el paciente tiene cuenta (CU-39). */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente, LocalDate fechaDesde, LocalDate fechaHasta,
            String texto, Boolean requierePase, String ubicacion) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, fechaDesde, fechaHasta,
            texto, requierePase, ubicacion, null, null);
    }

    /** Conserva la forma usada por los casos de uso que no filtran por ubicación (CU-30). */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente, LocalDate fechaDesde, LocalDate fechaHasta,
            String texto, Boolean requierePase) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, fechaDesde, fechaHasta,
            texto, requierePase, null, null, null);
    }

    /** Conserva la forma usada por los casos de uso que no usan búsqueda por texto ni el requisito de pase. */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente, LocalDate fechaDesde, LocalDate fechaHasta) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, fechaDesde, fechaHasta, null, null, null, null, null);
    }

    /** Conserva la forma usada por los casos de uso que no utilizan un rango de fechas. */
    public CriteriosBusquedaDTO(Long idMedico, String codigoEspecialidad, String dia, String codigoConsultorio,
            String estado, String ordenarPor, Boolean descendente) {
        this(idMedico, codigoEspecialidad, dia, codigoConsultorio, estado, ordenarPor, descendente, null, null, null, null, null, null);
    }

    /** Sin ningún criterio: devuelve todo, en el orden por defecto. */
    public static CriteriosBusquedaDTO vacio() {
        return new CriteriosBusquedaDTO(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
