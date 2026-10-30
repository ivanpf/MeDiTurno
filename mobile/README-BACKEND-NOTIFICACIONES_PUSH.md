# Notificaciones push

Cómo funcionan los avisos push de la app mobile (CU-8, CU-13 y CU-23). Los envía el backend
directo por **Firebase Cloud Messaging (FCM)**, con el mismo proyecto de Firebase de la
autenticación (`mediturno-auth-db915`).

## Cómo funciona

```
App (Android) → token de FCM → POST /api/notificaciones/push → tabla token_dispositivo
Backend (evento del caso de uso) → PushNotificationService → FirebaseMessaging → FCM → dispositivo
```

- **App:** al iniciar sesión pide el permiso de notificaciones, obtiene el token nativo de FCM
  (`Notifications.getDevicePushTokenAsync()`) y lo registra en `POST /api/notificaciones/push`.
  Al cerrar sesión lo elimina con `DELETE /api/notificaciones/push`. Si el registro falla, la
  app sigue funcionando y se reintenta en el próximo inicio de sesión (el error queda en el log
  de la app con el prefijo `[push]`).
- **Backend:** `PushNotificationService` guarda los tokens en `token_dispositivo` y envía con
  `FirebaseMessaging` (Firebase Admin SDK), usando la clave de servicio de
  `secrets/firebase-clave.json`. No necesita ninguna variable de entorno ni credencial adicional.

## Qué hace el backend

### `PushNotificationService`

- `registrar(idUsuario, token)`: crea el token asociado al usuario con `fechaRegistro = ahora`.
  Si el token ya existía y era de otra persona, lo reasigna: un dispositivo pertenece a la
  última persona que inició sesión en él. Un token vacío responde `400`.
- `eliminar(idUsuario)`: borra los tokens del usuario.
- `enviarYRegistrar(idNotificacion, idUsuario, titulo, mensaje)`: envía el aviso a todos los
  dispositivos del usuario con prioridad alta y el canal de notificaciones `default` de la app.
  Corre en un hilo aparte (`@Async`), así que no demora la operación que lo originó, y deja
  anotado en la notificación del canal `PUSH` cómo salió: `ENVIADA` (con la fecha de envío) si
  llegó a algún dispositivo, `FALLIDA` si no. Es de mejor esfuerzo: nunca lanza una excepción ni
  revierte la operación que lo originó (mismo criterio que el correo) y registra el resultado en
  el log. Si FCM responde `UNREGISTERED` o `INVALID_ARGUMENT`, borra ese token.
- `tieneDispositivos(idUsuario)`: los avisos solo registran una notificación `PUSH` si la persona
  tiene algún dispositivo registrado.

### Avisos (`NotificacionService`)

| Caso de uso | Método | Título del aviso |
|---|---|---|
| CU-8 | `notificarReservaConfirmada` | Reserva confirmada |
| CU-13 | `notificarSuspensionTurno` | Turno suspendido |
| CU-23 | `notificarConsultaRegistrada` | Nueva información en su historia clínica |

Cada aviso guarda la notificación de la aplicación (la campanita), la del canal `PUSH` (queda
`PENDIENTE` hasta que sale, y después `ENVIADA` o `FALLIDA`) y, en CU-13 y CU-23, manda el correo. El de CU-8 se registra con el tipo `RESERVA_CONFIRMADA` (migración `V2`) y se
dispara cuando la transacción de la reserva ya se confirmó, con el identificador de la reserva,
el número y la hora estimada de atención. Los textos no incluyen contenido clínico.

### Seguridad

`POST` y `DELETE /api/notificaciones/push` requieren sesión de Firebase y asocian el token al
`UsuarioActual`. No hay registro anónimo ni envíos masivos: el push es un aviso personal del
paciente.

## Configuración

- **`mobile/google-services.json`:** archivo de la app Android `com.mediturno.mobile` del proyecto
  `mediturno-auth-db915`. Se descarga en Firebase Console > Configuración del proyecto > Tus apps,
  y va en `mobile/google-services.json` (`app.json` ya lo referencia en
  `android.googleServicesFile`). Es configuración pública del cliente, como los datos de
  `extra` en `app.json`; no contiene claves privadas. Está versionado en el repositorio. Sin este
  archivo la app no compila para Android con Firebase y no se puede obtener el token.
- **Backend:** la clave de servicio en `secrets/firebase-clave.json` (la misma de la
  autenticación). La API «Firebase Cloud Messaging API (V1)» debe estar habilitada en el
  proyecto de Google Cloud (lo está por defecto).
- Si cambia `google-services.json` o el plugin de Google Services, hay que regenerar la carpeta
  nativa: `npx expo prebuild --platform android --clean` y volver a compilar.

## Probar

1. Levantar el backend y el frontend (`podman compose up -d --build`) y compilar la app en un
   development build (`npx expo run:android`; no funciona en Expo Go). En Windows definir antes
   `ANDROID_HOME` y `JAVA_HOME` (ver `mobile/README.md`).
2. En la app, iniciar sesión con `paciente@mediturno.com` y aceptar el permiso. Comprobar que
   quedó el token: `select * from token_dispositivo;`.
3. Reservar un turno con ese paciente (desde la web o la app). Debe llegar «Reserva confirmada»,
   también con la app en segundo plano o cerrada.
4. Si no llega, mirar el log del backend: `podman logs backend | findstr /i "push fcm"` muestra
   `Push enviado ...` o `FCM rechazó el push (...)` con el motivo. El resultado de cada envío
   también queda en la base:
   `select tipo, canal, estado, fecha_envio from notificacion where canal = 'PUSH' order by id_notificacion desc;`

## Pendiente

- Recordatorio de consulta (`RECORDATORIO_CONSULTA`): usa este mismo servicio cuando se
  implemente.
