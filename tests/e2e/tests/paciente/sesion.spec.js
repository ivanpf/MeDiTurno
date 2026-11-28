import { test, expect } from '@playwright/test'

// Prueba de humo: la sesión guardada del paciente funciona y la web muestra su menú.
// Si falla, el problema está en la configuración (sesiones/), no en un caso de uso.
test('con la sesión del paciente, la web abre su pantalla de inicio y su menú', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/paciente\/reservar$/)
  await expect(page.getByRole('heading', { name: 'Reservar consulta', level: 1 })).toBeVisible()
  for (const opcion of ['Mis reservas', 'Historia clínica', 'Cartelera de novedades']) {
    await expect(page.getByRole('link', { name: opcion })).toBeVisible()
  }
})
