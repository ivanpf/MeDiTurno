import { test, expect } from '@playwright/test'
import { ANA } from '../../utilidades/datos-seed.js'

// CU-10 Consulta del historial de reservas (en «Mis reservas», debajo de las próximas). Caso de uso NO crítico (solo
// lectura de datos propios): sus filtros y su orden se prueban por API. Acá va una prueba de humo.

test('CP-10-01 (humo): el historial muestra las reservas de la paciente en todos sus estados, con su detalle', async ({ page }) => {
  await page.goto('/paciente/reservas')

  const historial = page.getByRole('table', { name: 'Historial completo de reservas' })
  await expect(page.getByText(/^\d+ reservas encontradas\.$/)).toBeVisible()
  // Del seed: Ana tiene reservas en los seis estados.
  for (const estado of ['Vigente', 'Orden emitida', 'Atendida', 'Cancelada', 'Anulada', 'Vencida']) {
    await expect(historial).toContainText(estado)
  }

  await historial.getByRole('row').filter({ hasText: 'Atendida' }).first().getByRole('button', { name: 'Ver detalle' }).click()
  const detalle = page.getByRole('article', { name: 'Detalle de la reserva' })
  await expect(detalle).toContainText(`${ANA.nombre} · CI ${ANA.cedula}`)
  await expect(detalle).toContainText('Atendida')
  await expect(page.getByRole('alert')).toHaveCount(0)
})
