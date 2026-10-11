# Guía de trabajo en equipo — MeDiTurno

Esta guía explica cómo descargar el proyecto, trabajar con Git, probar cambios con Podman y enviar código mediante ramas y Pull Requests.

## Concepto principal

Cada integrante trabaja directamente en la carpeta que descarga con `git clone`.

No se edita el código dentro de los contenedores. Podman toma el código de la carpeta local, lo compila, crea una imagen y ejecuta un contenedor.

```text
GitHub
  ↓ git clone / git pull
Carpeta local del integrante
  ↓ edición en VS Code
Código modificado
  ↓ podman compose up --build
Imagen y contenedor local
  ↓ prueba en localhost
git commit + git push
  ↓
Pull Request hacia main
```

Cada computadora tendrá sus propios contenedores y su propia base de datos local. Git comparte el código y la configuración, no los contenedores ni los datos de PostgreSQL.

## Requisitos

Todos los integrantes necesitan:

- Git.
- Podman y Compose.
- Un editor, por ejemplo Visual Studio Code.
- Acceso como colaboradores al repositorio.

Repositorio:

```text
https://github.com/ivanpf/MeDiTurno
```

## Preparación en Windows

Instalar Git para Windows, Visual Studio Code y Podman Desktop.

Podman Desktop puede instalarse desde PowerShell con:

```powershell
winget install RedHat.Podman-Desktop
```

Completar el asistente de Podman Desktop, crear la máquina Podman y configurar Compose.

Verificar las herramientas:

```powershell
git --version
podman --version
podman compose version
podman machine list
```

Si la máquina Podman está detenida:

```powershell
podman machine start
```

## Preparación en Fedora

Verificar Git, Podman y Compose:

```bash
git --version
podman --version
podman compose version
```

Podman debe ejecutarse como usuario normal, sin `sudo`.

## Primera descarga del proyecto

Este procedimiento se realiza una sola vez por computadora.

### Windows PowerShell

```powershell
cd C:\Proyectos
git clone https://github.com/ivanpf/MeDiTurno.git
cd MeDiTurno
Copy-Item .env.example .env
```

### Fedora

```bash
cd ~/Proyectos
git clone https://github.com/ivanpf/MeDiTurno.git
cd MeDiTurno
cp .env.example .env
```

Comprobar el repositorio:

```bash
git status
```

## Primera ejecución con Podman

Desde la raíz de `MeDiTurno`:

```bash
podman compose up -d --build
```

Comprobar los servicios:

```bash
podman compose ps
```

Deben estar funcionando:

- `postgres-mediturno`: PostgreSQL.
- `backend`: Spring Boot.
- `frontend`: React servido por Nginx.

Abrir:

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- PostgreSQL: `localhost:5432`

Revisar los logs:

```bash
podman compose logs --tail=100
```

Seguir los logs en tiempo real:

```bash
podman compose logs -f
```

Salir de los logs con `Ctrl+C`. Esto no detiene los contenedores.

## Flujo para comenzar una tarea

Antes de comenzar una tarea, actualizar `main`:

```bash
git switch main
git pull
```

Crear una rama con un nombre descriptivo:

```bash
git switch -c feature/nombre-de-la-tarea
```

Ejemplos:

```bash
git switch -c feature/pantalla-login
git switch -c feature/api-pacientes
git switch -c fix/validacion-fecha
```

Comprobar la rama actual:

```bash
git branch --show-current
```

No se debe desarrollar directamente en `main`.

## Editar el código

Abrir la carpeta clonada en VS Code:

```bash
code .
```

Los archivos se editan en la computadora, por ejemplo:

```text
MeDiTurno/frontend/src/App.jsx
MeDiTurno/backend/src/main/java/com/mediturno/backend/
```

No se deben editar archivos usando `podman exec`. Los cambios hechos dentro de un contenedor pueden perderse al reconstruirlo.

## Probar cambios en los contenedores

Los contenedores actuales contienen código compilado. Después de modificar archivos es necesario reconstruir el servicio correspondiente.

### Si cambió solamente el frontend

```bash
podman compose up -d --build frontend
```

Probar en http://localhost:3000.

Si el navegador conserva una versión anterior, realizar una recarga fuerte con `Ctrl+Shift+R`.

Revisar sus logs:

```bash
podman compose logs --tail=100 frontend
```

### Si cambió solamente el backend

```bash
podman compose up -d --build backend
```

Probar en http://localhost:8080/api/health (debe responder `{"estado":"ok"}`).

Revisar sus logs:

```bash
podman compose logs -f backend
```

### Si cambiaron varios servicios

```bash
podman compose up -d --build
```

No es necesario ejecutar `podman compose down` antes de reconstruir. Compose reemplaza los contenedores necesarios y conserva el volumen de PostgreSQL.

## Ejemplo completo: pantalla de login

Actualizar `main` y crear una rama:

```bash
git switch main
git pull
git switch -c feature/pantalla-login
```

Editar en VS Code:

```text
frontend/src/features/auth/LoginPage.jsx
frontend/src/index.css
```

Ver los cambios:

