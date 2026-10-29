# MeDiTurno

Sistema online de reserva de órdenes de consulta para una institución de asistencia médica. Cuatro perfiles: paciente, funcionario, médico y administrador.

Monorepo: `backend/` (Spring Boot 4.1, Java 17, Maven, PostgreSQL 16), `frontend/` (React 19, Vite 8, React Router 7) y `mobile/` (sin estructura base todavía).
Cómo levantar el proyecto: `README.md`. Flujo con Git y Podman: `GUIA_TRABAJO_EQUIPO.md`.

## Estado actual

Es la **base del proyecto**: la estructura está creada y **están implementados CU-1, CU-2, CU-6, CU-7, CU-8, CU-9, CU-12, CU-13, CU-15, CU-16, CU-17, CU-18, CU-20, CU-21, CU-22, CU-23, CU-28, CU-29, CU-30, CU-31, CU-34, CU-37, CU-38, CU-39, CU-43 y CU-47**. CU-11 y CU-14 están implementados en el backend; sus pantallas web siguen pendientes. Los demás casos pendientes responden `501`.

- **Backend:** todos los servicios y controladores del Documento de diseño existen. Los métodos pendientes lanzan `NoImplementadoException` (respuesta `501`) y sus DTO siguen vacíos. CU-8 consulta las ocurrencias existentes y registra reservas mediante una transacción con bloqueo; las ocurrencias (las fechas reservables) las generan CU-12 al crear un turno y una tarea programada (ver «Tareas programadas»). Los listados básicos de especialidades, consultorios y médicos sirven para llenar las listas desplegables. El modelo de datos ya existe: la migración `V1` crea las 20 tablas y hay una entidad y un repositorio por tabla.
- **Frontend:** las pantallas tienen su opción en el menú del rol. Están hechas las de CU-1, CU-2, CU-6, CU-7, CU-8, CU-9, CU-12, CU-13, CU-15, CU-16, CU-17, CU-18, CU-20, CU-21, CU-22, CU-23, CU-28, CU-29, CU-30, CU-31, CU-34, CU-37, CU-38, CU-39, CU-43 y CU-47; el resto conserva `PaginaPendiente`.
- **Autenticación:** **Firebase Authentication** (correo y Google), implementada en CU-1, CU-2 y CU-7 (ver «Autenticación (Firebase)»). Faltan CU-3 y CU-4.

## Comandos

