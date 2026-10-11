# MeDiTurno

Sistema online de reserva de órdenes de consulta para una institución de asistencia médica. Cuatro perfiles: paciente, funcionario, médico y administrador.

Monorepo: `backend/` (Spring Boot 4.1, Java 17, Maven, PostgreSQL 16), `frontend/` (React 19, Vite 8, React Router 7) y `mobile/` (sin estructura base todavía).
Cómo levantar el proyecto: `README.md`. Flujo con Git y Podman: `GUIA_TRABAJO_EQUIPO.md`.

## Estado actual

Es la **base del proyecto**: la estructura está creada pero **ningún caso de uso está implementado**.

- **Backend:** todos los servicios y controladores del Documento de diseño existen. Sus métodos lanzan `NoImplementadoException` (la API responde `501`). Los DTO son `record` vacíos. No hay entidades ni repositorios.
- **Frontend:** todas las pantallas existen como `PaginaPendiente`, con su opción en el menú del rol. El login no valida nada: tiene un selector "Solo desarrollo" para elegir el rol.
- **Autenticación:** se usará **Firebase Authentication** (correo y Google). Falta implementarla (CU-1). El Documento de diseño todavía menciona Auth0 y validación de contraseñas propia; esas partes se van a corregir.

## Comandos

```bash
podman compose up -d --build          # todo en contenedores (frontend en :3000, backend en :8080)
podman compose up -d postgres         # solo la base, para desarrollar fuera de contenedores

cd backend  && ./mvnw spring-boot:run # backend (PowerShell: $env:DB_HOST="localhost"; .\mvnw.cmd spring-boot:run)
cd backend  && ./mvnw test            # tests del backend
cd frontend && npm run dev            # frontend con recarga automática (:5173)
cd frontend && npm run lint           # oxlint
cd frontend && npm run build          # compilación
```

Antes de subir cambios: `./mvnw test`, `npm run lint` y `npm run build` deben pasar.

## Cómo funciona

### Una petición, de punta a punta

```
Pantalla (React) → services/api.js → /api/... → Controller → Service → Repository → PostgreSQL
```

- En desarrollo, Vite redirige `/api` a `localhost:8080`. En contenedores lo hace Nginx. El front siempre llama a `/api/...`, nunca a una dirección completa.
- El **Controller** solo recibe la petición, delega en el Service y devuelve el resultado. La lógica de negocio va en el **Service**.
- Los errores salen con el formato `{ estado, mensaje, detalles, fecha }` (`GlobalExceptionHandler`). El front los recibe como `ApiError` desde `api.js`.

### Backend (`backend/src/main/java/com/mediturno/backend/`)

- **Organizado por funcionalidad**, no por capa. Cada paquete tiene `XService`, `XController` y `dto/`. Cuando se necesiten, se agregan `model/` (entidades) y `repository/` dentro del mismo paquete. La tabla de paquetes y casos de uso está en el `README.md`.
- Los nombres de servicios, métodos y DTO salen del **Documento de diseño**. No renombrarlos sin avisar: los diagramas y el código tienen que seguir coincidiendo.
- Todos los endpoints están bajo `/api`. Las rutas vienen de los diagramas de secuencia; las que el diagrama no indica son propuestas y se pueden ajustar.
- `common/`: `CorsConfig`, `exception/` (`ApiError`, `GlobalExceptionHandler`, `NoImplementadoException`), `seguridad/UsuarioActual`, `dto/` (DTO compartidos) y `health/` (`GET /api/health`).
- **`UsuarioActual`** entrega el id del usuario autenticado (paciente, médico, funcionario). Los controladores lo usan en lugar de recibir ese id por la URL o el cuerpo. Hoy lanza `NoImplementadoException`; se implementa con CU-1.
- **Métodos internos** (sin endpoint): `AuthService.crearCuenta`, `PaseService.tienePaseVigente`, `EmailService`, `PushNotificationService.enviarPush` y los avisos de `NotificacionService`. Los llaman otros servicios.

### Frontend (`frontend/src/`)

