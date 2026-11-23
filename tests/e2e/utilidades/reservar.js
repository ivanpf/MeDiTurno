import { expect } from '@playwright/test'
import { comoDiaYMes } from './fechas.js'

// Pasos de «Reservar consulta» (CU-8) que usan varias pruebas del paciente (CU-8 y CU-9).

// Elige especialidad y médico y muestra sus turnos de las próximas ocho semanas.
export async function verTurnosDe(page, { especialidad, medico }) {
  await page.goto('/paciente/reservar')
  await page.getByLabel('Especialidad').selectOption({ label: especialidad })
  await page.getByLabel('Médico').selectOption({ label: medico })
  await page.getByRole('button', { name: 'Ver turnos' }).click()
  await expect(page.getByRole('heading', { name: '2. Turnos disponibles (próximas ocho semanas)' })).toBeVisible()
}

// Botones de los turnos: cada uno dice la fecha («lun., 30/11»), el horario, el consultorio y los cupos libres.
export const botonesDeTurnos = (page) => page.getByRole('button', { name: /Consultorio:/ })
export const turnosConCupos = (page) => page.getByRole('button', { name: /Consultorio:/, disabled: false })

// El turno de una fecha (AAAA-MM-DD) y un horario («09:00–12:00», con guion largo como en la pantalla).
export const botonDelTurno = (page, fecha, horario) =>
  botonesDeTurnos(page).filter({ hasText: comoDiaYMes(fecha) }).filter({ hasText: horario })

// Botón de un número del turno elegido («N.º 5», su hora estimada y «Ocupado» si no está libre).
export const botonDelNumero = (page, numero) => page.getByRole('button', { name: new RegExp(`^N\\.º ${numero}(?!\\d)`) })
export const numerosLibres = (page) => page.getByRole('button', { name: /^N\.º \d+/, disabled: false })
