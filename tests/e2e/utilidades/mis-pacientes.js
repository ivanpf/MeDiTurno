import { expect } from '@playwright/test'
import { TURNO_DE_HOY } from './datos-seed.js'

// Pasos de «Mis pacientes» (CU-21) que usan varias pruebas: es la entrada del médico a la atención (CU-22, CU-23 y CU-24).

// Abre «Mis pacientes» (se abre con la fecha de hoy) y elige el turno de hoy de 18 a 21.
export async function abrirTurnoDeHoy(page) {
  await page.goto('/medico/pacientes')
  await page.getByRole('group', { name: 'Turnos de atención' })
    .getByRole('button', { name: new RegExp(TURNO_DE_HOY.horario) })
    .click()
  await expect(page.getByRole('heading', { name: TURNO_DE_HOY.titulo })).toBeVisible()
}

// Filas de pacientes de la tabla (sin la fila de encabezados, que no tiene celdas comunes).
export const filasDePacientes = (page) =>
  page.getByRole('table').getByRole('row').filter({ has: page.getByRole('cell') })

export const filaDe = (page, paciente) => filasDePacientes(page).filter({ hasText: paciente.nombre })