- **`routes.jsx` es la fuente única de las pantallas.** Cada entrada define roles, ruta, texto del menú, caso de uso, grupo del menú (opcional) y componente. De ahí salen las rutas de `App.jsx` y el menú de `layout/Layout.jsx`. Las entradas comunes a todos los perfiles van al final, así la primera de cada rol es su pantalla de inicio.
- `features/<área>/` tiene una carpeta por área (`reservas`, `turnos`, `consultas`, `pases`, `anuncios`, `cuenta`, `especialidades`, `pacientes`, `usuarios`, `consultorios`, `estadisticas`, `auth`). Cada pantalla es `NombrePage.jsx`.
- `auth/`: `AuthContext.jsx` guarda rol y nombre del usuario; se usa con `useAuth()`. `SoloRol.jsx` protege las rutas por rol. Cuando exista el login real, solo cambia `AuthContext.jsx` (rol, nombre y token vendrán del backend).
- `layout/Layout.jsx`: encabezado (logo, nombre, rol, cerrar sesión) y menú lateral con grupos desplegables.
- `services/api.js`: cliente HTTP único (`api.get/post/put/patch/delete`). No usar `fetch` directo en las pantallas.
- `components/`: `Logo`, `PantallaAcceso` (login, registro, recuperar) y `PaginaPendiente`.

## Cómo implementar un caso de uso

Ejemplo: CU-28, alta de especialidades.

1. **Rama** desde `main`: `git switch main && git pull && git switch -c cu-28-alta-especialidad`.
2. **Leer** el caso de uso y su diagrama de secuencia (documentos del equipo): pasos, flujos alternativos y errores.
3. **Backend** (paquete `especialidades`):
   - Completar los campos de los DTO que usa el caso de uso (`AltaEspecialidadDTO`, `EspecialidadResponseDTO`). Los comentarios de cada DTO resumen lo que dicen los documentos.
   - Crear la entidad en `model/` y el repositorio en `repository/`.
   - En `EspecialidadService.altaEspecialidad`, reemplazar el `throw new NoImplementadoException()` por la lógica: validar, buscar duplicados, guardar y devolver el DTO.
   - Cada flujo alternativo del caso de uso es un error con su código del diagrama: `throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya está en uso")`.
   - Validar el cuerpo con `@Valid` y anotaciones de `jakarta.validation` en el DTO.
   - Si la operación toca varias tablas o necesita consistencia (reservar, cancelar, suspender, emitir orden), usar `@Transactional`.
   - Escribir tests del servicio.
4. **Frontend** (`features/especialidades/AltaEspecialidadPage.jsx`):
   - Reemplazar `PaginaPendiente` por la pantalla real (el mock de referencia está en la rama `mocks`).
   - Llamar al backend con `api.post('/especialidades', datos)` y mostrar los errores (`ApiError.mensaje`).
5. **Probar** de punta a punta, correr `./mvnw test`, `npm run lint` y `npm run build`.
6. **Pull Request** hacia `main`.

## Guía de diseño

El aspecto de la aplicación es **sobrio y profesional**: azul y blanco, con mostaza solo para resaltar. Toda pantalla nueva tiene que verse como las que ya existen. Antes de crear un estilo nuevo, reutilizar las variables y clases de `frontend/src/index.css` (el único CSS del proyecto). Pantallas de referencia: `layout/Layout.jsx`, `features/auth/LoginPage.jsx` y `components/PaginaPendiente.jsx`.

### Colores (variables en `:root`)

| Variable | Valor | Uso |
|---|---|---|
| `--azul` | `#1d4f91` | color principal: botón principal, enlaces, foco |
| `--azul-oscuro` | `#12366b` | títulos, texto sobre mostaza, estados activos |
| `--azul-claro` | `#e8f0fa` | fondo suave: ítem activo del menú, `code`, hover de botones |
| `--mostaza` | `#e1ad01` | **solo para resaltar**: rol del usuario, barra del ítem activo, línea bajo el título, avisos |
| `--mostaza-oscuro` | `#b88700` | texto y borde sobre fondo mostaza claro |
| `--mostaza-claro` | `#fff6d6` | fondo de avisos |
| `--texto` | `#1c2733` | texto general |
| `--gris` | `#5b6b7c` | texto secundario, etiquetas de campos |
| `--borde` | `#d3dce8` | bordes de campos y separadores |
| `--fondo` | `#f4f7fb` | fondo de la página |
| `--blanco` | `#fff` | paneles, tarjetas, campos |

