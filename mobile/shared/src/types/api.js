/**
 * @fileoverview Tipos compartidos (JSDoc) para IntelliSense
 * Coinciden con los DTOs del backend (Spring Boot records)
 */

/**
 * @typedef {Object} LoginResponseDTO
 * @property {number} idUsuario
 * @property {string} nombre
 * @property {string} rol - 'paciente' | 'funcionario' | 'medico' | 'admin'
 */

/**
 * @typedef {Object} RegistroPacienteDTO
 * @property {string} cedula
 * @property {'PASSWORD'|'GOOGLE'} proveedor
 * @property {string} correo
 * @property {string} [contrasena] - Requerido si proveedor === 'PASSWORD'
 * @property {string} [tokenAcceso] - Requerido si proveedor === 'GOOGLE'
 * @property {string} [telefono]
 * @property {string} [domicilio]
 */

/**
 * @typedef {Object} VerificacionRegistroDTO
 * @property {string} cedula
 * @property {string} correo
 */

/**
 * @typedef {Object} PacienteResponseDTO
 * @property {number} id
 * @property {string} cedula
 * @property {string} nombre
 * @property {string} apellido
 * @property {string} correo
 * @property {string} [telefono]
 * @property {string} [domicilio]
 * @property {string} fechaNacimiento
 * @property {'ACTIVO'|'INACTIVO'} estado
 * @property {'PASSWORD'|'GOOGLE'} proveedorAcceso
 */

/**
 * @typedef {Object} EspecialidadResponseDTO
 * @property {string} codigo
 * @property {string} nombre
 * @property {string} descripcion
 * @property {boolean} requierePase
 * @property {'ACTIVO'|'INACTIVO'} estado
 */

/**
 * @typedef {Object} OcurrenciaTurnoDTO
 * @property {number} id
 * @property {string} fecha - YYYY-MM-DD
 * @property {string} horaInicio - HH:mm
 * @property {string} horaFin - HH:mm
 * @property {number} cupoMaximo
 * @property {number} cuposLibres
 * @property {Object} medico
 * @property {number} medico.id
 * @property {string} medico.nombre
 * @property {string} medico.apellido
 * @property {Object} consultorio
 * @property {number} consultorio.id
 * @property {string} consultorio.nombre
 * @property {number} duracionPorConsulta - minutos calculados
 */

/**
 * @typedef {Object} ReservaRequestDTO
 * @property {number} idOcurrenciaTurno
 * @property {number} numero
 */

/**
 * @typedef {Object} ReservaResponseDTO
 * @property {number} idReserva
 * @property {'VIGENTE'|'ORDEN_EMITIDA'|'ATENDIDA'|'CANCELADA'|'ANULADA'|'VENCIDA'} estado
 * @property {string} fecha - YYYY-MM-DD
 * @property {string} horaInicio - HH:mm:ss
 * @property {string} horaFin - HH:mm:ss
 * @property {number} numero
 * @property {string} horaEstimada - HH:mm:ss
 * @property {string} especialidad
 * @property {string} nombreMedico
 * @property {string} consultorio
 * @property {number|null} idPaciente
 * @property {string} nombrePaciente
 * @property {string} cedulaPaciente
 * @property {number|null} numeroOrden
 * @property {string|null} estadoOrden
 */

/**
 * Listado y vista previa de cancelación (CU-9).
 * @typedef {Object} ReservaCancelacionDTO
 * @property {ReservaResponseDTO} reserva
 * @property {boolean} puedeCancelar - Calculado por el backend desde el inicio del turno.
 * @property {string|null} motivoNoCancelable
 * @property {string|null} fechaCancelacion - Fecha y hora locales de la institución, YYYY-MM-DDTHH:mm:ss.
 */

/**
 * Consulta registrada (CU-23); la devuelven también CU-11, CU-22 y CU-26.
 * @typedef {Object} ConsultaResponseDTO
 * @property {number} idConsulta
 * @property {number} numeroOrden
 * @property {number} idReserva
 * @property {number} idPaciente
 * @property {string} nombrePaciente - «Apellido, Nombre»
 * @property {number} idMedico
 * @property {string} nombreMedico - «Apellido, Nombre»
 * @property {string} especialidad - nombre de la especialidad
 * @property {string} fecha - ISO datetime
 * @property {string} motivo
 * @property {string} observaciones
 * @property {string|null} indicaciones
 * @property {string} fechaRegistro - ISO datetime
 */

/**
 * @typedef {Object} AnuncioResponseDTO
 * @property {number} id
 * @property {string} titulo
 * @property {string} contenido
 * @property {string} fechaPublicacion - ISO datetime
 * @property {string} autor
 */

/**
 * @typedef {Object} PerfilDTO
 * @property {string} [telefono]
 * @property {string} [domicilio]
 */

/**
 * @typedef {Object} CriteriosBusquedaDTO
 * @property {number|string} [idMedico] - Identificador, no el nombre. En la URL se envía como texto.
 * @property {string} [codigoEspecialidad]
 * @property {string} [dia]
 * @property {string} [codigoConsultorio]
 * @property {string} [estado] - CU-10: estado de la reserva.
 * @property {string} [ordenarPor] - CU-10: fecha, medico, especialidad, estado o numero.
 * @property {boolean} [descendente=true] - CU-10 ordena por fecha descendente por defecto.
 * @property {string} [fechaDesde] - YYYY-MM-DD, inclusive.
 * @property {string} [fechaHasta] - YYYY-MM-DD, inclusive.
 */

/**
 * @typedef {Object} CambiarContrasenaDTO
 * @property {string} actual
 * @property {string} nueva
 */

/**
 * @typedef {Object} SolicitarRecuperacionDTO
 * @property {string} correo
 */

/**
 * @typedef {Object} RestablecerContrasenaDTO
 * @property {string} nuevaContrasena
 */

/**
 * @typedef {Object} RegistrarPushDTO
 * @property {string} token
 */

/**
 * @typedef {Object} NotificacionResponseDTO
 * @property {number} id
 * @property {string} titulo
 * @property {string} mensaje
 * @property {string} fecha - ISO datetime
 * @property {boolean} leida
 * @property {'RECORDATORIO_CONSULTA'|'SUSPENSION_TURNO'|'CONSULTA_REGISTRADA'|'RESERVA_CONFIRMADA'} tipo
 * @property {'EMAIL'|'PUSH'|'APLICACION'} canal
 */

/**
 * @typedef {'paciente'|'funcionario'|'medico'|'admin'} Rol
 */
