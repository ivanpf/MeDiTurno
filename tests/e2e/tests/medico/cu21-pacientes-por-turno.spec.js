import { test, expect } from '@playwright/test'
import { ANA, JOAQUIN, PEDRO, VALENTINA } from '../../utilidades/datos-seed.js'
import { fechaSinTurnosDeLaDraGomez } from '../../utilidades/fechas.js'
import { abrirTurnoDeHoy, filasDePacientes, filaDe } from '../../utilidades/mis-pacientes.js'

// CU-21 Consulta de pacientes por turno («Mis pacientes»). Caso de uso crítico: nivel E2E.
// Solo consulta: se puede repetir mientras el seed esté como se cargó y antes de las 21:00 (después la reserva
// vigente de Pedro vence y sale del listado).

test('CP-21-01: el turno de hoy lista las reservas vigentes y las órdenes emitidas, con número, hora estimada y estado', async ({ page }) => {
  await abrirTurnoDeHoy(page)

  await expect(page.getByText('3 pacientes en este turno.')).toBeVisible()
  await expect(filasDePacientes(page)).toHaveCount(3)

  // Columnas: número, paciente, hora estimada, estado de la orden, orden, historia clínica, consulta.
  // «Registrar consulta» solo se ofrece a quien tiene la orden emitida.
  await expect(filaDe(page, ANA).getByRole('cell')).toHaveText(
    [String(ANA.numero), ANA.nombre, ANA.hora, 'Orden emitida', /^\d+$/, 'Ver historia clínica', 'Registrar consulta'])
  await expect(filaDe(page, JOAQUIN).getByRole('cell')).toHaveText(
    [String(JOAQUIN.numero), JOAQUIN.nombre, JOAQUIN.hora, 'Orden emitida', /^\d+$/, 'Ver historia clínica', 'Registrar consulta'])
  await expect(filaDe(page, PEDRO).getByRole('cell')).toHaveText(
    [String(PEDRO.numero), PEDRO.nombre, PEDRO.hora, 'Sin emitir', '—', 'Ver historia clínica', '—'])

  // Clase inválida: una reserva cancelada no se lista.
  await expect(page.getByRole('table')).not.toContainText(VALENTINA.nombre)
})

test('CP-21-02: el listado se ordena por número, nombre y horario, ascendente y descendente', async ({ page }) => {
  await abrirTurnoDeHoy(page)
  const filas = filasDePacientes(page)
  const ordenEsperado = (...pacientes) => expect(filas).toHaveText(pacientes.map((p) => new RegExp(p.nombre)))

  // Por defecto, por número.
  await ordenEsperado(ANA, JOAQUIN, PEDRO)

  await page.getByLabel('Ordenar por').selectOption('nombre')
  await ordenEsperado(PEDRO, ANA, JOAQUIN) // Castro, López, Viera

  await page.getByLabel('Orden descendente').check()
  await ordenEsperado(JOAQUIN, ANA, PEDRO)

  await page.getByLabel('Ordenar por').selectOption('horario')
  await ordenEsperado(PEDRO, JOAQUIN, ANA)

  await page.getByLabel('Orden descendente').uncheck()
  await ordenEsperado(ANA, JOAQUIN, PEDRO)
})

test('CP-21-03: una fecha en la que la médica no atiende informa que no tiene turnos', async ({ page }) => {
  await page.goto('/medico/pacientes')
  await expect(page.getByRole('group', { name: 'Turnos de atención' })).toBeVisible()

  await page.getByLabel('Fecha de atención').fill(fechaSinTurnosDeLaDraGomez())

  await expect(page.getByText('No tiene turnos de atención registrados para esta fecha.')).toBeVisible()
  await expect(page.getByRole('group', { name: 'Turnos de atención' })).toHaveCount(0)

  // El botón «Hoy» vuelve a la fecha de hoy y a su turno.
  await page.getByRole('button', { name: 'Hoy', exact: true }).click()
  await expect(page.getByRole('group', { name: 'Turnos de atención' })).toBeVisible()
})
