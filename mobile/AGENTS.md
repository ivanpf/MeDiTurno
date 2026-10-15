# MeDiTurno Mobile — guía para agentes

App del paciente para **MeDiTurno** (React Native + Expo Router, JavaScript). Reutiliza el backend
Spring Boot y el diseño de la web. Cómo correr el proyecto: [`README.md`](README.md).

## Stack

- Expo SDK 57 + `expo-router` (rutas por archivos), React Native 0.86, React 19.
- Estilos: `StyleSheet` de React Native + paleta propia en `shared/src/theme/colors.js`
  (**sin NativeWind ni Tailwind**).
- Auth: Firebase (email/contraseña) en `shared/src/auth`.
- API: cliente `fetch` con token en `shared/src/api`, contra el backend en `:8080`.
- Safe area con `react-native-safe-area-context`.

## Estructura

```
app/                 # rutas de expo-router (cada archivo es una pantalla)
  _layout.js         # providers + SafeAreaProvider
  (auth)/            # login, registro, recuperar-acceso
  (app)/             # pantallas con sesión (paciente); _layout.js tiene header + menú
src/                 # código propio de la app (Logo, PantallaAcceso, providers)
shared/              # paquete compartido: api, auth, hooks, components, theme, navigation, utils
assets/              # logo e íconos (los mismos de la web)
```

Las pantallas y el menú del rol se definen en `shared/src/navigation/rutas.js` (equivalente a
`frontend/src/routes.jsx`); el layout con header y menú está en `app/(app)/_layout.js`.

## Comandos

```bash
npx expo install <paquete>   # SIEMPRE esto en vez de npm install (resuelve versiones del SDK)
npx expo start               # Metro
npx expo run:android         # build de desarrollo + instalar (JDK 17 + Android SDK)
npx expo-doctor              # diagnostica dependencias y configuración
```

## Reglas

- Las rutas van en `app/`; el resto del código (componentes, hooks, utils) va en `src/` o `shared/`.
- `android/` e `ios/` son generados (CNG) y están en `.gitignore`: no editarlos a mano, se
  configuran en `app.json`.
- Para librerías con código nativo hace falta un **development build** (`npx expo run:android`),
  no Expo Go.
- Expo cambia en cada SDK: verificá la versión de `expo` en `package.json` y consultá
  https://docs.expo.dev/versions/v<major>.0.0/ antes de usar una API.
- Correr `npx expo-doctor` antes de dar una tarea por terminada.
