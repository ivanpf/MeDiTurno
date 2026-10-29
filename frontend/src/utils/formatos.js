// Formatos de datos del backend que se repiten en varias pantallas.

// El backend manda las horas como «09:00:00»; se muestran como «09:00».
export const hora = (texto) => texto.slice(0, 5)

export const NOMBRE_DIA = {
  LUNES: 'Lunes',
  MARTES: 'Martes',
  MIERCOLES: 'Miércoles',
  JUEVES: 'Jueves',
  VIERNES: 'Viernes',
  SABADO: 'Sábado',
  DOMINGO: 'Domingo',
}

// El backend manda las fechas como «1990-05-17» (o «1990-05-17T10:20:30» si traen hora); se muestran como «17/05/1990».
export const fecha = (texto) => (texto ? texto.slice(0, 10).split('-').reverse().join('/') : '')

// Los roles de los usuarios internos como los manda el backend (AltaUsuarioInternoDTO.RolUsuarioInterno).
export const NOMBRE_ROL_INTERNO = {
  ADMINISTRADOR: 'Administrador',
  FUNCIONARIO: 'Funcionario',
  MEDICO: 'Médico',
}
