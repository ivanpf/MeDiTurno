# MediTurno

## Requisitos

- Git
- Podman (ejecutado como usuario normal)
- Para desarrollo local: Java 17, Node.js 20 y npm

En Windows, instalá **Podman Desktop** y creá/iniciá la máquina de Podman desde la aplicación. En Fedora, Podman ya funciona de forma rootless por defecto.

## Primera puesta en marcha

1. Copiá las variables de ejemplo si necesitás cambiarlas:

   ```bash
   cp .env.example .env
   ```

   En PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Levantá todos los servicios:

   ```bash
   podman compose up -d --build
   ```

3. Abrí:

   - Frontend: http://localhost:3000
   - Backend: http://localhost:8080
   - PostgreSQL: `localhost:5432`

Para seguir los logs:

```bash
podman compose logs -f
```

Para detener el entorno sin borrar datos:

```bash
podman compose down
```

## Desarrollo sin reconstruir imágenes

Levantá solamente PostgreSQL en un contenedor:

```bash
podman compose up -d postgres
```

En otra terminal, iniciá el backend local. Estas variables hacen que se conecte a PostgreSQL publicado en tu máquina, no al nombre interno del contenedor:

```bash
cd backend
DB_HOST=localhost ./mvnw spring-boot:run
```

En PowerShell:

```powershell
cd backend
$env:DB_HOST = "localhost"
.\mvnw.cmd spring-boot:run
```

En otra terminal, iniciá el frontend con recarga instantánea:

```bash
cd frontend
npm install
npm run dev
```

El frontend de Vite queda normalmente en http://localhost:5173. Las llamadas a `/api` se envían al backend local en el puerto 8080. En el frontend empaquetado, Nginx hace esa misma redirección hacia el backend del entorno.

## Credenciales de desarrollo

Los valores de `.env.example` sirven únicamente para desarrollo local. `.env` está ignorado por Git: no subas credenciales reales ni uses estas en producción.

## Troubleshooting

**El backend no conecta a la base de datos**

Revisá que PostgreSQL esté listo:

```bash
podman compose ps
podman compose logs postgres
```

Luego reiniciá el backend:

```bash
podman restart backend
```

**Puerto ocupado**

Cambiá el puerto del lado izquierdo en `compose.yml`, por ejemplo `"3001:80"`, y levantá de nuevo el servicio.

**Necesitás reiniciar la base desde cero**

Esto elimina todos los datos locales de PostgreSQL:

```bash
podman compose down -v
```
