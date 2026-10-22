// Cálculos de presentación del CU-8. Las reglas de autorización y disponibilidad
// las decide el backend; estos helpers preparan los datos que ya devolvió la API.

export function ordenarOcurrencias(lista) {
  return [...lista].sort((a, b) =>
    `${a.fecha} ${a.horaInicio}`.localeCompare(`${b.fecha} ${b.horaInicio}`));
}

export function obtenerMedicos(ocurrencias) {
  const porId = new Map();
  for (const turno of ocurrencias) {
    const id = String(turno.idMedico);
    porId.set(id, { value: id, label: turno.nombreMedico });
  }
  return [...porId.values()].sort((a, b) => a.label.localeCompare(b.label, 'es'));
}

// Usamos mediodía local: interpretar "YYYY-MM-DD" como UTC puede mostrar el día anterior.
const comoFechaLocal = (fecha) => new Date(`${fecha}T12:00:00`);
const fechaISO = (fecha) => [fecha.getFullYear(),
  String(fecha.getMonth() + 1).padStart(2, '0'),
  String(fecha.getDate()).padStart(2, '0')].join('-');

export function fechaVisible(fecha) {
  return comoFechaLocal(fecha).toLocaleDateString('es-UY', {
    weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric',
  });
}

export function agruparPorSemana(ocurrencias) {
  const semanas = new Map();
  for (const turno of ordenarOcurrencias(ocurrencias)) {
    const lunes = comoFechaLocal(turno.fecha);
    lunes.setDate(lunes.getDate() - (lunes.getDay() + 6) % 7);
    const inicio = fechaISO(lunes);
    if (!semanas.has(inicio)) semanas.set(inicio, { inicio, turnos: [] });
    semanas.get(inicio).turnos.push(turno);
  }
  return [...semanas.values()];
}

export function esNumeroLibre(turno, numero) {
  return !!turno && Number.isInteger(numero) && numero >= 1 && numero <= turno.cupos
    && !(turno.numerosNoDisponibles ?? []).includes(numero);
}

export function cuposLibres(turno) {
  return Array.from({ length: turno.cupos }, (_, i) => i + 1)
    .filter((numero) => esNumeroLibre(turno, numero)).length;
}

export function horaEstimada(turno, numero) {
  const minutos = (hora) => Number(hora.slice(0, 2)) * 60 + Number(hora.slice(3, 5));
  const inicio = minutos(turno.horaInicio);
  // Misma división entera que DisponibilidadService.horaEstimada del backend.
  // Redondear primero la duración de cada consulta acumularía diferencias.
  const estimada = inicio + Math.floor((numero - 1) * (minutos(turno.horaFin) - inicio) / turno.cupos);
  return `${String(Math.floor(estimada / 60)).padStart(2, '0')}:${String(estimada % 60).padStart(2, '0')}`;
}

export function numerosDelTurno(turno) {
  return Array.from({ length: turno.cupos }, (_, i) => ({
    numero: i + 1,
    disponible: esNumeroLibre(turno, i + 1),
    hora: horaEstimada(turno, i + 1),
  }));
}

export function comoError(fallo, mensaje) {
  return {
    estado: fallo?.estado ?? 0,
    mensaje: fallo?.estado ? (fallo.mensaje || fallo.message || mensaje) : mensaje,
    detalles: fallo?.detalles ?? [],
  };
}
