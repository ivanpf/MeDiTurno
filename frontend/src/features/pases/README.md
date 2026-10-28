# CU-24 — Emitir pase

## Flujo

1. Iniciar sesión como médico y abrir **Mis pacientes**.
2. Elegir un paciente con orden emitida y **Registrar consulta** (CU-23).
3. Al finalizar, elegir **Emitir pase a otra especialidad**. Se usa el `idConsulta` devuelto por el registro, no el número de orden.
4. Elegir una especialidad activa, revisar la confirmación y emitir el pase.
5. La pantalla muestra el número, el destino, el estado y las fechas que devuelve el backend. Emitir el pase no genera una reserva.

La opción del menú sin una consulta seleccionada explica cómo iniciar el flujo. Los listados de historia clínica e historial de atenciones todavía responden 501 en esta rama, por eso no se usan para elegir consultas anteriores.

## Contrato

- `GET /api/especialidades`: se muestran solo las de estado `ACTIVO`.
- `POST /api/consultas/{idConsulta}/pases`, cuerpo `{ "especialidadDestino": "CODIGO" }`.
- La vigencia es fija de 60 días según `VigenciaPase`; se muestra `fechaVigenciaCalculada` sin calcularla en el navegador. El mock antiguo permite elegir fecha, pero ese campo no forma parte del contrato del backend actual.
- La API valida el médico de la consulta y vuelve a comprobar que el destino esté activo.

## Verificación

- `npm run lint` y `npm run build` desde `frontend/`.
- `./mvnw test` (Windows: `.\mvnw.cmd test`) desde `backend/`. `EmisionPaseTest` verifica origen/destino distintos, vigencia y rechazo de consulta ajena o especialidad inactiva.
- Se verificó en navegador con API simulada: CU-23 → CU-24, cancelar confirmación sin enviar, emisión correcta con el identificador de consulta, error 403, reintento de carga y lista vacía. Sin desbordamiento horizontal a 320, 375, 768 y 1280 px.
- Para validar de punta a punta con Firebase, repetir el flujo con una cuenta de médico y una orden real de su turno. Las pruebas con API simulada no validan esa sesión real.
