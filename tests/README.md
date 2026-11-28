# Pruebas del sistema

Pruebas del equipo de testing sobre **el sistema completo** (web, backend y base de datos). Las pruebas unitarias y de integración de cada parte siguen junto a su código: `backend/src/test/java` (`./mvnw test`) y `mobile/tests`.

```
tests/
├── e2e/      Playwright: la web usada como lo haría una persona
├── api/      Postman: colecciones y entornos (pendiente)
└── carga/    k6 (pendiente)
```

## Pruebas de punta a punta (`e2e/`)

### Estructura

```
e2e/
├── playwright.config.js          configuración: dirección de la web, navegador, proyectos por rol y su orden, reportes
├── package.json                  dependencia (Playwright) y comandos (npm test, npm run reporte, ...)
├── .env.example                  copiar como .env y completar (el .env no se sube)
├── sesiones/
│   ├── roles.js                  roles, sus variables de entorno y dónde se guarda cada sesión
│   ├── iniciar-sesion.setup.js   inicia sesión una vez por rol, por la pantalla real, y la guarda
│   └── .guardadas/               sesiones guardadas (ignorado por Git: contiene tokens)
├── utilidades/
│   ├── datos-seed.js             datos del seed que esperan las pruebas (personas, turnos, reservas, anuncios)
│   ├── fechas.js                 fechas en la zona de Montevideo (hoy, sumar días, formatos de la pantalla)
│   ├── mis-pacientes.js          pasos compartidos de «Mis pacientes» (médico)
│   └── reservar.js               pasos compartidos de «Reservar consulta» (paciente)
└── tests/
    ├── publico/                  pantallas sin sesión
    ├── medico/                   con la sesión de la Dra. Laura Gómez: pruebas que solo consultan, en paralelo
    │   └── modifican-datos/      pruebas que cambian el seed: corren después, de a una y en orden alfabético
    └── paciente/                 con la sesión de Ana López: pruebas que solo consultan, en paralelo
        └── modifican-datos/      pruebas que cambian el seed: corren después, de a una y en orden alfabético
```

`reporte/` y `resultados/` se crean al correr las pruebas (reporte HTML y archivos de las que fallan) y no se suben.

Cada carpeta de `tests/` es un **proyecto** de Playwright con la sesión de ese rol (o sin sesión, `publico`). Antes de cada uno corre su proyecto `sesion-<rol>`, que inicia sesión y la guarda; así cada prueba arranca ya autenticada.

**Orden (el seed es uno solo).** Primero corren las pruebas que solo consultan, de todos los roles. Después, las de `modifican-datos/`, un rol por vez: primero las del paciente y después las del médico (las del médico le agregan a Ana una consulta y un pase, que cambiarían lo que ven las del paciente). Si falla una prueba de consulta o un inicio de sesión, las de `modifican-datos/` no corren; para correr igual las de un rol: `npm test -- --project=medico-modifican-datos --no-deps` (usa la sesión ya guardada).

### Pruebas por caso de uso

Los casos de uso **críticos** tienen pruebas E2E de sus reglas y flujos alternativos; los **no críticos**, una prueba de humo (la pantalla abre y muestra los datos correctos). Lo que la pantalla no deja provocar (403, 404, 409, límites de largo, plazos exactos) se prueba por API. Los IDs son los del Documento de Verificación.

**Médico** (`e2e/tests/medico/`)

| CU | Tipo | Casos de prueba | Archivo |
|---|---|---|---|
| CU-21 Pacientes por turno | E2E | CP-21-01 a CP-21-03 | `cu21-pacientes-por-turno.spec.js` |
| CU-22 Historia clínica durante la atención | E2E | CP-22-01 a CP-22-04 | `cu22-historia-clinica-atencion.spec.js` |
| CU-23 Registro de una consulta | E2E (modifica datos) | CP-23-01 a CP-23-04 | `modifican-datos/cu23-registro-consulta.spec.js` |
| CU-24 Emisión de un pase | E2E (modifica datos) | CP-24-01 a CP-24-03 | `modifican-datos/cu24-25-pase-y-reconsulta.spec.js` |
| CU-25 Habilitación de una reconsulta | E2E (modifica datos) | CP-25-01 | `modifican-datos/cu24-25-pase-y-reconsulta.spec.js` |
| CU-26 Historial de atenciones | Humo | CP-26-01 | `cu26-historial-atenciones.spec.js` |
| CU-27 Pases emitidos | Humo | CP-27-01 | `cu27-pases-emitidos.spec.js` |
| CU-5 Perfil (médico) | Humo | CP-05-01 | `cu05-perfil.spec.js` |

**Paciente** (`e2e/tests/paciente/`)

