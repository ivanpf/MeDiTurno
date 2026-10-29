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

- `app/(app)/reservas.js`: listado de hoy y fechas próximas, selección, datos de la consulta,
  vista previa y confirmación explícita. Mantiene el estilo del CU-8 (`reservar.js`), separado en comentarios.
- `shared/src/hooks/useReservas.js`: maneja los estados, los errores, las llamadas y la confirmación.
  Conserva separados los bloques CU-8 (crear) y CU-9 (cancelar).
- `src/features/reservas/cancelacionReserva.js`: muestra `puedeCancelar` y `motivoNoCancelable`
  enviados por el backend. Mobile no calcula el plazo con el reloj del teléfono.

Contrato de backend:

1. `GET /api/reservas/mias/proximas`: devuelve un array de `ReservaCancelacionDTO`.
   Cada elemento contiene `reserva` (con los campos de `ReservaResponseDTO`), `puedeCancelar`,
   `motivoNoCancelable` y `fechaCancelacion`. Incluye cancelaciones de hoy y fechas próximas.
2. `GET /api/reservas/{idReserva}/cancelacion/vista-previa`: al solicitar cancelar, vuelve a verificar
   propiedad, estado y plazo. Solo una vista previa válida habilita la confirmación, que muestra los datos recién recibidos.
3. `POST /api/reservas/{idReserva}/cancelacion`: solo al confirmar. El paciente se obtiene del token.
   El backend vuelve a validar y responde 204 al cancelar. Después se consulta nuevamente el listado
   para mostrar el estado y la fecha de cancelación definitivos, sin inventar una fecha local.

La regla del servidor exige reserva vigente y al menos 48 horas hasta el **inicio del turno**, inclusive;
la hora estimada del número se muestra como información y no define el plazo.
Un 404 o 409 en la vista previa actualiza la lista y muestra el motivo. Tras cualquier envío de cancelación,
incluida una respuesta incierta por conexión, se refresca el listado. Solo se repiten consultas GET, nunca el POST.
Si la actualización falla, se exige recargar antes de otro intento. Un bloqueo inmediato evita envíos duplicados,
y cambiar de reserva, conservarla o actualizar la lista descarta la vista previa anterior.

El historial completo con búsqueda del CU-10 usa su propio endpoint: `GET /api/reservas/mias`.
No se modifican backend, base de datos ni frontend web. No se agregan ni ejecutan tests en esta etapa.

## CU-10: historial de reservas desde mobile

En **Menú → Mis reservas**, la sección **Próximas** conserva el CU-9. La sección **Historial**
consulta las reservas futuras y anteriores, incluidas las canceladas. La lectura no modifica reservas.

- `app/(app)/reservas.js`: ambas secciones, formularios, listado y detalle del resultado elegido.
- `src/features/reservas/useHistorialReservas.js`: peticiones, filtros aplicados, errores y selección.
- `src/features/reservas/historialReservas.js`: parámetros, validación del período y textos.
- `shared/src/api/endpoints.js`: reutiliza `listarMisReservas(criterios)`.
- `shared/src/types/api.js`: documenta los nombres reales de `CriteriosBusquedaDTO`.

Contrato actual:

```text
GET /api/reservas/mias
  ?fechaDesde=YYYY-MM-DD
  &fechaHasta=YYYY-MM-DD
  &codigoEspecialidad=CODIGO
  &idMedico=ID
  &estado=VIGENTE
  &ordenarPor=fecha
  &descendente=true
```

Todos los filtros son opcionales. El paciente sale del token; no se envía un identificador de paciente.
Las fechas se escriben como `DD/MM/AAAA` y se envían como `YYYY-MM-DD`. **Buscar** aplica el formulario;
**Limpiar filtros** consulta de nuevo sin restricciones, con fecha descendente. Un aviso distingue los
cambios todavía sin aplicar de los resultados de la última consulta. Los resultados mantienen el orden
y el contenido enviados por backend. No hay filtros locales ni datos simulados.

La especialidad usa el catálogo existente, incluidas las inactivas. Si el catálogo no está disponible,
el código puede escribirse y el fallo se muestra con un botón para reintentar. Una respuesta vacía del
historial se distingue de un error, incluidos `400`, `403` y `501`; se conservan mensaje y detalles del servidor.
Las respuestas viejas se descartan al salir o al comenzar otra búsqueda.

El listado y el detalle muestran especialidad, médico, fecha, horario, consultorio, número y estado de
reserva; también número y estado de la orden cuando existe. El detalle usa el `ReservaResponseDTO` del
listado y se cierra al tocar de nuevo la reserva o con Atrás en Android.

### Pendientes del contrato de backend

- `ReservaResponseDTO` devuelve `nombreMedico` pero no `idMedico`. `GET /api/medicos` solo admite
  administrador y funcionario. Por indicación del usuario, el filtro se mantiene **habilitado** como
  campo **Médico (identificador)**, enviando `idMedico` sin inferirlo del nombre ni ocultar errores.
  Al completar el contrato se podrá ofrecer un selector por nombre.
- El backend de `origin/main` en `0bd25a5` tenía un import faltante de `NoImplementadoException`
  en `ReservaService`. Con autorización del usuario, `CU10-Mobile` agrega únicamente esa línea
  para poder compilar y levantar el backend actual. Los filtros usan el endpoint existente.

No se agregan ni ejecutan tests en esta etapa. El único cambio de código en backend es ese import.
