import { test, expect } from '@playwright/test'
import { ANA, JOAQUIN } from '../../../utilidades/datos-seed.js'
import { abrirTurnoDeHoy, filasDePacientes, filaDe } from '../../../utilidades/mis-pacientes.js'

// CU-23 Registro de una consulta realizada. Caso de uso crítico (escribe la historia clínica): nivel E2E.
// MODIFICA DATOS: CP-23-04 registra la consulta de Joaquín. Para repetirlo hay que recargar el seed.
// Las pruebas corren en orden: las primeras no guardan nada y la última registra.
// Ana queda con la orden emitida: la usa cu24-25-pase-y-reconsulta.spec.js, que corre después.
test.describe.configure({ mode: 'serial' })

async function abrirRegistroDe(page, paciente) {
  await abrirTurnoDeHoy(page)
  await filaDe(page, paciente).getByRole('link', { name: 'Registrar consulta' }).click()
  await expect(page).toHaveURL(/\/medico\/consulta$/)
  await expect(page.getByRole('heading', { name: 'Registrar consulta', level: 1 })).toBeVisible()
  await expect(page.getByRole('definition').filter({ hasText: paciente.nombre })).toBeVisible()
}

const completar = async (page, { motivo, observaciones, indicaciones = '' }) => {
  await page.getByLabel('Motivo de la consulta').fill(motivo)
  await page.getByLabel('Observaciones clínicas').fill(observaciones)
  await page.getByLabel('Indicaciones').fill(indicaciones)
}

test('CP-23-01: sin un paciente elegido en «Mis pacientes», la pantalla pide elegirlo primero', async ({ page }) => {
  await page.goto('/medico/consulta')

  await expect(page.getByText('Elija primero al paciente atendido desde «Mis pacientes».')).toBeVisible()
  await expect(page.getByRole('button', { name: 'Registrar consulta' })).toHaveCount(0)
})

test('CP-23-02: los datos obligatorios vacíos o con solo espacios no se registran (flujo 7.A)', async ({ page }) => {
  await abrirRegistroDe(page, ANA)
  const motivo = page.getByLabel('Motivo de la consulta')

  await test.step('vacíos: el navegador no deja enviar el formulario', async () => {
    await page.getByRole('button', { name: 'Registrar consulta' }).click()
    await expect(page.getByRole('dialog')).toHaveCount(0)
    expect(await motivo.evaluate((campo) => campo.validity.valueMissing)).toBe(true)
  })

  await test.step('solo espacios: la pantalla avisa y no pide confirmación', async () => {
    await completar(page, { motivo: '   ', observaciones: '   ' })
    await page.getByRole('button', { name: 'Registrar consulta' }).click()
    await expect(page.getByRole('alert')).toContainText('Complete los campos obligatorios.')
    await expect(page.getByRole('dialog')).toHaveCount(0)
  })
})

test('CP-23-03: cancelar la confirmación no registra la consulta', async ({ page }) => {
  await abrirRegistroDe(page, ANA)
  await completar(page, { motivo: 'Control (prueba E2E, cancelada)', observaciones: 'Sin cambios.' })
  await page.getByRole('button', { name: 'Registrar consulta' }).click()

  const confirmacion = page.getByRole('dialog')
  await expect(confirmacion).toContainText(`Se registrará la consulta de ${ANA.nombre}`)
  await confirmacion.getByRole('button', { name: 'Cancelar' }).click()

  await expect(confirmacion).toHaveCount(0)
  await expect(page.getByText('Consulta registrada correctamente.')).toHaveCount(0)
  // Ana sigue pendiente de atención en «Mis pacientes».
  await abrirTurnoDeHoy(page)
  await expect(filaDe(page, ANA).getByRole('link', { name: 'Registrar consulta' })).toBeVisible()
})

test('CP-23-04: el médico registra la consulta de un paciente sin historia clínica y deja de estar pendiente', async ({ page }) => {
  await abrirRegistroDe(page, JOAQUIN)
  await completar(page, {
    motivo: 'Dolor de pecho al hacer esfuerzo (prueba E2E).',
    observaciones: 'Presión 130/85. Ritmo regular. Electrocardiograma sin alteraciones.',
    indicaciones: 'Control en 30 días.',
  })
  await page.getByRole('button', { name: 'Registrar consulta' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Registrar' }).click()

  await expect(page.getByRole('status')).toContainText(
    'Consulta registrada correctamente. Se avisó al paciente que se incorporó nueva información a su historia clínica.')
  await expect(page.getByRole('link', { name: 'Emitir pase a otra especialidad' })).toBeVisible()
  await expect(page.getByRole('button', { name: 'Registrar consulta' })).toHaveCount(0)

  // La reserva pasa a ATENDIDA: Joaquín sale del listado del turno.
  await page.getByRole('link', { name: 'Volver a Mis pacientes' }).click()
  await abrirTurnoDeHoy(page)
  await expect(page.getByText('2 pacientes en este turno.')).toBeVisible()
  await expect(filasDePacientes(page)).toHaveCount(2)
  await expect(page.getByRole('table')).not.toContainText(JOAQUIN.nombre)
})
