# MeDiTurno · App mobile

Aplicación móvil de MeDiTurno, hecha con [Expo](https://expo.dev) y React Native.
Es una de las tres partes del proyecto, junto con `backend/` (Java) y `frontend/` (Vite).

Para el flujo de trabajo con Git, ramas y Pull Requests, ver
[GUIA_TRABAJO_EQUIPO.md](../GUIA_TRABAJO_EQUIPO.md) en la raíz del repositorio.

## Requisitos

- Node.js (versión LTS) con npm.
- El backend levantado con Podman, si se quiere probar la conexión.
- Para ver la app, una de estas opciones:
  - emulador de Android (Android Studio),
  - la app **Expo Go** en un celular físico,
  - o el navegador, con `npm run web`.

## Instalación

Las dependencias no se suben al repositorio: cada integrante las descarga en su
máquina. Desde la carpeta `mobile/`:

```bash
npm install
```

Esto lee `package.json` y crea la carpeta `node_modules/`. Se corre una sola vez,
y de nuevo cada vez que alguien agrega una dependencia nueva.

## Levantar la app

```bash
npx expo start
```

Queda un servidor corriendo que muestra un código QR y un menú de opciones. Desde
ahí se abre la app en el emulador, en Expo Go o en el navegador.

Atajos equivalentes definidos en `package.json`:

| Comando | Qué hace |
|---|---|
| `npm start` | Igual que `npx expo start` |
| `npm run android` | Abre directamente en el emulador de Android |
| `npm run web` | Abre directamente en el navegador |
| `npm run lint` | Revisa el estilo del código |

## Conexión con el backend

La pantalla principal consulta el backend para mostrar si responde. La dirección
que usa **no es siempre la misma**, porque depende de dónde se esté ejecutando la
app. Esa lógica está en [src/lib/api.ts](src/lib/api.ts):

| Dónde corre la app | Dirección que usa |
|---|---|
| Emulador de Android | `http://10.0.2.2:8080` |
| Navegador o simulador de iOS | `http://localhost:8080` |
| Celular físico | La que se defina en `.env` |

El emulador de Android es una máquina virtual con su propia red: si pidiera
`localhost` buscaría un servidor dentro del emulador, no en la PC. Android reserva
la dirección `10.0.2.2` como alias de la máquina anfitriona.

Para probar en un **celular físico**, la PC y el teléfono tienen que estar en la
misma red WiFi. Hay que crear un archivo `.env` dentro de `mobile/` con la IP de la
PC (se obtiene con `ipconfig`):

```bash
EXPO_PUBLIC_API_URL=http://192.168.0.15:8080
```

Después de editar el `.env` hay que reiniciar el servidor de Expo, porque esa
variable se reemplaza al arrancar. El archivo `.env` no se sube al repositorio.

### Levantar el backend

Desde la raíz del proyecto, no desde `mobile/`:

```bash
podman compose up -d --build backend
```

Queda respondiendo en `http://localhost:8080`. Si el backend está apagado, la app
igual abre, pero la pantalla muestra el error de conexión a los 5 segundos.

## Estructura del proyecto

```text
mobile/
├── src/
│   ├── app/          Pantallas. Cada archivo es una ruta (expo-router)
│   │   ├── _layout.tsx   Estructura común a todas las pantallas
│   │   ├── index.tsx     Pantalla inicial
│   │   └── explore.tsx
│   ├── components/   Piezas de interfaz reutilizables
│   ├── constants/    Colores, espaciados y tipografías (theme.ts)
│   ├── hooks/        Lógica reutilizable de React
│   └── lib/          Código auxiliar, como la conexión al backend (api.ts)
├── assets/           Íconos e imágenes
├── app.json          Configuración de la app (nombre, ícono, plugins)
└── package.json      Dependencias y scripts
```

Las rutas se definen por archivo: `src/app/explore.tsx` es la pantalla
`/explore`. No hay que registrar nada en ningún lado, alcanza con crear el archivo.

## Cosas a tener en cuenta

- **No correr `npm run reset-project`.** Ese comando viene de la plantilla de Expo
  y mueve todo el código a `app-example/` para dejar el proyecto en blanco. Borraría
  lo que está hecho.
- `node_modules/`, `.env` y `.expo/` no se suben al repositorio: están en
  [.gitignore](.gitignore). Sí se suben `package.json` y `package-lock.json`, que son
  los que permiten reconstruir las dependencias.
- Las carpetas `android/` e `ios/` tampoco se suben: Expo las genera cuando hacen
  falta.