Reglas: no agregar colores de marca nuevos y no escribir colores sueltos en los componentes (usar las variables). El mostaza se usa con moderación: si hay más de uno o dos elementos mostaza por pantalla, se está abusando.

### Tipografía, espacios y formas

- **Fuente:** la del sistema (`system-ui`), base de 15 px. Sin fuentes externas.
- **Títulos:** `h1` de 26 px en azul oscuro; la barra mostaza de debajo se agrega sola por CSS. `h2` de 18 px. Etiquetas de campos: 13 px en negrita y gris.
- **Espaciado:** escala de 4, 8, 12, 16, 24 y 32 px. Contenido con 32 px de margen interno; paneles con 24 px.
- **Bordes redondeados:** 8 px en campos y botones, 12 px en paneles, 14 px en la tarjeta de acceso y 999 px en etiquetas tipo "píldora".
- **Sombra:** una sola, `var(--sombra)`, para paneles y tarjetas.
- **Movimiento:** solo transiciones cortas (0,15 s). Sin animaciones decorativas.

### Estructura de una pantalla

- **Pantalla autenticada:** el `Layout` ya aporta encabezado y menú lateral. La pantalla devuelve un `<section className="panel">` con un `<h1>` y su contenido. No repetir el encabezado ni el menú.
- **Pantalla sin sesión** (login, registro, recuperar acceso): usar `PantallaAcceso` y, dentro, una `.tarjeta`.
- **Menú lateral:** se arma solo desde `routes.jsx` (grupos desplegables, ítem activo resaltado). No se escribe a mano.

### Componentes disponibles

- **Botones:** el botón común es secundario (borde azul, fondo blanco). Usar `className="principal"` para la acción principal; **un solo botón principal por pantalla o formulario**. Deshabilitado = opacidad reducida.
- **Formularios:** el `<label>` envuelve al campo (`<label>Nombre<input /></label>`). Los campos ocupan todo el ancho y tienen foco azul visible.
- **Aviso:** `.pendiente` (fondo mostaza claro con barra mostaza) para avisos informativos.
- **Logo:** componente `Logo`. `variante="oscuro"` sobre fondo claro; `variante="claro"` sobre azul (el logo va dentro de un círculo blanco, porque el logo es azul y se perdería). Imagen en `src/assets/logo.png`, ícono de pestaña en `public/favicon.png`.
- **Usuario en el encabezado:** avatar con iniciales, nombre y etiqueta del rol (mostaza).

### Todavía no existe (definirlo con este mismo estilo)

Tablas, mensajes de error y de éxito, ventanas de confirmación (modales), paginación y estados de carga. Quien primero los necesite los agrega a `index.css` usando las variables de arriba, para que los reutilicen los demás, y actualiza esta guía. Para error y éxito se necesitan variables nuevas (`--error`, `--exito`): agregarlas a `:root` y documentarlas en la tabla de colores.

### Responsive y accesibilidad

- **Responsive:** hay un punto de corte en 800 px (el menú pasa arriba y el login se apila). Probar cada pantalla con ancho de celular.
- **Contraste:** texto oscuro sobre fondo claro y blanco sobre azul. No poner texto gris claro sobre fondos de color.
- **Foco visible** en todos los controles (ya lo resuelve el CSS global; no quitarlo).
- Navegar con `<NavLink>`/`<Link>`; las acciones son `<button>`. Los desplegables llevan `aria-expanded`. Las imágenes decorativas tienen `alt=""`; las informativas, un `alt` descriptivo.

### Qué evitar

