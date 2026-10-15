/**
 * Utilidades de fecha y hora.
 * Equivalentes a las usadas en el frontend web.
 */

/**
 * Formatea una fecha ISO a DD/MM/YYYY
 * @param {string} fechaISO - ISO string (YYYY-MM-DD o datetime)
 * @returns {string}
 */
export function formatearFecha(fechaISO) {
  if (!fechaISO) return '';
  const fecha = new Date(fechaISO);
  if (isNaN(fecha.getTime())) return '';
  const dia = String(fecha.getDate()).padStart(2, '0');
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const anio = fecha.getFullYear();
  return `${dia}/${mes}/${anio}`;
}

/**
 * Formatea una hora ISO a HH:mm
 * @param {string} horaISO - ISO string o HH:mm:ss
 * @returns {string}
 */
export function formatearHora(horaISO) {
  if (!horaISO) return '';
  // Si ya viene como HH:mm o HH:mm:ss
  if (horaISO.includes(':')) {
    return horaISO.substring(0, 5);
  }
  const fecha = new Date(horaISO);
  if (isNaN(fecha.getTime())) return '';
  const horas = String(fecha.getHours()).padStart(2, '0');
  const minutos = String(fecha.getMinutes()).padStart(2, '0');
  return `${horas}:${minutos}`;
}

/**
 * Formatea datetime ISO a "DD/MM/YYYY HH:mm"
 * @param {string} datetimeISO
 * @returns {string}
 */
export function formatearFechaHora(datetimeISO) {
  if (!datetimeISO) return '';
  return `${formatearFecha(datetimeISO)} ${formatearHora(datetimeISO)}`;
}

/**
 * Calcula la hora estimada de atención.
 * duraciónPorConsulta = (horaFin - horaInicio) / cupoMaximo
 * horaEstimada = horaInicio + (numero - 1) * duraciónPorConsulta
 *
 * @param {string} horaInicio - HH:mm
 * @param {string} horaFin - HH:mm
 * @param {number} cupoMaximo
 * @param {number} numero - Número de la reserva (1-based)
 * @returns {string} - HH:mm
 */
export function calcularHoraEstimada(horaInicio, horaFin, cupoMaximo, numero) {
  const [hi, mi] = horaInicio.split(':').map(Number);
  const [hf, mf] = horaFin.split(':').map(Number);
  const inicioMin = hi * 60 + mi;
  const finMin = hf * 60 + mf;
  const duracion = Math.round((finMin - inicioMin) / cupoMaximo);
  const estimadaMin = inicioMin + (numero - 1) * duracion;
  const h = Math.floor(estimadaMin / 60);
  const m = estimadaMin % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/**
 * Obtiene el nombre del día de la semana en español corto.
 * @param {string} fechaISO - YYYY-MM-DD
 * @returns {string} - "Lun", "Mar", etc.
 */
export function obtenerDiaSemanaCorto(fechaISO) {
  const dias = ['Dom', 'Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb'];
  const fecha = new Date(fechaISO + 'T00:00:00');
  return dias[fecha.getDay()];
}

/**
 * Formatea fecha para mostrar en cards: "Lun 12/10"
 * @param {string} fechaISO - YYYY-MM-DD
 * @returns {string}
 */
export function formatearFechaCorta(fechaISO) {
  const dia = obtenerDiaSemanaCorto(fechaISO);
  const fecha = new Date(fechaISO + 'T00:00:00');
  const dd = String(fecha.getDate()).padStart(2, '0');
  const mm = String(fecha.getMonth() + 1).padStart(2, '0');
  return `${dia} ${dd}/${mm}`;
}

/**
 * Verifica si una fecha es hoy o futura.
 * @param {string} fechaISO - YYYY-MM-DD
 * @returns {boolean}
 */
export function esFechaFutura(fechaISO) {
  const hoy = new Date();
  hoy.setHours(0, 0, 0, 0);
  const fecha = new Date(fechaISO + 'T00:00:00');
  return fecha >= hoy;
}

/**
 * Calcula horas de diferencia entre ahora y una fecha/hora.
 * @param {string} fechaISO - YYYY-MM-DD
 * @param {string} horaISO - HH:mm
 * @returns {number} - Horas (decimal)
 */
export function horasHasta(fechaISO, horaISO) {
  const [h, m] = horaISO.split(':').map(Number);
  const objetivo = new Date(fechaISO + 'T00:00:00');
  objetivo.setHours(h, m, 0, 0);
  const ahora = new Date();
  const diffMs = objetivo - ahora;
  return diffMs / (1000 * 60 * 60);
}

/**
 * Verifica si se puede cancelar (>48 horas).
 * @param {string} fechaISO - YYYY-MM-DD
 * @param {string} horaISO - HH:mm
 * @returns {boolean}
 */
export function sePuedeCancelar(fechaISO, horaISO) {
  return horasHasta(fechaISO, horaISO) >= 48;
}