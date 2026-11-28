import { test, expect } from '@playwright/test'
import { ANA } from '../../utilidades/datos-seed.js'

// CU-27 Búsqueda y listado de pases médicos emitidos. Caso de uso NO crítico (solo lectura): sus filtros se prueban por
// API. Acá va solo una prueba de humo de la pantalla.

test('CP-27-01 (humo): la pantalla abre y lista los pases emitidos por la médica, con su estado', async ({ page }) => {
  await page.goto('/medico/pases')

  await expect(page.getByRole('heading', { name: 'Pases emitidos', level: 1 })).toBeVisible()
  // Del seed: la Dra. Gómez derivó a Ana a Dermatología y ese pase ya venció.
  const filas = page.getByRole('table').getByRole('row').filter({ has: page.getByRole('cell') })
  await expect(filas.filter({ hasText: ANA.nombre }).filter({ hasText: 'Dermatología' }).filter({ hasText: 'Vencido' })).not.toHaveCount(0)
  await expect(page.getByRole('alert')).toHaveCount(0)
})