- Librerías de interfaz o de CSS (Bootstrap, Material, Tailwind, etc.) y estilos en línea (`style={{...}}`).
- Colores o degradados nuevos (los degradados existentes son solo el encabezado y el panel de marca del login).
- Íconos, emojis o ilustraciones decorativas.
- Los **mocks HTML** (rama `mocks`) sirven para ver dónde va cada componente y cómo es el flujo, **no** como referencia de aspecto: su estilo es intencionalmente básico.

## Convenciones

### Generales

- **Español** en nombres de clases, métodos, DTO, rutas y textos, siguiendo los documentos (`TurnoSemanalService`, `reservarOrden`, `/api/turnos-semanales`). Excepción: palabras técnicas de Spring o React.
- **Texto de la interfaz** en español, tratando al usuario de **"usted"**: "ingrese", "tiene", "puede". No "ingresá", "tenés" ni "ingresa".
- **Mensajes de error** claros para el usuario; el detalle técnico va a los logs.
- **No subir secretos:** `.env`, claves de servicio (por ejemplo, de Firebase), credenciales. `.env` está ignorado por Git.

### Backend

- DTO como `record`. Entradas: `Alta...DTO`, `Modificar...DTO`, `...RequestDTO`. Salidas: `...ResponseDTO`.
- Nunca devolver una entidad directamente desde un controlador: siempre un DTO.
- El id del usuario autenticado sale de `UsuarioActual`, nunca del cliente.
- Las validaciones que el diagrama marca como "operaciones internas del método" son métodos privados del servicio, no métodos públicos nuevos.
- La estructura de tablas la maneja JPA con `ddl-auto: update` (provisorio). Cuando se acuerde el modelo de datos, evaluar migraciones (Flyway).

### Frontend

- JavaScript con JSX. Componentes en `PascalCase`; las pantallas terminan en `Page`.
- **Un solo CSS**, `src/index.css`, con variables en `:root`. Sin librerías de interfaz.
- Aspecto visual: seguir la **Guía de diseño** de este archivo.
- Agregar una pantalla = crear el componente en `features/<área>/` y sumar una línea en `routes.jsx`.
- No usar `fetch` directo: usar `api`.

## Reglas de negocio ya acordadas

- **Hora estimada de atención:** duración por consulta = (hora fin − hora inicio) ÷ máximo de pacientes del turno. La hora estimada del número N = hora inicio + (N − 1) × duración. Se muestra siempre como estimada (`~10:20`). *Definida por el equipo; falta incorporarla a los documentos.*
- **Reserva (CU-8):** el paciente elige el turno y después su número entre los libres. Si otro lo ocupa antes de confirmar, se informa y puede elegir otro.
- **Cancelación (CU-9):** solo si la reserva está vigente y faltan **48 horas o más**.
- **Orden (CU-16):** se emite hasta **15 minutos antes** de la hora estimada de atención.
- **Pase (CU-24) y reconsulta (CU-25):** vigencia máxima de **60 días**.
- **Registro (CU-7):** el paciente debe estar en el padrón (lo carga el administrador, CU-31) y no tener cuenta.
- **Inicio de sesión (CU-1):** el mensaje de error es genérico y no revela si la cuenta existe ni su estado.

## Git y Pull Requests

- **Una rama por caso de uso**, creada desde `main` actualizado, y Pull Request hacia `main`. Nunca desarrollar directamente en `main` ni usar `git push --force`.
- Antes de cada commit, ejecutar `git status` y agregar los archivos de a uno o por carpeta (evitar `git add .`).
- **No se versionan:** `.env`, `node_modules/`, `backend/target/`, `frontend/dist/`, logs.
- Mensajes de commit en español, en presente: "Agrega alta de especialidades".

## Documentación de referencia

- **Documento de casos de uso** y **Documento de diseño** (Google Docs del equipo): casos de uso, diagrama de clases y diagramas de secuencia. Son la fuente de nombres, flujos y endpoints.
- **Mocks HTML** de las pantallas: rama `mocks` (carpeta `mocks/`).
- Si el código y un documento no coinciden, no decidir por cuenta propia: avisar al equipo y corregir el que esté equivocado.
