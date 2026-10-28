// CU-9: el backend decide el plazo desde el inicio del turno con su reloj de Montevideo.
// La interfaz muestra puedeCancelar y motivoNoCancelable de ReservaCancelacionDTO; no calcula las 48 horas.
export function motivoNoCancelable(item) {
  if (!item?.reserva || typeof item.puedeCancelar !== 'boolean') {
    return 'No se pudo verificar esta reserva. Actualice sus reservas.';
  }
  return item.puedeCancelar ? '' : (item.motivoNoCancelable || 'La reserva no admite cancelación.');
}

export const ESTADOS_RESERVA = {
  VIGENTE: 'Vigente', ORDEN_EMITIDA: 'Orden emitida', ATENDIDA: 'Atendida',
  CANCELADA: 'Cancelada', ANULADA: 'Anulada', VENCIDA: 'Vencida',
};
