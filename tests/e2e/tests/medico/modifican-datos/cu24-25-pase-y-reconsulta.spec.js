import { test, expect } from '@playwright/test'
import { ANA, TURNO_DE_HOY } from '../../../utilidades/datos-seed.js'
import { diasEntre } from '../../../utilidades/fechas.js'
import { abrirTurnoDeHoy, filaDe } from '../../../utilidades/mis-pacientes.js'

// CU-24 Emisión de un pase a otra especialidad y CU-25 Habilitación de una reconsulta (botón dentro de «Emitir pase»).
// Casos de uso críticos (habilitan reservas en especialidades que exigen pase): nivel E2E.
// MODIFICA DATOS: registra la consulta de Ana y le emite un pase y una reconsulta. Para repetirlo hay que recargar el seed.
//
// «Emitir pase» recibe la consulta por la navegación (desde el registro de la consulta o desde el historial de atenciones).
// Las pruebas comparten una sola página, en orden: la precondición registra la consulta y deja la página en «Emitir pase».
test.describe.configure({ mode: 'serial' })

/** @type {import('@playwright/test').Page} */
let page

// Aunque la página no es la del fixture, al fallar el reporte igual trae captura y traza (no video).
test.beforeAll(async ({ browser }, testInfo) => {
  const { baseURL, storageState, locale, timezoneId, viewport } = testInfo.project.use
  page = await browser.newPage({ baseURL, storageState, locale, timezoneId, viewport })

  // Precondición (CU-23): registrar la consulta de Ana del turno de hoy.
  await abrirTurnoDeHoy(page)
  await filaDe(page, ANA).getByRole('link', { name: 'Registrar consulta' }).click()
  await page.getByLabel('Motivo de la consulta').fill('Lesiones en la piel de los antebrazos (prueba E2E).')
  await page.getByLabel('Observaciones clínicas').fill('Placas eritematosas descamativas. Se deriva a Dermatología.')
  await page.getByRole('button', { name: 'Registrar consulta' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Registrar' }).click()
  await expect(page.getByText('Consulta registrada correctamente.')).toBeVisible()

  await page.getByRole('link', { name: 'Emitir pase a otra especialidad' }).click()
  await expect(page.getByRole('heading', { name: 'Emitir pase a otra especialidad', level: 1 })).toBeVisible()
})

test.afterAll(async () => {
  await page?.close()
})

const selector = () => page.getByLabel('Especialidad de destino')

test('CP-24-01: solo se ofrecen las especialidades activas como destino', async () => {
  await expect(page.getByRole('definition').filter({ hasText: ANA.nombre })).toBeVisible()
  await expect(selector()).toBeEnabled()

  await expect(selector().getByRole('option', { name: 'Dermatología' })).toHaveCount(1)
  await expect(selector().getByRole('option', { name: 'Neurología' })).toHaveCount(0) // inactiva en el seed

  // Observación para el Documento de Verificación: CU-24 habla de «otra especialidad», pero la lista incluye la misma
  // especialidad de la consulta. No hace fallar la prueba: queda anotado en el reporte.
  if (await selector().getByRole('option', { name: TURNO_DE_HOY.especialidad }).count()) {
    test.info().annotations.push({
      type: 'observación',
      description: `El destino ofrece ${TURNO_DE_HOY.especialidad}, la misma especialidad de la consulta de origen.`,
    })
  }
})

test('CP-24-02: cancelar la confirmación no emite el pase', async () => {
  await selector().selectOption({ label: 'Dermatología' })
  await page.getByRole('button', { name: 'Emitir pase' }).click()

  const confirmacion = page.getByRole('dialog')
  await expect(confirmacion).toContainText(`Se emitirá un pase para ${ANA.nombre} a Dermatología`)
  await confirmacion.getByRole('button', { name: 'Cancelar' }).click()

  await expect(confirmacion).toHaveCount(0)
  await expect(page.getByText('Pase emitido correctamente')).toHaveCount(0)
  await expect(selector()).toBeVisible()
})

test('CP-24-03: el médico emite un pase a Dermatología, vigente por 60 días', async () => {
  await selector().selectOption({ label: 'Dermatología' })
  await page.getByRole('button', { name: 'Emitir pase' }).click()
  const respuesta = page.waitForResponse((r) => /\/api\/consultas\/\d+\/pases$/.test(r.url()) && r.request().method() === 'POST')
  await page.getByRole('dialog').getByRole('button', { name: 'Emitir pase' }).click()

  const pase = await (await respuesta).json()
  expect((await respuesta).status()).toBe(201)
  expect(pase.especialidadDestino).toBe('DERM')
  expect(pase.paseEstado).toBe('VIGENTE')
  expect(diasEntre(pase.fechaEmision, pase.fechaVigenciaCalculada)).toBe(60)

  await expect(page.getByRole('status')).toContainText('Pase emitido correctamente. No genera una reserva por sí mismo.')
  // La pantalla muestra la vigencia que devolvió el backend (formateada por el navegador, como lo hace la página).
  const validoHasta = await page.evaluate((valor) =>
    new Intl.DateTimeFormat('es-UY', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(valor)), pase.fechaVigenciaCalculada)
  await expect(dato('Especialidad de destino')).toHaveText('Dermatología')
  await expect(dato('Válido hasta')).toHaveText(validoHasta)
  await expect(dato('Estado')).toHaveText('Vigente')
})

test('CP-25-01: el médico habilita una reconsulta, que es un pase a la misma especialidad de la consulta', async () => {
  // La pantalla de éxito no ofrece emitir otro pase: el médico vuelve atrás y adelante con el navegador y la pantalla
  // se abre de nuevo con la misma consulta. (Observación: tampoco hay otro acceso; el historial de atenciones no lo ofrece.)
  await page.goBack()
  await page.goForward()
  await expect(page.getByRole('heading', { name: 'Emitir pase a otra especialidad', level: 1 })).toBeVisible()

  const respuesta = page.waitForResponse((r) => /\/api\/consultas\/\d+\/reconsultas$/.test(r.url()))
  await page.getByRole('button', { name: `Reconsulta (${TURNO_DE_HOY.especialidad})` }).click()

  const reconsulta = await (await respuesta).json()
  expect((await respuesta).status()).toBe(201)
  expect(reconsulta.especialidadDestino).toBe(reconsulta.especialidadOrigen)
  expect(diasEntre(reconsulta.fechaEmision, reconsulta.fechaVigenciaCalculada)).toBe(60)

  await expect(page.getByRole('status')).toContainText('Reconsulta habilitada correctamente. No genera una reserva por sí mismo.')
  await expect(dato('Especialidad de destino')).toHaveText(TURNO_DE_HOY.especialidad)
  await expect(dato('Estado')).toHaveText('Vigente')
})

// Valor (<dd>) de un dato del resumen del pase, por su título (<dt>): cada par está en su propio <div> dentro del <dl>.
const dato = (titulo) => page.locator('dl > div')
  .filter({ has: page.getByRole('term').getByText(titulo, { exact: true }) })
  .getByRole('definition')
