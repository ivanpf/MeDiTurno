import { test, expect } from '@playwright/test'

// Prueba de humo: la web responde y muestra la pantalla de inicio de sesión a quien no tiene sesión.
test('sin sesión, la web lleva a la pantalla de inicio de sesión', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveURL(/\/login$/)
  await expect(page.getByRole('heading', { name: 'Iniciar sesión' })).toBeVisible()
  await expect(page.getByLabel('Correo electrónico')).toBeVisible()
})
