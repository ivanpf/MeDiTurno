# MeDiTurno

Sistema online de reserva de órdenes de consulta para una institución de asistencia médica. Lo usan cuatro perfiles: paciente, funcionario, médico y administrador.

Este repositorio contiene la **base del proyecto**: la estructura del backend y del frontend ya está creada, y los casos de uso se van implementando uno por uno. Para entender cómo está organizado el código y cómo se desarrolla, ver [`CLAUDE.md`](CLAUDE.md). Para el flujo de trabajo con Git, ver [`GUIA_TRABAJO_EQUIPO.md`](GUIA_TRABAJO_EQUIPO.md).

| Parte | Tecnología |
|---|---|
| `backend/` | Spring Boot 4 · Java 17 · Maven · PostgreSQL |
| `frontend/` | React 19 · Vite · React Router |
| `mobile/` | Aplicación móvil del paciente (todavía sin estructura base) |
| `compose.yml` | Levanta PostgreSQL, backend y frontend con Podman |

## Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera reciente | clonar y subir cambios |
| Podman | versión reciente (con `podman compose`) | levantar el proyecto en contenedores y la base de datos |
| JDK | 17 o superior | solo para desarrollar el backend sin contenedores |
| Node.js y npm | Node 20 o superior | solo para desarrollar el frontend sin contenedores |

- **Windows:** instalar **Podman Desktop** y crear/iniciar la máquina de Podman desde la aplicación (o con `podman machine init` y `podman machine start`).
- **Fedora:** Podman ya funciona sin permisos de administrador (rootless).
- No hace falta instalar Maven: el proyecto trae `mvnw` (Maven Wrapper), que lo descarga solo.

## Arrancar el proyecto desde cero

### 1. Clonar el repositorio

```bash
git clone https://github.com/ivanpf/MeDiTurno.git
cd MeDiTurno
```

### 2. Variables de entorno (opcional)

Los valores por defecto sirven para desarrollo local, así que este paso solo hace falta si se quieren cambiar:

```bash
cp .env.example .env
```

En PowerShell:

```powershell
Copy-Item .env.example .env
```

### 3. Configurar Firebase

El inicio de sesión usa Firebase Authentication y casi todo ya viene configurado (datos públicos en `compose.yml`, usuarios de prueba cargados solos). Solo falta **copiar la clave de servicio** que te pasa el equipo a `secrets/firebase-clave.json` (la carpeta `secrets/` no se sube a Git). Los usuarios y contraseñas de prueba están en la sección *Autenticación (Firebase)* de [`CLAUDE.md`](CLAUDE.md).

### 4. Levantar todo en contenedores

```bash
podman compose up -d --build
```

La primera vez tarda varios minutos (descarga imágenes y compila el backend y el frontend).

### 5. Comprobar que funciona

| Qué | Dirección | Qué se debe ver |
|---|---|---|
| Frontend | http://localhost:3000 | la pantalla de inicio de sesión |
| Backend | http://localhost:8080/api/health | `{"estado":"ok"}` |
| PostgreSQL | `localhost:5432` | (para clientes de base de datos) |

Para **iniciar sesión** hay que usar uno de los usuarios de prueba (por ejemplo `admin@mediturno.com` / `admin123`; lista en *Autenticación (Firebase)* de [`CLAUDE.md`](CLAUDE.md)). El alta de pacientes (CU-31) y el alta de usuarios internos (CU-34) ya tienen pantalla; las demás opciones muestran una pantalla "pendiente de implementar".

La mayoría de los endpoints del backend todavía responde `501 Funcionalidad pendiente de implementar`; CU-1, CU-2, CU-7, CU-28, CU-31 y CU-34 están implementados. Para que CU-34 envíe credenciales, configure las variables `MAIL_*` del archivo `.env`.

### Comandos útiles

```bash
podman compose logs -f          # seguir los logs
podman compose ps               # ver el estado de los servicios
podman compose down             # detener sin borrar datos
podman compose down -v          # detener y BORRAR la base de datos local
```

## Desarrollar sin reconstruir imágenes

Es la forma cómoda de trabajar: el frontend se recarga solo al guardar. Se usan tres terminales.

**Terminal 1: solo la base de datos**

```bash
podman compose up -d postgres
```

**Terminal 2: backend** (conecta a PostgreSQL publicado en la máquina)

```bash
cd backend
DB_HOST=localhost ./mvnw spring-boot:run
```

En PowerShell:

```powershell
cd backend
$env:DB_HOST = "localhost"
.\mvnw.cmd spring-boot:run
```

**Terminal 3: frontend**

```bash
cd frontend
npm install
npm run dev
```

