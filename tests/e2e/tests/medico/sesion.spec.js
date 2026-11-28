import { test, expect } from '@playwright/test'

// Prueba de humo: la sesión guardada del médico funciona y la web muestra su menú.
// Si falla, el problema está en la configuración (sesiones/), no en un caso de uso.
test('con la sesión del médico, la web abre su pantalla de inicio y su menú', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/medico\/pacientes$/)
  await expect(page.getByRole('heading', { name: 'Mis pacientes', level: 1 })).toBeVisible()
  await expect(page.getByRole('link', { name: 'Historial de atenciones' })).toBeVisible()
})
