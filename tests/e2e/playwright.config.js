import { existsSync } from 'node:fs'
import { defineConfig, devices } from '@playwright/test'
import { ROLES, archivoSesion } from './sesiones/roles.js'

// Lee tests/e2e/.env si existe (correos, contraseñas y dirección de la web). Sin dependencias: lo hace Node.
try {
  process.loadEnvFile(new URL('./.env', import.meta.url))
} catch {
  // sin .env se usan las variables del sistema y los valores por defecto
}

const navegador = {
  ...devices['Desktop Chrome'],
  locale: 'es-UY',
  timezoneId: 'America/Montevideo', // la misma zona horaria que usa la aplicación
}

// Roles que tienen carpeta de tests (tests/<rol>/). Un rol sin carpeta no inicia sesión ni crea proyectos.
const roles = Object.keys(ROLES).filter((rol) => existsSync(new URL(`./tests/${rol}`, import.meta.url)))

// Orden en que corren las pruebas que modifican datos: un rol por vez y solo después de TODAS las que consultan, de todos
// los roles (el seed es uno solo). El paciente va antes que el médico: CU-23/24 le registran una consulta a Ana y le emiten
// un pase a Dermatología, y las pruebas del paciente esperan su historia clínica y sus pases como quedaron al cargar el seed.
const modificanDatos = ['paciente', 'medico', 'funcionario', 'admin'].filter((rol) => roles.includes(rol))

export default defineConfig({
  // La aplicación tiene que estar levantada antes de correr las pruebas (ver tests/README.md).
  use: {
    baseURL: process.env.E2E_URL ?? 'http://localhost:3000',
    trace: 'retain-on-failure', // al fallar, guarda la traza paso a paso (npx playwright show-trace)
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  fullyParallel: true,
  forbidOnly: !!process.env.CI, // en CI falla si quedó un test.only olvidado
  retries: process.env.CI ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'reporte' }]],
  outputDir: 'resultados',

  projects: [
    ...roles.flatMap((rol) => [
      // 1. Inicia sesión con ese rol y la guarda. Es un proyecto por rol: si falla el inicio de sesión de uno,
      //    no frena las pruebas de consulta de los demás.
      {
        name: `sesion-${rol}`,
        testDir: './sesiones',
        testMatch: /.*\.setup\.js/,
        grep: new RegExp(`sesión de ${rol}$`),
        use: navegador,
      },
      // 2. Corre tests/<rol>/ con la sesión de ese rol ya iniciada: las pruebas que solo consultan, en paralelo.
      {
        name: rol,
        testDir: `./tests/${rol}`,
        testIgnore: '**/modifican-datos/**',
        dependencies: [`sesion-${rol}`],
        use: { ...navegador, storageState: archivoSesion(rol) },
      },
    ]),
    // 3. Corre tests/<rol>/modifican-datos/ DESPUÉS de las de consulta de todos los roles, un rol por vez (en el orden de
    //    modificanDatos), de a un archivo y en orden alfabético (por eso los archivos empiezan con el número del caso de
    //    uso). Si falla una prueba anterior de la cadena, las siguientes no corren; para correrlas igual: --no-deps.
    ...modificanDatos.map((rol, i) => ({
      name: `${rol}-modifican-datos`,
      testDir: `./tests/${rol}/modifican-datos`,
      dependencies: i === 0 ? roles : [`${modificanDatos[i - 1]}-modifican-datos`],
      fullyParallel: false,
      workers: 1,
      use: { ...navegador, storageState: archivoSesion(rol) },
    })),

    // 4. Pantallas sin sesión (inicio de sesión, registro, recuperar acceso).
    { name: 'publico', testDir: './tests/publico', use: navegador },
  ],
})