Abrir la dirección que muestra Vite (normalmente http://localhost:5173). Las llamadas a `/api` se redirigen al backend en el puerto 8080.

- **Frontend:** al guardar un archivo se actualiza solo. Solo hay que reiniciar `npm run dev` si cambian `package.json` o `vite.config.js`.
- **Backend:** los cambios en Java requieren reiniciar `spring-boot:run`.

### Probar, revisar y compilar

```bash
cd backend && ./mvnw test        # tests del backend (la primera vez descargan un PostgreSQL embebido, hace falta internet)
cd frontend && npm run lint      # revisión de código del frontend
cd frontend && npm run build     # compilación del frontend
```

## Estructura del proyecto

```
backend/src/main/java/com/mediturno/backend/   código del backend, por funcionalidad
frontend/src/                                  código del frontend
compose.yml                                    servicios de Podman
.env.example                                   variables de entorno de ejemplo
CLAUDE.md                                      guía de desarrollo (arquitectura, convenciones, recetas)
GUIA_TRABAJO_EQUIPO.md                         flujo de trabajo con Git, ramas y Pull Requests
```

### Backend

Cada paquete tiene su servicio (con los métodos del Documento de diseño), su controlador (con los endpoints de los diagramas de secuencia) y una carpeta `dto`:

| Paquete | Servicio | Casos de uso |
|---|---|---|
| `auth` | `AuthService` | CU-1, CU-2, CU-3, CU-4 |
| `pacientes` | `PacienteService` | CU-5, CU-7, CU-31, CU-32, CU-33, CU-44 |
| `usuarios` | `UsuarioInternoService` | CU-5, CU-34, CU-35, CU-36, CU-45, CU-46 |
| `especialidades` | `EspecialidadService` | CU-28, CU-29, CU-30, CU-43 |
| `consultorios` | `ConsultorioService` | CU-37, CU-38, CU-39, CU-47 |
| `turnos` | `TurnoSemanalService` | CU-12, CU-13, CU-14, CU-20 y las fechas disponibles de CU-8 |
| `reservas` | `ReservaService` | CU-8, CU-9, CU-10, CU-15, CU-19 |
| `ordenes` | `OrdenService` | CU-16 |
| `consultas` | `ConsultaService` | CU-11, CU-21, CU-22, CU-23, CU-26 |
| `pases` | `PaseService` | CU-24, CU-25, CU-27 |
| `anuncios` | `AnuncioService` | CU-6, CU-17, CU-18 |
| `notificaciones` | `NotificacionService`, `EmailService`, `PushNotificationService` | avisos por correo, push y en la aplicación |
| `estadisticas` | `EstadisticaService` | CU-40, CU-41, CU-42 |
| `documentos` | `DocumentosService` | PDF de la orden (CU-16) |
| `common` | | CORS, formato de errores, usuario actual, DTOs compartidos y `/api/health` |

### Frontend

Cada caso de uso tiene su pantalla (`features/<área>/...Page.jsx`) y su opción en el menú lateral del rol. Las pantallas y el menú se definen en `frontend/src/routes.jsx`.

Donde el Documento de diseño une casos de uso en una sola pantalla (CU-13 con CU-20, CU-15 con CU-16) hay una sola opción. CU-9 y CU-10 también comparten pantalla ("Mis reservas").

## Variables de entorno

| Variable | Valor por defecto | Quién la usa |
|---|---|---|
| `POSTGRES_USER` | `mediturno` | PostgreSQL y backend |
| `POSTGRES_PASSWORD` | `mediturno123` | PostgreSQL y backend |
| `POSTGRES_DB` | `mediturno_db` | PostgreSQL y backend |
| `DB_HOST` | `postgres` (nombre del contenedor) | backend; usar `localhost` si corre fuera de contenedores |
| `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, `VITE_FIREBASE_APP_ID` | las del proyecto de Firebase de MeDiTurno | frontend (datos públicos de Firebase); se leen del `.env` de la raíz |
| `FIREBASE_CREDENCIALES` | `../secrets/firebase-clave.json` (en contenedores, `/secrets/firebase-clave.json`) | backend; ruta de la clave de servicio |

Los valores de `.env.example` sirven **únicamente para desarrollo local**. `.env` está ignorado por Git: no subir credenciales reales ni usar estas en producción.

## Base de datos

PostgreSQL arranca vacío. **Las tablas las crea el propio backend al iniciar**, con migraciones de Flyway (scripts SQL en `backend/src/main/resources/db/migration/`). No hay que ejecutar nada a mano.

- El backend necesita que PostgreSQL esté disponible para arrancar.
- Para empezar con la base desde cero (se pierden los datos locales): `podman compose down -v` y volver a levantar.
- Cómo cambiar la base y las reglas de las migraciones: ver la sección *Base de datos y migraciones* de [`CLAUDE.md`](CLAUDE.md).

## Problemas frecuentes

**`Cannot connect to Podman`**

La máquina de Podman está detenida. En Windows, iniciarla desde Podman Desktop o con:

```bash
podman machine start
```

**El backend no conecta a la base de datos**

Revisar que PostgreSQL esté listo y reiniciar el backend:

```bash
podman compose ps
podman compose logs postgres
podman restart backend
```

**Puerto ocupado**

Cambiar el puerto del lado izquierdo en `compose.yml` (por ejemplo `"3001:80"`) y levantar de nuevo el servicio. Si es Vite (`npm run dev`), muestra automáticamente otro puerto.

**El backend no arranca y el error dice `Schema-validation: missing column ...` o `missing table ...`**

Una entidad no coincide con su tabla. Falta la migración correspondiente o está mal escrita. Ver *Base de datos y migraciones* en `CLAUDE.md`.

**El backend no arranca y el error dice `Migration checksum mismatch`**

Alguien editó una migración que ya se había ejecutado en tu base local. Si solo pasó en tu máquina, reiniciá la base con `podman compose down -v`. Las migraciones ya integradas en `main` no se editan: el cambio va en un script nuevo.

**El backend responde `501`**

No es un error: el caso de uso todavía no está implementado.

**En Linux, `./mvnw: Permission denied`**

```bash
chmod +x backend/mvnw
```

**Hay que reiniciar la base de datos desde cero**

Elimina todos los datos locales de PostgreSQL:

```bash
podman compose down -v
```
