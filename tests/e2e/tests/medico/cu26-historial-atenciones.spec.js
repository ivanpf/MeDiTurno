import { test, expect } from '@playwright/test'

// CU-26 Consulta del historial de atenciones propias. Caso de uso NO crítico (solo lectura de datos propios): sus reglas
// se prueban por API. Acá va solo una prueba de humo de la pantalla.

test('CP-26-01 (humo): la pantalla abre y lista las atenciones registradas por la médica', async ({ page }) => {
  await page.goto('/medico/atenciones')

  await expect(page.getByRole('heading', { name: 'Historial de atenciones', level: 1 })).toBeVisible()
  await expect(page.getByText(/^\d+ atenciones encontradas\.$/)).toBeVisible()

  const filas = page.getByRole('table', { name: 'Atenciones propias' }).getByRole('row').filter({ has: page.getByRole('cell') })
  await expect(filas.first()).toContainText('Registrada')
  await expect(page.getByRole('alert')).toHaveCount(0)
})
