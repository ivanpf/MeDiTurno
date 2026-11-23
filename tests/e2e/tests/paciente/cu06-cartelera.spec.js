import { test, expect } from '@playwright/test'
import { ANUNCIOS } from '../../utilidades/datos-seed.js'

// CU-6 Consulta de la cartelera de novedades (la ven todos los roles; se prueba con la sesión del paciente).
// Caso de uso NO crítico (solo lectura): prueba de humo. La vigencia exacta de cada fecha se prueba por API.

test('CP-06-01 (humo): la cartelera muestra solo los anuncios vigentes, con sus imágenes', async ({ page }) => {
  await page.goto('/cartelera')
  await expect(page.getByRole('heading', { name: 'Cartelera de novedades', level: 1 })).toBeVisible()

  const anuncios = page.getByRole('article')
  await expect(anuncios).toHaveCount(ANUNCIOS.vigentes.length)
  for (const titulo of ANUNCIOS.vigentes) {
    await expect(page.getByRole('heading', { name: titulo, level: 2 })).toBeVisible()
  }
  // Clases inválidas: programado (empieza en unos días), vencido y eliminado.
  for (const titulo of ANUNCIOS.noVisibles) {
    await expect(page.getByRole('heading', { name: titulo })).toHaveCount(0)
  }
  // Las imágenes se piden aparte, con la sesión: que lleguen y tengan texto alternativo.
  for (const titulo of ANUNCIOS.conImagen) {
    const anuncio = anuncios.filter({ has: page.getByRole('heading', { name: titulo }) })
    await expect(anuncio.getByRole('img')).toBeVisible()
    await expect(anuncio.getByRole('img')).toHaveAttribute('alt', /\S/)
  }
  await expect(page.getByRole('alert')).toHaveCount(0)
})
