# MeDiTurno · App mobile

Aplicación móvil de MeDiTurno, hecha con [Expo](https://expo.dev) y React Native.
Es una de las tres partes del proyecto, junto con `backend/` (Spring Boot) y
`frontend/` (Vite).

Para el flujo de trabajo con Git, ramas y Pull Requests, ver
[GUIA_TRABAJO_EQUIPO.md](../GUIA_TRABAJO_EQUIPO.md) en la raíz del repositorio.

---

## 1. Qué hay que tener instalado

Estas son las versiones con las que el proyecto está funcionando. No hace falta
que coincidan exactamente, pero si algo falla, comparar contra esta tabla es el
primer paso.

| Herramienta | Versión probada | Para qué |
|---|---|---|
| Git | 2.50.1 | Clonar el repositorio |
| Node.js | 24.15.0 | Ejecutar Expo y las dependencias |
| npm | 11.12.1 | Viene incluido con Node |
| JDK | 17 (Temurin) | Compilar la app Android |
| Android Studio | Última estable | SDK, emulador y drivers |
| Podman Desktop | Última estable | Levantar el backend |

**El JDK tiene que ser la versión 17.** Con versiones más nuevas, el build de
Gradle falla.

No hace falta instalar Expo ni ninguna CLI global: se usa `npx`, que descarga lo
necesario en el momento.

---

## 2. Instalación paso a paso (Windows)

### Paso 1 — Git

```powershell
winget install Git.Git
```

Cerrar y reabrir la terminal, y verificar:

```powershell
git --version
```

### Paso 2 — Node.js

Instalar la versión LTS desde [nodejs.org](https://nodejs.org), o por terminal:

```powershell
winget install OpenJS.NodeJS.LTS
```

Verificar:

```powershell
node --version
npm --version
```

### Paso 3 — JDK 17

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
```

Verificar que quedó la versión correcta:

```powershell
java -version
```

Tiene que decir `17.x`. Después comprobar la variable de entorno:

```powershell
$env:JAVA_HOME
```

Si sale vacío, hay que crearla a mano:

1. Menú Inicio, buscar "Editar las variables de entorno del sistema"
2. Botón **Variables de entorno**
3. En *Variables del sistema*, **Nueva**:
   - Nombre: `JAVA_HOME`
   - Valor: la carpeta donde quedó instalado el JDK, por ejemplo
     `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`

### Paso 4 — Android Studio y el SDK

1. Descargar e instalar [Android Studio](https://developer.android.com/studio).
2. Abrirlo y completar el asistente inicial. Va a descargar el SDK, que ocupa
   varios GB.
3. Anotar dónde quedó el SDK: **Settings → Languages & Frameworks → Android SDK**,
   campo *Android SDK Location*. Por defecto suele ser
   `C:\Users\TU_USUARIO\AppData\Local\Android\Sdk`.

En **SDK Manager → pestaña SDK Tools**, marcar y aplicar:

- Android SDK Platform-Tools
- Android SDK Build-Tools
- Android Emulator
- Android SDK Command-line Tools

### Paso 5 — Variables de entorno de Android

Con la ruta anotada en el paso anterior, crear una variable **de usuario**:

- Nombre: `ANDROID_HOME`
- Valor: la ruta del SDK

Y agregar al `Path` estas dos entradas:

```text
%ANDROID_HOME%\platform-tools
%ANDROID_HOME%\emulator
```

Cerrar **todas** las terminales abiertas, abrir una nueva y verificar:

```powershell
$env:ANDROID_HOME
adb version
emulator -list-avds
```

Si `adb` no se encuentra, el `Path` quedó mal cargado.

### Paso 6 — Crear el emulador

En Android Studio: **Device Manager** → **Create Device**.

- Dispositivo: cualquier teléfono, por ejemplo *Medium Phone*
- Imagen del sistema: una que incluya **Google APIs**, arquitectura **x86_64**
- Anotar el nombre del AVD, hace falta para levantarlo desde la terminal

### Paso 7 — Podman, para el backend

```powershell
winget install RedHat.Podman-Desktop
```

Completar el asistente y crear la máquina Podman. El detalle está en
[GUIA_TRABAJO_EQUIPO.md](../GUIA_TRABAJO_EQUIPO.md).

### Paso 8 — Clonar el repositorio e instalar dependencias

```powershell
git clone https://github.com/ivanpf/MeDiTurno.git
cd MeDiTurno\mobile
npm install
```

`npm install` lee `package.json` y crea la carpeta `node_modules/`, que **no se
sube al repositorio** por su tamaño. Se corre una vez, y de nuevo cada vez que
alguien agrega una dependencia nueva.

### Paso 9 — Compilar la app (solo la primera vez)

Con el emulador **ya abierto**:

```powershell
npx expo run:android
```

Esto genera la carpeta `android/`, la compila con Gradle y la instala en el
emulador. **Tarda entre 10 y 20 minutos la primera vez.** Al terminar, la app se
abre sola.

Solo hay que repetirlo si se agrega una librería con código nativo, o si se
modifica `app.json`.

---

## 3. Uso diario

Ya instalado todo, para trabajar hacen falta **tres cosas prendidas a la vez**:

```powershell
# Terminal 1 - emulador (reemplazar por el nombre del AVD propio)
emulator -avd Medium_Phone

# Terminal 2 - backend, desde la RAIZ del repositorio
podman compose up -d backend

# Terminal 3 - la app, desde mobile/
npx expo start
```

Y en la Terminal 3, presionar **`a`** para abrir en Android.

Teclas útiles en esa terminal: `a` = Android · `w` = navegador · `r` = recargar ·
`Ctrl+C` = cortar.

Con eso alcanza: al guardar un archivo, la pantalla del emulador se actualiza
sola en un segundo. Se llama **Fast Refresh** y no hay que recompilar nada.

Para apagar todo: cerrar la ventana del emulador, `Ctrl+C` en Expo, y
`podman compose down` desde la raíz.

---

## 4. Conexión con el backend

La dirección que usa la app **cambia según dónde se ejecute**. La lógica está en
[src/lib/api.ts](src/lib/api.ts):

| Dónde corre la app | Dirección que usa |
|---|---|
| Emulador de Android | `http://10.0.2.2:8080` |
| Navegador | `http://localhost:8080` |
| Celular físico | La que se defina en `.env` |

El emulador es una máquina virtual con su propia red: si pidiera `localhost`
buscaría un servidor dentro del emulador, no en la PC. Android reserva la
dirección `10.0.2.2` como alias de la máquina anfitriona.

**Para probar en un celular físico**, la PC y el teléfono tienen que estar en la
misma red WiFi. Hay que crear un archivo `.env` dentro de `mobile/` con la IP de
la PC, que se obtiene con `ipconfig`:

```bash
EXPO_PUBLIC_API_URL=http://192.168.0.15:8080
```

Después de editar el `.env` hay que reiniciar Expo. Ese archivo no se sube al
repositorio.

### Usuario de prueba

El backend carga sus datos de demo solo al arrancar, así que no hay que cargar
nada a mano en la base:

```text
demo@mediturno.com / 1234
```

---

## 5. Estructura del proyecto

```text
mobile/
├── src/
│   ├── app/          Pantallas. Cada archivo es una ruta (expo-router)
│   │   ├── _layout.tsx   Estructura común: pestañas y tema
│   │   ├── index.tsx     Login y estado de conexión
│   │   └── turnos.tsx    Listado con scroll infinito
│   ├── components/   Piezas de interfaz reutilizables
│   ├── constants/    Colores, espaciados y tipografías
│   ├── hooks/        Lógica reutilizable de React
│   └── lib/          Código sin interfaz: api.ts y notificaciones.ts
├── assets/           Íconos e imágenes
├── app.json          Nombre, ícono y plugins de la app
└── package.json      Dependencias y scripts
```

Las rutas se definen por archivo: `src/app/turnos.tsx` es la pantalla `/turnos`.
No hay que registrarla en ningún lado, alcanza con crear el archivo.

Los archivos con sufijo `.web.tsx` son la versión para navegador del mismo
componente. React Native elige cuál usar según la plataforma, sin que haya que
escribir ningún `if`.

---

## 6. Problemas frecuentes

**`ConfigError: The expected package.json path ... does not exist`**

Se ejecutó `npx expo` desde la carpeta equivocada. Tiene que ser desde `mobile/`.

**La app muestra "Sin conexion / Failed to fetch"**

El backend está apagado. Levantarlo con `podman compose up -d backend` desde la
raíz, y comprobar en el navegador que `http://localhost:8080` responda.

**`ECONNREFUSED` al abrir la app en el emulador**

Metro no está corriendo. Levantar `npx expo start` desde `mobile/`.

**La app abre una pantalla que pide un "development server"**

Es el launcher de la development build. Escribir `10.0.2.2:8081` y tocar
*Connect*. Si no conecta, ejecutar en otra terminal:

```powershell
adb reverse tcp:8081 tcp:8081
```

**El build de Gradle falla con `Unable to delete directory` o `AccessDeniedException`**

Windows no permite borrar archivos que otro proceso tiene abiertos, y Gradle deja
procesos corriendo en segundo plano entre compilaciones. Cerrar Expo y ejecutar:

```powershell
cd android
.\gradlew.bat --stop
cd ..
npx expo run:android
```

Si vuelve a fallar, borrar a mano `android\app\build` y `android\build` y repetir.
Esas carpetas son generadas: no están en Git y se rehacen solas.

Como último recurso se pueden matar todos los procesos que retienen los archivos:

```powershell
Get-Process java,node -ErrorAction SilentlyContinue | Stop-Process -Force
```

**Ojo:** eso también apaga los contenedores de Podman. Después del build hay que
levantarlos de nuevo con `podman compose up -d backend` desde la raíz.

**El emulador no aparece en `adb devices`**

```powershell
adb kill-server
adb start-server
```

**Cambié una pantalla y no se actualiza**

Presionar `r` en la terminal de Expo. Si se creó o borró un archivo dentro de
`src/app/`, hay que reiniciar la app entera: las rutas se calculan al arrancar.

---

## 7. Cosas a tener en cuenta

- **No correr `npm run reset-project`.** Ese comando viene de la plantilla de Expo
  y mueve todo el código a `app-example/` para dejar el proyecto en blanco.
- `node_modules/`, `android/`, `.env` y `.expo/` **no se suben** al repositorio:
  están en [.gitignore](.gitignore). Sí se suben `package.json` y
  `package-lock.json`, que son los que permiten reconstruir las dependencias.
- El login **no es autenticación segura**: no emite token ni sesión, y el resto de
  los endpoints responden aunque no se haya pasado por él. Es un POC; para
  producción hace falta JWT.
- La notificación es **local**: la dispara la app misma, no un servidor. Se ve
  igual que una push, pero no hay Firebase ni credenciales detrás.
