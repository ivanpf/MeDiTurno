import { test, expect } from '@playwright/test'
import { DRA_GOMEZ } from '../../utilidades/datos-seed.js'

// CU-5 Consulta y edición del perfil (con la sesión del médico). Caso de uso NO crítico: sus reglas se prueban por API.
// Acá va solo una prueba de humo, que no guarda nada. No cambiar el correo en E2E: se cambia también en Firebase y deja
// a las demás pruebas sin poder iniciar sesión.

test('CP-05-01 (humo): el perfil muestra los datos fijos y deja editar solo correo y teléfono', async ({ page }) => {
  await page.goto('/perfil')

  await expect(page.getByRole('heading', { name: 'Mi perfil', level: 1 })).toBeVisible()
  await expect(page.getByRole('definition').filter({ hasText: DRA_GOMEZ.cedula })).toBeVisible()
  await expect(page.getByRole('definition').filter({ hasText: /^Médico$/ })).toBeVisible()

  await expect(page.getByLabel('Correo electrónico')).toHaveValue(DRA_GOMEZ.correo)
  await expect(page.getByLabel('Teléfono (opcional)')).toBeEditable()
  // El domicilio es solo para pacientes.
  await expect(page.getByLabel('Domicilio (opcional)')).toHaveCount(0)
})
