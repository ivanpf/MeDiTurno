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
 * @property {number} id
 * @property {string} codigo - ej: "RES-000123"
 * @property {'VIGENTE'|'ORDEN_EMITIDA'|'ATENDIDA'|'CANCELADA'|'ANULADA'|'VENCIDA'} estado
 * @property {string} fechaOcurrencia - ISO datetime
 * @property {string} horaInicio - HH:mm
 * @property {string} horaFin - HH:mm
 * @property {number} numero
 * @property {string} horaEstimadaAtencion - ~HH:mm
 * @property {Object} especialidad
 * @property {string} especialidad.codigo
 * @property {string} especialidad.nombre
 * @property {Object} medico
 * @property {number} medico.id
 * @property {string} medico.nombre
 * @property {string} medico.apellido
 * @property {Object} consultorio
 * @property {number} consultorio.id
 * @property {string} consultorio.nombre
 * @property {boolean} requierePase
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
 * @property {number} [pagina=0]
 * @property {number} [tamanio=20]
 * @property {string} [ordenPor]
 * @property {'ASC'|'DESC'} [ordenDireccion]
 * @property {string} [fechaDesde]
 * @property {string} [fechaHasta]
 * @property {string} [especialidadCodigo]
 * @property {number} [medicoId]
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
 * @property {'RECORDATORIO_CONSULTA'|'SUSPENSION_TURNO'|'CONSULTA_REGISTRADA'} tipo
 * @property {'EMAIL'|'PUSH'|'APLICACION'} canal
 */

/**
 * @typedef {'paciente'|'funcionario'|'medico'|'admin'} Rol
 */