# Autenticación (POC Firebase Auth + Spring Boot)

POC para probar el flujo de autenticación con Firebase Auth: login, roles (custom claims),
alta/baja de usuarios, recuperación de contraseña, etc. Sirve de base para la parte de
autenticación del proyecto MeDiTurno.

## Requisitos previos

- **Java 21+** (se probó con Java 26)
- **Maven** — no hace falta instalarlo a mano si tu IDE lo trae, pero si querés correrlo por
  consola y no lo tenés, bajalo de https://maven.apache.org/download.cgi y agregalo al PATH.
- Acceso al proyecto de Firebase del equipo (pedirle a alguien del grupo que te agregue como
  colaborador en la consola de Firebase, o compartan las credenciales de forma segura).

## 1. El archivo de credenciales (`serviceAccountKey.json`)

Este archivo le da a la app acceso total (admin) al proyecto de Firebase.

> ⚠️ **Por ahora está commiteado en este repo** para simplificar el setup.
> **Cuando se empiece la implementación definitiva (en una rama nueva) esto no se debe
> repetir**: el `serviceAccountKey.json` no va al control de versiones. Cada
> integrante debería generar su propia clave desde la consola de Firebase en ese momento.

Si por algún motivo el archivo no está o necesitás una copia nueva:

1. Entrá a la [consola de Firebase](https://console.firebase.google.com) → seleccioná el
   proyecto del equipo.
2. Ícono de engranaje (arriba a la izquierda) → **Configuración del proyecto**.
3. Pestaña **Cuentas de servicio** → **Generar nueva clave privada**.
4. Se descarga un `.json`. Renombralo a `serviceAccountKey.json` y colocalo en la raíz de esta
   carpeta (`autenticacion/serviceAccountKey.json`, al lado de `pom.xml`).

Si no tenés permisos para generar la clave, pedile a un compañero que ya la tenga que te la
pase por un canal seguro (no por el repo ni por chat público) — es la misma para todo el equipo.

## 2. Configuración

El archivo `src/main/resources/application.properties` ya trae la configuración por defecto:

```properties
server.port=3000
firebase.credenciales=./serviceAccountKey.json
admin.correo-protegido=springadmin@example.com
```

- `admin.correo-protegido`: ese correo nunca se borra con el endpoint de borrado masivo
  (`DELETE /usuarios`), para no quedarse sin ningún admin por accidente. Cambialo si tu equipo
  usa otro correo de admin de referencia.

No hace falta tocar nada más para levantarlo local.

## 3. Correrlo

Desde esta carpeta (`autenticacion`):

```bash
mvn spring-boot:run
```

Va a tardar unos segundos. Sabés que está listo cuando ves en la consola:
```
Started PocAuthApplication in X seconds
```

La API queda escuchando en `http://localhost:3000`.

Para pararlo: `Ctrl+C` en esa misma terminal.

## 4. Probarlo

Abrí en el navegador:
```
http://localhost:3000/login-test.html
```

Ahí podés loguearte (con Google o con correo/contraseña) y probar todos los endpoints desde
la interfaz, sin necesidad de Postman ni curl.

### Crear el primer usuario admin

Al principio no hay ningún usuario. Para crear el primero (con rol admin), podés usar la
sección "Crear usuario" de la página de prueba, o correr:

```bash
curl -X POST http://localhost:3000/usuarios \
  -H "Content-Type: application/json" \
  -d '{"email":"tu-correo@example.com","password":"unaClaveSegura123","rol":"admin"}'
```

Después iniciá sesión con ese usuario en `login-test.html` para probar las rutas que requieren
rol admin (`/solo-admin`, `/usuarios/{uid}`, etc.).

## Endpoints disponibles

| Método y ruta | Auth | Descripción |
|---|---|---|
| `GET /` | ninguna | chequeo de salud |
| `GET /me` | token válido | datos del usuario autenticado |
| `POST /usuarios` | ninguna (solo POC) | crea usuario + rol opcional |
| `POST /usuarios/{uid}/rol` | rol admin | asigna/cambia rol |
| `GET /usuarios/{uid}` | rol admin | consulta datos y claims de un usuario por uid |
| `GET /usuarios/por-correo?correo=` | rol admin | busca un usuario por correo |
| `POST /usuarios/{uid}/deshabilitar` | rol admin | deshabilita el login de un usuario |
| `POST /usuarios/{uid}/habilitar` | rol admin | revierte la deshabilitación |
| `DELETE /usuarios/{uid}` | rol admin | elimina un usuario puntual |
| `DELETE /usuarios?confirmar=si` | rol admin | elimina **todos** los usuarios (excepto el protegido) |
| `POST /usuarios/{uid}/revocar-tokens` | rol admin | fuerza el cierre de sesión de un usuario |
| `POST /usuarios/carga-masiva` | rol admin | alta masiva vía CSV (`email,password,rol`) |
| `GET /solo-admin` | rol admin | ruta de ejemplo protegida por rol |

## Notas

- Los roles se guardan como *custom claims* en el token de Firebase. Si le cambiás el rol a un
  usuario que ya tiene una sesión abierta, el cambio no se refleja hasta que el cliente pida un
  token nuevo (`getIdToken(true)`) o vuelva a loguearse.
- La consola de Firebase **no muestra los custom claims** — para ver el rol de un usuario hay
  que usar `GET /usuarios/{uid}` o `GET /usuarios/por-correo`.
- Hay dos CSV de prueba en esta carpeta (`usuarios_test_bulk50.csv` y
  `usuarios_test_bulk500.csv`) para probar la carga masiva con distintos volúmenes.
