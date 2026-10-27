# Notificaciones push: plan de acción para el equipo de backend

Este documento define las tareas del backend para que las notificaciones push de la
app mobile (CU-8, CU-13 y CU-23) queden operativas.

**Contexto:** la app mobile ya está lista y no requiere cambios adicionales. Desde el
último inicio de sesión define los permisos, obtiene el token del dispositivo (Expo Push
Token) y lo registra en `POST /api/notificaciones/push`; al cerrar sesión lo elimina vía
`DELETE /api/notificaciones/push`. Mientras los endpoints respondan 501, esas llamadas
fallan silenciosamente y se reintentan en el próximo inicio de sesión.

**Objetivo:** implementar la lógica de envío en el backend siguiendo el diseño ya
definido (autenticado y acotado por usuario).

## 1. Implementar `PushNotificationService.registrar(idUsuario, token)`

La entidad `TokenDispositivo` ya existe (tabla `token_dispositivo`: token único, FK a
usuario y `fechaRegistro`):

- Buscar por token; si no existe, crearlo asociado al `idUsuario` con
  `fechaRegistro = ahora`.
- Si el token ya existe y pertenece a otro usuario, reasignarlo: un dispositivo
  pertenece a la última persona que inició sesión en él.

## 2. Implementar `PushNotificationService.eliminar(idUsuario)`

Borrar físicamente los tokens del usuario en la tabla `token_dispositivo` (el modelo no
contempla activo/inactivo; borrado directo).

## 3. Implementar `PushNotificationService.enviarPush(idUsuario, titulo, mensaje[, datos])`

- Recuperar los tokens del usuario y armar un array JSON con un objeto por token:
  `{ "to": token, "title": titulo, "body": mensaje, "sound": "default", "data": {...} }`.
- Hacer un POST a `https://exp.host/--/api/v2/push/send` con los headers
  `Accept: application/json`, `Accept-encoding: gzip, deflate` y
  `Content-Type: application/json`.
- Best-effort: nunca lanzar excepción ni revertir la operación que lo originó (mismo
  criterio que el envío de correos); registrar el resultado en el log.
- En la respuesta, si un token responde `DeviceNotRegistered`, borrarlo de la tabla
  (limpieza de dispositivos desinstalados).
- Para producción: agregar el header `Authorization: Bearer <EXPO_ACCESS_TOKEN>` con
  `EXPO_ACCESS_TOKEN` como variable de entorno (gestionar por el canal privado de
  `secrets/`, nunca por Git; sin ese header la API funciona pero con límites por IP).

## 4. Enganchar los avisos en `NotificacionService` (que ya guarda la notificación de la aplicación y envía el correo)

- **CU-13** (`notificarSuspensionTurno`): después de guardar la APLICACION, llamar a
  `enviarPush` con el mismo mensaje ya armado («Su reserva ... fue anulada / no se
  atenderá porque el turno fue suspendido...»). Título: «Turno suspendido».
- **CU-23** (`notificarConsultaRegistrada`): ídem, con el mensaje ya armado sin
  contenido clínico («Se incorporó nueva información a su historia clínica...»).
  Título: «Nueva información en su historia clínica».
- Opcional: guardar también un registro por canal `PUSH` (el enum `CanalNotificacion`
  ya lo tiene) con estado `ENVIADA`/`FALLIDA`, para auditar entregas igual que con el
  canal `EMAIL`.

## 5. CU-8: aviso de reserva creada (hoy no existe ningún aviso)

Agregar tras la creación de la reserva (`ReservaService`) un método nuevo en
`NotificacionService` (por ejemplo `notificarReservaConfirmada(idReserva)`):

- Guardar una notificación APLICACION con el mensaje del enunciado: identificador de la
  reserva, número y hora estimada de atención (la hora estimada ya se calcula en
  `ReservaResponseDTO` para el front).
- Enviar el push equivalente (título: «Reserva confirmada»).

## 6. Mantener el flujo seguro ya definido

`POST/DELETE /api/notificaciones/push` autenticados con Firebase, con el token asociado
al `UsuarioActual`. No admitir registro anónimo de tokens ni envíos masivos: el push es
un aviso personal del paciente.

## 7. Mantenimiento de tokens huérfanos

Si el paciente desinstala la app sin cerrar sesión, el token queda en la base.
Sugerencia: tarea programada en `TareasProgramadas` (junto a las existentes) que limpie
tokens con más de X meses sin actualizarse, además de la limpieza por
`DeviceNotRegistered` del punto 3.

## 8. Criterio de despliegue

Mientras los endpoints respondan 501, la app funciona igual (las llamadas fallan en
silencio y se reintentan en el próximo inicio de sesión). Con los puntos 1 a 5
liberados, los avisos push de los 3 CUs llegan sin ningún cambio adicional en la app.

**Importante:** todo lo relacionado a Firebase (autenticación y notificaciones) se
gestiona sobre el proyecto Firebase ya existente en main (`mediturno-auth-db915`).
No se deben generar credenciales sobre ningún otro proyecto de Firebase ajeno
al repo.

## Proyecto Firebase para las credenciales de push

- Se usan las credenciales del **mismo proyecto Firebase que ya tiene el main para
  autenticación** (`mediturno-auth-db915`). Para eso, en Firebase Console se registra la
  app Android de package `com.mediturno.mobile` en ese proyecto y se descarga su
  `google-services.json`.
- Ese archivo se coloca en `mobile/google-services.json`: el `app.json` ya lo referencia
  (`android.googleServicesFile`) y queda embebido en el binario al compilar. Sin este
  archivo, la app no inicializa Firebase en Android y el token de push no se puede
  generar (en desarrollo ni en producción).
- Este requisito es del lado mobile; el backend no consume ese archivo (su acceso a
  Firebase es por la clave de servicio de `secrets/`).
- El identificador de proyecto Expo (`extra.eas.projectId` en `app.json`) es de EAS y es
  independiente de lo anterior; se mantiene el ya configurado.

## Notas para las pruebas

- El push solo funciona en un development build (`npx expo run:android`), no en Expo Go.
- El proyecto EAS para las notificaciones ya está configurado: el `projectId` figura en
  `mobile/app.json` (`extra.eas.projectId`).
- Prueba manual del canal sin backend: insertando la fila del token en
  `token_dispositivo` (o copiando el token desde los logs de desarrollo), un POST directo
  a `https://exp.host/--/api/v2/push/send` con ese token debe recibir la notificación en
  el dispositivo.
