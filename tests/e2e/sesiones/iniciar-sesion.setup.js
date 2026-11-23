import { test as setup, expect } from '@playwright/test'
import { ROLES, archivoSesion } from './roles.js'

// Inicia sesión una vez por rol, por la pantalla real (CU-1), y guarda la sesión para que los tests arranquen ya
// autenticados. Firebase guarda la sesión en IndexedDB y la web guarda rol y nombre en localStorage: por eso
// storageState se guarda con indexedDB: true (sin eso, la sesión no se conserva).
for (const [rol, variables] of Object.entries(ROLES)) {
  setup(`sesión de ${rol}`, async ({ page }) => {
    const correo = process.env[variables.correo]
    const clave = process.env[variables.clave]
    setup.skip(!correo || !clave, `Faltan ${variables.correo} o ${variables.clave} en tests/e2e/.env`)

    await page.goto('/login')
    await page.getByLabel('Correo electrónico').fill(correo)
    await page.getByLabel('Contraseña').fill(clave)
    await page.getByRole('button', { name: 'Ingresar' }).click()

    // Con la sesión iniciada, la web sale de /login hacia la pantalla de inicio del rol.
    await expect(page).not.toHaveURL(/\/login$/)
    await expect(page.getByRole('button', { name: 'Cerrar sesión' }).first()).toBeVisible()

    await page.context().storageState({ path: archivoSesion(rol), indexedDB: true })
  })
}