```bash
git status
git diff
```

Reconstruir y probar el frontend:

```bash
podman compose up -d --build frontend
podman compose logs --tail=100 frontend
```

Abrir http://localhost:3000. Si se necesitan correcciones, editar nuevamente y repetir el comando de reconstrucción.

## Guardar cambios en Git

Revisar los archivos modificados:

```bash
git status
git diff
```

Agregar archivos específicos:

```bash
git add frontend/src/App.jsx frontend/src/App.css
```

También se pueden agregar todos los cambios:

```bash
git add .
```

Después de `git add .`, ejecutar siempre `git status` para comprobar qué se incluirá.

Crear el commit:

```bash
git commit -m "Agrega pantalla de inicio de sesión"
```

Comprobar los últimos commits:

```bash
git log --oneline -5
```

## Subir la rama a GitHub

La primera vez que se sube una rama:

```bash
git push -u origin feature/pantalla-login
```

Después de realizar más commits en esa misma rama:

```bash
git push
```

Si GitHub rechaza el acceso, comprobar que el usuario haya aceptado la invitación como colaborador y esté autenticado con su propia cuenta.

## Crear un Pull Request

Después de subir la rama:

1. Abrir el repositorio en GitHub.
2. Seleccionar `Compare & pull request`.
3. Usar `main` como rama base.
4. Usar la rama de la tarea como rama de comparación.
5. Escribir un título claro.
6. Explicar qué cambió y cómo probarlo.
7. Solicitar la revisión de otro integrante.
8. Integrar mediante `Merge pull request` cuando esté aprobado.

El código no forma parte de `main` hasta que el Pull Request se integra.

## Después de integrar un Pull Request

Actualizar la copia local:

```bash
git switch main
git pull
```

Eliminar la rama local ya integrada:

```bash
git branch -d feature/pantalla-login
```

Reconstruir el entorno con el nuevo `main`:

```bash
podman compose up -d --build
```

## Probar la rama de otro integrante

Descargar las ramas remotas:

```bash
git fetch origin
```

Crear una rama local que siga la rama remota:

```bash
git switch --track origin/feature/api-pacientes
```

Reconstruir el entorno con el código de esa rama:

```bash
podman compose up -d --build
```

Para regresar al código principal:

```bash
git switch main
git pull
podman compose up -d --build
```

## Desarrollo rápido sin reconstruir constantemente

Para trabajar con recarga automática, puede ejecutarse solo PostgreSQL en Podman:

```bash
podman compose up -d postgres
```

### Backend en Fedora

```bash
cd backend
DB_HOST=localhost ./mvnw spring-boot:run
```

### Backend en Windows PowerShell

```powershell
cd backend
$env:DB_HOST = "localhost"
.\mvnw.cmd spring-boot:run
```

### Frontend

En otra terminal:

```bash
cd frontend
npm install
npm run dev
```

Este modo requiere Java 17, Node.js 20 y npm instalados. Antes de entregar una tarea, se recomienda probar también la integración completa:

```bash
podman compose up -d --build
```

## Comandos útiles de Podman

Ver servicios:

```bash
podman compose ps
```

Ver contenedores activos:

```bash
podman ps
```

Detener el entorno sin eliminar la base de datos:

```bash
podman compose down
```

Iniciar nuevamente:

```bash
podman compose up -d
```

Eliminar el entorno y todos los datos locales de PostgreSQL:

```bash
podman compose down -v
```

El último comando es destructivo y solo debe utilizarse cuando realmente se quiera reiniciar la base de datos desde cero.

## Qué se comparte mediante Git

Se comparte:

- Código fuente.
- `compose.yml`.
- `Containerfile`.
- `package.json` y `package-lock.json`.
- `pom.xml`.
- Documentación.
- `.env.example`.
- Migraciones de la base de datos (`backend/src/main/resources/db/migration/`).

No se comparte:

- `.env`.
- Contraseñas reales.
- Contenedores e imágenes locales.
- El volumen o los datos locales de PostgreSQL.
- `node_modules`.
- `frontend/dist`.
- `backend/target`.

## Reglas del equipo

- Clonar el repositorio una sola vez por computadora.
- Editar siempre dentro de la carpeta clonada.
- Crear una rama por tarea.
- Ejecutar `git pull` en `main` antes de crear una rama.
- Revisar `git status` antes de cada commit.
- Probar los cambios antes de subirlos.
- No editar dentro de los contenedores.
- No subir `.env`, contraseñas ni archivos compilados.
- No trabajar directamente en `main`.
- No utilizar `git push --force`.
- Cambiar la base de datos solo con migraciones nuevas (scripts en `db/migration`); no editar una migración ya integrada. Ver `CLAUDE.md`.
- Integrar cambios mediante Pull Requests.

## Flujo resumido

```bash
git switch main
git pull
git switch -c feature/nombre-de-la-tarea

# Editar y probar
podman compose up -d --build

# Guardar y subir
git status
git add .
git commit -m "Describe el cambio"
git push -u origin feature/nombre-de-la-tarea
```

Después de subir la rama, crear un Pull Request hacia `main`.
