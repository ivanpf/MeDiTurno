import { test, expect } from '@playwright/test'
import { CONSULTAS_DE_ANA } from '../../utilidades/datos-seed.js'
import { comoFechaIso, hoyEnMontevideo, sumarDias } from '../../utilidades/fechas.js'

// CU-11 Consulta de la historia clínica propia. Caso de uso crítico (datos clínicos): nivel E2E.
// Solo consulta. Corre antes que las pruebas del médico que modifican datos (CU-23/24 le agregan una consulta a Ana).
// El flujo 3.A (paciente sin atenciones) no se puede provocar en E2E: el único paciente con cuenta es Ana. Va por API.

const filas = (page) => page.getByRole('table').getByRole('row').filter({ has: page.getByRole('cell') })
const consultasEsperadas = (page, consultas) =>
  expect(filas(page)).toHaveText(consultas.map((c) => new RegExp(`${c.especialidad}.*${c.medico}`)))
// allTextContents() no espera: primero se espera a que la tabla tenga todas las consultas.
const fechasDeLasFilas = async (page) => {
  await expect(filas(page)).toHaveCount(CONSULTAS_DE_ANA.length)
  return (await filas(page).getByRole('cell').filter({ hasText: /^\d{2}\/\d{2}\/\d{4}$/ }).allTextContents()).map(comoFechaIso)
}

test.beforeEach(async ({ page }) => {
  await page.goto('/paciente/historia-clinica')
  await expect(page.getByRole('heading', { name: 'Mi historia clínica', level: 1 })).toBeVisible()
})

test('CP-11-01: la paciente ve sus consultas, también de otros médicos, de la más reciente a la más antigua', async ({ page }) => {
  await consultasEsperadas(page, CONSULTAS_DE_ANA)

  const fechas = await fechasDeLasFilas(page)
  expect(fechas).toHaveLength(CONSULTAS_DE_ANA.length)
  expect(fechas).toEqual([...fechas].sort().reverse())
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('CP-11-02: el detalle de una consulta muestra lo que registró el médico', async ({ page }) => {
  const [masReciente] = CONSULTAS_DE_ANA
  await filas(page).first().getByRole('button', { name: 'Ver detalle' }).click()

  await expect(page.getByRole('heading', { name: /^Atención del \d{2}\/\d{2}\/\d{4}$/, level: 2 })).toBeVisible()
  await expect(page.getByText(`${masReciente.especialidad} · ${masReciente.medico}`)).toBeVisible()
  for (const titulo of ['Motivo de consulta:', 'Indicaciones:', 'Observaciones:']) {
    await expect(page.getByRole('heading', { name: titulo, level: 3 })).toBeVisible()
  }
  await expect(page.getByText(masReciente.motivo)).toBeVisible()

  await page.getByRole('button', { name: 'Cerrar', exact: true }).click()
  await expect(page.getByRole('heading', { name: /^Atención del / })).toHaveCount(0)
})

test('CP-11-03: los filtros por especialidad y por médico y el orden por fecha', async ({ page }) => {
  await test.step('especialidad: Clínica Médica', async () => {
    await page.getByLabel('Especialidad').selectOption({ label: 'Clínica Médica' })
    await consultasEsperadas(page, CONSULTAS_DE_ANA.filter((c) => c.especialidad === 'Clínica Médica'))
  })
  await test.step('«Limpiar filtros» vuelve a mostrar todas', async () => {
    await page.getByRole('button', { name: 'Limpiar filtros' }).click()
    await consultasEsperadas(page, CONSULTAS_DE_ANA)
  })
  await test.step('médico: Rossi, Diego', async () => {
    await page.getByLabel('Médico').selectOption({ label: 'Rossi, Diego' })
    await consultasEsperadas(page, CONSULTAS_DE_ANA.filter((c) => c.medico === 'Rossi, Diego'))
    await page.getByRole('button', { name: 'Limpiar filtros' }).click()
  })
  await test.step('orden: más antiguas primero', async () => {
    await page.getByLabel('Ordenar por fecha').selectOption({ label: 'Más antiguas' })
    await consultasEsperadas(page, [...CONSULTAS_DE_ANA].reverse())
  })
})

test('CP-11-04: el período incluye sus dos límites, avisa si está invertido y si no tiene consultas', async ({ page }) => {
  const [fecha] = await fechasDeLasFilas(page) // la de la consulta más reciente

  await test.step('desde y hasta el mismo día: incluye la consulta de ese día (valor límite)', async () => {
    await page.getByLabel('Desde').fill(fecha)
    await page.getByLabel('Hasta').fill(fecha)
    await consultasEsperadas(page, CONSULTAS_DE_ANA.slice(0, 1))
  })
  await test.step('«Hasta» un día antes de «Desde»: avisa y no consulta', async () => {
    await page.getByLabel('Hasta').fill(sumarDias(fecha, -1))
    await expect(page.getByRole('alert')).toHaveText('La fecha «Desde» no puede ser posterior a la fecha «Hasta».')
  })
  await test.step('un período sin consultas', async () => {
    await page.getByLabel('Hasta').fill('')
    await page.getByLabel('Desde').fill(sumarDias(hoyEnMontevideo(), 1))
    await expect(page.getByRole('alert')).toHaveCount(0)
    await expect(page.getByText('No se encontraron atenciones con los criterios seleccionados.')).toBeVisible()
    await expect(page.getByRole('table')).toHaveCount(0)
  })
})
