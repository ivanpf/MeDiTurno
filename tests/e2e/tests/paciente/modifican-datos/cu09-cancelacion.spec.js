import { test, expect } from '@playwright/test'
import { RESERVAS_DE_ANA } from '../../../utilidades/datos-seed.js'
import { comoFecha, hoyEnMontevideo, primerDiaDeLaSemana, sumarDias } from '../../../utilidades/fechas.js'
import { botonDelNumero, botonDelTurno, verTurnosDe } from '../../../utilidades/reservar.js'

// CU-9 Cancelación de una reserva. Caso de uso crítico (plazo de 48 horas y liberación del número): nivel E2E.
// MODIFICA DATOS: CP-09-04 cancela la reserva de Cardiología de Ana. Para repetirlo hay que recargar el seed.
// Las pruebas corren en orden: las primeras no guardan nada.
// El límite exacto (48 horas justas se permiten; un minuto menos no) necesita fijar el reloj: lo cubre el backend
// (CancelacionReservaIntegracionTest). Acá se prueban las clases: menos de 48 horas (mañana) y 3 días o más.
test.describe.configure({ mode: 'serial' })

const { hoy, manana, cancelable } = RESERVAS_DE_ANA
const fechaCancelable = () => primerDiaDeLaSemana(sumarDias(hoyEnMontevideo(), 3), cancelable.dia)

// Fila de una reserva en «Próximas reservas», por su fecha (AAAA-MM-DD), especialidad y horario.
const filaDeReserva = (page, fecha, { especialidad, horario }) =>
  page.getByRole('table', { name: 'Próximas reservas y cancelaciones registradas' }).getByRole('row')
    .filter({ hasText: comoFecha(fecha) }).filter({ hasText: especialidad }).filter({ hasText: horario })

test.beforeEach(async ({ page }) => {
  await page.goto('/paciente/reservas')
  await expect(page.getByRole('heading', { name: 'Mis reservas', level: 1 })).toBeVisible()
})

test('CP-09-01: una reserva a menos de 48 horas del inicio del turno no se puede cancelar', async ({ page }) => {
  const fila = filaDeReserva(page, sumarDias(hoyEnMontevideo(), 1), manana)

  await expect(fila).toContainText('Vigente')
  await expect(fila).toContainText('La reserva no puede cancelarse: deben faltar al menos 48 horas para el inicio del turno.')
  await expect(fila.getByRole('button', { name: 'Cancelar reserva' })).toHaveCount(0)
})

test('CP-09-02: una reserva que no está vigente (con la orden emitida) no se puede cancelar', async ({ page }) => {
  const fila = filaDeReserva(page, hoyEnMontevideo(), hoy)

  await expect(fila).toContainText('Orden emitida')
  await expect(fila).toContainText('La reserva no puede cancelarse porque ya no está vigente.')
  await expect(fila.getByRole('button', { name: 'Cancelar reserva' })).toHaveCount(0)
})

test('CP-09-03: cerrar la confirmación sin confirmar no cancela la reserva', async ({ page }) => {
  const fila = filaDeReserva(page, fechaCancelable(), cancelable)
  await fila.getByRole('button', { name: 'Cancelar reserva' }).click()

  const confirmacion = page.getByRole('dialog')
  await expect(confirmacion).toContainText(`número ${cancelable.numero}`)
  await expect(confirmacion).toContainText(cancelable.especialidad)
  await expect(confirmacion).toContainText(cancelable.medico)
  await confirmacion.getByRole('button', { name: 'Cancelar', exact: true }).click()

  await expect(confirmacion).toHaveCount(0)
  await expect(fila).toContainText('Vigente')
  await expect(fila.getByRole('button', { name: 'Cancelar reserva' })).toBeVisible()
})

test('CP-09-04: la paciente cancela una reserva a 3 días o más y el número queda libre para otra reserva', async ({ page }) => {
  const fila = filaDeReserva(page, fechaCancelable(), cancelable)
  await fila.getByRole('button', { name: 'Cancelar reserva' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Confirmar cancelación' }).click()

  await expect(page.getByRole('status').filter({ hasText: 'Reserva cancelada correctamente.' }))
    .toHaveText('Reserva cancelada correctamente. El número quedó disponible para una nueva reserva.')
  // La reserva se conserva, cancelada y con la fecha de cancelación.
  await expect(fila).toContainText('Cancelada')
  await expect(fila).toContainText(/Cancelada el \d{2}\/\d{2}\/\d{4} a las \d{2}:\d{2}/)
  await expect(fila.getByRole('button', { name: 'Cancelar reserva' })).toHaveCount(0)

  // En «Reservar consulta», ese número del turno vuelve a estar libre.
  await verTurnosDe(page, cancelable)
  await botonDelTurno(page, fechaCancelable(), cancelable.horario).click()
  await expect(botonDelNumero(page, cancelable.numero)).toBeEnabled()
  await expect(botonDelNumero(page, cancelable.numero)).not.toContainText('Ocupado')
})
