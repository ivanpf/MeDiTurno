import { test, expect } from '@playwright/test'
import { PASE_VENCIDO, PASE_VIGENTE, RESERVAS_DE_ANA, TURNO_SIN_CUPOS } from '../../../utilidades/datos-seed.js'
import { hoyEnMontevideo, primerDiaDeLaSemana, sumarDias } from '../../../utilidades/fechas.js'
import { botonDelNumero, botonDelTurno, numerosLibres, turnosConCupos, verTurnosDe } from '../../../utilidades/reservar.js'

// CU-8 Reserva de una orden de consulta. Caso de uso crítico (cupos, números y pases): nivel E2E.
// MODIFICA DATOS: CP-08-04 reserva un número de Traumatología. Para repetirlo hay que recargar el seed.
// Las pruebas corren en orden: las primeras no guardan nada. No reservan en el turno de hoy de la Dra. Gómez (lo usan
// las pruebas del médico).
// No se prueba en E2E: que otro paciente ocupe el número mientras se confirma (409; solo Ana tiene cuenta) ni el límite
// de las ocho semanas (se ve mejor por API, con las fechas de cada turno).
test.describe.configure({ mode: 'serial' })

const { cancelable } = RESERVAS_DE_ANA

test('CP-08-01: una especialidad que exige pase, con el pase vencido, no ofrece turnos', async ({ page }) => {
  await page.goto('/paciente/reservar')
  await page.getByLabel('Especialidad').selectOption({ label: PASE_VENCIDO.especialidad })

  await expect(page.getByRole('alert')).toContainText('No posee un pase o habilitación de reconsulta vigente para esta especialidad')
  await expect(page.getByLabel('Médico')).toBeDisabled()
  await expect(page.getByRole('button', { name: 'Ver turnos' })).toBeDisabled()
})

test('CP-08-02: un turno sin cupos y los números ocupados no se pueden elegir', async ({ page }) => {
  await test.step('turno sin cupos: no se ofrece', async () => {
    await verTurnosDe(page, TURNO_SIN_CUPOS)
    const fecha = sumarDias(hoyEnMontevideo(), TURNO_SIN_CUPOS.enDias)
    // El backend no devuelve los turnos sin números libres. El otro turno del médico ese mismo día sí aparece.
    await expect(botonDelTurno(page, fecha, TURNO_SIN_CUPOS.otroHorarioDelDia)).toBeVisible()
    await expect(botonDelTurno(page, fecha, TURNO_SIN_CUPOS.horario)).toHaveCount(0)
  })

  await test.step('números ocupados: deshabilitados y marcados', async () => {
    await verTurnosDe(page, cancelable)
    const fecha = primerDiaDeLaSemana(sumarDias(hoyEnMontevideo(), 3), cancelable.dia)
    await botonDelTurno(page, fecha, cancelable.horario).click()
    await expect(page.getByRole('heading', { name: '3. Elija su número' })).toBeVisible()
    // Del seed: los números 1 (Luis), 5 (Ana) y 7 (María) de ese turno ya están reservados.
    for (const numero of [1, cancelable.numero, 7]) {
      await expect(botonDelNumero(page, numero)).toBeDisabled()
      await expect(botonDelNumero(page, numero)).toContainText('Ocupado')
    }
  })
})

test('CP-08-03: elegir un número y cancelar no reserva', async ({ page }) => {
  await verTurnosDe(page, PASE_VIGENTE)
  await turnosConCupos(page).last().click()
  await numerosLibres(page).first().click()
  await expect(page.getByRole('status').filter({ hasText: /^Número \d+ · / })).toBeVisible()

  let reservo = false
  page.on('request', (pedido) => { if (pedido.method() === 'POST' && pedido.url().includes('/api/')) reservo = true })
  await page.getByRole('button', { name: 'Cancelar', exact: true }).click()

  await expect(page.getByRole('heading', { name: '3. Elija su número' })).toHaveCount(0)
  await expect(page.getByText('Reserva realizada correctamente.')).toHaveCount(0)
  expect(reservo).toBe(false)
})

test('CP-08-04: la paciente reserva un número libre en una especialidad con pase, usando su pase vigente', async ({ page }) => {
  await verTurnosDe(page, PASE_VIGENTE)
  // El último turno ofrecido (el más lejano): no se cruza con las reservas que ya tiene Ana.
  await turnosConCupos(page).last().click()
  const libre = numerosLibres(page).first()
  const numero = Number((await libre.textContent()).match(/N\.º (\d+)/)[1])
  await libre.click()

  const respuesta = page.waitForResponse((r) => /\/api\/reservas$/.test(r.url()) && r.request().method() === 'POST')
  await page.getByRole('button', { name: 'Reservar', exact: true }).click()
  expect((await respuesta).status()).toBe(201)
  const reserva = await (await respuesta).json()
  expect(reserva.numero).toBe(numero)

  await expect(page.getByRole('status').filter({ hasText: 'Reserva realizada correctamente.' }))
    .toContainText(`Identificador: ${reserva.idReserva} · Número: ${numero} · Hora estimada: ~`)

  // La reserva nueva aparece en «Mis reservas», vigente.
  await page.goto('/paciente/reservas')
  const fila = page.getByRole('table', { name: 'Próximas reservas y cancelaciones registradas' })
    .getByRole('row').filter({ hasText: `#${reserva.idReserva}` })
  await expect(fila).toContainText(`Número ${numero}`)
  await expect(fila).toContainText(PASE_VIGENTE.especialidad)
  await expect(fila).toContainText('Vigente')
})
