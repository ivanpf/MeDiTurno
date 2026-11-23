import { test, expect } from '@playwright/test'
import { ANA, JOAQUIN, CONSULTAS_PREVIAS_DE_ANA, MEDICOS_DE_ANA } from '../../utilidades/datos-seed.js'
import { abrirTurnoDeHoy, filaDe } from '../../utilidades/mis-pacientes.js'

// CU-22 Consulta de la historia clínica durante la atención. Caso de uso crítico (datos clínicos): nivel E2E.
// Solo consulta. Funciona únicamente el día del turno: el seed tiene que haberse cargado hoy.

const SIN_AUTORIZACION = 'No existe una atención autorizada para consultar la historia clínica de este paciente.'

async function abrirHistoriaDe(page, paciente) {
  await abrirTurnoDeHoy(page)
  await filaDe(page, paciente).getByRole('link', { name: 'Ver historia clínica' }).click()
  await expect(page).toHaveURL(/\/medico\/historia-clinica\?idPaciente=\d+$/)
  await expect(page.getByRole('heading', { name: 'Historia clínica durante la atención', level: 1 })).toBeVisible()
}

// «dd/mm/aaaa hh:mm» → número comparable.
const comoNumero = (fechaHora) => {
  const [fecha, hora] = fechaHora.trim().split(' ')
  return Number(fecha.split('/').reverse().join('') + hora.replace(':', ''))
}

test('CP-22-01: la historia de un paciente del turno muestra sus consultas previas, también de otros médicos, y su detalle', async ({ page }) => {
  await abrirHistoriaDe(page, ANA)

  await expect(page.getByRole('heading', { name: ANA.nombre, level: 2 })).toBeVisible()
  const tabla = page.getByRole('table', { name: 'Consultas anteriores del paciente' })
  const filas = tabla.getByRole('row').filter({ has: page.getByRole('cell') })
  await expect(filas).toHaveCount(CONSULTAS_PREVIAS_DE_ANA)
  for (const medico of MEDICOS_DE_ANA) await expect(tabla).toContainText(medico)

  // Ordenadas de la más reciente a la más antigua.
  const fechas = (await filas.getByRole('cell').filter({ hasText: /^\d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/ }).allTextContents()).map(comoNumero)
  expect(fechas).toHaveLength(CONSULTAS_PREVIAS_DE_ANA)
  expect(fechas).toEqual([...fechas].sort((a, b) => b - a))

  // Detalle de la consulta más reciente.
  await filas.first().getByRole('button', { name: 'Ver detalle' }).click()
  const detalle = page.getByRole('article', { name: 'Detalle de la consulta' })
  await expect(detalle.getByRole('heading', { name: /^Consulta del \d{2}\/\d{2}\/\d{4} \d{2}:\d{2}$/ })).toBeVisible()
  for (const titulo of ['Motivo de la consulta', 'Observaciones clínicas', 'Indicaciones']) {
    await expect(detalle.getByRole('heading', { name: titulo })).toBeVisible()
  }
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('CP-22-02: un paciente sin consultas previas informa que no tiene antecedentes (flujo 5.A)', async ({ page }) => {
  await abrirHistoriaDe(page, JOAQUIN)

  await expect(page.getByText('El paciente no posee antecedentes de consultas disponibles para visualizar.')).toBeVisible()
  await expect(page.getByRole('table')).toHaveCount(0)
  await expect(page.getByRole('alert')).toHaveCount(0)
})

test('CP-22-03: con el idPaciente cambiado a mano en la dirección, se niega el acceso sin mostrar datos (flujo 2.A)', async ({ page }) => {
  // Paciente sin vínculo con los turnos de la médica (id inexistente: el backend responde igual que con uno ajeno).
  await page.goto('/medico/historia-clinica?idPaciente=999999999')

  await expect(page.getByRole('alert')).toContainText(SIN_AUTORIZACION)
  await expect(page.getByRole('table')).toHaveCount(0)
})

test('CP-22-04: un idPaciente vacío o no válido pide elegir al paciente desde «Mis pacientes»', async ({ page }) => {
  // Valores límite del identificador: vacío, texto, 0 y negativo (el primero válido es 1).
  for (const valor of ['', 'abc', '0', '-1']) {
    await test.step(`idPaciente=${valor || '(vacío)'}`, async () => {
      await page.goto(`/medico/historia-clinica?idPaciente=${valor}`)
      await expect(page.getByText('Seleccione primero un paciente de sus turnos para consultar su historia clínica.')).toBeVisible()
      await expect(page.getByRole('link', { name: 'Ir a Mis pacientes' })).toBeVisible()
    })
  }
})