| CU | Tipo | Casos de prueba | Archivo |
|---|---|---|---|
| CU-8 Reserva de una orden de consulta | E2E (modifica datos) | CP-08-01 a CP-08-04 | `modifican-datos/cu08-reserva.spec.js` |
| CU-9 Cancelación de una reserva | E2E (modifica datos) | CP-09-01 a CP-09-04 | `modifican-datos/cu09-cancelacion.spec.js` |
| CU-11 Historia clínica propia | E2E | CP-11-01 a CP-11-04 | `cu11-historia-clinica.spec.js` |
| CU-10 Historial de reservas | Humo | CP-10-01 | `cu10-historial-reservas.spec.js` |
| CU-6 Cartelera de novedades | Humo | CP-06-01 | `cu06-cartelera.spec.js` |
| CU-5 Perfil (paciente) | Humo | CP-05-02 | `cu05-perfil.spec.js` |

**Sin caso de uso propio:** `medico/sesion.spec.js` y `paciente/sesion.spec.js` comprueban que la sesión guardada de cada rol funciona (si fallan, el problema está en `sesiones/`), y `publico/acceso.spec.js`, que sin sesión la web lleva al inicio de sesión.

### Primera vez

```bash
cd tests/e2e
npm install
npm run instalar-navegador
```

Después, copiar `.env.example` como `.env` y completar las contraseñas de los usuarios de prueba (están en `CLAUDE.md`, «Usuarios de prueba»).

### Antes de cada corrida: la aplicación y los datos

Las pruebas usan la aplicación levantada con Podman y los **datos de prueba (seed)**. Las fechas del seed son relativas al día en que se cargó (por ejemplo, «el turno de hoy de 18 a 21 de la Dra. Gómez»), así que **hay que recargarlo el mismo día de la corrida**:

```bash
podman compose down -v
podman compose up -d --build
```

`down -v` borra los datos locales. Esperar a que el backend termine de arrancar (http://localhost:8080/api/health) antes de correr las pruebas.

### Correr

```bash
npm test                          # todas, sin ver el navegador (deja el seed modificado)
npm test -- --project=medico      # solo las del médico que consultan (lo mismo con --project=paciente)
npm test -- --project=paciente-modifican-datos # antes corren todas las que consultan
npm test -- --project=medico-modifican-datos   # antes corren todas las que consultan y las del paciente que modifican
npm test -- tests/medico/cu21     # solo un archivo
npm run test:ui                   # modo interactivo: elegir, ver paso a paso, volver a correr
npm run test:visible              # con el navegador a la vista
npm run reporte                   # abre el reporte HTML de la última corrida
```

Cuando una prueba falla, el reporte trae captura, video y la **traza** (cada paso con el estado de la página).

### Convenciones

- **Un archivo por caso de uso**, dentro de la carpeta del rol: `tests/medico/cu21-pacientes-por-turno.spec.js`.
- **Un `test` por caso de prueba**, con el mismo ID del Documento de Verificación al principio del nombre: `test('CP-21-01: ...')`.
- **Buscar los elementos como los ve una persona:** `getByRole`, `getByLabel`, `getByText`. No usar clases CSS (los CSS Modules cambian los nombres) ni XPath.
- **No usar pausas** (`waitForTimeout`): `expect(...)` ya espera solo hasta que se cumpla la condición.
- **Datos:** los tests que solo consultan se pueden repetir. Los que modifican datos van en `tests/<rol>/modifican-datos/` y dejan la base cambiada: para repetirlos, recargar el seed.
- **Si una corrida falla a la mitad, recargar el seed antes de repetirla.** Dentro de `modifican-datos/`, un archivo que falla no frena a los demás archivos del mismo rol, así que la base puede quedar cambiada en parte.
- **Las pruebas de un rol no modifican datos que usan las de otro** (por ejemplo, las del paciente no reservan en el turno de hoy de 18 a 21 de la Dra. Gómez, que usan las del médico).
- **Los datos del seed que usan las pruebas** están en `utilidades/datos-seed.js`: si el seed cambia, se actualizan ahí y no en cada test.
- **Horario:** correr el mismo día en que se cargó el seed (después de las 0:00 hay que recargarlo) y antes de las 21:00, cuando termina el turno de la Dra. Gómez y vencen sus reservas vigentes.
- **Contraseñas:** las del `.env` tienen que coincidir con las de Firebase. Si se cambia la contraseña de un usuario de prueba (por ejemplo, al probar CU-3 o CU-4 a mano), el inicio de sesión de ese rol falla con «No fue posible iniciar sesión».
- **Cerrar sesión (CU-2) revoca todas las sesiones de esa persona en Firebase.** Un test de CU-2 tiene que iniciar su propia sesión y no usar la guardada, o invalida la de los demás tests.
