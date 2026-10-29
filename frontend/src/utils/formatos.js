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
