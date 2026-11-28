# Pruebas de la API con Postman

Coleccion de solo lectura para verificar endpoints de MeDiTurno. No reserva, cancela, emite ordenes ni modifica datos.

## Importar

1. Abrir Postman en Local Mode.
2. Abrir la carpeta del repositorio como workspace de Postman.
3. Seleccionar el environment `MeDiTurno - Local` en `postman/environments/MeDiTurno - Local.environment.yaml`.

## Configurar

- `baseUrl` ya viene como `http://localhost:8080/api`.
- Completar `apiKey` con la clave publica de Firebase (`VITE_FIREBASE_API_KEY` de `.env` o `.env.example`).
- Completar `correo` y `clave` con una cuenta activa del proyecto (proveedor `PASSWORD`).
- Dejar `token` vacio. Se completa automaticamente al correr "Obtener token en Firebase".
- `idReserva` y `numeroOrden` son opcionales y se usan solo si se agregan requests de detalle.
- No commitear `apiKey`, `correo` ni `clave` con valores reales.

## Ejecutar

1. Levantar backend y base: `podman compose up -d --build`.
2. Correr el folder `Autenticacion` en orden.
3. Correr el folder `Solo lectura`.
4. Correr `Caso de error`.

No llamar a `POST /auth/logout` desde esta coleccion: revoca las sesiones guardadas de otros tests. Los endpoints de CU-3 y CU-4 no se incluyen como flujo exitoso porque las pantallas usan Firebase directo. La coleccion esta pensada para un usuario paciente; los endpoints de consultorios y medicos requieren rol administrador o funcionario.

## Guardar cambios

En Local Mode los cambios se guardan directamente en los archivos YAML de `postman/`. Mantener `clave` y `token` vacios antes de commitear.