```bash
podman compose up -d --build          # todo en contenedores (frontend en :3000, backend en :8080)
podman compose up -d postgres         # solo la base, para desarrollar fuera de contenedores

cd backend  && ./mvnw spring-boot:run # backend (PowerShell: $env:DB_HOST="localhost"; .\mvnw.cmd spring-boot:run)
cd backend  && ./mvnw test            # tests del backend (levantan un PostgreSQL embebido; la primera vez descargan binarios)
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
- **`UsuarioActual`** entrega el id del usuario autenticado (paciente, médico, funcionario). Los controladores lo usan en lugar de recibir ese id por la URL o el cuerpo. Ver «Autenticación (Firebase)».
- **Consultas de apoyo para las pantallas** (no son casos de uso): `GET /api/medicos` (`MedicoController`: médicos activos con sus especialidades activas, para administrador y funcionario), `GET /api/especialidades` (todas, ordenadas por nombre; la pantalla filtra por estado) y `GET /api/consultorios` (todos, ordenados por nombre; administrador y funcionario). Sin parámetros devuelven todo, para llenar listas desplegables; las de especialidades y consultorios aceptan además búsqueda y filtros (ver «Gestión de catálogos del administrador»).
- **Correo:** el backend envía correos con `EmailService` (hoy el aviso de suspensión de un turno, CU-13, y el de consulta registrada, CU-23). Se configura con `MAIL_HOST`, `MAIL_PORT`, `MAIL_USUARIO`, `MAIL_CLAVE`, `MAIL_AUTENTICAR`, `MAIL_STARTTLS` y `MAIL_REMITENTE` (ver `.env.example`). En desarrollo, `compose.yml` levanta **Mailpit**: recibe los correos sin enviarlos a nadie y se leen en http://localhost:8025. Los avisos a los pacientes se guardan en la tabla `notificacion` (una por canal: aplicación, correo; el push lo hará otro caso de uso) y, si el correo falla, queda `FALLIDA` sin revertir la operación que lo originó.
- **Consulta de reserva y orden (CU-15 y CU-16, una sola pantalla):** `GET /api/reservas/{id}` muestra los datos de la reserva con cualquier estado (identificador no válido `400`, inexistente `404`) y `POST /api/reservas/{id}/orden` emite la orden (`OrdenService`): solo si la reserva está `VIGENTE`, **el día del turno, desde una hora antes de que empieza hasta la hora en que termina** (da igual la hora estimada del número) (si no, `409` sin guardar nada). Pasa la reserva a `ORDEN_EMITIDA` y registra al funcionario y la fecha y hora. `GET /api/ordenes/{numeroOrden}/pdf` devuelve el PDF (`DocumentosService`, Apache PDFBox); la pantalla lo abre en otra pestaña para imprimirlo. `POST /api/ordenes/{numeroOrden}/correo` manda el PDF adjunto al correo del paciente (`EmailService.enviarOrdenPDF`; `503` si el correo falla; en desarrollo llega a Mailpit). `api.descargar` (front) pide archivos con el token.
- **Campanita (notificaciones de la aplicación):** `GET /api/notificaciones` devuelve las últimas 50 notificaciones del canal `APLICACION` de la persona con sesión (la más nueva primero) y `POST /api/notificaciones/{id}/lectura` marca una como leída (404 si no es suya). El front las muestra en `layout/Campanita.jsx`, en el encabezado, solo para el paciente (los avisos son para pacientes): contador de sin leer, panel desplegable, se actualiza cada 60 s y al volver a la pestaña. Es el único ícono de la interfaz (un SVG simple, sin librería).
- **Criterios de búsqueda:** `CriteriosBusquedaDTO` es compartido por los listados. Todos sus campos son opcionales; cada caso de uso usa los suyos y agrega los que necesite al final del record (hoy, los de CU-20: médico, especialidad, día, consultorio, estado, orden; los de los catálogos: `texto`, `requierePase`, `ubicacion`). En un test se usa `CriteriosBusquedaDTO.vacio()`.
- **Fecha y hora:** toda hora «de ahora» sale del `Clock` de la aplicación (`common/config/FechaConfig`, zona `America/Montevideo`), con `LocalDate.now(reloj)` o `LocalDateTime.now(reloj)`; **no usar `now()` sin reloj**. Así no depende de cómo esté configurado el servidor (el contenedor usa UTC) y las pruebas pueden fijar la hora (ver `ReservaIntegracionTest`).
- **Métodos internos** (sin endpoint): `AuthService.crearCuenta`, `PaseService.tienePaseVigente`, `EmailService`, `PushNotificationService.enviarPush` y los avisos de `NotificacionService`. Los llaman otros servicios.

### Frontend (`frontend/src/`)

- **`routes.jsx` es la fuente única de las pantallas.** Cada entrada define roles, ruta, texto del menú, caso de uso, grupo del menú (opcional) y componente. De ahí salen las rutas de `App.jsx` y el menú de `layout/Layout.jsx`. Las entradas comunes a todos los perfiles van al final, así la primera de cada rol es su pantalla de inicio.
- `features/<área>/` tiene una carpeta por área (`reservas`, `turnos`, `consultas`, `pases`, `anuncios`, `cuenta`, `especialidades`, `pacientes`, `usuarios`, `consultorios`, `estadisticas`, `auth`). Cada pantalla es `NombrePage.jsx`.
- `auth/`: `AuthContext.jsx` guarda rol y nombre del usuario (los devuelve el backend al iniciar sesión) y cierra la sesión; se usa con `useAuth()`. `SoloRol.jsx` protege las rutas por rol. El token de Firebase lo agrega `api.js` en cada llamada.
- `layout/Layout.jsx`: encabezado (logo, nombre, rol, cerrar sesión) y menú lateral con grupos desplegables.
- `services/api.js`: cliente HTTP único (`api.get/post/put/patch/delete`). No usar `fetch` directo en las pantallas.
- `components/`: `Logo`, `PantallaAcceso` (login, registro, recuperar), `Confirmacion`, `CargaMasivaCsv` (pantalla completa de alta masiva por CSV) y `PaginaPendiente`.
- `utils/`: `errores.js` (`comoError`, `soloMensaje`) y `formatos.js` (`hora`, `NOMBRE_DIA`). Las pantallas viejas aún tienen copias locales; las nuevas usan estas.
- `styles/`: CSS global compartido (`variables.css`, `base.css`, `compartidos.css`). Lo propio de cada componente o pantalla va en su `.module.css` (ver «Dónde va el CSS»).

### Autenticación (Firebase)

**CU-1 (inicio de sesión) y CU-2 (cierre) están implementados.** Firebase Authentication se ocupa de las contraseñas, de Google y de los tokens; el backend no guarda contraseñas.

- **Flujo:** el front inicia sesión en Firebase (`signInWithEmailAndPassword` o `signInWithPopup` de Google) y llama a `POST /api/auth/login` con el token. Esa y todas las demás llamadas a `/api` (salvo `/api/health`) pasan por `FiltroAutenticacion`: verifica el token, busca a la persona por `firebase_uid` y exige cuenta `ACTIVO`. Si algo falla, `401` con un mensaje genérico (no revela si la cuenta existe ni su estado).
- **Rol y nombre:** `login` los devuelve (`paciente`, `funcionario`, `medico`, `admin`); el rol sale de en cuál tabla está la persona. El menú se arma en el front con ese rol.
- **`UsuarioActual`** lee al usuario que dejó el filtro. En los controladores se usa `obtenerIdPacienteAutenticado()` (o `Medico`, `Funcionario`, `Administrador`); si la persona no tiene ese rol responde `403`. El **rol de cada endpoint** se declara con `@RolesPermitidos` (ver abajo); los métodos de `UsuarioActual` sirven para obtener el id.
- **Roles por endpoint (`@RolesPermitidos`):** el filtro solo comprueba que haya una sesión válida; qué rol puede usar cada endpoint se declara con la anotación `@RolesPermitidos` y la aplica `InterceptorRoles` (responde `403` si el rol no corresponde). Va en la clase (vale para todos sus endpoints) y/o en el método (reemplaza a la de la clase):

  ```java
  @RolesPermitidos(Administrador.class)                           // en la clase: por defecto, solo el administrador
  public class EspecialidadController {
      @GetMapping
      @RolesPermitidos                                            // sin valores: cualquier persona con sesión
      public List<EspecialidadResponseDTO> listar(...) { ... }

      @RolesPermitidos({Funcionario.class, Medico.class})         // varios roles
      ...
  }
  ```

  `RolesDeEndpointsTest` falla si un endpoint nuevo no tiene `@RolesPermitidos` (en el método o en la clase), así que no se puede olvidar. Los roles se definieron según quién ve cada pantalla en `routes.jsx`. Quedan **abiertos a más de un rol** los que usan varias pantallas: la lista y el detalle de especialidades (cualquier rol), la lista de consultorios (administrador y funcionario, para elegir el consultorio al dar de alta un turno), la consulta de médicos (administrador y funcionario) y la cartelera y las rutas de `auth` (cualquier rol); las notificaciones son solo del paciente. Si un caso de uso necesita otro rol, se cambia la anotación de ese endpoint.
- **Rutas públicas** (sin sesión): `/api/health`, `/api/pacientes/registro/**` (CU-7) y `/api/auth/recuperacion/**` (CU-4). Para agregar otra se edita `FiltroAutenticacion.shouldNotFilter`.
- **Cerrar sesión (CU-2):** `POST /api/auth/logout` revoca las sesiones en Firebase y el front cierra la sesión local. El token ya emitido sigue siendo válido hasta que vence (1 hora); para bloquear a alguien de inmediato, ponerlo `INACTIVO`: el filtro lo rechaza en la siguiente llamada.
- **Registro de pacientes (CU-7):** `POST /api/pacientes/registro` es público. La persona se identifica con la **cédula y el correo que figuran en el padrón** (los dos tienen que coincidir): si no hay coincidencia responde `422` con el mismo mensaje en ambos casos, para no revelar qué datos existen; si ya tiene cuenta, `409`. Con `proveedor: PASSWORD` el backend crea la cuenta en Firebase con ese correo (`AuthService.crearCuenta`; contraseña de al menos 6 caracteres, como exige Firebase). Con `GOOGLE`, el front primero llama a `POST /api/pacientes/registro/verificacion` (comprueba cédula y correo **antes** de abrir la ventana de Google), después inicia sesión con Google y manda el token en `tokenAcceso`; el backend lo verifica (`obtenerIdentidadVerificada`, `503` si no puede) y exige que la cuenta de Google use el mismo correo del padrón. Al final completa `firebase_uid`, `proveedor_acceso` y, si se enviaron, teléfono y domicilio. Si no puede vincular, borra de Firebase la cuenta que se acababa de crear.
- **Quien entra (login) con Google pero no está en la base** recibe el mismo error genérico (queda un usuario huérfano en Firebase, que se puede borrar desde la consola).
- **Tests:** `AutenticacionTest` usa un `ProveedorIdentidad` falso, sin Firebase real.

**Configuración:** los datos públicos de Firebase ya están en `compose.yml` y `.env.example`, y los cuatro usuarios de prueba se cargan solos al arrancar el backend. Para altas CU-34 también se configura SMTP con `MAIL_*` en `.env`. Lo único que hay que hacer es **copiar la clave de servicio** a `secrets/firebase-clave.json`: el archivo se lo pasa el equipo por un canal privado (nunca por Git: la carpeta `secrets/` está ignorada). Sin la clave, el backend arranca pero nadie puede iniciar sesión.

**Usuarios de prueba** (migración repetible `db/seed/R__usuarios_de_prueba.sql`, correo y contraseña en Firebase): `admin@mediturno.com`, `medico@mediturno.com`, `funcionario@mediturno.com` y `paciente@mediturno.com`, con contraseña `admin123`, `medico123`, `funcionario123` y `paciente123`.

**Cambiar el proyecto de Firebase o crear otro usuario de prueba a mano:**

1. Completar los cuatro datos públicos de la app web en `.env` (variables `VITE_FIREBASE_*`; ver `.env.example`).
2. Descargar la **clave de servicio** (consola de Firebase > Configuración del proyecto > Cuentas de servicio > Generar nueva clave privada) y guardarla como `secrets/firebase-clave.json`. Esa carpeta está ignorada por Git: **la clave nunca se sube**. Fuera de contenedores el backend la busca ahí (`../secrets/firebase-clave.json`); se puede cambiar con la variable `FIREBASE_CREDENCIALES`. Sin la clave, el backend arranca pero nadie puede iniciar sesión.
3. **Usuario nuevo de prueba** (no creado mediante CU-7 o CU-34): crear la cuenta en la consola de Firebase (Authentication > Users > Add user), copiar su *User UID* y ejecutar en la base (DBeaver) con ese UID:

```sql
INSERT INTO usuario (correo, estado, proveedor_acceso, fecha_creacion, cedula, nombre, apellido, fecha_nacimiento, firebase_uid)
VALUES ('admin@ejemplo.com', 'ACTIVO', 'PASSWORD', LOCALTIMESTAMP, '1.111.111-1', 'Nombre', 'Apellido', '1980-01-01', 'PEGAR_AQUI_EL_UID');
INSERT INTO administrador (id_usuario) SELECT id_usuario FROM usuario WHERE correo = 'admin@ejemplo.com';
```

### Rehabilitación de turnos (CU-14)

- Integrado desde `feature/CU-14Rehabilitacion-de-un-turno`: el backend rehabilita una suspensión vigente, valida horario, cupos, médico, especialidad, consultorio y superposiciones, reactiva las ocurrencias futuras compatibles y genera las fechas faltantes.
- `POST /api/turnos-semanales/{idTurnoSemanal}/rehabilitacion` acepta cambios opcionales de día, horario, consultorio y cupo. Un cuerpo `{}` conserva la configuración del turno.
- Las pruebas de rehabilitación están en `SuspensionTurnoIntegracionTest`. La pantalla `TurnosSuspendidosPage.jsx` todavía muestra `PaginaPendiente`.

### Pacientes por turno (CU-21)

- La pantalla del médico `features/consultas/PacientesPorTurnoPage.jsx` permite elegir una fecha (hoy en Montevideo al abrir), seleccionar un turno propio y ordenar los pacientes por número, nombre u horario, ascendente o descendente.
- `GET /api/turnos/mios?fecha=AAAA-MM-DD` devuelve las ocurrencias del médico autenticado para esa fecha. Sin fecha usa el `Clock` del backend. Incluye turnos sin pacientes y conserva el estado de cada ocurrencia.
- `GET /api/turnos/{idOcurrenciaTurno}/pacientes` conserva la ruta del diseño: devuelve solo reservas `VIGENTE` u `ORDEN_EMITIDA`, por número, con nombre, hora estimada, estado y número de orden si existe. Un turno ajeno o inexistente responde 404. No se aplica la ventana de reserva de CU-8 ni se ocultan números cuya hora ya pasó.
- Se mantiene `ConsultaService.consultarPacientesPorTurno` y se usa `ReservaResponseDTO`, compartido con CU-8 y CU-15: un solo record con el estado de la reserva en `estado` y constructores para cada caso (los campos que no corresponden quedan en nulo). No requiere migraciones ni dependencias nuevas. `PacientesPorTurnoIntegracionTest` verifica datos, estados, fecha y autorización con PostgreSQL embebido.

### Cancelación de una reserva (CU-9)

- En la web, `MisReservasPage.jsx` muestra las reservas propias de hoy y de fechas próximas, incluidas las canceladas. Es un listado de apoyo para CU-9; CU-10 tiene su búsqueda de historial en el backend, aunque la pantalla todavía está pendiente.
- `GET /api/reservas/mias` devuelve el historial completo del paciente autenticado, incluyendo todos los estados. Acepta filtros opcionales por médico, especialidad, estado y rango de fechas, y orden por fecha, médico, especialidad, estado o número. Un rango invertido, estado u orden inválidos responde `400`; el resultado incluye la orden asociada, si existe.
- `GET /api/reservas/mias/proximas` devuelve los datos de la reserva, si puede cancelarse, el motivo si no puede y la fecha de cancelación si existe. Solo devuelve reservas del paciente autenticado.
- Antes de mostrar la confirmación, `GET /api/reservas/{id}/cancelacion/vista-previa` valida propiedad, estado y plazo. `POST /api/reservas/{id}/cancelacion` vuelve a comprobarlos y responde 204 al cancelar.
- Solo se cancela una reserva `VIGENTE` si faltan **48 horas o más desde ahora hasta el inicio de la ocurrencia del turno**, no hasta la hora estimada del número. Exactamente 48 horas se permite. El reloj es el de Montevideo de la aplicación.
- La operación bloquea primero la ocurrencia y luego la reserva, conserva la fila, registra estado `CANCELADA`, fecha y motivo, y libera el número para CU-8. Reserva ajena o inexistente: 404; estado o plazo inválidos: 409. La vista previa no modifica nada.
- `CancelacionReservaIntegracionTest` cubre el límite, propiedad, estados, registro, reutilización del cupo y concurrencia con PostgreSQL embebido. No requiere migraciones ni nuevas dependencias.

### Historia clínica durante la atención (CU-22)

- Desde `Mis pacientes` se abre `HistoriaClinicaAtencionPage.jsx` con el paciente seleccionado. El menú de historia clínica orienta a elegirlo allí. También se puede abrir antes de registrar una consulta en CU-23.
- `GET /api/consultas/historia-clinica?idPaciente=...` devuelve las consultas previas finalizadas del paciente, incluidas las de otros médicos, por fecha descendente. `GET /api/consultas/historia-clinica/{idConsulta}?idPaciente=...` permite abrir un detalle y vuelve a validar el acceso.
- Regla temporal acordada: el médico solo puede consultar durante **el día del turno en Montevideo**, con una reserva propia del paciente `VIGENTE` o `ORDEN_EMITIDA` respaldada por una orden `EMITIDA`. La ocurrencia debe estar habilitada, dentro de vigencia y sin una suspensión vigente. Una atención pasada, futura, cancelada, anulada, atendida o vencida no habilita el acceso.
- Sin vínculo autorizado se responde 403 sin exponer antecedentes. El detalle de una consulta ajena o inexistente responde 404. No tener antecedentes devuelve una lista vacía. Ambos endpoints son de solo lectura y requieren rol médico.
- `HistoriaClinicaAtencionIntegracionTest` verifica autorización, fecha local, estados, turnos suspendidos, datos de otros pacientes, ordenamiento, consultas de otros médicos y revalidación de detalles. No requiere migraciones ni nuevas dependencias.

### Registro de una consulta (CU-23)

- `POST /api/consultas/{numeroOrden}` (solo médico) registra la consulta realizada con una orden emitida. Cuerpo: `motivo` y `observaciones` (obligatorios, hasta 5000 caracteres) e `indicaciones` (opcional). Responde `201` con `ConsultaResponseDTO`, que también usarán CU-11, CU-22 y CU-26.
- En una sola transacción (`ConsultaService.registrarConsulta`, con bloqueo de la reserva): guarda la consulta en la historia clínica del paciente (la abre si todavía no tenía una; hay una por paciente, y la historia general es el conjunto de sus consultas), pasa la reserva a `ATENDIDA` y la orden a `UTILIZADA`. Una consulta registrada no se puede modificar.
- Errores: `422` si la orden no existe, no es de un turno del médico o no está emitida (mismo mensaje en todos los casos, para no revelar órdenes de otros médicos); `409` si la consulta ya fue registrada; `400` por datos obligatorios (anotaciones del DTO).
- **Aviso al paciente** (`NotificacionService.notificarConsultaRegistrada`), después de confirmar el registro y sin contenido clínico: una notificación en la aplicación (tipo `CONSULTA_REGISTRADA`) y un correo (`EmailService.enviarAvisoHistoriaClinica`). Si el correo falla, queda `FALLIDA` y el registro no se revierte. **Push:** también se manda un aviso push (ver «Notificaciones push»).
- Pantalla: el médico elige al paciente en «Mis pacientes» (CU-21), que ofrece «Registrar consulta» solo para los pacientes con orden emitida; abre `RegistrarConsultaPage` con los datos del paciente (por el estado de la navegación; sin ellos, la pantalla pide elegirlo primero), pide confirmación y registra. Desde «Mis pacientes» y antes de registrar la consulta se puede abrir CU-22 para consultar los antecedentes del paciente durante una atención autorizada.
- `RegistroConsultaIntegracionTest` verifica el registro, los estados, la historia clínica, los errores y los avisos con PostgreSQL embebido.

### Cartelera: consulta y publicación de anuncios (CU-6, CU-17 y CU-18)

- `POST /api/anuncios` (solo funcionario) es **multipart**: la parte `datos` lleva el JSON (`AltaAnuncioDTO`: título, contenido, `fechaInicio`, `fechaFin` y `textoAlternativo`) y la parte `imagen`, el archivo (opcional). El front lo envía con `api.postForm`. El id del funcionario sale de `UsuarioActual` (por eso `publicarAnuncio(idFuncionario, datos, imagen)` recibe más parámetros que en el diseño).
- **Vigencia:** el anuncio queda siempre `PUBLICADO` (no hay borradores). Se ve desde `fechaInicio` (hoy si falta; una fecha futura lo deja programado) hasta `fechaFin` inclusive (sin fecha, no vence). El inicio no puede ser anterior a hoy ni el fin anterior al inicio (`400`).
- **Imagen:** como máximo **una por anuncio**, JPG o PNG (se reconoce por el contenido, no por la extensión), de hasta 5 MB (`mediturno.anuncios.tamano-maximo-bytes`). El archivo se guarda **en la base** (`imagen_anuncio.datos`, migración `V3`; una fila por anuncio) y no en disco ni como ruta, porque en Cloud Run el disco no persiste. Si se cambia el límite, también `spring.servlet.multipart` y `client_max_body_size` en `frontend/nginx.conf`.
- `GET /api/anuncios/imagenes/{idImagen}` (cualquier rol) devuelve el archivo; `404` si no existe o si su anuncio fue eliminado. Como un `<img>` no manda el token, el front la pide con `api.descargar` y la muestra con una URL temporal.
- El seed (`db/datos-prueba`) trae imagen en «Horario de verano» y «Campaña de vacunación»; «Traiga su cédula» no tiene imagen ni fecha de fin (no vence). `AnuncioIntegracionTest` cubre alta, vigencia, límite, formato falso y el endpoint de imágenes.
- **Cartelera (CU-6):** `GET /api/anuncios` (cualquier rol) devuelve los anuncios `PUBLICADO` que se ven hoy (fecha de Montevideo): desde `fechaInicio` hasta `fechaFin`, ambas inclusive; sin fecha de fin no vencen. Van del más nuevo al más viejo según `fechaPublicacion`. Cada uno trae título, contenido, fecha de publicación y, si tiene, los datos de su imagen (sin el archivo, que se pide aparte). Los programados, vencidos y eliminados no aparecen. No tiene paginación. La pantalla `CarteleraPage` muestra las tarjetas y carga la imagen con `ImagenAnuncio` (`api.descargar`). `CarteleraIntegracionTest` lo verifica.

- **Eliminar (CU-18):** `DELETE /api/anuncios/{id}` (solo funcionario; cualquiera puede eliminar cualquier anuncio) **no borra la fila**: pasa el anuncio a `ELIMINADO`, y deja de verse en la cartelera y de entregarse su imagen. Bloquea el anuncio durante la operación (`buscarConLock`). Inexistente `404`; ya eliminado `409`. La pantalla se alimenta de `GET /api/anuncios/gestion` (solo funcionario): todos los anuncios que hubo —también los `ELIMINADO`, que quedan como historial—, el más nuevo primero, con `estado` y `vigencia` (`PROGRAMADO`, `VIGENTE` o `VENCIDO`, calculada con las fechas y el reloj de Montevideo, no se guarda; `AnuncioResponseDTO` la trae siempre). Sin paginación: con muchos anuncios vencidos la lista crece. La pantalla marca los eliminados en rojo, sin botón, y pide confirmación antes de eliminar.

### Gestión de catálogos del administrador (listado, modificación y alta masiva)

Especialidades (CU-28 a CU-30 y CU-43) y consultorios (CU-37 a CU-39 y CU-47) siguen el mismo molde, y pacientes (CU-31 a CU-33 y CU-44) y usuarios internos (CU-34 a CU-36, CU-45 y CU-46) deberían seguirlo. Son solo del administrador; el listado básico de especialidades y de consultorios se abre además a otros roles porque llena listas desplegables. Lo que sigue vale para todos; abajo, solo lo propio de cada uno.

- **Listado y búsqueda (CU-30, CU-39):** `GET /api/<recurso>` con criterios opcionales en la URL (`CriteriosBusquedaDTO`) que se combinan: `texto` (busca en código y nombre, sin distinguir mayúsculas; `%` y `_` se buscan como texto), `estado` (`ACTIVO` o `INACTIVO`), el filtro propio de la entidad, `ordenarPor` (la primera propiedad permitida es la de por defecto) y `descendente`. Un valor inválido responde `400`; sin parámetros devuelve todo. Se implementa con una `Specification` (el repositorio extiende `JpaSpecificationExecutor`) y las piezas de `common/busqueda/FiltrosBusqueda` (`patronContiene`, `interpretarEstado`, `ordenar`). Sin paginación. `GET /api/<recurso>/{codigo}/detalle` devuelve la entidad con sus asociaciones vigentes (`404` si no existe); «vigentes» son los turnos semanales cuya `vigente_hasta` no pasó (vacía o de hoy en adelante), activos o suspendidos.
- **Modificación (CU-43, CU-47):** `PUT /api/<recurso>/{codigo}` con todos los campos editables (el código no cambia; `@Valid` en el DTO). El servicio recibe además el id del administrador (`UsuarioActual`), bloquea la fila (`buscarConLock`) y responde `404` si no existe y `409` por las reglas de la entidad. **Auditoría:** la tabla guarda solo el último cambio (`id_administrador_modificacion` y `fecha_modificacion`; migraciones `V4` y `V5`), y solo si algo cambió: enviar los mismos datos no guarda nada. Inactivar una entidad con turnos vigentes responde `409`; editar una ya inactiva sin reactivarla se permite.
- **Alta masiva por CSV (CU-29, CU-38; CU-32 y CU-35 igual):** dos pasos, multipart con la parte `archivo`: `POST /api/<recurso>/csv/previsualizacion` (no guarda nada) y `POST /api/<recurso>/csv`, que **vuelve a analizar el archivo** y, solo si todas las filas son válidas, crea todo en una transacción; si alguna falla no crea ninguna (`422`; `409` si otra persona registró uno en ese instante). Responde `ResultadoImportacionDTO` (columnas, filas con sus errores y `registrosCreados`). Archivo en UTF-8 (con o sin BOM), separador coma, punto y coma o tabulación, comillas para campos con separadores o saltos de línea, hasta 1000 filas; los encabezados se reconocen sin importar tildes, mayúsculas, espacios ni orden; las filas vacías se ignoran pero cuentan en el número de fila (el encabezado es la fila 1). Cada servicio declara sus `AnalizadorCsv.Columna` y una regla de validación por fila; `common/csv/AnalizadorCsv` hace el resto (encabezados, filas con más valores que columnas, duplicados dentro del archivo con `Repetidos`, el `422` y el `409` con `Analisis.importar`) y `LectorCsv` lee el archivo.
- **Pantallas:** el alta masiva es `components/CargaMasivaCsv` (la página solo pasa título, ruta, nombres y un archivo de ejemplo); el listado muestra filtros, tabla y un panel de detalle arriba de la tabla con las clases compartidas de `compartidos.css`; la modificación se alimenta del listado básico y también se abre desde «Modificar» en el listado, que pasa el código por el estado de la navegación.
- **Tests:** `Listado...`, `Modificar...` y `AltaMasiva...IntegracionTest` de cada entidad (PostgreSQL embebido).

**Especialidades.** Código y nombre únicos (el nombre sin distinguir mayúsculas). Filtro propio: `requierePase`. El detalle trae los médicos activos que la atienden y sus turnos vigentes. El requisito de pase de una modificación solo rige para las reservas nuevas. En el CSV, `código`, `nombre`, `descripción` y `requiere pase` (sí/no) son obligatorias; `estado` es opcional (sin él, activa).

**Consultorios.** Solo el código es único (el nombre puede repetirse). Filtro propio: `ubicacion`, exacta y sin distinguir mayúsculas (la pantalla ofrece las que existen). Cada consultorio del listado trae `cantidadTurnos` (turnos vigentes, el mismo criterio que el detalle y que la regla de inactivación). El detalle trae esos turnos. No se puede cambiar la ubicación mientras haya reservas pendientes (`VIGENTE` u `ORDEN_EMITIDA`) de sus turnos que todavía no terminaron (`ReservaRepository.existenReservasPendientesEnConsultorio`). En el CSV las cinco columnas (`código`, `nombre`, `ubicación`, `descripción` y `estado`) deben estar en el encabezado; la descripción puede quedar vacía, el estado no.

### Notificaciones push

- Los avisos salen **directo por Firebase Cloud Messaging (FCM)**, sin pasar por Expo ni por EAS. `PushNotificationService` guarda el token nativo de FCM de cada dispositivo (`POST/DELETE /api/notificaciones/push`; un token pertenece a la última persona que inició sesión) y envía con `FirebaseMessaging` del SDK de Firebase Admin, usando la misma clave de servicio de la autenticación (`secrets/firebase-clave.json`). Se envía en un hilo aparte (`@Async`, activado en `BackendApplication`) y es de mejor esfuerzo: no lanza excepciones ni revierte nada, y borra los tokens que FCM responde `UNREGISTERED` o `INVALID_ARGUMENT`. Cada push queda registrado en `notificacion` con canal `PUSH` (`ENVIADA` o `FALLIDA`), solo si la persona tiene dispositivos registrados.
- La app móvil obtiene el token nativo de FCM con `Notifications.getDevicePushTokenAsync()` y lo registra al iniciar sesión. Necesita `mobile/google-services.json` (app Android `com.mediturno.mobile` del proyecto de Firebase, versionado en el repositorio: es configuración pública del cliente). Detalle y pruebas en `mobile/README-BACKEND-NOTIFICACIONES_PUSH.md`.
- Avisos: CU-13 («Turno suspendido»), CU-23 («Nueva información en su historia clínica») y CU-8 (`notificarReservaConfirmada`, «Reserva confirmada», tipo `RESERVA_CONFIRMADA`, migración `V2`; se manda al confirmarse la transacción de la reserva).
- Probar: iniciar sesión en la app (acepta el permiso y se registra el token en `token_dispositivo`) y reservar un turno como paciente desde la web o la app; llega «Reserva confirmada». El log del backend muestra `Push enviado ...` o `FCM rechazó el push (...)`.

### Historial de atenciones propias (CU-26)

- `GET /api/consultas/mias` devuelve las consultas registradas por el médico autenticado, ordenadas por fecha descendente por defecto. Acepta filtros opcionales por rango de fechas y especialidad; también permite ordenar por fecha ascendente. Un rango invertido o un orden distinto de fecha responde `400`.
- `HistorialesIntegracionTest` verifica filtros, orden, datos asociados y que cada médico solo vea sus propias atenciones.

### Tareas programadas

El backend tiene `@EnableScheduling` y la clase `common/tareas/TareasProgramadas`. La lógica de cada tarea va en un servicio; la clase solo decide cuándo se ejecuta. Hoy hace una cosa:

- **Generar las ocurrencias de los turnos** (`GeneradorOcurrenciasService`): crea las fechas concretas en las que se atiende (y se puede reservar) para los turnos semanales `ACTIVO`. Corre **todos los días a las 2:00 y también al arrancar el backend**, y genera las fechas **hasta donde se permite reservar**: las próximas **8 semanas**, como dice CU-8 («la fecha seleccionada debe encontrarse dentro de las próximas ocho semanas»). Ese valor es `mediturno.reservas.semanas-adelante`, y lo lee un solo lugar, `DisponibilidadService` (`diasDeReserva()` y `ultimaFechaReservable(hoy)`), que usan tanto CU-8, para validar la fecha elegida, como la tarea y el alta de CU-12, para generar: las fechas generadas y las reservables nunca se desfasan. Si la ventana de reserva cambia, se cambia ahí y todo se ajusta solo. El horario de la tarea es `mediturno.ocurrencias.cron`. Cada ocurrencia copia el cupo y el horario del turno y queda `HABILITADA`. Solo crea las que faltan (es seguro repetirla), respeta la vigencia del turno, no crea las fechas cubiertas por una suspensión sin rehabilitar (también procesa los turnos `SUSPENDIDO`, porque una suspensión temporal deja libres las fechas posteriores) y procesa cada turno en su propia transacción (uno con problemas no frena a los demás). **La fecha de hoy solo se genera si el turno todavía no empezó**: uno en curso o ya terminado no se genera hoy; las fechas de días posteriores se generan siempre. (CU-8 aplica otra regla al ofrecer una fecha de hoy que ya existe, porque la generó la tarea de las 2:00: la ofrece hasta que el turno termina, y solo con los números cuya hora estimada todavía no pasó. Las dos reglas están juntas en `DisponibilidadService`.)
- **CU-12 también genera las fechas al crear el turno**, en la misma transacción (`GeneradorOcurrenciasService.generarOcurrenciasDe`): el turno y sus fechas se guardan juntos o no se guarda ninguno. Así un turno que se carga hoy y todavía no empezó ya se puede reservar hoy, sin esperar a las 2:00. La tarea programada sigue siendo necesaria para que la ventana avance cada día.
- **Estados de los turnos suspendidos** (`ActualizadorEstadosPorSuspensionService`, CU-13): la misma tarea, antes de generar las fechas, pasa a SUSPENDIDO los turnos cuya suspensión ya rige (llegó la fecha desde) y cierra sola las suspensiones temporales que terminaron (quedan con fecha de rehabilitación y el turno vuelve a ACTIVO). Una suspensión registrada **no cambia el estado del turno hasta que llega su fecha desde**; mientras tanto el turno y sus fechas anteriores siguen igual. Lo que decide si una fecha se puede reservar (o generar) son **las fechas que cubre cada suspensión sin rehabilitar** (`DisponibilidadService`, `GeneradorOcurrenciasService`), no el estado: por eso un turno con una suspensión temporal sigue reservable después de que ésta termina.
- **Faltan** (se agregan igual, con otro método `@Scheduled`): marcar las reservas vencidas y enviar los recordatorios.
- La hora sale del `Clock` de la aplicación (zona `America/Montevideo`), no de la configuración del servidor.

## Base de datos y migraciones (Flyway)

La estructura de la base **no la crea Hibernate**: la crean scripts SQL versionados que ejecuta **Flyway** cada vez que arranca el backend.

- Los scripts están en `backend/src/main/resources/db/migration/` y se llaman `V<número>__<descripcion>.sql` (con **dos** guiones bajos). Ejemplo: `V1__crear_tablas.sql`, `V2__agrega_vigencia_a_pase.sql`.
- Al arrancar, Flyway ejecuta en orden los scripts que todavía no se aplicaron en esa base y los anota en la tabla `flyway_schema_history`, junto con una huella (*checksum*) de su contenido.
- `spring.jpa.hibernate.ddl-auto` está en **`validate`**: Hibernate no crea ni modifica nada, solo comprueba que cada entidad coincida con su tabla. Si no coincide, el backend **no arranca** y el error dice qué falta (`Schema-validation: missing column [...]`). Es una ventaja: los desajustes se ven al instante.
- Para arrancar, el backend necesita PostgreSQL disponible (con `podman compose up -d postgres` alcanza).
- **Nombres:** tablas y columnas en `snake_case` (`turno_semanal`, `id_turno_semanal`). En las entidades se escribe en camelCase (`idTurnoSemanal`) y Spring lo traduce solo. Si el nombre de la tabla no sale igual, se indica con `@Table(name = "turno_semanal")`.

### Modelo de datos

La migración `V1__crear_tablas.sql` crea las 20 tablas del modelo. Hay una entidad JPA por tabla (en `<paquete>/model/`) y un repositorio vacío por entidad (en `<paquete>/repository/`). `medico_especialidad` es la tabla intermedia de `Medico.especialidades`, por eso no tiene entidad propia.

- **Usuarios:** `Usuario` es una clase abstracta y `Paciente`, `Funcionario`, `Medico` y `Administrador` extienden de ella (herencia `JOINED`: una tabla por tipo). **No hay columna `rol`:** el tipo de una persona sale de en cuál de las cuatro tablas está su id. Cada persona tiene un solo rol.
- **Autenticación (Firebase):** no se guardan contraseñas. `firebase_uid` es único y puede estar vacío: vacío significa que la persona está en el padrón pero todavía no creó su cuenta (CU-7).
- **Estados:** son `enum` de Java guardados como texto, y la base valida los valores con un `CHECK`:

| Enum | Valores |
|---|---|
| `EstadoActivacion` (usuario, especialidad, consultorio) | `ACTIVO`, `INACTIVO` |
| `ProveedorAcceso` | `PASSWORD`, `GOOGLE` |
| `EstadoTurnoSemanal` | `ACTIVO`, `SUSPENDIDO` |
| `TipoSuspension` | `TEMPORAL`, `PERMANENTE` |
| `DiaSemana` | `LUNES` … `DOMINGO` |
| `EstadoOcurrenciaTurno` | `HABILITADA`, `SUSPENDIDA`, `FINALIZADA` |
| `EstadoReserva` | `VIGENTE`, `ORDEN_EMITIDA`, `ATENDIDA`, `CANCELADA`, `ANULADA`, `VENCIDA` |
| `EstadoOrden` | `EMITIDA`, `UTILIZADA` |
| `EstadoPase` | `VIGENTE`, `USADO`, `VENCIDO` |
| `EstadoAnuncio` | `PUBLICADO`, `ELIMINADO` |
| `EstadoNotificacion` | `PENDIENTE`, `ENVIADA`, `FALLIDA`, `LEIDA` |
| `TipoNotificacion` | `RECORDATORIO_CONSULTA`, `SUSPENSION_TURNO`, `CONSULTA_REGISTRADA`, `RESERVA_CONFIRMADA` (migración `V2`) |
| `CanalNotificacion` | `EMAIL`, `PUSH`, `APLICACION` |

- **Flujo de estados de una reserva:** `VIGENTE` → `ORDEN_EMITIDA` (CU-16) → `ATENDIDA` (CU-23). Desde `VIGENTE`: `CANCELADA` (el paciente, CU-9) o `ANULADA` (suspensión del turno, CU-13). Si pasa la fecha sin atención, `VENCIDA`.
- **Reglas que ya impone la base de datos:**
  - Un número de una ocurrencia solo puede estar en una reserva que lo conserve; las `CANCELADA` y `ANULADA` lo liberan (`uq_reserva_numero_ocupado`).
  - Una orden por reserva, y una consulta por orden y por reserva.
  - Una ocurrencia por turno y fecha.
  - `hora_fin` posterior a `hora_inicio`; `cupo_maximo` y `cupos` de al menos 1.
  - El nombre de la especialidad es único.
  - Una suspensión temporal exige fecha de fin.
- **`ocurrencia_turno` guarda una copia** del cupo y del horario del turno al crearse. Si luego se cambia el turno, las ocurrencias ya creadas (y sus reservas) no se modifican.
- **Cupos libres:** no se guardan. Se calculan como `cupos` menos las reservas que conservan su número.
- **Vigencia de los pases:** el enum `VigenciaPase` define los 60 días; un pase vence 60 días después de `fecha_emision`.
- **Tipos:** los textos largos (antecedentes, motivo, indicaciones, contenido del anuncio) son `TEXT`; las fechas con hora, `TIMESTAMP` (`LocalDateTime`); los horarios, `TIME`.
- **Prueba automática:** `EsquemaBaseDeDatosTest` levanta un PostgreSQL embebido (no necesita Podman; la primera vez descarga los binarios), ejecuta las migraciones, comprueba que cada entidad coincida con su tabla y verifica las reglas más importantes. Se ejecuta con `./mvnw test`.

### Datos de prueba (seed)

Para probar los casos de uso sin cargar datos a mano hay dos scripts de población:

- `db/seed/R__usuarios_de_prueba.sql`: los cuatro usuarios con cuenta de Firebase (ver «Usuarios de prueba»).
- `db/datos-prueba/R__zz_datos_de_prueba.sql`: todo lo demás (especialidades, consultorios, médicos, pacientes, turnos, ocurrencias, reservas, historia clínica, cartelera). Reutiliza a esas cuatro personas.

Cómo se cargan:

- **Solo en desarrollo.** `db/datos-prueba` se agrega a las ubicaciones de Flyway únicamente con el perfil `dev` de Spring (`application-dev.yaml`). `compose.yml` ya activa ese perfil, así que con Podman los datos aparecen solos al levantar el backend con la base vacía. **En producción no se activa el perfil `dev`.**
- **Fuera de contenedores**, activar el perfil al arrancar el backend: `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` (PowerShell: `$env:SPRING_PROFILES_ACTIVE = "dev"` y después `.\mvnw.cmd spring-boot:run`).
- **Es una migración repetible** (`R__`). El nombre empieza con `zz` para que corra después de `R__usuarios_de_prueba.sql` (Flyway ordena las repetibles por nombre). Si la base ya tiene otros usuarios (además de los cuatro de prueba), especialidades, turnos o reservas, **no hace nada** y lo avisa en el log del backend (nunca falla el arranque).
- **Empezar de cero** (borra todos los datos locales): `podman compose down -v` y volver a levantar.
- **Las fechas son relativas al día en que se cargan** (en Montevideo, como el `Clock` de la aplicación, aunque la base esté en UTC): hay historia de las últimas 13 semanas, las ocurrencias cubren las próximas 8, y hay tres turnos hoy y uno mañana. Pasadas unas semanas conviene recargar.
- **Si el script cambió y su base ya tenía datos**, Flyway lo vuelve a ejecutar pero no carga nada (lo protege el bloque contra duplicados): para ver los datos nuevos, `podman compose down -v`.

**Personas de prueba:**

| Rol | Personas |
|---|---|
| Administrador | Carlos Méndez (`admin@mediturno.com`) |
| Funcionarios | Juan Pérez (`funcionario@mediturno.com`), Marta Silva (`funcionario2@mediturno.test`) |
| Médicos | Laura Gómez (`medico@mediturno.com`; Cardiología y Clínica Médica), Diego Rossi (Traumatología), Sofía Acosta (Pediatría), Andrés Cabrera (Clínica Médica), Valeria Núñez (Dermatología), Ricardo Pereira (**inactivo**) |
| Pacientes | Ana López (`paciente@mediturno.com`), Luis Díaz, María Fernández, Pedro Castro (sin teléfono ni domicilio), Lucía Ramos, Jorge Benítez, Valentina Olivera, Martín Sosa, Carolina Techera, Federico Pintos, Gabriela Rodríguez, Tomás y Camila Ferreira (niños), Joaquín Viera (sin historia clínica), Rosa Álvarez (**inactiva**) |

Solo las cuatro personas `@mediturno.com` tienen cuenta de Firebase; las `@mediturno.test` están «en el padrón, sin cuenta» (sirven para probar el registro, CU-7). Lo pensado para probar a mano está armado sobre esas cuatro cuentas, sobre todo **Ana López** y la **Dra. Gómez**.

**Turnos** (los días fijos no cambian; los de hoy y mañana dependen del día de carga):

| Turno | Para qué sirve |
|---|---|
| Cardiología lunes 9–12 y Clínica Médica miércoles 14–17 (Dra. Gómez), Pediatría martes 8–11 (Dra. Acosta), Traumatología jueves 14–17 (Dr. Rossi) | Turnos activos con historia y reservas |
| Traumatología viernes 9–12 (Dr. Rossi) | **Suspensión temporal** que rige desde hace una semana y termina en dos (fechas `SUSPENDIDA`; las posteriores se pueden reservar) |
| Pediatría sábado 9–12 (Dra. Acosta) | **Suspensión permanente** |
| Dermatología martes 14–17 (Dra. Núñez) | Turno nuevo, vigente desde hoy y solo por 4 semanas (`vigente_hasta`) |
| Hoy 8–12 y 13–17 (Dr. Cabrera, Clínica Médica) y hoy 18–21 (Dra. Gómez, Cardiología) | La atención del día. Juntos cubren de 7 a 21 la ventana para emitir órdenes |
| Mañana 18–20 (Dr. Cabrera) | Reservas a menos de 48 horas |

Ningún médico ni consultorio tiene dos turnos superpuestos.

**Qué se puede probar con estos datos:**

| Caso de uso | Con qué datos |
|---|---|
| CU-8 reserva | Fechas con números libres y ocupados. El turno de la tarde del Dr. Cabrera de **la semana que viene está sin cupos**. **Traumatología y Dermatología requieren pase**: Ana tiene un pase vigente a Traumatología, pero el de Dermatología **venció** (puede probar el rechazo con el turno de la Dra. Núñez) |
| CU-9 cancelación | Ana: Cardiología a 3 días o más (se puede cancelar), **mañana** (a menos de 48 horas: no se puede) y la de hoy con orden emitida (no vigente) |
| CU-10 historial | Ana tiene reservas en los seis estados (atendidas, vencida, cancelada, con orden, vigentes y anulada) |
| CU-11, CU-22 | Ana: cinco atenciones con la Dra. Gómez, el Dr. Rossi y el Dr. Cabrera, en Clínica Médica, Traumatología y Cardiología. Los demás pacientes tienen historial generado |
| CU-12, CU-20 | Once turnos semanales: activos, suspendidos (temporal y permanente), con fin de vigencia, de hoy y de mañana |
| CU-13, CU-14 | Las dos suspensiones (una con la reserva anulada de Ana, otra con la de Camila Ferreira). Para probar una suspensión nueva, cualquier turno activo tiene reservas futuras |
| CU-15, CU-16 | Reservas vigentes de hoy en los tres turnos del día (Lucía, María, Luis, Gabriela, Pedro): a cualquier hora entre las 7 y las 21 hay una a la que se le puede emitir la orden. Ana y Joaquín ya tienen la orden emitida |
| CU-21, CU-22, CU-23, CU-24 | El turno de **hoy de 18 a 21 de la Dra. Gómez**: Ana (orden emitida, con antecedentes), Joaquín (orden emitida, **sin historia clínica**: CU-22 vacía y CU-23 se la abre), Pedro (vigente) y una cancelada. Al registrar la consulta de Ana se puede emitir un pase (por ejemplo, a Dermatología) |
| CU-26 | La Dra. Gómez tiene muchas atenciones propias en Cardiología y Clínica Médica |
| Campanita | Ana tiene avisos sin leer (reservas confirmadas y una suspensión) y otros leídos. El correo a Camila figura `FALLIDA` |
| CU-28, CU-34, CU-37, CU-39, CU-47 | Neurología (especialidad inactiva), el consultorio 4 (inactivo) y un médico inactivo. Los consultorios con turnos vigentes no se pueden inactivar, y los de turnos con reservas futuras no pueden cambiar de ubicación |
| Estadísticas (CU-40 a CU-42) | 13 semanas de historia: Cardiología con más demanda que Clínica Médica, Pediatría y Traumatología con poca |
| Cartelera | Tres anuncios vigentes (dos con imagen —«Ventanilla» y «Vacunación»— y uno sin imagen ni fecha de fin), uno programado (empieza en unos días), uno vencido y uno eliminado |

**Cómo mantener el script:**

- Lo hecho a mano (los casos de Ana, de la Dra. Gómez y de hoy) va arriba; después, dos bucles generan con una regla fija el historial de los demás pacientes y las reservas de las próximas semanas, saltando los números y los pacientes que ya están en esa fecha.
- Se puede editar, pero hay que respetar el bloque que lo protege contra duplicados y **no usar ids fijos**: se obtienen con `RETURNING ... INTO`, porque los ids son automáticos. Las fechas salen de `v_hoy` y `v_ahora` (Montevideo), nunca de `CURRENT_DATE`.
- Si una migración nueva cambia una tabla (por ejemplo, agrega una columna obligatoria), hay que actualizar también el script de datos.
- `DatosDePruebaTest` carga el script sobre una base real, comprueba lo que contiene, que sea coherente con las reglas (cada atención con su orden y su consulta, reservas con pase donde se exige, sin turnos superpuestos, sin reservas pendientes en fechas pasadas) y que volver a ejecutarlo no duplica nada. Si el script deja de ser válido, ese test falla.

### Pasos para cambiar la base

1. **Crear un script nuevo** con el número siguiente al último que haya en `db/migration` de `main`. Ejemplo: `V3__agrega_motivo_a_suspension_turno.sql` con `ALTER TABLE suspension_turno ADD COLUMN motivo VARCHAR(255);`.
2. **Actualizar la entidad JPA** para que coincida (campo, tipo y si admite nulo).
3. **Reiniciar el backend** (o `podman compose up -d --build backend`). Flyway aplica el script y Hibernate valida.
4. Si el backend no arranca con `Schema-validation`, la entidad y el script no coinciden: corregir cualquiera de los dos.
5. **Subir el script y la entidad en el mismo Pull Request.** Antes de abrirlo, mirar si alguien integró en `main` otro script con tu mismo número; si pasó, renombrar el tuyo al siguiente.

### Reglas

- **Nunca editar un script que ya se aplicó** (que esté en `main` o que otro compañero haya ejecutado). Flyway detecta el cambio y no arranca (`Migration checksum mismatch`). Todo cambio nuevo va en un script nuevo.
- **Excepción:** un script que existe solo en tu rama y que nadie más ejecutó se puede editar. Después hay que reiniciar tu base local: `podman compose down -v` (borra los datos) y volver a arrancar.
- Un script por cambio lógico, corto y legible en una revisión.
- No usar `ddl-auto: update` ni `create`, y no modificar el esquema a mano con un cliente SQL: si hace falta un cambio, se escribe el script.
- Los datos de prueba (*seed*) **no** van en migraciones `V`: van en `db/seed/` y `db/datos-prueba/` (ver «Datos de prueba»).

## Cómo implementar un caso de uso

Ejemplo: CU-28, alta de especialidades.

1. **Rama** desde `main`: `git switch main && git pull && git switch -c cu-28-alta-especialidad`.
2. **Leer** el caso de uso y su diagrama de secuencia (documentos del equipo): pasos, flujos alternativos y errores.
3. **Backend** (paquete `especialidades`):
   - Completar los campos de los DTO que usa el caso de uso (`AltaEspecialidadDTO`, `EspecialidadResponseDTO`). Los comentarios de cada DTO resumen lo que dicen los documentos.
   - Crear la entidad en `model/` y el repositorio en `repository/`. Si el caso de uso necesita cambiar la base, agregar la migración (ver «Base de datos y migraciones»).
   - En `EspecialidadService.altaEspecialidad`, reemplazar el `throw new NoImplementadoException()` por la lógica: validar, buscar duplicados, guardar y devolver el DTO.
   - Cada flujo alternativo del caso de uso es un error con su código del diagrama: `throw new ResponseStatusException(HttpStatus.CONFLICT, "El código ya está en uso")`.
   - Revisar que los endpoints del caso de uso tengan el `@RolesPermitidos` correcto (ya están puestos según `routes.jsx`).
   - Validar el cuerpo con `@Valid` y anotaciones de `jakarta.validation` en el DTO.
   - Si la operación toca varias tablas o necesita consistencia (reservar, cancelar, suspender, emitir orden), usar `@Transactional`.
   - Escribir tests del servicio.
4. **Frontend** (`features/especialidades/AltaEspecialidadPage.jsx`):
   - Reemplazar `PaginaPendiente` por la pantalla real (el mock de referencia está en la rama `mocks`).
   - Llamar al backend con `api.post('/especialidades', datos)` y mostrar los errores (`error.mensaje`, con los datos inválidos en `error.detalles`).
5. **Probar** de punta a punta, correr `./mvnw test`, `npm run lint` y `npm run build`.
6. **Pull Request** hacia `main`.

## Guía de diseño

El aspecto de la aplicación es **sobrio y profesional**: azul y blanco, con mostaza solo para resaltar. Toda pantalla nueva tiene que verse como las que ya existen. Antes de crear un estilo nuevo, reutilizar las variables y las piezas compartidas de `frontend/src/styles/`. Pantallas de referencia: `layout/Layout.jsx`, `features/auth/LoginPage.jsx` y `components/PaginaPendiente.jsx`.

### Dónde va el CSS

- **Global, en `src/styles/`** (cambia poco y lo tocan pocas personas):
  - `variables.css`: colores, sombra y demás variables.
  - `base.css`: estilos de los elementos HTML (tipografía, enlaces, formularios y botones).
  - `compartidos.css`: piezas que usan varias pantallas (`.panel`, `.tarjeta`, `.pendiente`, `.enlace-volver`, `.tabla-scroll` y las de los listados con detalle: `.listado-*`, `.detalle-*`, `.estado-activo`, `.estado-inactivo`, `.celda-botones`, `.solo-lectores`).
- **Propio de un componente o pantalla: un CSS Module** junto al componente, con el mismo nombre (`AltaTurnoPage.jsx` + `AltaTurnoPage.module.css`). Las clases de un módulo son **locales a ese archivo**, así que no pueden chocar con las de otro (es el equivalente a los estilos de componente de Angular). Se usa así:

  ```jsx
  import estilos from './AltaTurnoPage.module.css'

  <div className={estilos.grilla}>...</div>
  ```

- **Reglas:** no agregar reglas específicas de una pantalla a los CSS globales. Si algo se repite en tres o más pantallas, pasarlo a `compartidos.css`. Los colores y sombras siempre salen de las variables. Los CSS Modules no necesitan configuración: Vite los soporta si el archivo termina en `.module.css`.
- **Con `NavLink`** (menús): el enlace activo recibe una clase global `active` que un módulo no puede ver; se resuelve con la función de `className` (ver `layout/Layout.jsx`).

### Colores (variables en `src/styles/variables.css`)

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
| `--error` / `--error-claro` | `#b42318` / `#fdecea` | mensajes de error (`.mensaje-error`): texto y borde / fondo |
| `--exito` / `--exito-claro` | `#1e7b4a` / `#e6f4ec` | mensajes de éxito (`.mensaje-exito`): texto y borde / fondo |

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
- **Formularios:** el `<label>` envuelve al campo (`<label>Nombre<input /></label>`). Los campos ocupan todo el ancho y tienen foco azul visible. Un campo que no corresponde en ese momento no se oculta: se deja `disabled` y se ve en gris (ver «Hasta» en `TurnosSemanalesPage`).
- **Aviso:** `.pendiente` (fondo mostaza claro con barra mostaza) para avisos informativos. **Error:** `.mensaje-error` (fondo rojo claro con barra roja). **Éxito:** `.mensaje-exito` (fondo verde claro con barra verde).
- **Tabla:** clase `.tabla` dentro de `<div className="tabla-scroll">` (ver `TurnosSemanalesPage`).
- **Panel y tarjeta:** `.panel` es el contenedor de una pantalla con sesión; `.tarjeta` es la de las pantallas de acceso. Ambas son clases globales de `compartidos.css`.
- **Logo:** componente `Logo`. `variante="oscuro"` sobre fondo claro; `variante="claro"` sobre azul (el logo va dentro de un círculo blanco, porque el logo es azul y se perdería). Imagen en `src/assets/logo.png`, ícono de pestaña en `public/favicon.png`.
- **Usuario en el encabezado:** avatar con iniciales, nombre y etiqueta del rol (mostaza).

### Todavía no existe (definirlo con este mismo estilo)

Paginación y estados de carga. Quien primero los necesite los agrega a `styles/compartidos.css` (si los van a usar varias pantallas) usando las variables de arriba, para que los reutilicen los demás, y actualiza esta guía. Ya existen los mensajes de error y de éxito (`.mensaje-error`, `.mensaje-exito`) y la ventana de confirmación (componente `Confirmacion`, clase `.modal`).

### Responsive y accesibilidad

- **Responsive:** el punto de corte es **800 px** (celular y tablet vertical). Debajo de ese ancho:
  - El encabezado muestra solo el logo y un botón **Menú**. Nombre, rol y «Cerrar sesión» pasan al principio del menú, que está oculto hasta que se abre y se cierra al elegir una opción (`layout/Layout.jsx`).
  - El login se apila (panel de marca arriba, formulario abajo); en celulares muy chicos (480 px) el panel se compacta.
  - Los títulos bajan a 22 px y los campos a 16 px (con menos, algunos celulares agrandan la pantalla al tocarlos).
- **Qué hacer en cada pantalla nueva:**
  - Probarla con el navegador en modo celular (320, 375 y 768 px) y comprobar que **no aparece desplazamiento horizontal de toda la página**.
  - Las **tablas anchas** van dentro de `<div className="tabla-scroll">` para que se desplacen de costado dentro de su contenedor.
  - Usar `min-width: 0` en un hijo de flex o grid que pueda desbordar. Los textos largos sin espacios (rutas, correos) ya se cortan solos (`overflow-wrap`).
  - El CSS específico para pantallas chicas se escribe con `@media (max-width: 800px)` dentro del `.module.css` de la pantalla. No usar anchos fijos en píxeles para contenedores.
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
- El id del usuario autenticado sale de `UsuarioActual`, nunca del cliente. **Todo endpoint lleva `@RolesPermitidos`** con los roles que lo pueden usar (ver «Autenticación (Firebase)»).
- Las validaciones que el diagrama marca como "operaciones internas del método" son métodos privados del servicio, no métodos públicos nuevos.
- **Dónde va cada validación:** los campos obligatorios, los largos y los tipos simples se validan con anotaciones en el DTO y `@Valid` en el controlador (el resultado es el mismo `400` con la lista de campos). Lo que necesita lógica o consultar la base (formato de una cédula, unicidad, reglas de negocio) va en métodos privados del servicio. Un cuerpo ilegible (JSON roto, un valor que no es del tipo del campo) también responde `400`: lo resuelve `GlobalExceptionHandler`.
- **Repositorios con bloqueo:** el método que busca una fila y la bloquea (`@Lock(PESSIMISTIC_WRITE)`, un `SELECT ... FOR UPDATE`) se llama `buscarConLock`. Hay que llamarlo dentro de una transacción (`@Transactional`); el bloqueo dura hasta que termina.
- **Nombre de una persona:** en DTO, listados y documentos se usa `usuario.nombreCompleto()` («Apellido, Nombre»); no armarlo a mano. Se exceptúan los textos en prosa (el saludo del encabezado y los avisos), donde se lee «Nombre Apellido».
- **Conversión a DTO:** los métodos que arman un DTO de respuesta se llaman `convertirAXxxResponseDTO` (por ejemplo, `convertirAPacienteResponseDTO`).
- Los cambios en la base van **siempre** con migraciones de Flyway (ver «Base de datos y migraciones»). Hibernate solo valida.

### Frontend

- JavaScript con JSX. Componentes en `PascalCase`; las pantallas terminan en `Page`.
- **CSS:** global solo para variables, estilos base y piezas compartidas (`src/styles/`); lo demás, en un **CSS Module** por componente o pantalla (`Nombre.module.css`). Ver «Dónde va el CSS». Sin librerías de interfaz.
- Aspecto visual: seguir la **Guía de diseño** de este archivo.
- Agregar una pantalla = crear el componente en `features/<área>/` y sumar una línea en `routes.jsx`.
- No usar `fetch` directo: usar `api`.

## Reglas de negocio ya acordadas

- **Hora estimada de atención:** duración por consulta = (hora fin − hora inicio) ÷ máximo de pacientes del turno. La hora estimada del número N = hora inicio + (N − 1) × duración. Se muestra siempre como estimada (`~10:20`). *Definida por el equipo; falta incorporarla a los documentos.*
- **Reserva (CU-8):** el paciente elige el turno y después su número entre los libres. Si otro lo ocupa antes de confirmar, se informa y puede elegir otro.
- **Cancelación (CU-9):** solo si la reserva está vigente y faltan **48 horas o más para el inicio del turno**, inclusive.
- **Orden (CU-16):** se emite el día del turno, desde **1 hora antes** de que empieza hasta que **termina** (hora de fin del turno).
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
