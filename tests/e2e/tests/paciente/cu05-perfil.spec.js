import { test, expect } from '@playwright/test'
import { PERFIL_DE_ANA } from '../../utilidades/datos-seed.js'

// CU-5 Consulta y edición del perfil, del lado del paciente (el del médico está en tests/medico/cu05-perfil.spec.js).
// Caso de uso NO crítico: sus reglas se prueban por API. Acá va una prueba de humo que comprueba que al paciente le llegan
// sus datos y que «Descartar» no guarda nada. No cambiar el correo en E2E: se cambia también en Firebase.

// Valor (<dd>) de un dato fijo del perfil por su título (<dt>).
const dato = (page, titulo) => page.locator('dl > div')
  .filter({ has: page.getByRole('term').getByText(titulo, { exact: true }) })
  .getByRole('definition')

test('CP-05-02 (humo): el perfil del paciente muestra sus datos y su domicilio, y «Descartar» no guarda los cambios', async ({ page }) => {
  await page.goto('/perfil')
  await expect(page.getByRole('heading', { name: 'Mi perfil', level: 1 })).toBeVisible()

  await test.step('datos fijos y de contacto', async () => {
    await expect(dato(page, 'Nombre')).toHaveText(PERFIL_DE_ANA.nombre)
    await expect(dato(page, 'Cédula')).toHaveText(PERFIL_DE_ANA.cedula)
    await expect(dato(page, 'Perfil')).toHaveText('Paciente')
    await expect(dato(page, 'Fecha de nacimiento')).toHaveText(PERFIL_DE_ANA.nacimiento)
    await expect(page.getByLabel('Correo electrónico')).toHaveValue(PERFIL_DE_ANA.correo)
    await expect(page.getByLabel('Teléfono (opcional)')).toHaveValue(PERFIL_DE_ANA.telefono)
    // El domicilio es solo del paciente (el médico no lo tiene).
    await expect(page.getByLabel('Domicilio (opcional)')).toHaveValue(PERFIL_DE_ANA.domicilio)
  })

  await test.step('«Descartar» vuelve a los datos guardados sin enviar nada', async () => {
    let guardo = false
    page.on('request', (pedido) => { if (pedido.method() === 'PUT' && pedido.url().includes('/api/')) guardo = true })

    await page.getByLabel('Teléfono (opcional)').fill('099 999 999')
    await page.getByLabel('Domicilio (opcional)').fill('Otra dirección 1')
    await page.getByRole('button', { name: 'Descartar' }).click()

    await expect(page.getByLabel('Teléfono (opcional)')).toHaveValue(PERFIL_DE_ANA.telefono)
    await expect(page.getByLabel('Domicilio (opcional)')).toHaveValue(PERFIL_DE_ANA.domicilio)
    expect(guardo).toBe(false)
  })
})
