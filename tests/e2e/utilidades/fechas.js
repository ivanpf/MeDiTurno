import { DIAS_DE_LA_DRA_GOMEZ } from './datos-seed.js'

// Fechas en la zona de la aplicación (America/Montevideo), como el Clock del backend y la pantalla, sin depender de la
// zona horaria de la computadora que corre las pruebas. Formato AAAA-MM-DD, el de los <input type="date">.

export const hoyEnMontevideo = () =>
  new Intl.DateTimeFormat('sv-SE', { timeZone: 'America/Montevideo' }).format(new Date())

// Suma días a una fecha AAAA-MM-DD (se calcula en UTC para que no influyan los cambios de hora).
export const sumarDias = (fecha, dias) => {
  const d = new Date(`${fecha}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + dias)
  return d.toISOString().slice(0, 10)
}

const diaDeLaSemana = (fecha) => new Date(`${fecha}T00:00:00Z`).getUTCDay()

// Próxima fecha en la que la Dra. Gómez no tiene turnos: ni lunes, ni miércoles, ni el día de la semana en que se cargó
// el seed (su turno de 18 a 21 se repite ese día todas las semanas). Supone que el seed se cargó hoy.
export const fechaSinTurnosDeLaDraGomez = () => {
  const hoy = hoyEnMontevideo()
  const ocupados = [...DIAS_DE_LA_DRA_GOMEZ, diaDeLaSemana(hoy)]
  for (let dias = 1; dias <= 7; dias++) {
    const fecha = sumarDias(hoy, dias)
    if (!ocupados.includes(diaDeLaSemana(fecha))) return fecha
  }
  throw new Error('No hay ningún día de la semana sin turnos de la Dra. Gómez: revisar DIAS_DE_LA_DRA_GOMEZ')
}

// Primera fecha a partir de «desde» (inclusive) que cae en ese día de la semana (0 = domingo … 6 = sábado).
export const primerDiaDeLaSemana = (desde, dia) => {
  for (let dias = 0; dias < 7; dias++) {
    const fecha = sumarDias(desde, dias)
    if (diaDeLaSemana(fecha) === dia) return fecha
  }
}

// AAAA-MM-DD → «dd/mm/aaaa» (listados) y «dd/mm» (botones de los turnos en «Reservar consulta»).
export const comoFecha = (fecha) => fecha.split('-').reverse().join('/')
export const comoDiaYMes = (fecha) => comoFecha(fecha).slice(0, 5)
// «dd/mm/aaaa» → AAAA-MM-DD (para un <input type="date">).
export const comoFechaIso = (texto) => texto.split('/').reverse().join('-')

// Diferencia en días entre dos fechas con hora del backend (LocalDateTime, sin zona: "2026-11-22T19:10:00").
// Se compara al segundo (el backend puede mandar fracciones de segundo con más dígitos de los que lee Date.parse).
const alSegundo = (fechaHora) => Date.parse(`${fechaHora.slice(0, 19)}Z`)
export const diasEntre = (desde, hasta) => (alSegundo(hasta) - alSegundo(desde)) / 86_400_000
