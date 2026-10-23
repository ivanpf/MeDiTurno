// CU-9: comprobaciones orientativas de la interfaz. La autorización definitiva es del backend.
// Se usa el horario de la institución, aunque el teléfono esté en otra zona horaria.
const formatoInstitucion = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'America/Montevideo', year: 'numeric', month: '2-digit', day: '2-digit',
  hour: '2-digit', minute: '2-digit', second: '2-digit', hourCycle: 'h23',
});

export function motivoNoCancelable(reserva, ahora = new Date()) {
  if (!reserva || reserva.estado !== 'VIGENTE') return 'La reserva no está vigente y no puede cancelarse.';
  const hora = reserva.horaEstimada || reserva.horaInicio;
  if (!/^\d{4}-\d{2}-\d{2}$/.test(reserva.fecha || '') || !/^\d{2}:\d{2}(:\d{2})?$/.test(hora || '')) {
    return 'No se puede comprobar el plazo de cancelación. Actualice sus reservas.';
  }
  const partes = Object.fromEntries(formatoInstitucion.formatToParts(ahora)
    .filter((parte) => parte.type !== 'literal').map((parte) => [parte.type, parte.value]));
  // Fechas civiles de Montevideo, como LocalDateTime del backend; sin usar la zona del dispositivo.
  const ahoraInstitucion = Date.UTC(+partes.year, +partes.month - 1, +partes.day,
    +partes.hour, +partes.minute, +partes.second, ahora.getMilliseconds());
  const consulta = Date.parse(`${reserva.fecha}T${hora.length === 5 ? `${hora}:00` : hora}Z`);
  if (!Number.isFinite(consulta)) return 'No se puede comprobar el plazo de cancelación. Actualice sus reservas.';
  return consulta - ahoraInstitucion >= 48 * 60 * 60 * 1000 ? ''
    : 'Venció el plazo de cancelación: deben faltar al menos 48 horas para la consulta.';
}

export const ESTADOS_RESERVA = {
  VIGENTE: 'Vigente', ORDEN_EMITIDA: 'Orden emitida', ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada', ANULADA: 'Anulada', VENCIDA: 'Vencida',
};
