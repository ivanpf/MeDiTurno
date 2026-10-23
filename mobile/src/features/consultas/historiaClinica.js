// Cálculos de presentación del CU-11. Qué atenciones ve el paciente lo decide el backend
// (solo las finalizadas y propias); estos helpers preparan los filtros y los datos que devolvió la API.

// Las fechas del período se escriben como DD/MM/AAAA; al backend se mandan como AAAA-MM-DD.
export const FILTROS_VACIOS = { fechaDesde: '', fechaHasta: '', codigoEspecialidad: '', idMedico: '', descendente: true };

export const ORDENES = [
  { value: 'true', label: 'Más recientes' },
  { value: 'false', label: 'Más antiguas' },
];

// «2026-10-22T09:15:00» → «22/10/2026»
export function fechaVisible(iso) {
  return iso.slice(0, 10).split('-').reverse().join('/');
}

// Mientras se escribe, agrega las barras: «2210» → «22/10».
export function formatearEntradaFecha(texto) {
  const digitos = texto.replace(/\D/g, '').slice(0, 8);
  return [digitos.slice(0, 2), digitos.slice(2, 4), digitos.slice(4)].filter(Boolean).join('/');
}

// «22/10/2026» → «2026-10-22», o null si no es una fecha real.
export function fechaISO(texto) {
  const partes = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(texto);
  if (!partes) return null;
  const [dia, mes, anio] = partes.slice(1).map(Number);
  const fecha = new Date(anio, mes - 1, dia);
  if (fecha.getFullYear() !== anio || fecha.getMonth() !== mes - 1 || fecha.getDate() !== dia) return null;
  return `${partes[3]}-${partes[2]}-${partes[1]}`;
}

// Una fecha del período a medio escribir todavía no filtra.
export const fechaIncompleta = (texto) => texto.length > 0 && texto.length < 10;

// Devuelve el mensaje del primer problema del período, o null si se puede buscar.
export function validarPeriodo({ fechaDesde, fechaHasta }) {
  const desde = fechaDesde && fechaISO(fechaDesde);
  const hasta = fechaHasta && fechaISO(fechaHasta);
  if (desde === null) return 'La fecha «Desde» no es válida. Ingrésela como DD/MM/AAAA.';
  if (hasta === null) return 'La fecha «Hasta» no es válida. Ingrésela como DD/MM/AAAA.';
  if (desde && hasta && desde > hasta) return 'La fecha «Desde» no puede ser posterior a la fecha «Hasta».';
  return null;
}

// Parámetros de CriteriosBusquedaDTO para GET /pacientes/me/historia-clinica: el backend solo ordena por fecha
// y los criterios vacíos no filtran.
export function criteriosDeConsulta(filtros) {
  const criterios = { ordenarPor: 'fecha', descendente: filtros.descendente };
  if (filtros.fechaDesde) criterios.fechaDesde = fechaISO(filtros.fechaDesde);
  if (filtros.fechaHasta) criterios.fechaHasta = fechaISO(filtros.fechaHasta);
  if (filtros.codigoEspecialidad) criterios.codigoEspecialidad = filtros.codigoEspecialidad;
  if (filtros.idMedico) criterios.idMedico = filtros.idMedico;
  return criterios;
}

// Las opciones de los filtros salen de las atenciones del paciente: solo los médicos y especialidades que lo atendieron.
// La consulta devuelve el nombre de la especialidad; el código para filtrar se toma del listado de especialidades.
export function opcionesDeFiltro(atenciones, especialidades) {
  const medicos = new Map(atenciones.map((a) => [String(a.idMedico), a.nombreMedico]));
  const nombres = new Set(atenciones.map((a) => a.especialidad));
  return {
    medicos: [...medicos].map(([value, label]) => ({ value, label })).sort((a, b) => a.label.localeCompare(b.label, 'es')),
    especialidades: especialidades.filter((e) => nombres.has(e.nombre)).map((e) => ({ value: e.codigo, label: e.nombre })),
  };
}

// Secciones del detalle, en el orden de la interfaz acordada. ConsultaResponseDTO solo trae estos tres textos
// clínicos; antecedentes, diagnóstico y tratamiento no forman parte de la respuesta del backend.
export function seccionesDelDetalle(atencion) {
  return [
    { titulo: 'Motivo de consulta', texto: atencion.motivo },
    { titulo: 'Indicaciones', texto: atencion.indicaciones || 'Sin indicaciones.' },
    { titulo: 'Observaciones', texto: atencion.observaciones || 'Sin observaciones.' },
  ];
}

export function comoError(fallo, mensaje) {
  return {
    estado: fallo?.estado ?? 0,
    mensaje: fallo?.estado ? (fallo.mensaje || fallo.message || mensaje) : mensaje,
    detalles: fallo?.detalles ?? [],
  };
}
