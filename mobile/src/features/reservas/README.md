# CU-8: reservar una consulta desde mobile

El paciente elige especialidad, médico, fecha y número de atención, revisa el resumen y confirma.
La pantalla de éxito distingue el **identificador de la reserva** del **número de atención**.

## Cómo leer la implementación

1. `app/(app)/reservar.js`: JSX y estilos. Cada selector o botón llama una acción del hook.
2. `src/features/reservas/useReservaConsulta.js`: estados, acciones y consultas al backend.
3. `src/features/reservas/reservaConsulta.js`: cálculos de presentación que no necesitan React.

Ejemplo del recorrido:

```text
Select.onValueChange(codigo)
  → seleccionarEspecialidad(codigo)
  → GET ocurrencias-disponibles
  → setOcurrencias(lista)
  → React vuelve a dibujar el componente
  → aparecen los médicos y, después de elegir uno, sus fechas
```

`useState` conserva datos y dispara actualizaciones de pantalla. `useEffect` carga las especialidades
al entrar y limpia solicitudes al salir. `useRef` guarda controles que no necesitan dibujarse:
el número de la solicitud vigente y el bloqueo inmediato del envío, por ejemplo.
Los médicos, semanas y números se calculan desde los datos recibidos; no duplicamos esos datos en otros estados.

## Contrato y alternativas del diagrama

- `GET /api/especialidades`: se muestran solo las activas.
- `GET /api/turnos-semanales/ocurrencias-disponibles?codigoEspecialidad=…`: el backend valida el pase
  y determina la ventana reservable. Cada ocurrencia incluye médico, fecha, horario, consultorio,
  `cupos` y `numerosNoDisponibles` (incluye ocupados y horas que ya pasaron hoy).
- `POST /api/reservas`: solo `{ idOcurrenciaTurno, numero }`. El paciente se obtiene del token,
  y el backend vuelve a validar disponibilidad dentro de una transacción.
- `201`: confirmación con `idReserva`, `numero` y `horaEstimada` definitivos del servidor.
- `403`: se informa el motivo y se limpia la selección que ya no puede confirmarse.
- `409`: se vuelve a consultar disponibilidad y se exige elegir nuevamente; no se reenvía el POST.
- Otros errores: se informa el fallo. Si se pierde la conexión al confirmar, el resultado puede
  ser incierto; no se hace un reintento automático.

Cambiar especialidad limpia médico, fecha y número. Cambiar médico limpia fecha y número.
Las respuestas de consultas anteriores se descartan. Un bloqueo inmediato evita dos envíos por doble toque.
La hora estimada usa la misma división entera que `DisponibilidadService` del backend.

## Probar

Desde `mobile/`:

```powershell
npm run test:cu8
npx expo-doctor
npx expo export --platform android --output-dir .expo/validation-export-cu8
```

Las pruebas usan React real y sustituyen las funciones de API, sin Firebase ni acceso a la red.
`react-test-renderer` es una herramienta de desarrollo; React 19 emite un aviso de deprecación al usarla.

Para cargar datos sintéticos **solo en la base local**, con los valores por defecto de `compose.yml`:

```powershell
Get-Content .\tests\fixtures\cu8-demo.sql -Raw | podman exec -i postgres-mediturno psql -U mediturno -d mediturno_db -v ON_ERROR_STOP=1
```

Entrar con el paciente de prueba documentado en el README de mobile. Elegir «Clínica general
(demo CU-8)»: hay fechas semanales y el número 2 de la primera está ocupado. «Especialidad con pase
(demo CU-8)» permite comprobar el rechazo por falta de pase. El script no crea cuentas Firebase,
no es una migración y no modifica registros ajenos a los códigos `CU8_DEMO` y `CU8_PASE`.

Las reservas se crean solo al presionar **Confirmar reserva**. «Cambiar fecha» cambia la selección
local, y no llama a la cancelación de reservas (CU-9).


## CU-9: cancelación de una reserva desde mobile

- `app/(app)/reservas.js`: listado, selección, datos de la consulta y confirmación explícita.
  Reutiliza colores y componentes del CU-8 (`reservar.js`), identificado por separado en comentarios.
- `shared/src/hooks/useReservas.js`: separa los bloques CU-8 (crear) y CU-9 (cancelar).
  Emplea `idReserva`, `fecha`, `horaEstimada`, `especialidad`, `nombreMedico` y `consultorio` del DTO real.
- `src/features/reservas/cancelacionReserva.js`: estado vigente y comprobación orientativa de las 48 horas
  desde la hora estimada de consulta (o el inicio del turno si no hay hora estimada), en America/Montevideo.
  La autorización y la validación definitiva corresponden al backend.
- `GET /api/reservas/mias`: listado de apoyo para seleccionar una reserva. Los filtros de CU-10 quedan fuera de este cambio.
- `POST /api/reservas/{idReserva}/cancelacion`: solo al confirmar. Se conserva el historial y se muestra
  el estado cancelado únicamente después de una respuesta exitosa. No se envía el id del paciente.

Al implementar esta interfaz, ambos métodos del servicio backend siguen pendientes (501).
Se muestra un aviso de función pendiente, sin simular datos ni una cancelación exitosa.
Tras un rechazo o una pérdida de conexión, se conserva el estado y se exige actualizar la lista antes de
otro intento. No se repite el POST automáticamente. Un bloqueo inmediato evita duplicados por doble toque.
