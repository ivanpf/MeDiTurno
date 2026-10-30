// CU-10: presentación y parámetros del historial. El backend filtra, ordena y limita
// los resultados al paciente autenticado; no se sustituye su búsqueda por filtros locales.
import { ESTADOS_RESERVA } from './cancelacionReserva';

export const FILTROS_HISTORIAL = {
  fechaDesde: '', fechaHasta: '', codigoEspecialidad: '', idMedico: '', estado: '',
  ordenarPor: 'fecha', descendente: 'true',
};

export const ESTADOS_HISTORIAL = [
  { value: '', label: 'Todos los estados' },
  ...Object.entries(ESTADOS_RESERVA).map(([value, label]) => ({ value, label })),
];

export const ORDENES_HISTORIAL = [
  { value: 'fecha', label: 'Fecha' },
  { value: 'medico', label: 'Médico' },
  { value: 'especialidad', label: 'Especialidad' },
  { value: 'estado', label: 'Estado' },
  { value: 'numero', label: 'Número de atención' },
];

export const DIRECCIONES_HISTORIAL = [
  { value: 'true', label: 'Descendente' },
  { value: 'false', label: 'Ascendente' },
];

export const ESTADOS_ORDEN_HISTORIAL = { EMITIDA: 'Emitida', UTILIZADA: 'Utilizada' };

export function formatearFechaHistorial(texto) {
  const digitos = texto.replace(/\D/g, '').slice(0, 8);
  return [digitos.slice(0, 2), digitos.slice(2, 4), digitos.slice(4)].filter(Boolean).join('/');
}

function fechaISO(texto) {
  const partes = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(texto);
  if (!partes) return null;
  const [dia, mes, anio] = partes.slice(1).map(Number);
  // UTC evita que el huso horario del dispositivo altere la validación del día.
  const fecha = new Date(Date.UTC(anio, mes - 1, dia));
  if (fecha.getUTCFullYear() !== anio || fecha.getUTCMonth() !== mes - 1 || fecha.getUTCDate() !== dia) return null;
  return `${partes[3]}-${partes[2]}-${partes[1]}`;
}

export function validarPeriodoHistorial({ fechaDesde, fechaHasta }) {
  const desde = fechaDesde ? fechaISO(fechaDesde) : '';
  const hasta = fechaHasta ? fechaISO(fechaHasta) : '';
  if (desde === null) return 'La fecha «Desde» no es válida. Ingrésela como DD/MM/AAAA.';
  if (hasta === null) return 'La fecha «Hasta» no es válida. Ingrésela como DD/MM/AAAA.';
  if (desde && hasta && desde > hasta) return 'La fecha «Desde» no puede ser posterior a la fecha «Hasta».';
  return null;
}

// CriteriosBusquedaDTO actual: los identificadores se envían como texto en la query,
// sin redondearlos ni inferirlos desde nombres. Spring interpreta idMedico como Long.
export function criteriosDelHistorial(filtros) {
  const criterios = { ordenarPor: filtros.ordenarPor, descendente: filtros.descendente === 'true' };
  if (filtros.fechaDesde) criterios.fechaDesde = fechaISO(filtros.fechaDesde);
  if (filtros.fechaHasta) criterios.fechaHasta = fechaISO(filtros.fechaHasta);
  if (filtros.codigoEspecialidad.trim()) criterios.codigoEspecialidad = filtros.codigoEspecialidad.trim();
  if (filtros.idMedico.trim()) criterios.idMedico = filtros.idMedico.trim();
  if (filtros.estado) criterios.estado = filtros.estado;
  return criterios;
}

export const horaDelHistorial = (hora) => hora?.slice(0, 5) || 'Sin horario';

export function errorDelHistorial(fallo, mensaje) {
  return { estado: fallo?.estado ?? 0, mensaje: fallo?.mensaje || mensaje, detalles: fallo?.detalles ?? [] };
}
